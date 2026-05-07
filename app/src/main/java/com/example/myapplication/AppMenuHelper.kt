package com.example.myapplication

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.Settings
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Helper object to build and display the context menu for applications.
 * Handles pinning, docking, locking, hiding, and app info/uninstall actions.
 * Supports paginated navigation with smooth slide animations.
 */
object AppMenuHelper {

    fun showMenu(
        activity: AppCompatActivity,
        anchor: View,
        app: AppModel,
        onPinnedChanged: () -> Unit
    ): Dialog {
        val dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            val view = LayoutInflater.from(activity).inflate(R.layout.app_popup, null)
            setContentView(view)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setWindowAnimations(R.style.PopupAnimation)

            setupMenuPosition(this, anchor)
            bindMenuContent(view, activity, app, onPinnedChanged, this)
        }

        dialog.show()
        return dialog
    }

    private fun bindMenuContent(
        view: View,
        activity: AppCompatActivity,
        app: AppModel,
        onPinnedChanged: () -> Unit,
        dialog: Dialog
    ) {
        val repository = AppRepository.getInstance(activity)
        val userManager = activity.getSystemService(Context.USER_SERVICE) as android.os.UserManager
        val serial = userManager.getSerialNumberForUser(app.userHandle)
        val uniqueId = "${app.packageName}:$serial"

        // App basic info
        view.findViewById<ImageView>(R.id.menu_icon).setImageDrawable(app.icon)
        view.findViewById<TextView>(R.id.menu_header).text = app.label

        // Pagination Logic
        val container = view.findViewById<LinearLayout>(R.id.menu_pages_container)
        val btnNext = view.findViewById<View>(R.id.menu_next)
        val btnPrev = view.findViewById<View>(R.id.menu_prev)

        btnNext.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            slideMenu(container, toLeft = true)
        }

        btnPrev.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            slideMenu(container, toLeft = false)
        }

        // --- States & Bindings ---

        // Pin State
        val isPinned = repository.isPinned(app)
        view.findViewById<TextView>(R.id.menu_pin_text).setText(
            if (isPinned) R.string.menu_unpin else R.string.menu_pin
        )
        view.findViewById<ImageView>(R.id.menu_pin_icon).setImageResource(
            if (isPinned) R.drawable.ic_pin_off else R.drawable.ic_pin
        )

        // Dock State
        val isDocked = repository.isDocked(app)
        view.findViewById<TextView>(R.id.menu_dock_text).setText(
            if (isDocked) R.string.menu_undock else R.string.menu_dock
        )
        view.findViewById<ImageView>(R.id.menu_dock_icon).setImageResource(
            if (isDocked) R.drawable.ic_dock_off else R.drawable.ic_dock
        )

        // Lock State
        val lockPrefs = activity.getSharedPreferences("locked_prefs", Context.MODE_PRIVATE)
        val isLocked = (lockPrefs.getStringSet("locked_apps", emptySet()) ?: emptySet()).contains(uniqueId)
        view.findViewById<TextView>(R.id.menu_lock_text).setText(
            if (isLocked) R.string.menu_unlock else R.string.menu_lock
        )

        // Hide State
        val isHidden = repository.isHidden(app)
        view.findViewById<TextView>(R.id.menu_hide_text).setText(
            if (isHidden) R.string.menu_unhide else R.string.menu_hide
        )

        // Hide Pin and Dock if the app is currently hidden
        if (isHidden) {
            view.findViewById<View>(R.id.menu_pin).visibility = View.GONE
            view.findViewById<View>(R.id.menu_dock).visibility = View.GONE
            // Also hide the "More" button on Page 1 if Page 2's main features are gone
            btnNext.visibility = View.GONE
        }

        // --- Click Listeners ---

        view.findViewById<View>(R.id.menu_open).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            LaunchApp.launch(activity, app)
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.menu_pin).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val result = repository.togglePin(app)
            when (result) {
                AppRepository.ToggleResult.ADDED -> {
                    Toast.makeText(activity, R.string.msg_pinned, Toast.LENGTH_SHORT).show()
                    onPinnedChanged()
                }
                AppRepository.ToggleResult.REMOVED -> {
                    Toast.makeText(activity, R.string.msg_unpinned, Toast.LENGTH_SHORT).show()
                    onPinnedChanged()
                }
                AppRepository.ToggleResult.NO_SPACE -> {
                    Toast.makeText(activity, R.string.msg_no_space_pinned, Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.menu_dock).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val result = repository.toggleDock(app)
            when (result) {
                AppRepository.ToggleResult.ADDED -> {
                    Toast.makeText(activity, R.string.msg_docked, Toast.LENGTH_SHORT).show()
                    onPinnedChanged()
                }
                AppRepository.ToggleResult.REMOVED -> {
                    Toast.makeText(activity, R.string.msg_undocked, Toast.LENGTH_SHORT).show()
                    onPinnedChanged()
                }
                AppRepository.ToggleResult.NO_SPACE -> {
                    Toast.makeText(activity, R.string.msg_no_space_dock, Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.menu_hide).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val nextHideState = !isHidden
            repository.setHidden(app, nextHideState)
            
            val msgRes = if (nextHideState) R.string.msg_hidden else R.string.msg_unhidden
            Toast.makeText(activity, msgRes, Toast.LENGTH_SHORT).show()
            
            onPinnedChanged()
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.menu_lock).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            handleLockAction(activity, app, uniqueId, isLocked) {
                onPinnedChanged()
                dialog.dismiss()
            }
        }

        view.findViewById<ImageView>(R.id.menu_info).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openAppInfo(activity, app)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.menu_uninstall).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${app.packageName}")
            }
            activity.startActivity(intent)
            dialog.dismiss()
        }
    }

    private fun slideMenu(view: View, toLeft: Boolean) {
        // Assuming page width is 240dp. We calculate actual pixels.
        val pageWidth = view.context.resources.displayMetrics.density * 240
        val targetX = if (toLeft) -pageWidth else 0f
        
        ObjectAnimator.ofFloat(view, "translationX", targetX).apply {
            duration = 350
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun openAppInfo(activity: AppCompatActivity, app: AppModel) {
        val launcher = activity.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            val activities = launcher.getActivityList(app.packageName, app.userHandle)
            if (activities.isNotEmpty()) {
                launcher.startAppDetailsActivity(activities[0].componentName, app.userHandle, null, null)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", app.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.unable_open_info, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMenuPosition(dialog: Dialog, anchor: View) {
        dialog.window?.let { window ->
            val lp = window.attributes
            val location = IntArray(2)
            anchor.getLocationOnScreen(location)

            lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            lp.x = 0

            val menuHeightEstimate = 500
            val gap = 40
            lp.y = location[1] - (menuHeightEstimate + gap)

            if (lp.y < 100) {
                lp.y = location[1] + anchor.height + gap
            }

            window.attributes = lp
        }
    }

    private fun handleLockAction(
        activity: AppCompatActivity,
        app: AppModel,
        uniqueId: String,
        currentlyLocked: Boolean,
        onComplete: () -> Unit
    ) {
        AppAuthenticator(activity).authenticate { isSuccess ->
            if (isSuccess) {
                val prefs = activity.getSharedPreferences("locked_prefs", Context.MODE_PRIVATE)
                val lockedSet = prefs.getStringSet("locked_apps", emptySet())?.toMutableSet() ?: mutableSetOf()

                val willBeLocked = !currentlyLocked
                if (currentlyLocked) {
                    lockedSet.remove(uniqueId)
                } else {
                    lockedSet.add(uniqueId)
                }

                prefs.edit().putStringSet("locked_apps", lockedSet).apply()
                
                val msgRes = if (willBeLocked) R.string.msg_locked else R.string.msg_unlocked
                Toast.makeText(activity, msgRes, Toast.LENGTH_SHORT).show()
                
                onComplete()
            }
        }
    }
}
