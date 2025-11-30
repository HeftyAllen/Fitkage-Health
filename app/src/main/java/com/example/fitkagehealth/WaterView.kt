package com.example.fitkagehealth

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator

class WaterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var waterLevel = 0f
    private var targetLevel = 0f
    private var waveOffset = 0f
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        style = Paint.Style.FILL
    }

    fun setWaterLevel(level: Float) {
        targetLevel = level.coerceIn(0f, 1f) // Ensure the water level is between 0 and 1
        val animator = ValueAnimator.ofFloat(waterLevel, targetLevel)
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { waterLevel = it.animatedValue as Float; invalidate() }
        animator.start()
    }

    fun addWater(amount: Float) {
        waterLevel += amount
        if (waterLevel > 1f) waterLevel = 1f // Cap at 100%
        setWaterLevel(waterLevel)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val glassRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val radius = 40f
        val path = Path()
        path.moveTo(glassRect.left, glassRect.top + radius)
        path.lineTo(glassRect.left, glassRect.bottom - radius)
        path.quadTo(glassRect.left, glassRect.bottom, glassRect.left + radius, glassRect.bottom)
        path.lineTo(glassRect.right - radius, glassRect.bottom)
        path.quadTo(glassRect.right, glassRect.bottom, glassRect.right, glassRect.bottom - radius)
        path.lineTo(glassRect.right, glassRect.top + radius)
        canvas.drawPath(path, borderPaint)

        val waterTop = height * (1 - waterLevel)
        val wavePath = Path()
        val amplitude = 15f
        val wavelength = width / 1.5f
        wavePath.moveTo(0f, waterTop)
        var x = 0f
        while (x <= width) {
            val y = (waterTop + amplitude * Math.sin((x / wavelength + waveOffset) * Math.PI * 2)).toFloat()
            wavePath.lineTo(x, y)
            x += 10f
        }
        wavePath.lineTo(width.toFloat(), height.toFloat())
        wavePath.lineTo(0f, height.toFloat())
        wavePath.close()
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(glassRect, radius, radius, Path.Direction.CW) })
        canvas.drawPath(wavePath, waterPaint)
        canvas.restore()

        waveOffset += 0.01f
        if (waveOffset > 1f) waveOffset -= 1f
        postInvalidateOnAnimation()
    }
}