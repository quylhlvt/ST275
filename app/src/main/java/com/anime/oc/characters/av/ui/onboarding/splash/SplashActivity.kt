package com.anime.oc.characters.av.ui.onboarding.splash

import com.anime.oc.characters.av.core.extention.*
import android.animation.ValueAnimator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.anime.oc.characters.av.AppSession
import com.anime.oc.characters.av.core.base.BackPressHandler
import com.anime.oc.characters.av.core.base.BaseActivity
import com.anime.oc.characters.av.core.helper.SharedPreferencesManager
import com.anime.oc.characters.av.R
import com.anime.oc.characters.av.databinding.ActivitySplashBinding

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>(
    ActivitySplashBinding::inflate,
    SplashViewModel::class.java
) , BackPressHandler {
    private val mainViewModel: AppSession get() = appSession
    private var pendingNavigate = false
    private var navigateJob: Job? = null
//    var interCallBack: InterCallback? = null
    private var progressAnimator: ValueAnimator? = null
    private var currentOverlayFraction = 1f
    private var hasNavigated = false

    companion object {
        private const val MIN_SPLASH_MS  = 3_000L
        private const val API_TIMEOUT_MS = 8_000L
    }

    // ── INIT ──────────────────────────────────────────────────────────────────

    override fun initView() {
//        Admob.getInstance().setTimeLimitShowAds(30000)
//        Admob.getInstance().setTimeCountdownNativeCollab(20000)
//        Admob.getInstance().loadInterAll(this, getString(R.string.inter_all))
        preloadLanguageDrawables()


//        interCallBack = object : InterCallback() {
//            override fun onNextAction() {
//                super.onNextAction()
//                viewModel.triggerNavigate()
//            }
//        }
//
//        Admob.getInstance().loadSplashInterAds(
//            this@SplashActivity, getString(R.string.inter_splash), 30000, 3000, interCallBack
//        )
    }

    private fun preloadLanguageDrawables() {
        val requests = com.bumptech.glide.Glide.with(applicationContext)
        listOf(
            R.drawable.ic_flag_hindi, R.drawable.ic_flag_spanish,
            R.drawable.ic_flag_french, R.drawable.ic_flag_english,
            R.drawable.ic_flag_portugeese, R.drawable.ic_flag_indo,
            R.drawable.ic_flag_germani, R.drawable.ic_select_lang,
            R.drawable.ic_un_select_lang, R.drawable.select_language, R.drawable.back_app
        ).forEach { resource ->
            requests.load(resource)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.RESOURCE)
                .override(64, 64)
                .preload()
        }
        appSession.notifyImagesReady()
    }

    override fun viewListener() {}

    // Trong Fragment, observeData():
    override fun observeData() {
        viewModel.startSplashTimer(
            hasNetwork = isNetworkAvailable(),
            hasOnlineTemplates = mainViewModel.templates.value.any { it.id.startsWith("online_") },
            templatesFlow = mainViewModel.templates,
            imagesReadyFlow = mainViewModel.imagesReady,
            localDataReadyFlow = mainViewModel.localDataReady
        )
        this@SplashActivity.lifecycleScope.launch {
            viewModel.dataReadySignal.first { it }
            viewModel.triggerNavigate()
        }

    }

    override fun bindViewModel() {}


    // ── PROGRESS ──────────────────────────────────────────────────────────────

    // ── NAVIGATE ──────────────────────────────────────────────────────────────

    private fun goToHome() {
        if (hasNavigated) return
        hasNavigated = true  // ✅ set trước

        if (!isScreenAvailable || isDestroyed || isFinishing || isDestroyed) {
            pendingNavigate = true  // ✅ defer sang onResume
            return
        }
        doNavigate()
    }
    private fun doNavigate() {
        if (!SharedPreferencesManager.isLanuageScreen()) { toLanguage(); return }
        toIntro()
    }
    // ── NETWORK ───────────────────────────────────────────────────────────────

    private fun isNetworkAvailable(): Boolean = try {
        val cm = this@SplashActivity.getSystemService(ConnectivityManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    } catch (e: Exception) { false }

    // ── LIFECYCLE ─────────────────────────────────────────────────────────────
    override fun onPause() {
        super.onPause()
        progressAnimator?.pause()
        navigateJob?.cancel()  // ✅ Cancel khi pause, onResume sẽ tạo lại
    }

    override fun onResume() {
        super.onResume()
        progressAnimator?.resume()

        if (hasNavigated) return

        // ✅ Check ngay nếu đã ready
        if (viewModel.navigateSignal.value) {
            goToHome()
            return
        }

        // ✅ Cancel job cũ trước khi tạo mới, tránh chồng chéo
        navigateJob?.cancel()
        navigateJob = this@SplashActivity.lifecycleScope.launch {
            viewModel.navigateSignal.first { it }
            goToHome()
        }
    }

    override fun onDestroy() {
        navigateJob?.cancel()
        navigateJob = null
        progressAnimator?.cancel()
        progressAnimator = null
        super.onDestroy()
    }

    override fun onScreenBackPressed(): Boolean {
        return true
    }
}
