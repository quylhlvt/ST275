package com.anime.oc.characters.avatar.core.extention

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.res.ResourcesCompat
import com.anime.oc.characters.avatar.R

class CurvedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPath = Path()
    private val oval = RectF()
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)

    private var text = ""
    private var textSize = 28f * resources.displayMetrics.scaledDensity
    private var textColor = Color.WHITE
    private var curveDepth = 0.12f
    private var centerAngle = -90f
    private var strokeWidth = 0f
    private var strokeColor = Color.BLACK
    private var fontFamily: Typeface? = null

    private var scrollOffset = 0f
    private var textWidth = 0f
    private var isMarqueeRunning = false
    private val marqueeSpeed = 1.5f
    private val handler = Handler(Looper.getMainLooper())

    private var clipLeft = 0f
    private var clipRight = 0f
    private var hasValidSize = false  // ← THÊM FLAG NÀY

    private val marqueeRunnable = object : Runnable {
        override fun run() {
            if (!isMarqueeRunning) return
            scrollOffset += marqueeSpeed
            val gap = textWidth * 1.5f
            if (scrollOffset > textWidth + gap) scrollOffset = 0f
            invalidate()
            handler.postDelayed(this, 8)
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.CurvedTextView)
            text        = a.getString(R.styleable.CurvedTextView_android_text) ?: ""
            textSize    = a.getDimension(R.styleable.CurvedTextView_android_textSize, textSize)
            textColor   = a.getColor(R.styleable.CurvedTextView_android_textColor, Color.WHITE)
            curveDepth  = a.getFloat(R.styleable.CurvedTextView_curveDepth, 0.12f)
            centerAngle = a.getFloat(R.styleable.CurvedTextView_arc_centerAngle2, -90f)
            strokeWidth = a.getDimension(R.styleable.CurvedTextView_arc_strokeWidth, 0f)
            strokeColor = a.getColor(R.styleable.CurvedTextView_arc_strokeColor, Color.BLACK)
            val fontRes = a.getResourceId(R.styleable.CurvedTextView_android_fontFamily, -1)
            if (fontRes != -1) fontFamily = ResourcesCompat.getFont(context, fontRes)
            a.recycle()
        }
        paintText.apply {
            this.textSize = this@CurvedTextView.textSize
            color         = textColor
            typeface      = fontFamily
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return  // ← GUARD: bỏ qua nếu chưa có size thực

        hasValidSize = true
        val paddingLeftRatio  = 36f / 314f
        val paddingRightRatio = 36f / 314f
        clipLeft  = w * paddingLeftRatio
        clipRight = w * (1f - paddingRightRatio)

        buildArcPath(w.toFloat(), h.toFloat())

        // Luôn restart check khi size thay đổi
        handler.removeCallbacks(marqueeRunnable)
        isMarqueeRunning = false
        checkMarquee()
    }

    private fun buildArcPath(w: Float, h: Float) {
        val rise   = w * curveDepth
        val radius = (w * w) / (8f * rise) + rise / 2f
        val cx     = w / 2f
        val arcCy  = h + radius - rise - rise - rise / 2f

        paintText.textSize = textSize
        textWidth = paintText.measureText(text)

        val circumference = (2 * Math.PI * radius).toFloat()
        val textAngle     = textWidth * 360f / circumference

        oval.set(cx - radius, arcCy - radius, cx + radius, arcCy + radius)

        textPath.reset()
        val ribbonWidth = clipRight - clipLeft
        if (textWidth > ribbonWidth * 0.9f) {
            // Text dài → marquee
            textPath.addArc(oval, centerAngle - textAngle * 2f, textAngle * 5f)
            scrollOffset = 0f
        } else {
            // ← Arc vừa đúng bằng textAngle, bắt đầu tại centerAngle - textAngle/2
            // → text tự động nằm giữa tại hOffset = 0
            textPath.addArc(oval, centerAngle - textAngle / 2f, textAngle * 2f)
            scrollOffset = 0f  // ← offset 0 là đúng giữa rồi
        }
    }

    private fun checkMarquee() {
        if (!hasValidSize) return  // ← GUARD: không check nếu chưa layout

        val ribbonWidth = clipRight - clipLeft
        paintText.textSize = textSize
        textWidth = paintText.measureText(text)

        val needsMarquee = text.isNotEmpty() && textWidth > ribbonWidth * 0.9f
        if (needsMarquee && !isMarqueeRunning) {
            isMarqueeRunning = true
            scrollOffset = 0f
            handler.post(marqueeRunnable)
        } else if (!needsMarquee && isMarqueeRunning) {
            isMarqueeRunning = false
            scrollOffset = 0f
            handler.removeCallbacks(marqueeRunnable)
        }
    }
    override fun onDraw(canvas: Canvas) {
        if (text.isEmpty() || !hasValidSize) return

        val fm      = paintText.fontMetrics
        val vOffset = -(fm.ascent + fm.descent) / 2f

        // ← BỎ clipRect hoàn toàn khi text ngắn không marquee
        if (isMarqueeRunning) {
            canvas.save()
            canvas.clipRect(clipLeft, 0f, clipRight, height.toFloat())

            if (strokeWidth > 0f) {
                paintText.apply {
                    style            = Paint.Style.STROKE
                    this.strokeWidth = this@CurvedTextView.strokeWidth
                    strokeJoin       = Paint.Join.ROUND
                    color            = strokeColor
                }
                val gap = textWidth * 1.5f
                canvas.drawTextOnPath(text, textPath, -scrollOffset, vOffset, paintText)
                canvas.drawTextOnPath(text, textPath, -(scrollOffset - textWidth - gap), vOffset, paintText)
            }

            paintText.apply {
                style = Paint.Style.FILL
                color = textColor
            }
            val gap = textWidth * 1.5f
            canvas.drawTextOnPath(text, textPath, -scrollOffset, vOffset, paintText)
            canvas.drawTextOnPath(text, textPath, -(scrollOffset - textWidth - gap), vOffset, paintText)

            canvas.restore()
        } else {
            // Text ngắn — không clip, vẽ 1 bản tại offset 0
            if (strokeWidth > 0f) {
                paintText.apply {
                    style            = Paint.Style.STROKE
                    this.strokeWidth = this@CurvedTextView.strokeWidth
                    strokeJoin       = Paint.Join.ROUND
                    color            = strokeColor
                }
                canvas.drawTextOnPath(text, textPath, 0f, vOffset, paintText)
            }
            paintText.apply {
                style = Paint.Style.FILL
                color = textColor
            }
            canvas.drawTextOnPath(text, textPath, 0f, vOffset, paintText)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (hasValidSize) {
            post { checkMarquee() }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isMarqueeRunning = false
        handler.removeCallbacks(marqueeRunnable)
    }

    fun startMarqueeWhenVisible() {
        if (hasValidSize) {
            handler.removeCallbacks(marqueeRunnable)
            isMarqueeRunning = false
            checkMarquee()
        } else {
            // View chưa measure → đợi layout xong
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    checkMarquee()
                }
            })
        }
    }

    fun setText(newText: String) {
        this.text = newText
        if (hasValidSize) {
            buildArcPath(width.toFloat(), height.toFloat())
            handler.removeCallbacks(marqueeRunnable)
            isMarqueeRunning = false
            checkMarquee()
        }
        invalidate()
    }

    fun setTypeface(tf: Typeface?) {
        fontFamily = tf
        paintText.typeface = tf
        invalidate()
    }
}