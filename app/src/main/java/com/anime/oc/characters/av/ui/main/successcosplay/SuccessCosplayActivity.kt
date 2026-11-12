package com.anime.oc.characters.av.ui.main.successcosplay

import com.anime.oc.characters.av.core.extention.*
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.anime.oc.characters.av.core.base.BaseActivity
import com.anime.oc.characters.av.R
import com.anime.oc.characters.av.databinding.ActivitySuccessCosplayBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuccessCosplayActivity : BaseActivity<ActivitySuccessCosplayBinding, SuccessCosplayViewModel>( ActivitySuccessCosplayBinding::inflate, SuccessCosplayViewModel::class.java) {
    override fun onContentReady(view: View, savedInstanceState: Bundle?) {
        super.onContentReady(view, savedInstanceState)
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        this@SuccessCosplayActivity.onBackPressedDispatcher.addCallback(
            this@SuccessCosplayActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    restartShow()
                }
            }
        )
    }


    override fun initView() {
        binding.apply {
            setupActionBar()

            val userBitmap = appSession.userResultBitmap
            if (userBitmap != null && !userBitmap.isRecycled) {
                imvImage.setImageBitmap(userBitmap)
            }

            val percent = appSession.cosplayPercent.coerceIn(0, 100)
            imgLevel.setImageResource(
                when {
                    percent >= 95 -> R.drawable.img_level3
                    percent >= 45 -> R.drawable.img_level2
                    else -> R.drawable.img_level1
                }
            )
        }
    }

    private fun ActivitySuccessCosplayBinding.setupActionBar() {
        actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setImageActionBar(btnActionBarCenter2, R.drawable.ic_home)

        }
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.onClick { restartShow() }
        binding.actionBar.btnActionBarCenter2.onClick { openHome() }
    }

    private fun restartShow() {
        if (!isScreenAvailable || isNavigating) return
        appSession.shouldRestartShow = true
        popBack()
    }

    override fun observeData() {}

    override fun bindViewModel() {}
}
