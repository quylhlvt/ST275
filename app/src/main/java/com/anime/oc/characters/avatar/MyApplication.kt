package com.anime.oc.characters.avatar

import android.app.Application
import android.util.Log
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp                     // QUAN TRỌNG NHẤT – KHÔNG ĐƯỢC THIẾU
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val mmkvDir = File(filesDir, "mmkv_store").also { it.mkdirs() }
//        AppOpenManager.getInstance().disableAppResumeWithActivity(MyApplication::class.java)

        MMKV.initialize(this, mmkvDir.absolutePath)
        Log.d("MyApplication", "MMKV initialized at: ${mmkvDir.absolutePath}")
    }

}
