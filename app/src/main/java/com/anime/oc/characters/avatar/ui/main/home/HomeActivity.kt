package com.anime.oc.characters.avatar.ui.main.home

import com.anime.oc.characters.avatar.core.extention.*
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.anime.oc.characters.avatar.AppSession
import com.anime.oc.characters.avatar.core.base.BackPressHandler
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.core.navigation.Screen
import com.anime.oc.characters.avatar.core.helper.RateHelper
import com.anime.oc.characters.avatar.utils.state.RateState
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.ActivityHomeBinding
import com.anime.oc.characters.avatar.utils.BlockableFrameLayout
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding, HomeViewModel>(
    ActivityHomeBinding::inflate, HomeViewModel::class.java
), BackPressHandler {

    private val mainViewModel: AppSession get() = appSession
    private var countRate = 0


    override fun initView() {
        Admob.getInstance().loadNativeAll(this@HomeActivity, getString(R.string.native_all))
        initNativeCollab()
        binding.rcvMain.adapter = HomeMenuAdapter(::onHomeMenuClick)
        binding.actionBar.apply {
            setImageActionBar(btnActionBarRight, R.drawable.ic_settings)
        }
    }
    private fun initNativeCollab() {
        loadNativeCollabAds(
            getString(R.string.native_cl_home), binding.flNativeCollab
        )
    }


    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (intent.getBooleanExtra(com.anime.oc.characters.avatar.core.extention.EXTRA_OPEN_MY_CREATIONS, false)) {
            intent.removeExtra(com.anime.oc.characters.avatar.core.extention.EXTRA_OPEN_MY_CREATIONS)
            openScreen(Screen.MY_PONY)
        }
        Log.d("PERF2", "HomeActivity onResume: ${System.currentTimeMillis()}")
    }

    override fun onRestart() {
        super.onRestart()
        initNativeCollab()
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarRight.onClick { toSettingFromHome() }
    }

    private fun onHomeMenuClick(action: HomeMenuAction) {
        when (action) {
            HomeMenuAction.CREATE -> navigateWithCheck(Screen.CHOOSE_PONY)

            HomeMenuAction.COSPLAY -> navigateWithCheck(Screen.COSPLAY)
            HomeMenuAction.RANDOM -> navigateWithCheck(Screen.RANDOM)
            HomeMenuAction.MY_ALBUM -> {
                Admob.getInstance().showInterAll(this@HomeActivity, object : InterCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        openScreen(Screen.MY_PONY)
                    }
                })
            }
        }
    }

    private fun navigateWithCheck(destination: Screen) {
        val hasNetwork = InternetExtension.isInternetAvailable(this@HomeActivity) &&
                InternetExtension.isNetworkConnected(this@HomeActivity)
        val hasData = appSession.templates.value.isNotEmpty()

        when {
            !hasNetwork -> showUnstableNetworkDialog()
            !hasData -> {
                mainViewModel.fetchOnlineTemplates()
                showLoadingDataDialog()
            }

            else -> openScreen(destination)
        }
    }

    override fun observeData() {
        binding.root.post {
            Log.d("PERF2", "HomeActivity first frame: ${System.currentTimeMillis()}")
            if (!isScreenAvailable || isDestroyed) return@post
            this@HomeActivity.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mainViewModel.error.collect { error ->
                        error?.let { Log.e("HomeActivity", "❌ $it") }
                    }
                }
            }
        }
    }

    override fun bindViewModel() {}

    override fun onScreenBackPressed(): Boolean {
        countRate = sharedPreferences.isBackRequest() + 1
        sharedPreferences.setBackRequest(countRate)
        if (!sharedPreferences.isRateRequest() && countRate % 2 == 0) {
            RateHelper.showRateDialog(this@HomeActivity, sharedPreferences) { state ->
                if (state != RateState.CANCEL) showToast(R.string.have_rated)
                this@HomeActivity.finishAffinity()
                Process.killProcess(Process.myPid())
                exitProcess(0)
            }
        } else {
            this@HomeActivity.finishAffinity()
            exitProcess(0)
        }
        return true
    }
}
