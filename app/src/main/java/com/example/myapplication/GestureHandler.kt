package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class GestureHandler(
    private val context: Context,
    private val activity: FragmentActivity,
    private val repository: AppRepository,
    private val lifecycleOwner: LifecycleOwner
) {
    private val prefs = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    fun handleGestureAction(action: String?, gestureName: String, onOpenQuickMenu: () -> Unit, onOpenDrawer: (String, Boolean) -> Unit, onOpenHiddenApps: () -> Unit) {
        val fullKey = when (gestureName) {
            "swipe_up" -> "gesture_swipe_up"
            "swipe_down" -> "gesture_swipe_down"
            "swipe_left" -> "gesture_swipe_left"
            "swipe_right" -> "gesture_swipe_right"
            "double_tap" -> "gesture_double_tap"
            else -> gestureName
        }

        when (action) {
            "none" -> {}
            "quick_menu" -> {
                onOpenQuickMenu()
            }
            "app_drawer" -> {
                onOpenDrawer(gestureName, false)
            }
            "hidden_apps" -> {
                onOpenHiddenApps()
            }
            "notifications" -> {
                if (!ScreenLockService.expandNotifications()) {
                    Toast.makeText(context, "Enable Accessibility to expand notifications", Toast.LENGTH_SHORT).show()
                    openAccessibilitySettings()
                }
            }
            "quick_settings" -> {
                if (!ScreenLockService.expandQuickSettings()) {
                    Toast.makeText(context, "Enable Accessibility to expand quick settings", Toast.LENGTH_SHORT).show()
                    openAccessibilitySettings()
                }
            }
            "search" -> {
                onOpenDrawer(gestureName, true)
            }
            "assistant" -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(context, "Assistant not found", Toast.LENGTH_SHORT).show()
                }
            }
            "lock_screen" -> {
                if (!ScreenLockService.lock()) {
                    Toast.makeText(context, "Enable Accessibility to lock screen", Toast.LENGTH_SHORT).show()
                    openAccessibilitySettings()
                }
            }
            "settings" -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            "open_app" -> {
                val appId = prefs.getString("${fullKey}_app_id", "") ?: ""
                if (appId.isNotEmpty()) {
                    lifecycleOwner.lifecycleScope.launch {
                        val allApps = repository.getAllApps()
                        val app = allApps.find { repository.getAppUniqueId(it) == appId }
                        if (app != null) {
                            LaunchApp.launch(context, app)
                        } else {
                            Toast.makeText(context, "Assigned app not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "No app assigned for this gesture", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
