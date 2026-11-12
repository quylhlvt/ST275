package com.anime.oc.characters.av.ui.main.setting

import com.anime.oc.characters.av.core.extention.*
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.anime.oc.characters.av.core.base.BaseActivity
import com.anime.oc.characters.av.core.helper.RateHelper
import com.anime.oc.characters.av.utils.state.RateState
import com.anime.oc.characters.av.R
import com.anime.oc.characters.av.databinding.ActivitySettingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingActivity : BaseActivity<ActivitySettingBinding, SettingViewModel>(
    ActivitySettingBinding::inflate,
    SettingViewModel::class.java
) {
    override fun onContentReady(view: View, savedInstanceState: Bundle?) {
        super.onContentReady(view, savedInstanceState)
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        this@SettingActivity.onBackPressedDispatcher.addCallback(
            this@SettingActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    popBack()
                }
            }
        )
    }

    override fun initView() {
        binding.apply {

            setupActionBar()
            setupActionTiltleBar()
            setupRateButton()
        }
    }

    private fun ActivitySettingBinding.setupActionBar() {
        actionBar.apply {
            setImageActionBar(
                btnActionBarLeft,
                R.drawable.back_app
            )
        }
    }

    private fun ActivitySettingBinding.setupActionTiltleBar() {
        binding.apply {
            txt1.isSelected = true
            txt2.isSelected = true
            txt3.isSelected = true
            txt4.isSelected = true
        }
    }

    private fun ActivitySettingBinding.setupRateButton() {
        if (sharedPreferences.isRateRequest()) {
            btnRate.gone()
        } else {
            btnRate.visible()
        }
    }

    override fun viewListener() {
        binding.apply {
            setupActionBarListeners()
            setupNavigationListeners()
        }
    }

    private fun ActivitySettingBinding.setupActionBarListeners() {
        actionBar.btnActionBarLeft.onClick {
            popBack()
        }
    }

    private fun ActivitySettingBinding.setupNavigationListeners() {
        btnLang.onClick {
            toLangFromSetting()
        }
        btnPolicy.onClick {
            policy()
        }
        btnRate.onClick {
            RateHelper.showRateDialog(this@SettingActivity, sharedPreferences) { state ->
                if (state != RateState.CANCEL) {
                    btnRate.gone()
                    showToast(R.string.have_rated)
                }
            }
        }
        btnShare.onClick {
            shareApp()
        }
    }

    override fun observeData() {}
    override fun bindViewModel() {}
}