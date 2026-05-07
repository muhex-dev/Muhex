package com.example.myapplication

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.AttrRes
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

/**
 * MuhexScroller: A custom sidebar index with a "liquid" bending effect.
 */
class MuhexScroller @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- CONFIGURATION (Adjustable via Settings) ---
    var edgePaddingRatio = 0.12f
    var maxBendingDistance = 300f
    var curveSpread = 7.5f
    var baseTextSize = 28f
    var selectedTextScale = 2.4f
    var touchSlop = 80f
    var lineOffsetFromLetter = 40f
    var hapticEnabled = true
    var customColor: Int? = null
    var lineStrokeWidth = 3f
    var lineMaxAlpha = 90
    var baseLetterAlpha = 130
    var animationDuration = 250L

    // --- CALLBACKS ---
    var onLetterSelected: ((Char) -> Unit)? = null
    var onActionUp: (() -> Unit)? = null

    private var alphabet = listOf<Char>()
    private val textBounds = Rect()
    private var selectedIndex = -1
    private var focusIndex = -1f
    private var isTouching = false
    private var interpolation = 0f
    private var interpolationAnimator: ValueAnimator? = null

    private val linePath = Path()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
        invalidate()
    }

    private var cachedColor = Color.GRAY

    private fun resolveColorAttr(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        val materialAttr = com.google.android.material.R.attr.colorOnSurface
        return if (context.theme.resolveAttribute(materialAttr, typedValue, true)) {
            typedValue.data
        } else {
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            typedValue.data
        }
    }

    fun setAlphabet(list: List<Char>) {
        this.alphabet = list
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateColorsAndGradient()
    }

    fun updateColorsAndGradient() {
        if (height == 0) return
        cachedColor = customColor ?: resolveColorAttr(com.google.android.material.R.attr.colorOnSurface)
        
        val colors = intArrayOf(Color.TRANSPARENT, cachedColor, Color.TRANSPARENT)
        val positions = floatArrayOf(0.0f, 0.5f, 1.0f)

        linePaint.strokeWidth = lineStrokeWidth
        linePaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            colors, positions,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (alphabet.isEmpty()) return

        val itemHeight = height / alphabet.size.toFloat()
        val restingX = width - (width * edgePaddingRatio)

        val xPositions = FloatArray(alphabet.size)
        var maxIntensity = 0f

        for (i in alphabet.indices) {
            val proximity = if (focusIndex == -1f) 100f else abs(i - focusIndex)
            val curveIntensity = if (focusIndex == -1f) 0f else exp(-(proximity.pow(2) / curveSpread)) * interpolation
            
            if (curveIntensity > maxIntensity) maxIntensity = curveIntensity
            xPositions[i] = restingX - (curveIntensity * maxBendingDistance)
        }

        if (interpolation > 0.01f) {
            drawLiquidLine(canvas, xPositions, restingX, itemHeight, maxIntensity)
        }

        drawLetters(canvas, xPositions, itemHeight)
    }

    private fun drawLiquidLine(canvas: Canvas, xPositions: FloatArray, restingX: Float, itemHeight: Float, maxIntensity: Float) {
        linePath.reset()
        val lineAnchorX = restingX + lineOffsetFromLetter
        linePath.moveTo(lineAnchorX, 0f)

        for (i in 0 until alphabet.size - 1) {
            val currentY = (i * itemHeight) + (itemHeight / 2f)
            val currentX = xPositions[i] + lineOffsetFromLetter

            val nextY = ((i + 1) * itemHeight) + (itemHeight / 2f)
            val nextX = xPositions[i + 1] + lineOffsetFromLetter

            val controlY = (currentY + nextY) / 2f
            linePath.cubicTo(currentX, controlY, nextX, controlY, nextX, nextY)
        }

        linePath.lineTo(lineAnchorX, height.toFloat())
        linePaint.alpha = (maxIntensity * lineMaxAlpha).toInt().coerceIn(0, 255)
        canvas.drawPath(linePath, linePaint)
    }

    private fun drawLetters(canvas: Canvas, xPositions: FloatArray, itemHeight: Float) {
        textPaint.color = cachedColor
        
        for (i in alphabet.indices) {
            val proximity = if (focusIndex == -1f) 100f else abs(i - focusIndex)
            val curveIntensity = if (focusIndex == -1f) 0f else exp(-(proximity.pow(2) / curveSpread)) * interpolation

            val targetSize = baseTextSize + (curveIntensity * (baseTextSize * (selectedTextScale - 1f)))
            val animAlphaBoost = ((255 - baseLetterAlpha) * 0.2f * interpolation).toInt()
            val currentBaseAlpha = baseLetterAlpha + animAlphaBoost
            
            textPaint.textSize = targetSize
            textPaint.alpha = (currentBaseAlpha + (curveIntensity * (255 - currentBaseAlpha))).toInt().coerceIn(0, 255)

            val letter = alphabet[i].toString()
            textPaint.getTextBounds(letter, 0, letter.length, textBounds)
            val centerY = (i * itemHeight) + (itemHeight / 2f) - textBounds.exactCenterY()

            canvas.drawText(letter, xPositions[i], centerY, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val restingX = width - (width * edgePaddingRatio)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x >= (restingX - touchSlop)) {
                    isTouching = true
                    animateInterpolation(1f)
                    updateSelection(event.y)
                } else return false
            }
            MotionEvent.ACTION_MOVE -> if (isTouching) updateSelection(event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTouching) {
                    isTouching = false
                    selectedIndex = -1
                    animateInterpolation(0f)
                    onActionUp?.invoke()
                }
            }
        }
        return true
    }

    private fun animateInterpolation(target: Float) {
        interpolationAnimator?.cancel()
        interpolationAnimator = ValueAnimator.ofFloat(interpolation, target).apply {
            duration = animationDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                interpolation = it.animatedValue as Float
                if (interpolation == 0f && !isTouching) focusIndex = -1f
                invalidate()
            }
            start()
        }
    }

    private fun updateSelection(y: Float) {
        val preciseIndex = (y / height * alphabet.size).coerceIn(0f, alphabet.size - 1f)
        focusIndex = preciseIndex
        
        val index = preciseIndex.toInt()
        if (index != selectedIndex) {
            selectedIndex = index
            onLetterSelected?.invoke(alphabet[selectedIndex])
            if (hapticEnabled) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
        invalidate()
    }
}
