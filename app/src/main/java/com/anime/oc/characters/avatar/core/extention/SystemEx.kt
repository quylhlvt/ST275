package com.anime.oc.characters.avatar.core.extention

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment

fun Activity.hideNavigation(isBlack: Boolean = false) {
    // Vẽ nội dung xuyên ra sau status bar trong suốt, nhưng vẫn giữ
    // status bar của hệ thống (chỉ ẩn navigation bar).
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT

    // Allow the app background to fill the area around notches/water-drop cameras.
    // Without this, Android 9-14 can letterbox the window below the display cutout.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    // Khong re-apply systemUiVisibility ngay trong callback thay doi visibility.
    // Callback do co the lap lien tuc khi quay lai tu Settings/share Activity va
    // lam window bi ket trong qua trinh chuyen focus.
    window.decorView.setOnSystemUiVisibilityChangeListener(null)
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = isBlack
        isAppearanceLightNavigationBars = false
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        show(WindowInsetsCompat.Type.statusBars())
        hide(WindowInsetsCompat.Type.navigationBars())
    }
}
fun Fragment.hideSoftKeyboard() {
    val inputMethodManager = requireContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)
}
