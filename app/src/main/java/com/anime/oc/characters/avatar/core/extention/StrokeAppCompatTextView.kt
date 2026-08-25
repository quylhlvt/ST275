package com.anime.oc.characters.avatar.core.extention

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toColorInt
import com.anime.oc.characters.avatar.R

class StrokeAppCompatTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var strokeWidthPx = 0f
    private var strokeColor = Color.WHITE
    private var strokeJoin = Paint.Join.ROUND
    private var strokeMiter = 5f

    /*
     * Ngăn setTextColor() trong onDraw() tạo vòng lặp invalidate.
     */
    private var isInternalDrawing = false

    /*
     * Phủ nhẹ màu fill lên mép trong của stroke,
     * tránh anti-alias làm màu stroke liếm vào chữ.
     */
    private val fillEdgeCoverPx: Float
        get() = resources.displayMetrics.density * 0.35f

    init {
        readAttributes(attrs, defStyleAttr)
        updateLayerType()
    }

    private fun readAttributes(
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.StrokeTextView,
            defStyleAttr,
            0
        )

        try {
            strokeWidthPx = typedArray.getDimension(
                R.styleable.StrokeTextView_strokeWidth,
                0f
            )

            strokeColor = typedArray.getColor(
                R.styleable.StrokeTextView_strokeColor,
                Color.WHITE
            )

            strokeMiter = typedArray.getDimension(
                R.styleable.StrokeTextView_strokeMiter,
                5f
            )

            strokeJoin = when (
                typedArray.getInt(
                    R.styleable.StrokeTextView_strokeJoinStyle,
                    2
                )
            ) {
                0 -> Paint.Join.MITER
                1 -> Paint.Join.BEVEL
                else -> Paint.Join.ROUND
            }
        } finally {
            typedArray.recycle()
        }
    }

    private fun updateLayerType() {
        setLayerType(
            if (strokeWidthPx > 0f) {
                LAYER_TYPE_SOFTWARE
            } else {
                LAYER_TYPE_NONE
            },
            null
        )
    }

    /**
     * widthPx được tính bằng pixel.
     */
    fun setStroke(
        widthPx: Float,
        color: Int,
        join: Paint.Join = Paint.Join.ROUND,
        miter: Float = 5f
    ) {
        strokeWidthPx = widthPx.coerceAtLeast(0f)
        strokeColor = color
        strokeJoin = join
        strokeMiter = miter.coerceAtLeast(0f)

        updateLayerType()
        requestLayout()
        invalidate()
    }

    /**
     * widthDp được tính bằng dp.
     */
    fun setStrokeDp(
        widthDp: Float,
        color: Int,
        join: Paint.Join = Paint.Join.ROUND,
        miter: Float = 5f
    ) {
        setStroke(
            widthPx = widthDp * resources.displayMetrics.density,
            color = color,
            join = join,
            miter = miter
        )
    }

    fun setStrokeColor(color: Int) {
        if (strokeColor == color) return

        strokeColor = color
        invalidate()
    }

    fun setStrokeWidthPx(widthPx: Float) {
        val newWidth = widthPx.coerceAtLeast(0f)

        if (strokeWidthPx == newWidth) return

        strokeWidthPx = newWidth

        updateLayerType()
        requestLayout()
        invalidate()
    }

    fun setStrokeWidthDp(widthDp: Float) {
        setStrokeWidthPx(
            widthDp * resources.displayMetrics.density
        )
    }

    fun setStrokeTitle() {
        setStrokeDp(
            widthDp = 2.5f,
            color = "#FFFFFF".toColorInt(),
            join = Paint.Join.ROUND,
            miter = 5f
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (strokeWidthPx <= 0f || text.isNullOrEmpty()) {
            super.onDraw(canvas)
            return
        }

        val textPaint = paint
        val originalTextColors = textColors

        val originalStyle = textPaint.style
        val originalStrokeWidth = textPaint.strokeWidth
        val originalStrokeJoin = textPaint.strokeJoin
        val originalStrokeMiter = textPaint.strokeMiter
        val originalShader = textPaint.shader
        val originalAlpha = textPaint.alpha
        val originalAntiAlias = textPaint.isAntiAlias
        val originalXfermode = textPaint.xfermode

        val originalShadowRadius = shadowRadius
        val originalShadowDx = shadowDx
        val originalShadowDy = shadowDy
        val originalShadowColor = shadowColor

        isInternalDrawing = true

        try {
            // =====================================================
            // LƯỢT 1: Vẽ stroke trước
            // =====================================================

            super.setTextColor(strokeColor)

            textPaint.clearShadowLayer()
            textPaint.shader = null
            textPaint.xfermode = null
            textPaint.style = Paint.Style.STROKE
            textPaint.strokeWidth = strokeWidthPx
            textPaint.strokeJoin = strokeJoin
            textPaint.strokeMiter = strokeMiter
            textPaint.alpha = 255
            textPaint.isAntiAlias = true

            super.onDraw(canvas)

            // =====================================================
            // LƯỢT 2: Vẽ fill lên trên stroke
            // =====================================================

            super.setTextColor(originalTextColors)

            textPaint.shader = originalShader
            textPaint.xfermode = originalXfermode

            /*
             * Dùng FILL_AND_STROKE với stroke rất nhỏ cùng màu chữ.
             * Phần này che mép stroke bị anti-alias làm lem vào fill.
             */
            textPaint.style = Paint.Style.FILL_AND_STROKE
            textPaint.strokeWidth = minOf(
                fillEdgeCoverPx,
                strokeWidthPx * 0.25f
            )

            textPaint.strokeJoin = Paint.Join.ROUND
            textPaint.strokeMiter = originalStrokeMiter
            textPaint.alpha = originalAlpha
            textPaint.isAntiAlias = true

            textPaint.setShadowLayer(
                originalShadowRadius,
                originalShadowDx,
                originalShadowDy,
                originalShadowColor
            )

            super.onDraw(canvas)
        } finally {
            // =====================================================
            // Khôi phục trạng thái gốc
            // =====================================================

            super.setTextColor(originalTextColors)

            textPaint.style = originalStyle
            textPaint.strokeWidth = originalStrokeWidth
            textPaint.strokeJoin = originalStrokeJoin
            textPaint.strokeMiter = originalStrokeMiter
            textPaint.shader = originalShader
            textPaint.xfermode = originalXfermode
            textPaint.alpha = originalAlpha
            textPaint.isAntiAlias = originalAntiAlias

            textPaint.setShadowLayer(
                originalShadowRadius,
                originalShadowDx,
                originalShadowDy,
                originalShadowColor
            )

            isInternalDrawing = false
        }
    }

    override fun invalidate() {
        if (isInternalDrawing) return
        super.invalidate()
    }

    override fun postInvalidate() {
        if (isInternalDrawing) return
        super.postInvalidate()
    }
}