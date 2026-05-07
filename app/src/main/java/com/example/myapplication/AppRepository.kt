package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.UserManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * AppRepository: Manages the retrieval and pinning of installed applications.
 */
class AppRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val pinPrefs = appContext.getSharedPreferences("pinned_prefs", Context.MODE_PRIVATE)
    private val dockPrefs = appContext.getSharedPreferences("dock_prefs", Context.MODE_PRIVATE)
    private val hiddenPrefs = appContext.getSharedPreferences("hidden_prefs", Context.MODE_PRIVATE)
    private val recentsPrefs = appContext.getSharedPreferences("recents_prefs", Context.MODE_PRIVATE)
    private val userManager = appContext.getSystemService(Context.USER_SERVICE) as UserManager
    
    private var cachedSystemApps: List<AppModel>? = null

    // Flow to notify observers (like DrawerFragment) when apps change
    private val _appsChangedFlow = MutableSharedFlow<Unit>(replay = 0)
    val appsChangedFlow = _appsChangedFlow.asSharedFlow()

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context).also { INSTANCE = it }
            }
        }
    }

    init {
        registerAppChangeReceiver()
        pruneUninstalledApps()
        seedDefaultAppsIfEmpty()
    }

    private fun seedDefaultAppsIfEmpty() {
        if (getDockIds().isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val allApps = getAllSystemApps(false)
                val defaultPackages = listOf(
                    "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
                    "com.android.messaging", "com.google.android.apps.messaging",
                    "com.android.chrome", "com.google.android.browser", "org.mozilla.firefox",
                    "com.android.camera", "com.google.android.GoogleCamera", "com.sec.android.app.camera"
                )

                val toSeed = mutableListOf<String>()
                
                // Try to find by package name first for common ones
                for (pkg in defaultPackages) {
                    allApps.find { it.packageName == pkg }?.let {
                        val id = getAppUniqueId(it)
                        if (!toSeed.contains(id)) toSeed.add(id)
                    }
                    if (toSeed.size >= 4) break
                }

                // If still empty or few, try to find by intent
                if (toSeed.size < 4) {
                    val categories = listOf(
                        Intent.CATEGORY_APP_CONTACTS,
                        Intent.CATEGORY_APP_MESSAGING,
                        Intent.CATEGORY_APP_BROWSER,
                        Intent.CATEGORY_APP_GALLERY
                    )

                    for (category in categories) {
                        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
                        val resolveInfo = appContext.packageManager.resolveActivity(intent, 0)
                        resolveInfo?.activityInfo?.packageName?.let { pkg ->
                            allApps.find { it.packageName == pkg }?.let {
                                val id = getAppUniqueId(it)
                                if (!toSeed.contains(id)) toSeed.add(id)
                            }
                        }
                        if (toSeed.size >= 4) break
                    }
                }

                if (toSeed.isNotEmpty()) {
                    updateDockOrderByIds(toSeed)
                }
            }
        }
    }

    private fun registerAppChangeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Clear cache and notify observers on background scope
                cachedSystemApps = null
                CoroutineScope(Dispatchers.IO).launch {
                    pruneUninstalledApps()
                    _appsChangedFlow.emit(Unit)
                }
            }
        }
        appContext.registerReceiver(receiver, filter)
    }

    private fun pruneUninstalledApps() {
        CoroutineScope(Dispatchers.IO).launch {
            val allApps = getAllSystemApps(true)
            val allValidIds = allApps.map { getAppUniqueId(it) }.toSet()

            // Prune Pinned
            val currentPinned = getPinnedIds()
            val validPinned = currentPinned.filter { allValidIds.contains(it) }
            if (currentPinned.size != validPinned.size) {
                updatePinnedOrderByIds(validPinned)
            }

            // Prune Dock
            val currentDock = getDockIds()
            val validDock = currentDock.filter { allValidIds.contains(it) }
            if (currentDock.size != validDock.size) {
                updateDockOrderByIds(validDock)
            }

            // Prune Hidden
            val currentHidden = getHiddenIds()
            val validHidden = currentHidden.filter { allValidIds.contains(it) }.toSet()
            if (currentHidden.size != validHidden.size) {
                hiddenPrefs.edit().putStringSet("hidden_app_ids", validHidden).apply()
            }
            
            // Prune Recents
            val recentsString = recentsPrefs.getString("recent_apps_list", "") ?: ""
            if (recentsString.isNotEmpty()) {
                val currentRecents = recentsString.split(",")
                val validRecents = currentRecents.filter { allValidIds.contains(it) }
                if (currentRecents.size != validRecents.size) {
                    recentsPrefs.edit().putString("recent_apps_list", validRecents.joinToString(",")).apply()
                }
            }
        }
    }

    fun getAppUniqueId(app: AppModel): String {
        val serialNumber = userManager.getSerialNumberForUser(app.userHandle)
        return "${app.packageName}:$serialNumber"
    }

    suspend fun getDrawerApps(forceRefresh: Boolean = false): List<AppModel> = withContext(Dispatchers.IO) {
        val allApps = getAllSystemApps(forceRefresh)
        val hiddenIds = getHiddenIds()
        allApps.filter { app -> !hiddenIds.contains(getAppUniqueId(app)) }
    }

    suspend fun getHiddenApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val allApps = getAllSystemApps(false)
        val hiddenIds = getHiddenIds()
        allApps.filter { app -> hiddenIds.contains(getAppUniqueId(app)) }
    }

    suspend fun getPinnedApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val allApps = getAllSystemApps(false)
        val pinnedIds = getPinnedIds()
        val hiddenIds = getHiddenIds()
        
        pinnedIds.mapNotNull { savedId -> 
            if (hiddenIds.contains(savedId)) null
            else allApps.find { getAppUniqueId(it) == savedId } 
        }
    }

    suspend fun getDockApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val allApps = getAllSystemApps(false)
        val dockIds = getDockIds()
        val hiddenIds = getHiddenIds()
        
        dockIds.mapNotNull { savedId ->
            if (hiddenIds.contains(savedId)) null
            else allApps.find { getAppUniqueId(it) == savedId }
        }
    }

    suspend fun getRecentApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val allApps = getAllSystemApps(false)
        val recentsString = recentsPrefs.getString("recent_apps_list", "") ?: ""
        if (recentsString.isEmpty()) return@withContext emptyList()
        
        val hiddenIds = getHiddenIds()
        val recentIds = recentsString.split(",")
        recentIds.mapNotNull { savedId ->
            if (hiddenIds.contains(savedId)) null
            else allApps.find { getAppUniqueId(it) == savedId }
        }
    }

    suspend fun getAllApps(forceRefresh: Boolean = false): List<AppModel> = withContext(Dispatchers.IO) {
        getAllSystemApps(forceRefresh)
    }

    fun getPinnedIds(): List<String> {
        return (0 until 6).mapNotNull { i ->
            pinPrefs.getString("favorite_id_$i", null)
        }
    }

    fun getDockIds(): List<String> {
        return (0 until 8).mapNotNull { i ->
            dockPrefs.getString("dock_id_$i", null)
        }
    }

    fun getHiddenIds(): Set<String> {
        return hiddenPrefs.getStringSet("hidden_app_ids", emptySet()) ?: emptySet()
    }

    private suspend fun getAllSystemApps(forceRefresh: Boolean): List<AppModel> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedSystemApps != null) return@withContext cachedSystemApps!!

        val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val profiles = launcherApps.profiles
        val updateTimestamp = if (forceRefresh) System.currentTimeMillis() else 0L

        val allApps = coroutineScope {
            profiles.flatMap { profile ->
                launcherApps.getActivityList(null, profile).map { ri ->
                    async {
                        AppModel(
                            label = ri.label.toString(),
                            packageName = ri.applicationInfo.packageName,
                            icon = ri.getIcon(0),
                            userHandle = profile,
                            lastUpdated = updateTimestamp
                        )
                    }
                }
            }.awaitAll()
        }

        val sortedList = allApps.sortedBy { it.label.lowercase() }
        cachedSystemApps = sortedList
        sortedList
    }

    fun updatePinnedOrderByIds(newList: List<String>) {
        pinPrefs.edit().apply {
            for (i in 0 until 6) remove("favorite_id_$i")
            newList.take(6).forEachIndexed { index, id ->
                putString("favorite_id_$index", id)
            }
            apply()
        }
        notifyAppsChanged()
    }

    fun updateDockOrderByIds(newList: List<String>) {
        dockPrefs.edit().apply {
            for (i in 0 until 8) remove("dock_id_$i")
            newList.take(8).forEachIndexed { index, id ->
                putString("dock_id_$index", id)
            }
            apply()
        }
        notifyAppsChanged()
    }

    private fun notifyAppsChanged() {
        CoroutineScope(Dispatchers.IO).launch {
            _appsChangedFlow.emit(Unit)
        }
    }

    fun isPinned(app: AppModel): Boolean {
        val appId = getAppUniqueId(app)
        return getPinnedIds().contains(appId)
    }

    fun isDocked(app: AppModel): Boolean {
        val appId = getAppUniqueId(app)
        return getDockIds().contains(appId)
    }

    fun togglePin(app: AppModel): ToggleResult {
        val appId = getAppUniqueId(app)
        val currentIds = getPinnedIds().toMutableList()

        return if (currentIds.contains(appId)) {
            currentIds.remove(appId)
            updatePinnedOrderByIds(currentIds)
            ToggleResult.REMOVED
        } else if (currentIds.size < 6) {
            currentIds.add(appId)
            updatePinnedOrderByIds(currentIds)
            ToggleResult.ADDED
        } else {
            ToggleResult.NO_SPACE
        }
    }

    fun toggleDock(app: AppModel): ToggleResult {
        val appId = getAppUniqueId(app)
        val currentIds = getDockIds().toMutableList()

        return if (currentIds.contains(appId)) {
            currentIds.remove(appId)
            updateDockOrderByIds(currentIds)
            ToggleResult.REMOVED
        } else if (currentIds.size < 8) {
            currentIds.add(appId)
            updateDockOrderByIds(currentIds)
            ToggleResult.ADDED
        } else {
            ToggleResult.NO_SPACE
        }
    }

    enum class ToggleResult {
        ADDED, REMOVED, NO_SPACE
    }

    fun isHidden(app: AppModel): Boolean {
        return getHiddenIds().contains(getAppUniqueId(app))
    }

    fun setHidden(app: AppModel, hide: Boolean) {
        val appId = getAppUniqueId(app)
        val currentHidden = getHiddenIds().toMutableSet()
        
        if (hide) {
            currentHidden.add(appId)
            
            // If hiding, also remove from Pin and Dock lists to free up slots
            val currentPinned = getPinnedIds().toMutableList()
            if (currentPinned.remove(appId)) {
                updatePinnedOrderByIds(currentPinned)
            }

            val currentDocked = getDockIds().toMutableList()
            if (currentDocked.remove(appId)) {
                updateDockOrderByIds(currentDocked)
            }
        } else {
            currentHidden.remove(appId)
        }
        
        hiddenPrefs.edit().putStringSet("hidden_app_ids", currentHidden).apply()
        notifyAppsChanged()
    }
}
