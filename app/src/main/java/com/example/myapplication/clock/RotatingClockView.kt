package com.example.myapplication.clock

import android.content.Context
import android.graphics.*
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.myapplication.FontManager
import com.example.myapplication.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * RotatingClockView: A custom clock view featuring rotating minute and second rings.
 * The time is indicated by a fixed "pill" marker.
 *
 * Optimized for smooth 60fps animations and clear code structure.
 */
class RotatingClockView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- Configuration Properties ---
    private var _clockColor = AndroidColor.WHITE
    private var _ringColor = AndroidColor.WHITE
    private var _dateColor = AndroidColor.WHITE
    private var _ringNumColor = AndroidColor.WHITE
    private var _auraColor = AndroidColor.parseColor("#20FFFFFF")
    private var _centerDiscColor = AndroidColor.parseColor("#10FFFFFF")
    
    private object Color {
        const val BLACK = AndroidColor.BLACK
        const val TRANSPARENT = AndroidColor.TRANSPARENT
    }

    private var _clockFontFamily = "sans-serif-condensed"
    private var _dateFontFamily = "sans-serif-condensed"
    private var _fontStyle = Typeface.NORMAL
    
    private var clockTypeface = FontManager.resolveTypeface(_clockFontFamily, _fontStyle)
    private var dateTypeface = FontManager.resolveTypeface(_dateFontFamily, _fontStyle)

    private var _verticalOffset = 0f
    private var _horizontalOffset = -1f
    private var _dateVerticalOffset = 0f
    private var _dateHorizontalOffset = 0f
    private var _clockScale = 1.0f
    private var _pillAngle = 0f
    private var _ringThickness = 3f
    private var _pillThickness = 2f

    private var _showClock = true
    private var _showSeconds = true
    private var _showDate = true
    private var _showRingNumbers = true
    private var _showCustomText = false
    private var _showCenterDisc = true
    private var _showAura = true

    private var _hourTextSize = 120f
    private var _pillTextSize = 48f
    private var _customTextSize = 34f

    private var _customText = "My Awesome App"
    private var _customTextVerticalOffset = 400f
    private var _customTextHorizontalOffset = 0f
    private var _customTextFontFamily = "sans-serif-condensed"
    private var customTextTypeface = Typeface.SANS_SERIF

    private var _quote = ""
    private var _showQuote = true

    // --- Internal State & Animation ---
    private val calendar = Calendar.getInstance()
    private val pillRect = RectF()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())

    private var centerX = 0f
    private var centerY = 0f
    private var minuteRadius = 0f
    private var secondRadius = 0f

    // --- Animation Runnable (60 FPS) ---
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isShown) {
                invalidate()
                // Post at 16ms interval for ~60fps smooth rotation
                mainHandler.postDelayed(this, 16)
            }
        }
    }

    // --- Paint Definitions ---
    private inner class ClockPaints {
        val hour = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = _clockColor
            textSize = _hourTextSize
            typeface = clockTypeface
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 0f, 2f, Color.BLACK)
        }

        val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = _clockColor
            textSize = _pillTextSize
            typeface = clockTypeface
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, Color.BLACK)
        }

        val day = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = _dateColor
            textSize = 44f
            typeface = dateTypeface
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
            setShadowLayer(8f, 0f, 2f, Color.BLACK)
        }

        val date = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = _dateColor
            textSize = 34f
            typeface = dateTypeface
            textAlign = Paint.Align.CENTER
            alpha = 180
            setShadowLayer(6f, 0f, 1f, Color.BLACK)
        }

        val customText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = _clockColor
            textSize = _customTextSize
            typeface = dateTypeface
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, Color.BLACK)
        }

        val ringNum = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = _clockColor
            textSize = 24f
            typeface = clockTypeface
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 1f, Color.BLACK)
        }

        val ringLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }

        val pillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        
        val centerDisc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.BLACK
            alpha = 20
        }

        val aura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val quote = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = _clockColor
            textSize = 28f
            typeface = dateTypeface
            textAlign = Paint.Align.CENTER
            alpha = 140
        }
    }

    private val paints = ClockPaints()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mainHandler.post(updateRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mainHandler.removeCallbacks(updateRunnable)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            mainHandler.removeCallbacks(updateRunnable)
            mainHandler.post(updateRunnable)
        } else {
            mainHandler.removeCallbacks(updateRunnable)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDimensions()
    }

    private fun updateDimensions() {
        if (width <= 0 || height <= 0) return
        
        centerX = if (_horizontalOffset < 0) width / 2f else _horizontalOffset
        centerY = (height / 2f) + _verticalOffset
        minuteRadius = width * 0.22f
        secondRadius = width * 0.34f
        
        val auraRadius = secondRadius * 1.5f
        if (auraRadius > 0) {
            paints.aura.shader = RadialGradient(
                centerX, centerY, auraRadius,
                intArrayOf(applyAlpha(_ringColor, 25), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
    }

    private fun applyAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    override fun onDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        
        calendar.timeInMillis = System.currentTimeMillis()
        
        // Dynamic re-centering for preview/offset updates
        centerX = if (_horizontalOffset < 0) width / 2f else _horizontalOffset
        centerY = (height / 2f) + _verticalOffset

        if (_showClock) drawAura(canvas)
        if (_showDate) drawDateBlock(canvas)
        
        if (_showClock) {
            canvas.save()
            canvas.scale(_clockScale, _clockScale, centerX, centerY)
            drawRotatingClock(canvas)
            canvas.restore()
        }

        if (_showCustomText) drawCustomText(canvas)
        if (_showQuote) drawQuote(canvas)
    }

    private fun drawQuote(canvas: Canvas) {
        if (_quote.isEmpty()) {
            val prefs = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)
            _showQuote = prefs.getBoolean("quotes_enabled", true)
            if (_showQuote) {
                _quote = listOf(
                    "Believe you can and you're halfway there.",
                    "Your only limit is your mind.",
                    "Dream big. Work hard. Stay focused.",
                    "Stay positive, work hard, make it happen.",
                    "The best way to predict the future is to create it."
                ).random()
            }
        }
        if (_showQuote && _quote.isNotEmpty()) {
            canvas.drawText(_quote, centerX, centerY + (_customTextVerticalOffset + 80f), paints.quote)
        }
    }

    private fun drawAura(canvas: Canvas) {
        if (!_showAura) return
        canvas.drawCircle(centerX, centerY, secondRadius * 1.5f, paints.aura)
    }

    private fun drawRotatingClock(canvas: Canvas) {
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        val millis = calendar.get(Calendar.MILLISECOND)

        // Sub-second precision for ultra-smooth 60fps rotation
        val secRotation = (seconds * 6f) + (millis * 0.006f)
        val minRotation = (minutes * 6f) + (seconds * 0.1f) + (millis * 0.0001f)
        
        // 1. Center Hour
        if (_showCenterDisc) {
            canvas.drawCircle(centerX, centerY, minuteRadius * 0.7f, paints.centerDisc)
        }
        val hourText = String.format("%02d", if (hours == 0) 12 else hours)
        val hourYOffset = (paints.hour.descent() + paints.hour.ascent()) / 2f
        canvas.drawText(hourText, centerX, centerY - hourYOffset, paints.hour)

        // 2. Rings
        paints.ringLine.color = _ringColor
        drawRing(canvas, minuteRadius, minutes, minRotation)
        if (_showSeconds) drawRing(canvas, secondRadius, seconds, secRotation)

        // 3. Indicator Pill
        drawIndicator(canvas, minutes, seconds)
    }

    private fun drawIndicator(canvas: Canvas, minutes: Int, seconds: Int) {
        canvas.save()
        canvas.rotate(_pillAngle, centerX, centerY)
        
        paints.pillStroke.color = _ringColor
        val pillHalfHeight = _pillTextSize * 1.1f
        val pillWidth = (secondRadius - minuteRadius) + 130f
        val pillLeft = centerX + minuteRadius - 40f
        
        pillRect.set(pillLeft, centerY - pillHalfHeight, pillLeft + pillWidth, centerY + pillHalfHeight)
        canvas.drawRoundRect(pillRect, pillHalfHeight, pillHalfHeight, paints.pillStroke)

        val textYOffset = (paints.pillText.descent() + paints.pillText.ascent()) / 2f
        
        // Render text without rotation inside the indicator
        canvas.save()
        val mTextX = centerX + minuteRadius + 45f
        canvas.translate(mTextX, centerY)
        canvas.rotate(-_pillAngle)
        canvas.drawText(String.format("%02d", minutes), 0f, -textYOffset, paints.pillText)
        canvas.restore()

        if (_showSeconds) {
            canvas.save()
            val sTextX = centerX + secondRadius + 45f
            canvas.translate(sTextX, centerY)
            canvas.rotate(-_pillAngle)
            canvas.drawText(String.format("%02d", seconds), 0f, -textYOffset, paints.pillText)
            canvas.restore()
        }
        
        canvas.restore()
    }

    private fun drawRing(canvas: Canvas, radius: Float, current: Int, rotation: Float) {
        val pillHalfHeight = _pillTextSize * 1.1f
        val gapAngleDeg = Math.toDegrees(Math.atan2(pillHalfHeight.toDouble(), radius.toDouble())).toFloat()

        canvas.save()
        canvas.rotate(_pillAngle - rotation, centerX, centerY)
        
        for (i in 0..59) {
            val tickAngle = i * 6f
            var relAngle = (tickAngle - rotation) % 360f
            if (relAngle > 180f) relAngle -= 360f
            if (relAngle < -180f) relAngle += 360f
            
            // Skip ticks covered by the pill
            if (Math.abs(relAngle) < gapAngleDeg + 1.5f) continue

            canvas.save()
            canvas.rotate(tickAngle, centerX, centerY)
            
            val isMajor = i % 5 == 0
            val isActive = i == current
            
            paints.ringLine.alpha = when {
                isActive -> 255
                isMajor -> 160
                else -> 70
            }
            
            val lineLength = if (isMajor) 35f else 20f
            canvas.drawLine(centerX + radius, centerY, centerX + radius + lineLength, centerY, paints.ringLine)
            
            if (_showRingNumbers && isMajor) {
                drawRingNumber(canvas, radius, lineLength, tickAngle, rotation, i, isActive)
            }
            canvas.restore()
        }
        canvas.restore()
    }

    private fun drawRingNumber(canvas: Canvas, radius: Float, lineLength: Float, tickAngle: Float, rotation: Float, value: Int, isActive: Boolean) {
        canvas.save()
        canvas.translate(centerX + radius + lineLength + 40f, centerY)
        canvas.rotate(-(tickAngle + (_pillAngle - rotation)), 0f, 0f)
        
        paints.ringNum.alpha = if (isActive) 255 else 130
        val numOffset = (paints.ringNum.descent() + paints.ringNum.ascent()) / 2f
        canvas.drawText(String.format("%02d", value), 0f, -numOffset, paints.ringNum)
        canvas.restore()
    }

    private fun drawDateBlock(canvas: Canvas) {
        if (!_showDate) return
        val dateCX = centerX + _dateHorizontalOffset
        val dateCY = centerY + _dateVerticalOffset

        val dayStr = dayFormat.format(calendar.time)
        val dateStr = dateFormat.format(calendar.time)

        val dayHeight = paints.day.textSize
        val dateHeight = paints.date.textSize

        canvas.drawText(dayStr, dateCX, dateCY - (dayHeight * 0.2f), paints.day)
        canvas.drawText(dateStr, dateCX, dateCY + (dateHeight * 1.1f), paints.date)
    }

    private fun drawCustomText(canvas: Canvas) {
        if (!_showCustomText || _customText.isEmpty()) return
        val x = centerX + _customTextHorizontalOffset
        val y = centerY + _customTextVerticalOffset
        canvas.drawText(_customText, x, y, paints.customText)
    }

    // --- Public Configuration APIs ---
    fun setClockColor(color: Int) { 
        _clockColor = color
        paints.hour.color = color
        paints.pillText.color = color
        paints.ringNum.color = color
        updateDimensions()
        invalidate() 
    }
    
    fun setRingColor(color: Int) { 
        _ringColor = color
        paints.ringLine.color = color
        paints.pillStroke.color = color
        invalidate() 
    }

    fun setRingNumberColor(color: Int) {
        _ringNumColor = color
        paints.ringNum.color = color
        invalidate()
    }

    fun setAuraColor(color: Int) {
        _auraColor = color
        paints.aura.color = color
        invalidate()
    }

    fun setCenterDiscColor(color: Int) {
        _centerDiscColor = color
        paints.centerDisc.color = color
        invalidate()
    }

    fun setShowCenterDisc(show: Boolean) { _showCenterDisc = show; invalidate() }
    fun setShowAura(show: Boolean) { _showAura = show; invalidate() }
    
    fun setDateColor(color: Int) { 
        _dateColor = color
        paints.day.color = color
        paints.date.color = color
        invalidate() 
    }

    fun setVerticalOffset(offset: Float) { 
        _verticalOffset = offset
        updateDimensions()
        invalidate() 
    }
    
    fun setHorizontalOffset(offset: Float) { 
        _horizontalOffset = offset
        updateDimensions()
        invalidate() 
    }
    
    fun setDateVerticalOffset(offset: Float) { _dateVerticalOffset = offset; invalidate() }
    fun setDateHorizontalOffset(offset: Float) { _dateHorizontalOffset = offset; invalidate() }
    
    fun setHourTextSize(size: Float) { 
        _hourTextSize = size
        paints.hour.textSize = size
        invalidate() 
    }
    
    fun setPillTextSize(size: Float) { 
        _pillTextSize = size
        paints.pillText.textSize = size
        invalidate() 
    }

    fun setShowClock(show: Boolean) { _showClock = show; invalidate() }
    fun setShowSeconds(show: Boolean) { _showSeconds = show; invalidate() }
    fun setShowDate(show: Boolean) { _showDate = show; invalidate() }
    fun setShowRingNumbers(show: Boolean) { _showRingNumbers = show; invalidate() }
    fun setClockScale(scale: Float) { _clockScale = scale; invalidate() }
    fun setPillAngle(angle: Float) { _pillAngle = angle; invalidate() }
    
    fun setDateTextSize(size: Float) { 
        paints.day.textSize = size + 10f
        paints.date.textSize = size
        invalidate() 
    }

    fun setShowCustomText(show: Boolean) { _showCustomText = show; invalidate() }
    fun setCustomText(text: String) { _customText = text; invalidate() }
    fun setCustomTextSize(size: Float) { 
        _customTextSize = size
        paints.customText.textSize = size
        invalidate() 
    }

    fun setCustomTextVerticalOffset(offset: Float) { _customTextVerticalOffset = offset; invalidate() }
    fun setCustomTextHorizontalOffset(offset: Float) { _customTextHorizontalOffset = offset; invalidate() }
    
    fun setRingThickness(thickness: Float) { 
        _ringThickness = thickness
        paints.ringLine.strokeWidth = thickness
        invalidate() 
    }
    
    fun setPillThickness(thickness: Float) { 
        _pillThickness = thickness
        paints.pillStroke.strokeWidth = thickness
        invalidate() 
    }

    fun setCustomTextFont(font: String) {
        _customTextFontFamily = font
        updateTypefaces()
    }
    fun setCustomTextColor(color: Int) { 
        paints.customText.color = color
        invalidate() 
    }

    fun setClockFont(family: String) { _clockFontFamily = family; updateTypefaces() }
    fun setDateFont(family: String) { _dateFontFamily = family; updateTypefaces() }
    fun setFontStyle(style: Int) { _fontStyle = style; updateTypefaces() }

    private fun updateTypefaces() {
        clockTypeface = FontManager.resolveTypeface(_clockFontFamily, _fontStyle)
        dateTypeface = FontManager.resolveTypeface(_dateFontFamily, _fontStyle)
        customTextTypeface = FontManager.resolveTypeface(_customTextFontFamily, _fontStyle)

        paints.hour.typeface = clockTypeface
        paints.pillText.typeface = clockTypeface
        paints.ringNum.typeface = clockTypeface

        paints.day.typeface = dateTypeface
        paints.date.typeface = dateTypeface
        paints.customText.typeface = customTextTypeface

        invalidate()
    }
}
