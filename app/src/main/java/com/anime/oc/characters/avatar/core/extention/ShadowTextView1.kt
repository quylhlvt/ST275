package com.anime.oc.characters.avatar.core.extention

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class ShadowTextView1 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        val originalColor = textColors.defaultColor

        // Lớp shadow lan rộng
        repeat(10) {
            paint.color = Color.WHITE
            paint.setShadowLayer(20f, 0f, 0f, Color.WHITE)
            super.onDraw(canvas)
        }

        // Lớp shadow trung bình
        repeat(10) {
            paint.color = Color.WHITE
            paint.setShadowLayer(12f, 0f, 0f, Color.WHITE)
            super.onDraw(canvas)
        }

        // Lớp shadow sát chữ (đậm nhất)
        repeat(10) {
            paint.color = Color.WHITE
            paint.setShadowLayer(5f, 0f, 0f, Color.WHITE)
            super.onDraw(canvas)
        }

        // Vẽ chữ thật lên trên
        paint.color = originalColor
        paint.clearShadowLayer()
        super.onDraw(canvas)
    }
}