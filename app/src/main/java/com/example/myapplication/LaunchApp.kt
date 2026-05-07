package com.example.myapplication

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import android.widget.Toast

/**
 * LaunchApp: Utility object to handle application launching with security checks.
 */
object LaunchApp {

    fun launch(context: Context, app: AppModel) {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val serial = userManager.getSerialNumberForUser(app.userHandle)
        val uniqueId = "${app.packageName}:$serial"

        val prefs = context.getSharedPreferences("locked_prefs", Context.MODE_PRIVATE)
        val lockedApps = prefs.getStringSet("locked_apps", emptySet()) ?: emptySet()

        if (lockedApps.contains(uniqueId)) {
            AppAuthenticator(context).authenticate { isSuccess ->
                if (isSuccess) {
                    addToRecents(context, uniqueId)
                    executeLaunch(context, app)
                } else {
                    Toast.makeText(context, "${app.label} is locked", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            addToRecents(context, uniqueId)
            executeLaunch(context, app)
        }
    }

    private fun addToRecents(context: Context, appId: String) {
        val prefs = context.getSharedPreferences("recents_prefs", Context.MODE_PRIVATE)
        val recentsString = prefs.getString("recent_apps_list", "") ?: ""
        val recentsList = if (recentsString.isEmpty()) mutableListOf() else recentsString.split(",").toMutableList()
        
        // Remove if already exists to move to top
        recentsList.remove(appId)
        // Add to the beginning
        recentsList.add(0, appId)
        
        // Keep only the last 10
        val limitedList = recentsList.take(10)
        prefs.edit().putString("recent_apps_list", limitedList.joinToString(",")).apply()
    }

    private fun executeLaunch(context: Context, app: AppModel) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            val activities = launcherApps.getActivityList(app.packageName, app.userHandle)
            if (activities.isNotEmpty()) {
                launcherApps.startMainActivity(activities[0].componentName, app.userHandle, null, null)
            } else {
                Toast.makeText(context, "App not found for this profile", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error launching ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }
}
