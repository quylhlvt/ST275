package com.anime.oc.characters.avatar.ui.language

import com.anime.oc.characters.avatar.core.extention.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.anime.oc.characters.avatar.core.base.BackPressHandler
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.core.helper.SharedPreferencesManager
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.ActivityLanguageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding, LanguageViewModel>(
    ActivityLanguageBinding::inflate, LanguageViewModel::class.java
), BackPressHandler {

    private val languageAdapter by lazy { LanguageAdapter(this@LanguageActivity) }
    private var isFromSetting = false

    override fun onScreenBackPressed(): Boolean {

        when {
            isFromSetting -> {
                toSettingFromLang()
            }
            SharedPreferencesManager.isLanuageScreen() -> {
                popBack()
            }
            else -> {
                this@LanguageActivity.finishAffinity()
                exitProcess(0)
            }
        }
        return true
    }
    override fun setupPreViews() {

        val isFirst = !SharedPreferencesManager.isLanuageScreen()


        binding.recycleLanguage.apply {
            adapter = languageAdapter
            itemAnimator = null
        }

        val currentLang = SharedPreferencesManager.isLanguageKey()
        viewModel.setFirstLanguage(isFirst = isFirst)
        viewModel.loadLanguages(currentLang)

        val list = viewModel.languageList.value
        if (list.isNotEmpty()) {
            languageAdapter.submitList(list)
        }
    }
    private fun updateActionBar(isFirst: Boolean) {
        binding.apply {
            if (isFirst) {
                setTextActionBar(actionBar.tvStart, getString(R.string.language))
                actionBar.btnActionBarRight.setImageResource(R.drawable.select_language)
            } else {
                setTextActionBar(actionBar.tvCenter, getString(R.string.language))
                actionBar.btnActionBarLeft.visible()
//                actionBar.btnActionBarRight.setImageResource(R.drawable.select_language)
            }
        }
        updateDoneButtonVisibility()
    }

    private fun updateDoneButtonVisibility() {
        val hasSelectedLanguage = viewModel.codeLang.value.isNotEmpty()

        // Nút tick trên action bar: chỉ dùng cho lần đầu (không từ Setting)
        if (!isFromSetting && hasSelectedLanguage) binding.actionBar.btnActionBarRight.visible()
        else binding.actionBar.btnActionBarRight.invisible()

        // btnSave: chỉ hiện khi vào từ Setting, lần đầu thì ẩn
        if (isFromSetting) binding.btnSave.visible() else binding.btnSave.gone()
    }
    override fun viewListener() {
        binding.apply {
            // handleDone guards navigation; a language tap must not debounce Save.
            actionBar.btnActionBarRight.setOnClickListener {
                handleDone()
            }
            binding.btnSave.setOnClickListener {
                handleDone()
            }
            actionBar.btnActionBarLeft.setOnClickListener {
                // Dùng chung logic với onBackPressed
                onScreenBackPressed()
            }

        }

        handleRcv()
    }


    override fun initView() {
        binding.apply {
            actionBar.btnActionBarLeft.invisible()
            actionBar.btnActionBarRight.setImageResource(R.drawable.select_language)
            txtSave.isSelected = true
        }
        isFromSetting = intent.getBooleanExtra(com.anime.oc.characters.avatar.core.extention.EXTRA_FROM_SETTINGS, false)
        binding.actionBar.apply {
            btnActionBarRight.invisible()
            btnActionBarLeft.setImageResource(R.drawable.back_app)

        }


        // ✅ Bỏ initRcv() — đã làm trong setupPreViews
        updateActionBar(viewModel.isFirstLanguage.value)
    }


    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isFirstLanguage.collect { isFirst ->
                        updateActionBar(isFirst)
                    }
                }
                launch {
                    viewModel.languageList.collect { list ->
                        if (list.isNotEmpty()) {
                            languageAdapter.submitList(list)
                        }
                    }
                }
                launch {
                    viewModel.codeLang.collect {
                        updateDoneButtonVisibility()
                    }
                }
            }
        }
    }



    override fun bindViewModel() {
    }

    private fun initRcv() {
        binding.recycleLanguage.apply {
            adapter = languageAdapter
            itemAnimator = null

        }
    }
    private fun handleRcv() {
        binding.apply {
            languageAdapter.onItemClick = { code ->
                viewModel.selectLanguage(code)
                updateDoneButtonVisibility()
            }
        }
    }

    private fun handleDone() {
        if (isNavigating) return
        val code = viewModel.codeLang.value
        if (code.isEmpty()) {
            showToast(R.string.not_select_lang)
            return
        }

        val isFirstLanguage = viewModel.isFirstLanguage.value && !isFromSetting
        sharedPreferences.saveLanguageSelection(code)

        // BaseActivity applies the saved locale when it creates the next screen.
        if (isFirstLanguage) {
            toIntroFromLanguage()
        } else {
            toHomeFromLanguage()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
    }

}
