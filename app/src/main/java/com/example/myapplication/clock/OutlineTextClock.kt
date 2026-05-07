package com.example.myapplication.clock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.TextClock

class OutlineTextClock @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextClock(context, attrs, defStyleAttr) {

    private var strokeWidthValue = 4f
    private var strokeColorValue = Color.WHITE

    override fun onDraw(canvas: Canvas) {
        // Draw Outline
        val originalColor = currentTextColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidthValue
        
        // Use current text color for the outline
        super.onDraw(canvas)

        // If you want it hollow, we are done. 
        // If you wanted fill, you would set style to FILL and call super again with a color.
    }
}