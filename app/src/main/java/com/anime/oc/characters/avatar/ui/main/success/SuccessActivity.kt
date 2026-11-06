package com.anime.oc.characters.avatar.ui.main.success

import com.anime.oc.characters.avatar.core.extention.*
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.activity.viewModels
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.core.navigation.Screen
import com.anime.oc.characters.avatar.core.helper.PermissionRequestHelper
import com.anime.oc.characters.avatar.databinding.ActivitySuccessBinding
import com.anime.oc.characters.avatar.ui.onboarding.permission.PermissionViewModel
import com.anime.oc.characters.avatar.utils.share.SocialShareManager
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class SuccessActivity : BaseActivity<ActivitySuccessBinding, SuccessViewModel>(
    ActivitySuccessBinding::inflate,
    SuccessViewModel::class.java
) {
    private val storageHelper = PermissionRequestHelper()

    private val permissionViewModel: PermissionViewModel by viewModels()
    private val socialShareManager by lazy(LazyThreadSafetyMode.NONE) {
        SocialShareManager(this@SuccessActivity)
    }
    private var currentImagePath: String = ""
    private val imagePath: String by lazy { intent.extras?.getString("imagePath") ?: "" }
    private val avatarUrl: String by lazy { intent.extras?.getString("avatarUrl") ?: "" }

    private val imageType: Int    by lazy { intent.extras?.getInt("imageType", 0) ?: 0 }
    private val idEdit: String    by lazy { intent.extras?.getString("idEdit") ?: "" }

    private val windowFocusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
        if (hasFocus && isScreenResumed && contentView != null) restoreSuccessInteractions()
    }
    private fun initNativeCollab() {
        loadNativeCollabAds(
            getString(R.string.native_success), binding.flNativeCollab
        )
    }

    override fun onResume() {
        super.onResume()
        restoreSuccessInteractions()
        val successView = binding.root
        successView.post {
            if (contentView === successView && isScreenResumed) restoreSuccessInteractions()
        }
    }

    override fun onContentReady(view: View, savedInstanceState: Bundle?) {
        super.onContentReady(view, savedInstanceState)
        view.viewTreeObserver.addOnWindowFocusChangeListener(windowFocusListener)
    }

    override fun onDestroy() {
        contentView?.viewTreeObserver?.takeIf { it.isAlive }
            ?.removeOnWindowFocusChangeListener(windowFocusListener)
        super.onDestroy()
    }

    private fun restoreSuccessInteractions() {
        // Covers first entry after an ad as well as returning from sharing/settings.
        hideLoadingSafe()
        hideGlobalDialogSafe()
        restoreWindowInteractions()
        restoreViewInteractions()
    }



    private fun restoreViewInteractions() {
        binding.root.isEnabled = true
        binding.actionBar.root.isEnabled = true
        binding.actionBar.btnActionBarLeft.isEnabled = true
        binding.actionBar.btnActionBarLeft.isClickable = true
        binding.actionBar.btnActionBarNextToRight.isEnabled = true
        binding.actionBar.btnActionBarNextToRight.isClickable = true
        binding.actionBar.btnActionBarRight.isEnabled = true
        binding.actionBar.btnActionBarRight.isClickable = true
        binding.btnBottomLeft.isEnabled = true
        binding.btnBottomLeft.isClickable = true
        binding.btnBottomRight.isEnabled = true
        binding.btnBottomRight.isClickable = true
        binding.btnBottomLeftSocial.isEnabled = true
        binding.btnBottomLeftSocial.isClickable = true
        binding.btnBottomRightSocial.isEnabled = true
        binding.btnBottomRightSocial.isClickable = true
        binding.root.requestLayout()
        binding.root.invalidate()
    }

    private fun restoreWindowInteractions() {
        this@SuccessActivity.window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        this@SuccessActivity.window.decorView.isEnabled = true
    }

    override fun initView() {
        initNativeCollab()
        currentImagePath = imagePath
        binding.apply {
            setImageActionBar(actionBar.btnActionBarLeft, R.drawable.back_app)
            loadImage(this@SuccessActivity, imagePath, imvImage)
            txtLeftSocial.apply  {
                isSelected =true
            }
            txtRightSocial.apply  {
                isSelected =true
            }
            txtLeft.apply  {
                isSelected =true
                visible();}
            txtRight.apply {
                isSelected =true
                visible();}
            setImageActionBar(actionBar.btnActionBarNextToRight, R.drawable.ic_share_success)
            setImageActionBar(actionBar.btnActionBarRight, R.drawable.ic_home)
            setTextActionBar(actionBar.tvCenter, getString(R.string.successful))
            tvSuccess.isSelected = true
        }
    }
    private  fun showInter(action: (()-> Unit)){
        Admob.getInstance().showInterAll(this@SuccessActivity, object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                action()

            }
        })
    }
    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.onClick1 { popBack() }

            // Home
            actionBar.btnActionBarRight.onClick1 {
                showInter {
                    openHome()
                }
            }
            // Share
            actionBar.btnActionBarNextToRight.onClick( 1500) { shareImage() }

            // MyCreation
            btnBottomLeft.onClick1 {
                showInter {
                    openMyCreations()
                }
            }

            // Download
            btnBottomRight.onClick1 { downloadImage() }
            btnBottomLeftSocial.onClick1 {
                logSocialShareEvent("facebook")
                shareToSocialApp(SocialShareManager.SocialApp.FACEBOOK)
            }
            btnBottomRightSocial.onClick1 {
                logSocialShareEvent("instagram")
                shareToSocialApp(SocialShareManager.SocialApp.INSTAGRAM)
            }
        }
    }
    private fun logSocialShareEvent(socialName: String) {
        val dataName = Uri.parse(avatarUrl).pathSegments
            .dropLast(1)
            .lastOrNull()
            .orEmpty()

        logEventSocial(
            "click_share_$socialName",
            "click_share_${socialName}_$dataName",
            avatarUrl
        )
        Log.d("LogEvenA", "click_share_${socialName}_$dataName -- ${avatarUrl}")

    }
    private fun shareToSocialApp(app: SocialShareManager.SocialApp) {
        val path = currentImagePath.takeIf { it.isNotBlank() } ?: imagePath
        when (socialShareManager.shareImage(path, app)) {
            SocialShareManager.ShareResult.Started -> Unit
            SocialShareManager.ShareResult.ImageNotFound -> showToast(getString(R.string.image_not_found))
            is SocialShareManager.ShareResult.AppNotAvailable -> showToast(getString(if (app == SocialShareManager.SocialApp.FACEBOOK) R.string.facebook_not_available else R.string.instagram_not_available))
            is SocialShareManager.ShareResult.Failed -> showToast(getString(R.string.share_failed))
        }
    }
    private fun shareImage() {
        if (imagePath.isEmpty()) return
        val uri = FileProvider.getUriForFile(
            this@SuccessActivity,
            "${this@SuccessActivity.packageName}.provider",
            File(imagePath)
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }
// ViewActivity.kt

    // Thêm vào ViewActivity
    private fun downloadImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { performDownload(); return }
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            this@SuccessActivity.checkPermissions(arrayOf(permission)) -> performDownload()
            permissionViewModel.shouldGoToSettings(isStorage = true) -> {
                this@SuccessActivity.goToSettings()
            }
            else -> downloadPermissionLauncher.launch(arrayOf(permission))
        }
    }

    private val downloadPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                permissionViewModel.onStorageGranted()
                performDownload()
            } else {
                permissionViewModel.onStorageDenied()
                // ✅ Chỉ toast, KHÔNG check goToSettings ở đây
                // goToSettings sẽ được check ở downloadImage() lần nhấn tiếp theo
                showToast(getString(R.string.download_failed_please_try_again_later))
            }
        }
    private fun performDownload() {
        viewModel.downloadFile(this@SuccessActivity, imagePath) { success ->
            showToast(
                if (success) getString(R.string.download_success, getString(R.string.app_name))
                else getString(R.string.download_failed_please_try_again_later)
            )
        }
    }



    private fun showToast(msg: String) =
        Toast.makeText(this@SuccessActivity, msg, Toast.LENGTH_SHORT).show()

    override fun observeData() {
        appSession.updatedImagePaths.observe(this) { (characterId, newPath) ->
                if (idEdit.isEmpty() || characterId != idEdit || newPath.isEmpty()) return@observe
                currentImagePath = newPath
                loadImage(this@SuccessActivity, currentImagePath, binding.imvImage)
            }
    }
    override fun bindViewModel() {}
}
