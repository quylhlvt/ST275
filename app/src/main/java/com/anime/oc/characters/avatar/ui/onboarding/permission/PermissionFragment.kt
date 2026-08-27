package com.anime.oc.characters.avatar.ui.onboarding.permission

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.anime.oc.characters.avatar.core.base.BackPressHandler
import com.anime.oc.characters.avatar.core.base.BaseFragment
import com.anime.oc.characters.avatar.core.extention.checkPermissions
import com.anime.oc.characters.avatar.core.extention.goToSettings
import com.anime.oc.characters.avatar.core.extention.gone
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.requestPermission
import com.anime.oc.characters.avatar.core.extention.select
import com.anime.oc.characters.avatar.core.extention.setTextActionBar
import com.anime.oc.characters.avatar.core.extention.toHomeFromPermission
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.core.helper.PermissionHelper
import com.anime.oc.characters.avatar.core.helper.StringHelper
import com.anime.oc.characters.avatar.utils.key.RequestKey
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.FragmentPermissionBinding
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.callback.NativeCallback
import com.lvt.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
class PermissionFragment : BaseFragment<FragmentPermissionBinding, PermissionViewModel>(
    FragmentPermissionBinding::inflate, PermissionViewModel::class.java
), BackPressHandler {
    // Some tablet builds dismiss the system permission popup when tapping
    // outside and return an empty result. Track that request across onResume.
    private var pendingPermissionRequest = false
    private var pendingStorageRequest = false
    private var inter: InterstitialAd? = null
    override fun viewListener() {
        binding.swPermission.onClick(1500) { handlePermissionRequest(isStorage = true) }
        binding.swNotification.onClick(1500) { handlePermissionRequest(isStorage = false) }
        binding.tvContinue.onClick(1000) {
            Admob.getInstance().showInterAds(requireActivity(), inter, object : InterCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    handleContinue()}
            })
        }
    }
    private fun isNetworkAvailable(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }
    private fun updateContinueMargin() {
        val marginPx = if (!isNetworkAvailable()) {
            resources.getDimensionPixelSize(R.dimen.dimension_200)
        } else {
            resources.getDimensionPixelSize(R.dimen.dimension_10)
        }
        val params = binding.tvContinue.layoutParams as? ViewGroup.MarginLayoutParams
        params?.bottomMargin = marginPx  // hoặc topMargin tuỳ layout
        binding.tvContinue.layoutParams = params
    }
    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentPermissionBinding = FragmentPermissionBinding.inflate(inflater, container, false)

    override fun initView() {
        updateContinueMargin()
        Admob.getInstance().loadInterAds(requireContext(), getString(R.string.inter_per), object : InterCallback() {
            override fun onAdLoadSuccess(interstitialAd: InterstitialAd?) {
                super.onAdLoadSuccess(interstitialAd)
                inter = interstitialAd
            }
        })
        Admob.getInstance().loadNativeAd(requireContext(), getString(R.string.native_per), object : NativeCallback() {
            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                binding.nativeAds.gone()
            }

            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                super.onNativeAdLoaded(nativeAd)
                binding.nativeAds.visible()
                val adView =
                    LayoutInflater.from(requireContext()).inflate(R.layout.ads_native_medium_btn_bottom_2, null) as NativeAdView
                binding.nativeAds.removeAllViews()
                binding.nativeAds.addView(adView)
                Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
            }
        })
        binding.setupActionBar()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            binding.btnStorage.visible()
            binding.btnNotification.gone()
            binding.space.gone()
        } else {
            binding.btnNotification.visible()
            binding.btnStorage.gone()
            binding.space.gone()
        }
        // cập nhật UI switch khi vào màn
        updatePermissionUI(requireContext().checkPermissions(PermissionHelper.storagePermission), true)
        updatePermissionUI(requireContext().checkPermissions(PermissionHelper.notificationPermission), false)
    }

    private fun FragmentPermissionBinding.setupActionBar() {
        actionBar.apply {
            tvStart.select()
            setTextActionBar(tvStart, getString(R.string.permission))
        }
    }

// ❌ Xóa 2 dòng này
// private var storageDenyCount = 0
// private var notificationDenyCount = 0

    private fun handlePermissionRequest(isStorage: Boolean) {
        val perms = if (isStorage) PermissionHelper.storagePermission
        else PermissionHelper.notificationPermission

        when {
            requireContext().checkPermissions(perms) ->
                showToast(if (isStorage) R.string.granted_storage else R.string.granted_notification)

            // ✅ Dùng ViewModel thay vì local count
            viewModel.shouldGoToSettings(isStorage) -> activity?.goToSettings()

            else -> {
                pendingPermissionRequest = true
                pendingStorageRequest = isStorage
                requestPermission(
                    perms,
                    if (isStorage) RequestKey.STORAGE_PERMISSION_CODE
                    else RequestKey.NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // A dismissed system popup may not invoke onRequestPermissionsResult.
        if (pendingPermissionRequest) {
            val isStorage = pendingStorageRequest
            val permissions = if (isStorage) PermissionHelper.storagePermission
            else PermissionHelper.notificationPermission
            if (!requireContext().checkPermissions(permissions)) {
                if (isStorage) viewModel.onStorageDenied()
                else viewModel.onNotificationDenied()
            }
            pendingPermissionRequest = false
        }
        // ✅ Cập nhật lại UI khi quay về từ Settings hoặc sau khi grant
        updatePermissionUI(
            requireContext().checkPermissions(PermissionHelper.storagePermission),
            true
        )
        updatePermissionUI(
            requireContext().checkPermissions(PermissionHelper.notificationPermission),
            false
        )
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val requestWasPending = pendingPermissionRequest
        pendingPermissionRequest = false

        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when (requestCode) {
            RequestKey.STORAGE_PERMISSION_CODE -> {
                if (granted) {
                    viewModel.onStorageGranted()
                } else if (requestWasPending) {
                    viewModel.onStorageDenied()
                }
                // ✅ Luôn update UI dù granted hay denied
                updatePermissionUI(granted, true)
            }
            RequestKey.NOTIFICATION_PERMISSION_CODE -> {
                if (granted) {
                    viewModel.onNotificationGranted()
                } else if (requestWasPending) {
                    viewModel.onNotificationDenied()
                }
                // ✅ Luôn update UI dù granted hay denied
                updatePermissionUI(granted, false)
            }
        }
    }

    private fun updatePermissionUI(granted: Boolean, isStorage: Boolean) {
        val imageView = if (isStorage) binding.swPermission else binding.swNotification
        imageView.setImageResource(if (granted) R.drawable.switch_on else R.drawable.switch_off)
    }


    override fun observeData() {}

    override fun initText() {
        binding.actionBar.tvCenter.select()
        val textRes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            R.string.to_access_13 else R.string.to_access

        binding.txtPermission.text = buildString {
            append(getString(R.string.allow))
            append(" ")
            append(getString(R.string.app_name))
            append(" ")
            append(getString(textRes))
        }
    }

    private fun handleContinue() {
        sharedPreferences.setPermissionScreen(true)
        toHomeFromPermission()
    }

    override fun bindViewModel() {}

    private fun createColoredText(
        @StringRes textRes: Int,
        @ColorRes colorRes: Int,
        font: Int = R.font.baloo2_bold
    ) = StringHelper.changeColor(requireContext(), getString(textRes), colorRes, font)

    override fun onBackPressed(): Boolean {
        requireActivity().finishAffinity()
        exitProcess(0)
        return true
    }
}
