package com.anime.oc.characters.avatar.ui.language

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.anime.oc.characters.avatar.core.base.BackPressHandler
import com.anime.oc.characters.avatar.core.base.BaseFragment
import com.anime.oc.characters.avatar.core.extention.invisible
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.popBack
import com.anime.oc.characters.avatar.core.extention.setTextActionBar
import com.anime.oc.characters.avatar.core.extention.toHomeFromLanguage
import com.anime.oc.characters.avatar.core.extention.toIntroFromLanguage
import com.anime.oc.characters.avatar.core.extention.toSettingFromLang
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.core.helper.LanguageHelper
import com.anime.oc.characters.avatar.core.helper.SharedPreferencesManager
import com.anime.oc.characters.avatar.utils.LanguageManager
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.gone
import com.anime.oc.characters.avatar.databinding.FragmentLanguageBinding
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.lvt.ads.callback.NativeCallback
import com.lvt.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.system.exitProcess

@AndroidEntryPoint
class LanguageFragment : BaseFragment<FragmentLanguageBinding, LanguageViewModel>(
    FragmentLanguageBinding::inflate, LanguageViewModel::class.java
), BackPressHandler {
    private val languageAdapter by lazy { LanguageAdapter(requireContext()) }
    private var isFromSetting = false

    override fun onBackPressed(): Boolean {

        when {
            isFromSetting -> {
                toSettingFromLang()
            }
            SharedPreferencesManager.isLanuageScreen() -> {
                popBack()
            }
            else -> {
                requireActivity().finishAffinity()
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
        if (hasSelectedLanguage) {
            binding.actionBar.btnActionBarRight.visible()
        } else {
            binding.actionBar.btnActionBarRight.invisible()
        }
    }
    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.onClick {
                handleDone()
            }
            actionBar.btnActionBarLeft.onClick( 500) {
                // Dùng chung logic với onBackPressed
                onBackPressed()
            }

        }
        
        handleRcv()
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentLanguageBinding = FragmentLanguageBinding.inflate(inflater, container, false)

    override fun initView() {
        Admob.getInstance().loadNativeAd(
            requireContext(),
            getString(R.string.native_language),
            object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    if (!isAdded) return

                    Log.d("LanguageNativeAd", "Native ad loaded: ${nativeAd != null}")
                    val adView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.ads_native_big_btn_top, null) as NativeAdView
                    binding.nativeAds.visible()
                    binding.nativeAds.removeAllViews()
                    binding.nativeAds.addView(adView)
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)

                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    Log.e("LanguageNativeAd", "Native ad failed to load")
                    binding.nativeAds.gone()
                }
            }
        )
        binding.apply {
            actionBar.btnActionBarLeft.invisible()
            actionBar.btnActionBarRight.setImageResource(R.drawable.select_language)
        }
        isFromSetting = findNavController().previousBackStackEntry?.destination?.id == R.id.setting
        binding.actionBar.apply {
            btnActionBarRight.invisible()
            btnActionBarLeft.setImageResource(R.drawable.back_app)
            tvStart.isSelected = true
            tvCenter.isSelected = true
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
        val code = viewModel.codeLang.value
        if (code.isEmpty()) {
            showToast(R.string.not_select_lang)
            return
        }

        sharedPreferences.setLanguageKey(code)
        LanguageHelper.setLocale(requireContext(), code)
        LanguageManager.updateLanguage(code)

        if (viewModel.isFirstLanguage.value) {
            sharedPreferences.setLanuageScreen(true)
            Log.d("LANG", "Navigating to Intro")
            toIntroFromLanguage()
        }  else {
        // Update locale cho Activity context ngay lập tức
        val locale = Locale(code)
        val config = Configuration(requireActivity().resources.configuration)
        config.setLocale(locale)
        requireActivity().resources.updateConfiguration(config, requireActivity().resources.displayMetrics)

        // Rồi mới navigate
        toHomeFromLanguage()
    }
    }
}
