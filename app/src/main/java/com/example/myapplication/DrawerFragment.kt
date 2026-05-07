package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.FragmentDrawerBinding
 import com.example.myapplication.clock.ClockSettingsSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * DrawerFragment: Displays the list of all installed applications.
 */
class DrawerFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!

    private lateinit var allAppsAdapter: AppGridAdapter
    private val repository by lazy { AppRepository.getInstance(requireContext()) }
    private val prefs by lazy { requireContext().getSharedPreferences("launcher_settings", Context.MODE_PRIVATE) }
    
    private lateinit var gestureDetector: GestureDetector
    private var isSettingsVisible by mutableStateOf(false)
    private var isClockSettingsVisible by mutableStateOf(false)
    private var isFontSettingsVisible by mutableStateOf(false)

    private var fontPickerTargetKey by mutableStateOf("drawer_font_family")
    private var fontPickerTitle by mutableStateOf("Drawer Font")

    private var fullAppsList: List<AppModel> = emptyList()

    private var showHiddenOnly = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        showHiddenOnly = arguments?.getBoolean("show_hidden_only", false) ?: false
        if (showHiddenOnly) {
            binding.searchViewApps.queryHint = "Search Hidden Apps"
        }
        setupRecyclerView()
        setupMenuAction()
        setupMuhexScroller()
        setupSettingsCompose()
        setupGestures()
        setupSearch()
        setupAppChangeObserver()

        applyCurrentSettings()
        refreshAppList()
        
        prefs.registerOnSharedPreferenceChangeListener(this)

        val openedByGesture = arguments?.getString("opened_by_gesture") ?: "swipe_up"
        val focusSearch = arguments?.getBoolean("focus_search", false) ?: false

        // Check if Circle Reveal animation is enabled
        if (prefs.getString("drawer_open_anim", "fade") == "circle") {
            binding.drawerCard.visibility = View.INVISIBLE
            startCircularEnterAnimation()
        }

        // Auto-focus search if opened by search gesture or explicitly requested
        if (openedByGesture == "search" || focusSearch) {
            binding.searchViewApps.isIconified = false
            binding.searchViewApps.requestFocus()
        }
    }

    private fun setupSearch() {
        binding.searchViewApps.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterApps(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText)
                return true
            }
        })
    }

    private fun filterApps(query: String?) {
        val filtered = if (query.isNullOrBlank()) {
            fullAppsList
        } else {
            fullAppsList.filter { it.label.contains(query, ignoreCase = true) }
        }
        allAppsAdapter.submitList(filtered)
        
        // Hide/Show scroller during search
        binding.muhexScroller.visibility = if (query.isNullOrBlank()) View.VISIBLE else View.GONE
    }

    private fun startCircularEnterAnimation() {
        binding.root.post {
            if (_binding == null) return@post
            val view = binding.drawerCard
            view.visibility = View.VISIBLE
            
            val cx = view.width
            val cy = view.height
            val finalRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()

            val anim = android.view.ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius)
            anim.duration = 500
            anim.start()
        }
    }

    fun startCircularExitAnimation(onComplete: () -> Unit) {
        val view = binding.drawerCard
        val cx = view.width
        val cy = view.height
        val initialRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()

        val anim = android.view.ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0f)
        anim.duration = 400
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                view.visibility = View.INVISIBLE
                onComplete()
            }
        })
        anim.start()
    }

    private fun setupAppChangeObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.appsChangedFlow.collectLatest {
                    refreshAppList(forceRefresh = true)
                }
            }
        }
    }

    private fun setupSettingsCompose() {
        binding.drawerSettingsCompose.setContent {
            UnifiedSettingsSheet(
                repository = repository,
                prefs = prefs,
                isVisible = isSettingsVisible,
                initialTab = 4,
                onOpenFontPicker = { key, title ->
                    fontPickerTargetKey = key
                    fontPickerTitle = title
                    isSettingsVisible = false
                    isFontSettingsVisible = true
                },
                onOpenClockSettings = {
                    isSettingsVisible = false
                    isClockSettingsVisible = true
                },
                onDismiss = {
                    isSettingsVisible = false
                    binding.drawerSettingsCompose.visibility = View.GONE
                }
            )

            ClockSettingsSheet(
                prefs = prefs,
                isVisible = isClockSettingsVisible,
                onOpenFontPicker = { key, title ->
                    fontPickerTargetKey = key
                    fontPickerTitle = title
                    isClockSettingsVisible = false
                    isFontSettingsVisible = true
                },
                onDismiss = {
                    isClockSettingsVisible = false
                    binding.drawerSettingsCompose.visibility = View.GONE
                }
            )

            FontSettings(
                prefs = prefs,
                targetKey = fontPickerTargetKey,
                title = fontPickerTitle,
                isVisible = isFontSettingsVisible,
                onDismiss = {
                    isFontSettingsVisible = false
                    if (fontPickerTargetKey.contains("clock") || fontPickerTargetKey.contains("date")) {
                        isClockSettingsVisible = true
                    } else {
                        isSettingsVisible = true
                    }
                }
            )
        }
    }

    private fun setupGestures() {
        val openedByGesture = arguments?.getString("opened_by_gesture") ?: "swipe_up"

        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffY = e1.y - e2.y
                val diffX = e1.x - e2.x
                
                val absDiffX = Math.abs(diffX)
                val absDiffY = Math.abs(diffY)

                val isHorizontal = absDiffX > absDiffY
                val isSignificant = if (isHorizontal) absDiffX > 100 else absDiffY > 100
                if (!isSignificant || Math.abs(if (isHorizontal) velocityX else velocityY) < 100) return false

                val triggeredClose = when (openedByGesture) {
                    "swipe_up" -> !isHorizontal && diffY < -100
                    "swipe_down" -> !isHorizontal && diffY > 100
                    "swipe_left" -> isHorizontal && diffX < -100
                    "swipe_right" -> isHorizontal && diffX > 100
                    else -> false
                }

                if (triggeredClose) {
                    if (!isHorizontal) {
                        if (openedByGesture == "swipe_up" && binding.appRecyclerView.canScrollVertically(-1)) return false
                        if (openedByGesture == "swipe_down" && binding.appRecyclerView.canScrollVertically(1)) return false
                    }
                    (requireActivity() as? MainActivity)?.closeDrawer()
                    return true
                }
                return false
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })

        binding.drawerCard.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        
        binding.appRecyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }
        })
    }

    private fun showDrawerSettings() {
        isSettingsVisible = true
        binding.drawerSettingsCompose.visibility = View.VISIBLE
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (isAdded && _binding != null) {
            val drawerKeys = listOf(
                "drawer_columns", "drawer_opacity", "drawer_item_opacity",
                "drawer_border_color", "drawer_item_border_color", "drawer_display_mode",
                "drawer_icon_size", "drawer_label_size", "drawer_font_family"
            )
            val scrollerKeys = listOf(
                "scroller_padding", "scroller_bending", "scroller_spread",
                "scroller_text_size", "scroller_scale", "scroller_line_offset",
                "scroller_line_thickness", "scroller_line_alpha", "scroller_base_alpha",
                "scroller_anim_duration", "scroller_touch_slop", "scroller_haptic"
            )

            if (key in drawerKeys) {
                applyCurrentSettings()
                allAppsAdapter.notifyDataSetChanged()
            } else if (key in scrollerKeys) {
                updateScrollerSettings()
            }
        }
    }

    private fun applyCurrentSettings() {
        configureGridLayout()
        updateDrawerAppearance()
        updateScrollerSettings()
        updateSearchFont()
    }

    private fun updateSearchFont() {
        val fontFamily = prefs.getString("drawer_font_family", "sans-serif-condensed") ?: "sans-serif-condensed"
        val typeface = FontManager.resolveTypeface(fontFamily)
        
        // Update SearchView text font
        val searchText = binding.searchViewApps.findViewById<android.widget.TextView>(androidx.appcompat.R.id.search_src_text)
        searchText?.typeface = typeface

        // Update MuhexScroller font
        binding.muhexScroller.setTypeface(typeface)
    }

    private fun configureGridLayout() {
        val columns = prefs.getInt("drawer_columns", 4)
        val layoutManager = binding.appRecyclerView.layoutManager as? GridLayoutManager
        
        when (columns) {
            1 -> {
                layoutManager?.spanCount = 1
                layoutManager?.spanSizeLookup = GridLayoutManager.DefaultSpanSizeLookup()
            }
            2 -> {
                layoutManager?.spanCount = 2
                layoutManager?.spanSizeLookup = GridLayoutManager.DefaultSpanSizeLookup()
            }
            3 -> {
                layoutManager?.spanCount = 4
                layoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val cyclePos = position % 6
                        return if (cyclePos == 0 || cyclePos == 5) 2 else 1
                    }
                }
            }
            else -> {
                layoutManager?.spanCount = 4
                layoutManager?.spanSizeLookup = GridLayoutManager.DefaultSpanSizeLookup()
            }
        }
    }

    private fun updateDrawerAppearance() {
        val opacityProgress = prefs.getInt("drawer_opacity", 100)
        val borderColor = prefs.getInt("drawer_border_color", Color.WHITE)
        
        val background = binding.drawerCard.background?.mutate() as? GradientDrawable
        if (background != null) {
            val alpha = (opacityProgress * 2.55).toInt().coerceIn(0, 255)
            background.alpha = alpha
            background.setStroke(resources.getDimensionPixelSize(R.dimen.drawer_border_width), borderColor)
        }
    }

    private fun updateScrollerSettings() {
        binding.muhexScroller.apply {
            edgePaddingRatio = prefs.getFloat("scroller_padding", 0.12f)
            maxBendingDistance = prefs.getFloat("scroller_bending", 300f)
            curveSpread = prefs.getFloat("scroller_spread", 7.5f)
            baseTextSize = prefs.getFloat("scroller_text_size", 28f)
            selectedTextScale = prefs.getFloat("scroller_scale", 2.4f)
            lineOffsetFromLetter = prefs.getFloat("scroller_line_offset", 40f)
            lineStrokeWidth = prefs.getFloat("scroller_line_thickness", 3f)
            lineMaxAlpha = prefs.getInt("scroller_line_alpha", 90)
            baseLetterAlpha = prefs.getInt("scroller_base_alpha", 130)
            animationDuration = prefs.getInt("scroller_anim_duration", 250).toLong()
            touchSlop = prefs.getFloat("scroller_touch_slop", 80f)
            hapticEnabled = prefs.getBoolean("scroller_haptic", true)
            
            customColor = null
            
            updateColorsAndGradient()
            invalidate()
        }
    }

    private fun setupMuhexScroller() {
        binding.muhexScroller.onLetterSelected = { letter ->
            showBubble(letter.toString())
            scrollToLetter(letter.toString())
        }
        binding.muhexScroller.onActionUp = { hideBubble() }
    }

    private fun showBubble(letter: String) {
        val bubble = binding.letterPreviewBubble
        if (bubble.visibility != View.VISIBLE) {
            bubble.alpha = 0f
            bubble.scaleX = 0.5f
            bubble.scaleY = 0.5f
            bubble.visibility = View.VISIBLE
            bubble.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).setInterpolator(OvershootInterpolator()).start()
        }
        if (bubble.text != letter) {
            bubble.text = letter
            animateBubbleBounce(bubble)
        }
    }

    private fun animateBubbleBounce(view: View) {
        view.animate().scaleY(1.4f).scaleX(0.8f).setDuration(80).withEndAction {
            view.animate().scaleY(1f).scaleX(1f).setInterpolator(OvershootInterpolator(1.5f)).setDuration(250).start()
        }.start()
    }

    private fun hideBubble() {
        binding.letterPreviewBubble.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(200).withEndAction {
            binding.letterPreviewBubble.visibility = View.GONE
        }.start()
    }

    private fun scrollToLetter(letter: String) {
        val pos = allAppsAdapter.currentList.indexOfFirst { it.label.uppercase().startsWith(letter) }
        if (pos != -1) {
            (binding.appRecyclerView.layoutManager as GridLayoutManager).scrollToPositionWithOffset(pos, 0)
        }
    }

    private fun setupRecyclerView() {
        allAppsAdapter = AppGridAdapter(
            activity = requireActivity() as AppCompatActivity,
            onAppLongClick = { app, iconView -> 
                iconView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                AppMenuHelper.showMenu(requireActivity() as AppCompatActivity, iconView, app) {
                    refreshAppList()
                }
            }
        )
        binding.appRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = allAppsAdapter
            setHasFixedSize(true)
            itemAnimator = null 
        }
    }

    private fun refreshAppList(forceRefresh: Boolean = false) {
        viewLifecycleOwner.lifecycleScope.launch {
            fullAppsList = if (showHiddenOnly) {
                repository.getHiddenApps()
            } else {
                repository.getDrawerApps(forceRefresh)
            }
            allAppsAdapter.submitList(fullAppsList) { updateScrollerAlphabet() }
        }
    }

    private fun updateScrollerAlphabet() {
        val alphabet = allAppsAdapter.currentList.map { it.label.uppercase().first() }.distinct().sorted()
        binding.muhexScroller.setAlphabet(alphabet)
    }

    private fun setupMenuAction() {
        binding.btnMenu.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showPopupMenu(view)
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Refresh")
        
        // Dynamically change menu item based on current view
        if (showHiddenOnly) {
            popup.menu.add("Show All Apps")
        } else {
            popup.menu.add("Hidden Apps")
        }
        
        popup.menu.add("Drawer Settings")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Refresh" -> {
                    ObjectAnimator.ofFloat(binding.btnMenu, "rotation", 0f, 360f).apply {
                        duration = 500
                        interpolator = LinearInterpolator()
                        start()
                    }
                    refreshAppList(forceRefresh = true)
                }
                "Hidden Apps" -> {
                    AppAuthenticator(requireContext()).authenticate { success ->
                        if (success) {
                            showHiddenOnly = true
                            // Update search hint to reflect mode
                            binding.searchViewApps.queryHint = "Search Hidden Apps"
                            refreshAppList()
                        }
                    }
                }
                "Show All Apps" -> {
                    showHiddenOnly = false
                    binding.searchViewApps.queryHint = "Search Apps"
                    refreshAppList()
                }
                "Drawer Settings" -> {
                    showDrawerSettings()
                }
            }
            true
        }
        popup.show()
    }

    override fun onResume() {
        super.onResume()
        applyCurrentSettings()
        refreshAppList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        _binding = null
    }
}
