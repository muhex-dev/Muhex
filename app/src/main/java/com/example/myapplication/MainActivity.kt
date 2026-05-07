package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import com.example.myapplication.databinding.ActivityMainBinding

/**
 * MainActivity serves as the primary host for the launcher's UI.
 * It manages the transitions between the Home screen and the App Drawer.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("launcher_settings", MODE_PRIVATE) }
    
    var isDrawerOpen = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemUI()
        setupBackNavigation()
        setupPermissionOverlay()
    }

    private fun setupPermissionOverlay() {
        val composeView = ComposeView(this).apply {
            setContent {
                PermissionCheckerPopup(onAllGranted = {
                    // Refresh or notify fragments if needed
                })
            }
        }
        binding.root.addView(composeView)
    }

    private fun setupSystemUI() {
        // Fullscreen transparent bars
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Hide status bar on home by default for a minimal look
        setStatusBarVisible(false)

        // Handle insets for the drawer container to prevent content overlap with system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerContainer) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isDrawerOpen) {
                    closeDrawer()
                }
                // Back button on Home is usually handled by the system or ignored
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Return to home if drawer is open when Home button/Intent is triggered
        if (isDrawerOpen) closeDrawer()
    }

    /**
     * Toggles the visibility of the status bar.
     */
    fun setStatusBarVisible(visible: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            if (visible) {
                show(WindowInsetsCompat.Type.statusBars())
            } else {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    /**
     * Opens the app drawer with specified configuration and animation.
     */
    fun openDrawer(
        openedByGesture: String = "swipe_up",
        showHiddenOnly: Boolean = false,
        focusSearch: Boolean = false
    ) {
        if (isDrawerOpen) return

        val animType = prefs.getString("drawer_open_anim", "fade")
        val (enter, exit) = getAnimationResources(animType)

        val fragment = DrawerFragment().apply {
            arguments = Bundle().apply {
                putString("opened_by_gesture", openedByGesture)
                putBoolean("show_hidden_only", showHiddenOnly)
                putBoolean("focus_search", focusSearch)
            }
        }

        supportFragmentManager.commit {
            setCustomAnimations(enter, exit, 0, 0)
            replace(R.id.drawer_container, fragment)
        }

        isDrawerOpen = true
        binding.drawerContainer.visibility = View.VISIBLE
        setStatusBarVisible(true)

        // Smoothly fade out the Home screen
        animateHomeAlpha(0f, 400) {
            binding.fragmentContainer.visibility = View.GONE
        }
    }

    /**
     * Closes the drawer and brings back the Home screen.
     */
    fun closeDrawer() {
        if (!isDrawerOpen) return

        val animType = prefs.getString("drawer_close_anim", "fade")
        val fragment = supportFragmentManager.findFragmentById(R.id.drawer_container)

        // Special handling for circular reveal exit if implemented in Fragment
        if (animType == "circle" && fragment is DrawerFragment) {
            fragment.startCircularExitAnimation {
                supportFragmentManager.commit { remove(fragment) }
                finishClosingDrawer()
            }
            return
        }

        val (enter, exit) = getAnimationResources(animType, isClosing = true)

        if (fragment != null) {
            supportFragmentManager.commit {
                setCustomAnimations(enter, exit)
                remove(fragment)
            }
        }
        finishClosingDrawer()
    }

    private fun finishClosingDrawer() {
        isDrawerOpen = false
        setStatusBarVisible(false)

        binding.fragmentContainer.visibility = View.VISIBLE
        animateHomeAlpha(1f, 400)

        // Delay hiding the container until animation is fully complete
        binding.drawerContainer.postDelayed({
            if (!isDrawerOpen) binding.drawerContainer.visibility = View.GONE
        }, 450)
    }

    private fun animateHomeAlpha(target: Float, duration: Long, onEnd: (() -> Unit)? = null) {
        binding.fragmentContainer.animate()
            .alpha(target)
            .setDuration(duration)
            .withEndAction { onEnd?.invoke() }
            .start()
    }

    private fun getAnimationResources(type: String?, isClosing: Boolean = false): Pair<Int, Int> {
        return when (type) {
            "premium" -> if (isClosing) 0 to R.anim.premium_exit else R.anim.premium_enter to 0
            "slide_up" -> if (isClosing) 0 to R.anim.slide_out_down else R.anim.slide_in_up to 0
            "slide_down" -> if (isClosing) 0 to R.anim.slide_in_up else R.anim.slide_out_down to 0
            "scale" -> if (isClosing) 0 to R.anim.popup_exit else R.anim.popup_enter to 0
            "fade" -> android.R.anim.fade_in to android.R.anim.fade_out
            else -> 0 to 0
        }
    }
}
