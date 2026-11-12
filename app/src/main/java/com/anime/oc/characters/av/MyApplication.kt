package com.anime.oc.characters.av

import android.app.Application
import android.util.Log
import com.anime.oc.characters.av.ui.onboarding.splash.SplashActivity
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp                     // QUAN TRỌNG NHẤT – KHÔNG ĐƯỢC THIẾU
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        isTablet = resources.configuration.smallestScreenWidthDp >= TABLET_MIN_WIDTH_DP
        // Fragment navigation must wait for the interstitial to close. The SDK's
        // default navigates while its full-screen/loading windows still own input.
//        Admob.getInstance().setOpenActivityAfterShowInterAds(false)
//        Admob.getInstance().setOpenShowAllAds(false)
        val mmkvDir = File(filesDir, "mmkv_store").also { it.mkdirs() }
//        Admob.getInstance().setShowAdsDeviceTest(true)
//        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)

        MMKV.initialize(this, mmkvDir.absolutePath)
        Log.d("MyApplication", "MMKV initialized at: ${mmkvDir.absolutePath}")
    }

    companion object {
        private const val TABLET_MIN_WIDTH_DP = 600

        var isTablet: Boolean = false
            private set
    }
}
