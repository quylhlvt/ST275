package com.anime.oc.characters.avatar.core.navigation

import android.app.Activity

/** Explicit destinations; Android owns the Activity back stack. */
enum class Screen(val activityClass: Class<out Activity>) {
    SPLASH(com.anime.oc.characters.avatar.ui.onboarding.splash.SplashActivity::class.java),
    LANGUAGE(com.anime.oc.characters.avatar.ui.language.LanguageActivity::class.java),
    INTRO(com.anime.oc.characters.avatar.ui.onboarding.intro.IntroActivity::class.java),
    PERMISSION(com.anime.oc.characters.avatar.ui.onboarding.permission.PermissionActivity::class.java),
    HOME(com.anime.oc.characters.avatar.ui.main.home.HomeActivity::class.java),
    CHOOSE_PONY(com.anime.oc.characters.avatar.ui.main.createPony.ChoosePonyActivity::class.java),
    MY_PONY(com.anime.oc.characters.avatar.ui.main.myPony.MyPonyActivity::class.java),
    CUSTOMIZE(com.anime.oc.characters.avatar.ui.main.customize.CustomizeActivity::class.java),
    ADD_CHARACTER(com.anime.oc.characters.avatar.ui.main.add_character.AddCharacterActivity::class.java),
    VIEW(com.anime.oc.characters.avatar.ui.main.view.ViewActivity::class.java),
    COSPLAY(com.anime.oc.characters.avatar.ui.main.cosplay.CosplayActivity::class.java),
    SHOW(com.anime.oc.characters.avatar.ui.main.show.ShowActivity::class.java),
    SUCCESS_COSPLAY(com.anime.oc.characters.avatar.ui.main.successcosplay.SuccessCosplayActivity::class.java),
    RANDOM(com.anime.oc.characters.avatar.ui.main.random.RandomActivity::class.java),
    SUCCESS(com.anime.oc.characters.avatar.ui.main.success.SuccessActivity::class.java),
    SETTING(com.anime.oc.characters.avatar.ui.main.setting.SettingActivity::class.java)
}
