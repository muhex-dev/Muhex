package com.example.myapplication

import android.app.Dialog
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.clock.ClockSettingsSheet
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

/**
 * HomeFragment: The primary screen of the launcher.
 * Manages the clock, dock, pinned apps, gestures, and customization overlays.
 */
class HomeFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    // region Properties & State
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var topPagesManager: TopPagesManager

    private val viewModel: HomeViewModel by viewModels()
    private val repository by lazy { AppRepository.getInstance(requireContext()) }
    private val prefs by lazy { requireContext().getSharedPreferences("launcher_settings", Context.MODE_PRIVATE) }

    private lateinit var gestureHandler: GestureHandler
    private lateinit var gestureDetector: GestureDetector
    private var currentMenuDialog: Dialog? = null

    private var isFlashlightOn = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = (level * 100 / scale.toFloat()).toInt()
            UIHelper.updateBatteryUI(topPagesManager.getAllPages(), batteryPct)
        }
    }
    // endregion

    // region Lifecycle
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        prefs.registerOnSharedPreferenceChangeListener(this)
        requireContext().registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onResume() {
        super.onResume()
        refreshData()
        checkDefaultLauncherStatus()
        topPagesManager.applyClockSettings()
        applyPinnedAppsVisibility()
        applyDockVisibility()
        topPagesManager.applyTopSectionVisibility()
        refreshQuotesUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        requireContext().unregisterReceiver(batteryReceiver)
        _binding = null
    }
    // endregion

    // region Initialization
    private fun setupUI() {
        topPagesManager = TopPagesManager(requireContext(), binding, prefs)
        topPagesManager.setupTopPages(layoutInflater)
        
        gestureHandler = GestureHandler(requireContext(), requireActivity(), repository, viewLifecycleOwner)
        setupGestures(binding.root)
        setupComposeUIs()
        binding.btnSetDefaultLauncher.setOnClickListener { promptSetDefaultLauncher() }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.appsChangedFlow.collect {
                refreshData()
            }
        }
    }

    private fun refreshData() {
        loadAllApps()
        loadPinnedApps()
        loadDockApps()
    }
    // endregion

    // region Compose UI Setup
    private fun setupComposeUIs() {
        binding.pinnedAppsCompose.setContent {
            PinnedApps(
                apps = viewModel.pinnedAppsList,
                isEditMode = false,
                onAppClick = { app -> LaunchApp.launch(requireActivity(), app) },
                onAppLongClick = { app, anchorView ->
                    currentMenuDialog = AppMenuHelper.showMenu(requireActivity() as AppCompatActivity, anchorView, app) {
                        viewModel.loadPinnedApps()
                    }
                },
                onEmptyLongClick = {
                    viewModel.isHomePopupVisible = true
                    binding.homePopupCompose.visibility = View.VISIBLE
                }
            )
        }

        binding.dockCompose.setContent {
            DockBar(
                repository = repository,
                apps = viewModel.dockAppsList,
                onAppClick = { app -> LaunchApp.launch(requireContext(), app) },
                onAppLongClick = { app, anchorView ->
                    currentMenuDialog = AppMenuHelper.showMenu(requireActivity() as AppCompatActivity, anchorView, app) {
                        viewModel.loadDockApps()
                    }
                },
                onOrderChanged = { newList: List<AppModel> ->
                    viewModel.dockAppsList = newList
                }
            )
        }

        binding.homePopupCompose.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(viewModel.isQuickMenuVisible) {
                    if (viewModel.isQuickMenuVisible) {
                        viewModel.updateRecentApps()
                    }
                }

                HomePopup(
                    isVisible = viewModel.isHomePopupVisible,
                    onAction = { handleHomePopupAction(it) }
                )

                UnifiedSettingsSheet(
                    repository = repository,
                    prefs = prefs,
                    isVisible = viewModel.isUnifiedSettingsVisible,
                    initialTab = viewModel.unifiedSettingsInitialTab,
                    onOpenFontPicker = { key, title ->
                        viewModel.fontPickerTargetKey = key
                        viewModel.fontPickerTitle = title
                        viewModel.isUnifiedSettingsVisible = false
                        viewModel.isFontSettingsVisible = true
                    },
                    onOpenClockSettings = {
                        viewModel.isUnifiedSettingsVisible = false
                        viewModel.isClockSettingsVisible = true
                    },
                    onDismiss = {
                        viewModel.isUnifiedSettingsVisible = false
                        binding.homePopupCompose.visibility = View.GONE
                        viewModel.loadPinnedApps()
                    }
                )

                ClockSettingsSheet(
                    prefs = prefs,
                    isVisible = viewModel.isClockSettingsVisible,
                    onOpenFontPicker = { key, title ->
                        viewModel.fontPickerTargetKey = key
                        viewModel.fontPickerTitle = title
                        viewModel.isClockSettingsVisible = false
                        viewModel.isFontSettingsVisible = true
                    },
                    onDismiss = {
                        viewModel.isClockSettingsVisible = false
                        binding.homePopupCompose.visibility = View.GONE
                    }
                )

                QuickMenu(
                    isVisible = viewModel.isQuickMenuVisible,
                    apps = viewModel.allAppsList,
                    recentApps = viewModel.recentAppsList,
                    onAction = { handleQuickMenuAction(it) },
                    onAppClick = { app ->
                        LaunchApp.launch(requireContext(), app)
                        dismissComposeOverlays()
                    }
                )

                FontSettings(
                    prefs = prefs,
                    targetKey = viewModel.fontPickerTargetKey,
                    title = viewModel.fontPickerTitle,
                    isVisible = viewModel.isFontSettingsVisible,
                    onDismiss = {
                        viewModel.isFontSettingsVisible = false
                        if (viewModel.fontPickerTargetKey.contains("clock") || viewModel.fontPickerTargetKey.contains("date")) {
                            viewModel.isClockSettingsVisible = true
                        } else {
                            binding.homePopupCompose.visibility = View.GONE
                        }
                    }
                )

                MuhexSettingsSheet(
                    prefs = prefs,
                    isVisible = viewModel.isMuhexSettingsVisible,
                    onDismiss = {
                        viewModel.isMuhexSettingsVisible = false
                        binding.homePopupCompose.visibility = View.GONE
                    }
                )

                if (viewModel.isPermissionCheckerVisible) {
                    PermissionCheckerPopup(onAllGranted = { viewModel.isPermissionCheckerVisible = false })
                }
            }
        }
    }

    private fun handleHomePopupAction(action: HomePopupAction) {
        when (action) {
            HomePopupAction.Wallpaper -> {
                dismissComposeOverlays()
                openWallpaperPicker()
            }
            HomePopupAction.Muhex -> {
                viewModel.isHomePopupVisible = false
                viewModel.isMuhexSettingsVisible = true
                binding.homePopupCompose.visibility = View.VISIBLE
            }
            HomePopupAction.Dock -> {
                viewModel.isHomePopupVisible = false
                viewModel.unifiedSettingsInitialTab = 5 // Dock
                viewModel.isUnifiedSettingsVisible = true
            }
            HomePopupAction.PinnedApps -> {
                viewModel.isHomePopupVisible = false
                viewModel.unifiedSettingsInitialTab = 2 // Pinned Apps
                viewModel.isUnifiedSettingsVisible = true
            }
            HomePopupAction.Gestures -> {
                viewModel.isHomePopupVisible = false
                viewModel.unifiedSettingsInitialTab = 4 // Gestures
                viewModel.isUnifiedSettingsVisible = true
            }
            HomePopupAction.Settings -> {
                viewModel.isHomePopupVisible = false
                viewModel.unifiedSettingsInitialTab = 0 // Default settings tab
                viewModel.isUnifiedSettingsVisible = true
                binding.homePopupCompose.visibility = View.VISIBLE
            }
            HomePopupAction.Dismiss -> dismissComposeOverlays()
        }
    }

    private fun handleQuickMenuAction(action: QuickMenuAction) {
        when (action) {
            is QuickMenuAction.Dismiss -> dismissComposeOverlays()
            is QuickMenuAction.Flashlight -> toggleFlashlight()
            is QuickMenuAction.Settings -> {
                dismissComposeOverlays()
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            is QuickMenuAction.Apps -> {
                dismissComposeOverlays()
                (requireActivity() as? MainActivity)?.openDrawer("quick_menu")
            }
            is QuickMenuAction.Wifi -> {
                dismissComposeOverlays()
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            is QuickMenuAction.Bluetooth -> {
                dismissComposeOverlays()
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            is QuickMenuAction.Calculator -> {
                dismissComposeOverlays()
                launchCommonApp("calculator")
            }
            is QuickMenuAction.Calendar -> {
                dismissComposeOverlays()
                launchCommonApp("calendar")
            }
            is QuickMenuAction.Search -> {
                dismissComposeOverlays()
                (requireActivity() as? MainActivity)?.openDrawer("search", focusSearch = true)
            }
            is QuickMenuAction.Files -> {
                dismissComposeOverlays()
                launchCommonApp("files")
            }
            is QuickMenuAction.Browser -> {
                dismissComposeOverlays()
                launchCommonApp("browser")
            }
            else -> {}
        }
    }

    private fun dismissComposeOverlays() {
        viewModel.dismissComposeOverlays()
        binding.homePopupCompose.visibility = View.GONE
    }
    // endregion

    private fun loadAllApps() {
        viewModel.refreshData()
    }
    // endregion

    // region Pinned Apps Management
    private fun applyPinnedAppsVisibility() {
        if (_binding == null) return
        binding.pinnedAppsCompose.visibility = if (prefs.getBoolean("show_now_apps", true)) View.VISIBLE else View.GONE
    }

    private fun loadPinnedApps() {
        viewModel.loadPinnedApps()
    }

    private fun applyDockVisibility() {
        if (_binding == null) return
        binding.dockCompose.visibility = if (prefs.getBoolean("show_dock_bar", true)) View.VISIBLE else View.GONE
    }

    private fun loadDockApps() {
        viewModel.loadDockApps()
    }
    // endregion

    // region Top Pages Management
    // Delegated to TopPagesManager
    // endregion
    // endregion

    // region Gesture Handling
    private fun setupGestures(root: View) {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                handleGestureAction(prefs.getString("gesture_double_tap", "lock_screen"), "double_tap")
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                viewModel.isHomePopupVisible = true
                viewModel.isQuickMenuVisible = false
                viewModel.isFontSettingsVisible = false
                binding.homePopupCompose.visibility = View.VISIBLE
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffY = e1.y - e2.y
                val diffX = e1.x - e2.x

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                        if (diffX > 0) {
                            handleGestureAction(prefs.getString("gesture_swipe_left", "none"), "swipe_left")
                        } else {
                            handleGestureAction(prefs.getString("gesture_swipe_right", "none"), "swipe_right")
                        }
                        return true
                    }
                } else {
                    if (Math.abs(diffY) > 100 && Math.abs(velocityY) > 100) {
                        if (diffY > 0) {
                            handleGestureAction(prefs.getString("gesture_swipe_up", "app_drawer"), "swipe_up")
                        } else {
                            handleGestureAction(prefs.getString("gesture_swipe_down", "notifications"), "swipe_down")
                        }
                        return true
                    }
                }
                return false
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })

        // Apply to the root to ensure gestures are caught
        root.setOnTouchListener { v, event ->
            // Pass the event to the gesture detector
            val handled = gestureDetector.onTouchEvent(event)

            // If it's an UP or CANCEL event, ensure we don't leave the view in a weird state
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                v.performClick()
            }

            // Return true if handled or if we want to continue receiving events
            handled || true
        }
    }

    private fun handleGestureAction(action: String?, gestureType: String) {
        gestureHandler.handleGestureAction(
            action, 
            gestureType,
            onOpenQuickMenu = {
                viewModel.isQuickMenuVisible = true
                binding.homePopupCompose.visibility = View.VISIBLE
            },
            onOpenDrawer = { type, focus ->
                (requireActivity() as? MainActivity)?.openDrawer(type, focusSearch = focus)
            },
            onOpenHiddenApps = {
                AppAuthenticator(requireContext()).authenticate { success ->
                    if (success) {
                        (requireActivity() as? MainActivity)?.openDrawer(gestureType, showHiddenOnly = true)
                    }
                }
            }
        )
    }
    // endregion

    // region Preferences & Constraints
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (isAdded && _binding != null) {
            when (key) {
                "clock_show_main" -> topPagesManager.applyTopSectionVisibility()
                "clock_index", "added_clocks", "added_clocks_ordered" -> topPagesManager.updateTopViewPager()
                "show_now_apps" -> applyPinnedAppsVisibility()
                "show_dock_bar" -> applyDockVisibility()
                "quotes_enabled" -> refreshQuotesUI()
            }
            if (key?.startsWith("clock_") == true || key?.startsWith("date_") == true) {
                topPagesManager.applyClockSettings()
            }
        }
    }
    // endregion

    // region Helper Methods & Navigation
    private fun toggleFlashlight() {
        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            Toast.makeText(context, if (isFlashlightOn) "Flashlight ON" else "Flashlight OFF", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCommonApp(type: String) {
        val intent = when (type) {
            "calculator" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
            "calendar" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
            "files" -> Intent(Intent.ACTION_GET_CONTENT).setType("*/*")
            "browser" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
            else -> null
        }

        if (intent != null) {
            try {
                startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWallpaperPicker() {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
        startActivity(Intent.createChooser(intent, "Select Wallpaper"))
    }

    private fun openAccessibilitySettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
                val componentName = ComponentName(requireContext(), ScreenLockService::class.java).flattenToString()
                putExtra("android.intent.extra.COMPONENT_NAME", componentName)
            }
        } else {
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            catch (ex: Exception) { Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun checkDefaultLauncherStatus() {
         binding.btnSetDefaultLauncher.visibility = if (com.example.myapplication.isDefaultLauncher(requireContext())) View.GONE else View.VISIBLE
    }

    private fun promptSetDefaultLauncher() {
        com.example.myapplication.requestDefaultLauncher(requireActivity())
    }

    private fun refreshQuotesUI() {
        if (_binding == null) return
        topPagesManager.getAllPages().forEach { page ->
            UIHelper.updateInspirationalQuote(page)
        }
    }

}
