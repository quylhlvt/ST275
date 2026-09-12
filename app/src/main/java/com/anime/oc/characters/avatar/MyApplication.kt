package com.anime.oc.characters.avatar

import android.app.Application
import android.util.Log
import com.lvt.ads.util.Admob
import com.lvt.ads.util.AdsApplication
import com.lvt.ads.util.AppOpenManager
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp                     // QUAN TRỌNG NHẤT – KHÔNG ĐƯỢC THIẾU
class MyApplication : AdsApplication() {
    override fun onCreate() {
        super.onCreate()
        val mmkvDir = File(filesDir, "mmkv_store").also { it.mkdirs() }
//        Admob.getInstance().setShowAdsDeviceTest(true)
        AppOpenManager.getInstance().disableAppResumeWithActivity(MyApplication::class.java)

        MMKV.initialize(this, mmkvDir.absolutePath)
        Log.d("MyApplication", "MMKV initialized at: ${mmkvDir.absolutePath}")
    }
    override fun enableAdsResume(): Boolean {
        return true
    }

    override fun getListTestDeviceId(): MutableList<String>? {
        return null
    }

    override fun getResumeAdId(): String {
        return getString(R.string.open_resume)
    }

    override fun buildDebug(): Boolean {
        return true
    }

}
