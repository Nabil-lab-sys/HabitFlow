package com.example.todo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Circular timer ring: a full track with a progress arc and a round knob that
 * follows the end of the arc, like a kitchen timer dial.
 */
class TimerRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.timer_track)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.timer_accent)
    }

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.timer_accent)
    }

    private val ringBounds = RectF()
    private var strokeWidth = dp(14f)
    private var knobRadius = dp(21f)
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    /** 0 = no time elapsed yet, 1 = session finished. */
    private var progress = 0f

    fun setProgress(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (clamped != progress) {
            progress = clamped
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(availableWidth, availableHeight).coerceAtMost(dp(340f).toInt())
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h).toFloat()
        strokeWidth = size * 0.045f
        knobRadius = size * 0.075f

        trackPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth

        centerX = w / 2f
        centerY = h / 2f
        radius = size / 2f - knobRadius - dp(2f)
        ringBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (radius <= 0f) return

        canvas.drawCircle(centerX, centerY, radius, trackPaint)

        if (progress <= 0f) return

        val startAngle = -90f
        val sweep = 360f * progress
        canvas.drawArc(ringBounds, startAngle, sweep, false, progressPaint)

        val endAngle = Math.toRadians((startAngle + sweep).toDouble())
        val knobX = centerX + radius * cos(endAngle).toFloat()
        val knobY = centerY + radius * sin(endAngle).toFloat()
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
