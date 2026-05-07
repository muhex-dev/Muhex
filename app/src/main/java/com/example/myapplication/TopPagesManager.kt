package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.myapplication.clock.RotatingClockView
import com.example.myapplication.databinding.FragmentHomeBinding

class TopPagesManager(
    private val context: Context,
    private val binding: FragmentHomeBinding,
    private val prefs: android.content.SharedPreferences
) {

    private val displayPages = mutableListOf<View>()
    private var currentPageIndex = 0

    lateinit var rotatingClock1: RotatingClockView
    
    lateinit var clockDatePage: View
    lateinit var modernStackPage: View
    lateinit var infoClockPage: View
    lateinit var greetingPage: View
    lateinit var bigNumericPage: View
    lateinit var heyTodayPage: View

    private var isInitialized = false
    private lateinit var gestureDetector: GestureDetector
    private var isAnimating = false

    fun setupTopPages(layoutInflater: LayoutInflater) {
        // Direct View instantiations
        rotatingClock1 = RotatingClockView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Layout-based instantiations
        clockDatePage = inflatePage(layoutInflater, R.layout.view_top_clock_date)
        modernStackPage = inflatePage(layoutInflater, R.layout.view_top_modern_stack)
        infoClockPage = inflatePage(layoutInflater, R.layout.view_top_info_clock)
        greetingPage = inflatePage(layoutInflater, R.layout.view_top_greeting)
        bigNumericPage = inflatePage(layoutInflater, R.layout.view_top_big_numeric)
        heyTodayPage = inflatePage(layoutInflater, R.layout.view_top_hey_today)

        isInitialized = true

        setupGestures()
        updateTopViewPager()
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e1.x - e2.x
                val diffY = e1.y - e2.y
                
                // Only trigger if it's a clear horizontal swipe (3x more horizontal than vertical)
                // and meets the minimum velocity/distance thresholds
                if (Math.abs(diffX) > Math.abs(diffY) * 3 && Math.abs(diffX) > 150 && Math.abs(velocityX) > 200) {
                    if (diffX > 0) {
                        showNextPage()
                    } else {
                        showPreviousPage()
                    }
                    return true
                }
                return false
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })

        binding.topContainer.setOnTouchListener { v, event ->
            val handled = gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                v.performClick()
            }
            // We return true to consume the event if it's a horizontal swipe, 
            // but for vertical swipes we want them to bubble up to HomeFragment for drawer/notifications.
            handled
        }
    }

    private fun showNextPage() {
        if (isAnimating || displayPages.size <= 1) return
        val oldIndex = currentPageIndex
        currentPageIndex = (currentPageIndex + 1) % displayPages.size
        animateTransition(oldIndex, currentPageIndex, true)
    }

    private fun showPreviousPage() {
        if (isAnimating || displayPages.size <= 1) return
        val oldIndex = currentPageIndex
        currentPageIndex = if (currentPageIndex <= 0) displayPages.size - 1 else currentPageIndex - 1
        animateTransition(oldIndex, currentPageIndex, false)
    }

    private fun animateTransition(oldIndex: Int, newIndex: Int, isNext: Boolean) {
        if (oldIndex == newIndex || isAnimating) return
        val oldView = displayPages[oldIndex]
        val newView = displayPages[newIndex]

        isAnimating = true
        
        // Ensure new view is in the container
        (newView.parent as? ViewGroup)?.removeView(newView)
        binding.topContainer.addView(newView)
        
        val width = binding.topContainer.width.toFloat()
        val height = binding.topContainer.height.toFloat()
        
        // 3D parameters for "Pro" feel
        val density = context.resources.displayMetrics.density
        val cameraDist = 8000f * density
        oldView.cameraDistance = cameraDist
        newView.cameraDistance = cameraDist
        
        // Pivot points for a 3D Cube Rotation effect
        oldView.pivotX = if (isNext) width else 0f
        oldView.pivotY = height / 2f
        
        newView.pivotX = if (isNext) 0f else width
        newView.pivotY = height / 2f

        // Initial state for incoming view (rotated 90 degrees and invisible)
        newView.translationX = if (isNext) width else -width
        newView.rotationY = if (isNext) 90f else -90f
        newView.alpha = 0f
        newView.scaleX = 0.9f
        newView.scaleY = 0.9f

        val duration = 600L
        val interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()

        // Animate Outgoing View: Rotate and fade out
        oldView.animate()
            .rotationY(if (isNext) -90f else 90f)
            .translationX(if (isNext) -width else width)
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction {
                binding.topContainer.removeView(oldView)
                // Reset properties for future use
                oldView.rotationY = 0f
                oldView.translationX = 0f
                oldView.alpha = 1f
                oldView.scaleX = 1f
                oldView.scaleY = 1f
                isAnimating = false
                updatePrefs()
                
                // Hide indicators when sliding ends
                binding.pageIndicatorContainer.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        binding.pageIndicatorContainer.visibility = View.GONE
                    }
                    .start()
            }
            .start()

        // Show indicators during sliding
        if (binding.pageIndicatorContainer.visibility != View.VISIBLE) {
            binding.pageIndicatorContainer.alpha = 0f
            binding.pageIndicatorContainer.visibility = View.VISIBLE
        }
        binding.pageIndicatorContainer.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Dim background during transition
        binding.topDimOverlay.animate()
            .alpha(0.4f)
            .setDuration(duration / 2)
            .setInterpolator(interpolator)
            .withEndAction {
                binding.topDimOverlay.animate()
                    .alpha(0f)
                    .setDuration(duration / 2)
                    .setInterpolator(interpolator)
                    .start()
            }
            .start()

        // Animate Incoming View: Rotate and fade in
        newView.animate()
            .rotationY(0f)
            .translationX(0f)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withStartAction {
                updateIndicators()
            }
            .start()
    }

    private fun updateActivePage() {
        isAnimating = false
        binding.topContainer.removeAllViews()
        if (displayPages.isEmpty()) return
        
        // Ensure index is within bounds to avoid IndexOutOfBoundsException from stale prefs
        if (currentPageIndex !in displayPages.indices) {
            currentPageIndex = 0
        }
        
        val view = displayPages[currentPageIndex]
        (view.parent as? ViewGroup)?.removeView(view)
        binding.topContainer.addView(view)

        // Reset view properties to default
        view.translationX = 0f
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f

        binding.pageIndicatorContainer.animate().cancel()
        binding.pageIndicatorContainer.visibility = View.GONE
        updateIndicators()
        updatePrefs()
    }

    private fun updateIndicators() {
        val container = binding.pageIndicatorContainer
        if (displayPages.size <= 1) {
            container.visibility = View.GONE
            return
        }
        
        val density = context.resources.displayMetrics.density
        val dotSize = (7 * density).toInt()
        val pillWidth = (18 * density).toInt() // Premium wider look for active dot

        if (container.childCount != displayPages.size) {
            container.removeAllViews()
            for (i in displayPages.indices) {
                val isActive = i == currentPageIndex
                val dot = View(context).apply {
                    val w = if (isActive) pillWidth else dotSize
                    layoutParams = android.widget.LinearLayout.LayoutParams(w, dotSize).apply {
                        setMargins((5 * density).toInt(), 0, (5 * density).toInt(), 0)
                    }
                    setBackgroundResource(R.drawable.pill_white_solid)
                    alpha = if (isActive) 1.0f else 0.3f
                    setOnClickListener {
                        if (!isAnimating && i != currentPageIndex) {
                            val oldIdx = currentPageIndex
                            currentPageIndex = i
                            animateTransition(oldIdx, currentPageIndex, i > oldIdx)
                        }
                    }
                }
                container.addView(dot)
            }
        } else {
            for (i in 0 until container.childCount) {
                val dot = container.getChildAt(i)
                val isActive = i == currentPageIndex
                
                val targetWidth = if (isActive) pillWidth else dotSize
                val params = dot.layoutParams as android.widget.LinearLayout.LayoutParams
                
                if (params.width != targetWidth) {
                    val animator = android.animation.ValueAnimator.ofInt(params.width, targetWidth)
                    animator.addUpdateListener { anim ->
                        params.width = anim.animatedValue as Int
                        dot.layoutParams = params
                    }
                    animator.duration = 300
                    animator.interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
                    animator.start()
                }

                dot.animate()
                    .alpha(if (isActive) 1.0f else 0.3f)
                    .setDuration(300)
                    .withStartAction {
                        dot.setBackgroundResource(R.drawable.pill_white_solid)
                    }
                    .start()
            }
        }
    }

    private fun updatePrefs() {
        val addedClocksStr = prefs.getString("added_clocks_ordered", null) 
            ?: prefs.getStringSet("added_clocks", setOf("0"))?.joinToString(",") 
            ?: "0"
        val addedClocksList = addedClocksStr.split(",").filter { it.isNotEmpty() }
        val realIdx = addedClocksList.getOrNull(currentPageIndex)?.toIntOrNull() ?: 0
        prefs.edit().putInt("clock_index", realIdx).apply()
    }

    private fun inflatePage(inflater: LayoutInflater, layoutId: Int): View {
        return inflater.inflate(layoutId, null).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    fun updateTopViewPager() {
        if (!isInitialized) return
        val addedClocksStr = prefs.getString("added_clocks_ordered", null) 
            ?: prefs.getStringSet("added_clocks", setOf("0"))?.joinToString(",") 
            ?: "0"
        val addedClocksList = addedClocksStr.split(",").filter { it.isNotEmpty() }

        val allPages = getAllPages()
        
        val filteredPages = addedClocksList.mapNotNull { indexStr ->
            val idx = indexStr.toIntOrNull()
            if (idx != null && idx in allPages.indices) allPages[idx] else null
        }

        displayPages.clear()
        if (filteredPages.isEmpty()) {
            displayPages.add(allPages[0])
        } else {
            displayPages.addAll(filteredPages)
        }
        
        val currentIndex = prefs.getInt("clock_index", 0)
        currentPageIndex = addedClocksList.indexOf(currentIndex.toString()).coerceAtMost(displayPages.size - 1).coerceAtLeast(0)
        updateActivePage()
    }

    fun getAllPages(): List<View> {
        if (!isInitialized) return emptyList()
        return listOf(
            rotatingClock1, 
            clockDatePage, 
            modernStackPage, 
            infoClockPage,
            greetingPage,
            bigNumericPage,
            heyTodayPage
        )
    }

    fun applyClockSettings() {
        if (!isInitialized) return
        applyRotatingClockSettings(rotatingClock1, "")

        applyTopSectionVisibility()
    }

    private fun applyRotatingClockSettings(clockView: RotatingClockView, suffix: String) {
        clockView.apply {
            setShowClock(prefs.getBoolean("clock_show_main$suffix", true))
            setShowSeconds(prefs.getBoolean("clock_show_seconds$suffix", true))
            setShowDate(prefs.getBoolean("clock_show_date$suffix", true))
            setShowRingNumbers(prefs.getBoolean("clock_show_ring_nums$suffix", true))
            setClockScale(prefs.getFloat("clock_scale$suffix", 1.0f))
            setPillAngle(prefs.getFloat("clock_pill_angle$suffix", 0f))
            setHorizontalOffset(prefs.getFloat("clock_horizontal$suffix", -1f))
            setVerticalOffset(prefs.getFloat("clock_vertical$suffix", 0f))
            
            setDateTextSize(prefs.getFloat("clock_date_size$suffix", 34f))
            setDateHorizontalOffset(prefs.getFloat("clock_date_horizontal$suffix", 0f))
            setDateVerticalOffset(prefs.getFloat("clock_date_vertical$suffix", 0f))

            setHourTextSize(prefs.getFloat("clock_hour_size$suffix", 120f))
            setPillTextSize(prefs.getFloat("clock_pill_size$suffix", 48f))
            setRingThickness(prefs.getFloat("clock_ring_thickness$suffix", 3f))
            setPillThickness(prefs.getFloat("clock_pill_thickness$suffix", 2f))

            setShowCustomText(prefs.getBoolean("clock_show_custom_text$suffix", false))
            setCustomText(prefs.getString("clock_custom_text$suffix", "My Awesome App") ?: "My Awesome App")
            setCustomTextSize(prefs.getFloat("clock_custom_text_size$suffix", 34f))
            setCustomTextVerticalOffset(prefs.getFloat("clock_custom_text_vertical$suffix", 400f))
            setCustomTextHorizontalOffset(prefs.getFloat("clock_custom_text_horizontal$suffix", 0f))
            setCustomTextColor(prefs.getInt("clock_custom_text_color$suffix", Color.WHITE))

            val clockFont = prefs.getString("clock_font_family$suffix", "sans-serif-condensed") ?: "sans-serif-condensed"
            val dateFont = prefs.getString("date_font_family$suffix", "sans-serif-condensed") ?: "sans-serif-condensed"
            val customTextFont = prefs.getString("custom_text_font_family$suffix", "sans-serif-condensed") ?: "sans-serif-condensed"

            var style = Typeface.NORMAL
            if (prefs.getBoolean("clock_font_bold$suffix", true)) style = style or Typeface.BOLD
            if (prefs.getBoolean("clock_font_italic$suffix", false)) style = style or Typeface.ITALIC

            setClockFont(clockFont)
            setDateFont(dateFont)
            setCustomTextFont(customTextFont)
            setFontStyle(style)

            setClockColor(prefs.getInt("clock_color$suffix", Color.WHITE))
            setRingColor(prefs.getInt("clock_ring_color$suffix", Color.WHITE))
            setRingNumberColor(prefs.getInt("clock_ring_num_color$suffix", Color.WHITE))
            setDateColor(prefs.getInt("clock_date_color$suffix", Color.WHITE))
            setAuraColor(prefs.getInt("clock_aura_color$suffix", AndroidColor.parseColor("#20FFFFFF")))
            setCenterDiscColor(prefs.getInt("clock_center_disc_color$suffix", AndroidColor.parseColor("#10FFFFFF")))
            
            setShowAura(prefs.getBoolean("clock_show_aura$suffix", true))
            setShowCenterDisc(prefs.getBoolean("clock_show_center_disc$suffix", true))
        }
    }

    fun applyTopSectionVisibility() {
        val showClock = prefs.getBoolean("clock_view_enabled", true)
        binding.topContainer.visibility = if (showClock) View.VISIBLE else View.GONE
        
        val params = binding.topSectionGuideline.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.guidePercent = if (showClock) 0.4f else 0.0f
        binding.topSectionGuideline.layoutParams = params
    }
}
