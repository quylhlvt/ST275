package com.anime.oc.characters.avatar.core.extention

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Join
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.anime.oc.characters.avatar.R
import ir.kotlin.quyhcolorpicker.dp
class OuterStrokeTextView : AppCompatTextView {

    private var outerStrokeWidth = 0f
    private var outerStrokeColor: Int = Color.WHITE
    private var outerStrokeJoin: Join = Join.ROUND
    private var strokeMiter = 5f
    private var extraPadding = 0
    private var isInternalDrawing = false

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs == null) return

        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.OuterStrokeTextView
        )

        try {
            outerStrokeWidth = a.getDimension(
                R.styleable.OuterStrokeTextView_outerStrokeWidth,
                0f
            )

            outerStrokeColor = a.getColor(
                R.styleable.OuterStrokeTextView_outerStrokeColor,
                Color.WHITE
            )

            outerStrokeJoin = when (a.getInt(
                R.styleable.OuterStrokeTextView_outerStrokeJoinStyle, 2)) {
                0 -> Join.MITER
                1 -> Join.BEVEL
                2 -> Join.ROUND
                else -> Join.ROUND
            }
        } finally {
            a.recycle()
        }
        if (outerStrokeWidth > 0f) {
            extraPadding = (outerStrokeWidth * dp(5)).toInt()
        }
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // ✅ Apply sau khi XML padding đã được set xong
        if (extraPadding > 0) {
            setPadding(
                paddingLeft + extraPadding,
                paddingTop + extraPadding,
                paddingRight + extraPadding,
                paddingBottom + extraPadding
            )
            extraPadding = 0  // reset tránh apply 2 lần
        }
    }
    override fun onDraw(canvas: Canvas) {
        if (outerStrokeWidth <= 0f) {
            super.onDraw(canvas)
            return
        }

        val textPaint = paint
        val originalTextColors = textColors
        val originalStyle = textPaint.style
        val originalStrokeWidth = textPaint.strokeWidth
        val originalStrokeJoin = textPaint.strokeJoin
        val originalStrokeMiter = textPaint.strokeMiter
        val originalAntiAlias = textPaint.isAntiAlias

        isInternalDrawing = true
        try {
            super.setTextColor(outerStrokeColor)
            textPaint.style = Paint.Style.STROKE
            textPaint.strokeWidth = outerStrokeWidth * dp(1.5)
            textPaint.strokeJoin = outerStrokeJoin
            textPaint.strokeMiter = strokeMiter
            textPaint.isAntiAlias = true
            super.onDraw(canvas)

            super.setTextColor(originalTextColors)
            textPaint.style = Paint.Style.FILL
            super.onDraw(canvas)
        } finally {
            super.setTextColor(originalTextColors)
            textPaint.style = originalStyle
            textPaint.strokeWidth = originalStrokeWidth
            textPaint.strokeJoin = originalStrokeJoin
            textPaint.strokeMiter = originalStrokeMiter
            textPaint.isAntiAlias = originalAntiAlias
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
