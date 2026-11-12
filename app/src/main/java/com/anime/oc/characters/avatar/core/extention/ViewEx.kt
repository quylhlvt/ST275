package com.anime.oc.characters.avatar.core.extention

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.anime.oc.characters.avatar.core.helper.CustomTypefaceSpan
import com.anime.oc.characters.avatar.utils.DataLocal
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

fun Int.dp(context: Context): Int =
    (this * context.resources.displayMetrics.density).roundToInt()

fun Number.dp(context: Context): Int =
    (this.toFloat() * context.resources.displayMetrics.density).roundToInt()
fun dpToPx(context: Context, dp: Int): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
}
fun View.dpToPx(dp: Int): Float {
    return dp * resources.displayMetrics.density
}
fun TextView.setFont(@FontRes resId: Int) {
    typeface = ResourcesCompat.getFont(context, resId)
}

fun Context.strings(resId: Int): String {
    return getString(resId)
}

fun setImageActionBar(imageView: ImageView, res: Int) {
    imageView.setImageResource(res)
    imageView.visible()
}

fun setMaterialCardViewActionBar1(materialCardView: FrameLayout, textView: TextView, text: String) {
    textView.text = text
    materialCardView.visible()
    textView.isSelected = true
}

fun setTextActionBar( textView: TextView, text: String) {
    textView.text = text
    textView.visible()
    textView.isSelected = true
}


fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.toggetShow() {
    visibility = if (visibility == View.VISIBLE) {
        View.INVISIBLE
    } else {
        View.VISIBLE
    }
}

fun View.select() {
    isSelected = true
}

fun View.onClick1(interval: Long = 500, action: (View) -> Unit) {
    setOnClickListener {
        val lastClickTime = (this.getTag(DataLocal.KEY_LAST_CLICK_TIME) as? Long) ?: 0L
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastClickTime >= interval) {
            action(it)

            this.setTag(DataLocal.KEY_LAST_CLICK_TIME, currentTime)
        }
    }
}
private val lastClickTime = AtomicLong(0L)

fun View.onClick(interval: Long = 200, action: (View) -> Unit) {
    setOnClickListener {
        val now = System.currentTimeMillis()
        if (lastClickTime.getAndSet(now).let { now - it >= interval }) {
            action(it)
        }
    }
}
fun View.drawToBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        width.coerceAtLeast(1),
        height.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    // Draw background nếu có
    background?.draw(canvas)

    // Draw view
    draw(canvas)

    return bitmap
}

/**
 * Check xem view đã được layout chưa
 */
fun View.isLaidOut(): Boolean {
    return width > 0 && height > 0
}
fun View.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

/**
 * Lưu Bitmap thành file PNG trong filesDir/avatars/.
 * Trả về absolute path hoặc null nếu lỗi.
 */
fun Bitmap.saveToFile(context: Context, prefix: String = "avatar"): String? {
    return try {
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "${prefix}_$timestamp.png")
        FileOutputStream(file).use { compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
fun changeText(
    context: Context,
    text: String,
    @ColorRes colorRes: Int,
    @FontRes fontFamily: Int
): SpannableString {

    val spannableString = SpannableString(text)

    // resolve color
    val colorInt = ContextCompat.getColor(context, colorRes)
    spannableString.setSpan(
        ForegroundColorSpan(colorInt),
        0,
        text.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    // font
    ResourcesCompat.getFont(context, fontFamily)?.let { font ->
        spannableString.setSpan(
            CustomTypefaceSpan("", font),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    return spannableString
}