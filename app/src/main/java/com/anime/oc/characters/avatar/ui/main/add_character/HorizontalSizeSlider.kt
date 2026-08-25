package com.anime.oc.characters.avatar.ui.main.add_character

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt

/** Slider kích thước nằm ngang: track nhỏ bên trái và lớn dần về bên phải. */
class HorizontalSizeSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var progress: Float = 0.5f
        set(value) {
            val newValue = value.coerceIn(0f, 1f)
            if (field == newValue) return
            field = newValue
            invalidate()
            onProgressChanged?.invoke(field)
        }

    var onProgressChanged: ((Float) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF8C66".toColorInt()
        style = Paint.Style.FILL
    }
    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
    }
    private val trackPath = Path()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Không giữ kích thước cố định; View sử dụng không gian do parent cấp.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = thumbRadius()
        val startX = paddingLeft + radius
        val endX = (width - paddingRight - radius).coerceAtLeast(startX)
        val contentHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0).toFloat()
        val centerY = paddingTop + contentHeight / 2f
        val smallHalfHeight = minOf(contentHeight * 0.10f, radius)
        val largeHalfHeight = minOf(contentHeight * 0.25f, radius)

        trackPath.reset()
        trackPath.moveTo(startX, centerY - smallHalfHeight)
        // Bo tròn đầu trái theo đúng độ dày của track.
        trackPath.cubicTo(
            startX - smallHalfHeight * 4f / 3f, centerY - smallHalfHeight,
            startX - smallHalfHeight * 4f / 3f, centerY + smallHalfHeight,
            startX, centerY + smallHalfHeight
        )
        trackPath.cubicTo(
            startX + (endX - startX) * 0.28f, centerY + smallHalfHeight,
            startX + (endX - startX) * 0.65f, centerY + largeHalfHeight,
            endX, centerY + largeHalfHeight
        )
        // Bo tròn đầu phải theo đúng độ dày của track.
        trackPath.cubicTo(
            endX + largeHalfHeight * 4f / 3f, centerY + largeHalfHeight,
            endX + largeHalfHeight * 4f / 3f, centerY - largeHalfHeight,
            endX, centerY - largeHalfHeight
        )
        trackPath.cubicTo(
            startX + (endX - startX) * 0.65f, centerY - largeHalfHeight,
            startX + (endX - startX) * 0.28f, centerY - smallHalfHeight,
            startX, centerY - smallHalfHeight
        )
        trackPath.close()
        canvas.drawPath(trackPath, trackPaint)

        val thumbX = startX + progress * (endX - startX)
        thumbStrokePaint.strokeWidth = minOf(contentHeight * 0.125f, radius * 0.3f)
        val strokeRadius = (radius - thumbStrokePaint.strokeWidth / 2f).coerceAtLeast(0f)
        canvas.drawCircle(thumbX, centerY, strokeRadius, thumbPaint)
        canvas.drawCircle(thumbX, centerY, strokeRadius, thumbStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                updateProgress(event.x)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updateProgress(event.x)
                return true
            }

            MotionEvent.ACTION_UP -> {
                updateProgress(event.x)
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateProgress(touchX: Float) {
        val radius = thumbRadius()
        val startX = paddingLeft + radius
        val trackLength = (width - paddingLeft - paddingRight - radius * 2f)
            .coerceAtLeast(1f)
        progress = (touchX - startX) / trackLength
    }

    private fun thumbRadius(): Float {
        val contentWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0) / 2f
        val contentHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0).toFloat()
        return minOf(contentWidth, contentHeight * 0.44f)
    }
}
