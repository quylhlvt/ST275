package com.anime.oc.characters.avatar.core.extention

import android.content.Intent
import android.os.Bundle
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.core.navigation.Screen

const val EXTRA_FROM_SETTINGS = "from_settings"
const val EXTRA_OPEN_MY_CREATIONS = "open_my_creations"

/** Returns false for repeated clicks or callbacks after the source has finished. */
fun BaseActivity<*, *>.openScreen(
    screen: Screen,
    args: Bundle? = null,
    replaceCurrent: Boolean = false,
    resetTask: Boolean = false
): Boolean {
    if (!isScreenAvailable || isNavigating) return false
    val intent = Intent(this, screen.activityClass)
    args?.let(intent::putExtras)
    if (resetTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    markNavigating()
    hideGlobalLoading()
    hideGlobalDialog()
    startActivity(intent)
    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    if (replaceCurrent) finish()
    return true
}

fun BaseActivity<*, *>.popBack(): Boolean {
    if (!isScreenAvailable || isNavigating) return false
    markNavigating()
    finish()
    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    return true
}

fun BaseActivity<*, *>.openHome() = openScreen(Screen.HOME, resetTask = true)

/** Keep Home below the album and remove editors/results above it. */
fun BaseActivity<*, *>.openMyCreations() {
    if (!isScreenAvailable || isNavigating) return
    markNavigating()
    hideGlobalLoading()
    hideGlobalDialog()
    startActivity(Intent(this, Screen.HOME.activityClass).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(EXTRA_OPEN_MY_CREATIONS, true)
    })
    finish()
}

fun BaseActivity<*, *>.toLanguage() = openScreen(Screen.LANGUAGE, replaceCurrent = true)
fun BaseActivity<*, *>.toIntro() = openScreen(Screen.INTRO, replaceCurrent = true)
fun BaseActivity<*, *>.toPermission() = openScreen(Screen.PERMISSION, replaceCurrent = true)
fun BaseActivity<*, *>.toHome() = openScreen(Screen.HOME, replaceCurrent = true)
fun BaseActivity<*, *>.toHomeFromPermission() = toHome()
fun BaseActivity<*, *>.toLangFromSetting() = openScreen(Screen.LANGUAGE, Bundle().apply {
    putBoolean(EXTRA_FROM_SETTINGS, true)
})
fun BaseActivity<*, *>.toHomeFromSetting() = openHome()
fun BaseActivity<*, *>.toSettingFromLang() = popBack()
fun BaseActivity<*, *>.toIntroFromLanguage() = openScreen(Screen.INTRO, replaceCurrent = true)
fun BaseActivity<*, *>.toHomeFromLanguage() = openHome()
fun BaseActivity<*, *>.toSettingFromHome() = openScreen(Screen.SETTING)
