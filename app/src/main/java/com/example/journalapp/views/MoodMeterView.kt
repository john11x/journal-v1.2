package com.example.journalapp.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.journalapp.R

class MoodMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnMoodChangeListener {
        fun onMoodChanged(moodValue: Int)
    }

    private var listener: OnMoodChangeListener? = null
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var trackHeight = 8f.dpToPx()
    private var thumbRadius = 24f.dpToPx()
    private var thumbPosition = 0.5f
    private var isDragging = false

    private val moodLabels = listOf("Angry", "Sad", "Neutral", "Happy", "Ecstatic")
    private val moodColors = listOf(
        ContextCompat.getColor(context, R.color.mood_angry),
        ContextCompat.getColor(context, R.color.mood_sad),
        ContextCompat.getColor(context, R.color.mood_neutral),
        ContextCompat.getColor(context, R.color.mood_happy),
        ContextCompat.getColor(context, R.color.mood_ecstatic)
    )

    init {
        trackPaint.style = Paint.Style.FILL
        trackPaint.strokeWidth = trackHeight

        thumbPaint.color = ContextCompat.getColor(context, R.color.purple_200)
        thumbPaint.style = Paint.Style.FILL

        textPaint.color = Color.WHITE
        textPaint.textSize = 36f.dpToPx()
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        labelPaint.color = Color.WHITE
        labelPaint.textSize = 12f.dpToPx()
        labelPaint.textAlign = Paint.Align.CENTER
    }

    fun setMoodChangeListener(listener: OnMoodChangeListener) {
        this.listener = listener
    }

    fun setCurrentMood(moodValue: Int) {
        thumbPosition = (moodValue + 10) / 20f
        invalidate()
    }

    fun resetMood() {
        thumbPosition = 0.5f
        invalidate()
        listener?.onMoodChanged(0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val left = paddingStart + thumbRadius
        val right = width - paddingEnd - thumbRadius
        val centerY = height / 2f

        // Draw mood gradient track
        val gradient = LinearGradient(
            left, centerY, right, centerY,
            moodColors.toIntArray(),
            null,
            Shader.TileMode.CLAMP
        )
        trackPaint.shader = gradient

        canvas.drawRoundRect(
            left,
            centerY - trackHeight/2,
            right,
            centerY + trackHeight/2,
            trackHeight/2,
            trackHeight/2,
            trackPaint
        )

        // Draw thumb
        val thumbX = left + (right - left) * thumbPosition
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbPaint)

        // Draw mood label
        val moodIndex = (thumbPosition * (moodLabels.size - 1)).toInt()
        canvas.drawText(
            moodLabels[moodIndex],
            width / 2f,
            centerY - thumbRadius - 20f.dpToPx(),
            textPaint
        )

        // Draw mood scale labels
        canvas.drawText("Negative", left, centerY + thumbRadius + 30f.dpToPx(), labelPaint)
        canvas.drawText("Positive", right, centerY + thumbRadius + 30f.dpToPx(), labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isInThumb(event.x, event.y)) {
                    isDragging = true
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateThumbPosition(event.x)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density

    private fun isInThumb(x: Float, y: Float): Boolean {
        val thumbX = paddingStart + thumbRadius + (width - paddingStart - paddingEnd - 2 * thumbRadius) * thumbPosition
        val centerY = height / 2f
        return Math.sqrt(
            Math.pow((x - thumbX).toDouble(), 2.0) +
                    Math.pow((y - centerY).toDouble(), 2.0)
        ) <= thumbRadius
    }

    private fun updateThumbPosition(x: Float) {
        val left = paddingStart + thumbRadius
        val right = width - paddingEnd - thumbRadius
        thumbPosition = ((x - left) / (right - left)).coerceIn(0f, 1f)

        val moodValue = (thumbPosition * 20 - 10).toInt()
        listener?.onMoodChanged(moodValue)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (thumbRadius * 4 + textPaint.textSize).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, height)
    }
}