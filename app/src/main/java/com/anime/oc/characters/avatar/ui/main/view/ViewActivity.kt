package com.anime.oc.characters.avatar.ui.main.view

import com.anime.oc.characters.avatar.core.extention.*
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.activity.viewModels
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.core.navigation.Screen
import com.anime.oc.characters.avatar.core.helper.PermissionRequestHelper
import com.anime.oc.characters.avatar.ui.main.customize.CustomizeActivity
import com.anime.oc.characters.avatar.ui.onboarding.permission.PermissionViewModel
import com.anime.oc.characters.avatar.utils.share.SocialShareManager
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isInternetAvailable
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isNetworkConnected
import com.anime.oc.characters.avatar.databinding.ActivityViewBinding
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlin.compareTo

@AndroidEntryPoint
class ViewActivity : BaseActivity<ActivityViewBinding, ViewViewModel>(
    ActivityViewBinding::inflate,
    ViewViewModel::class.java
) {
    private val storageHelper = PermissionRequestHelper()

    private val permissionViewModel: PermissionViewModel by viewModels()
    private val socialShareManager by lazy(LazyThreadSafetyMode.NONE) {
        SocialShareManager(this@ViewActivity)
    }
    private var currentImagePath: String = ""
    private var isReturningFromExternalScreen = false
    private val imagePath: String by lazy { intent.extras?.getString("imagePath") ?: "" }
    private val imageType: Int by lazy { intent.extras?.getInt("imageType", 0) ?: 0 }
    private val idEdit: String by lazy { intent.extras?.getString("idEdit") ?: "" }


    override fun onResume() {
        super.onResume()
        hideLoadingSafe()
        hideGlobalDialogSafe()
        if (!isReturningFromExternalScreen) return

        restoreWindowInteractions()
        binding.root.post {
            if (!isScreenAvailable || contentView == null) return@post
            restoreWindowInteractions()
            restoreViewInteractions()
        }
        binding.root.postDelayed({
            if (!isScreenAvailable || contentView == null) return@postDelayed
            restoreWindowInteractions()
            restoreViewInteractions()
            isReturningFromExternalScreen = false
        }, EXTERNAL_SCREEN_RESTORE_DELAY_MS)
    }

    private fun restoreViewInteractions() {
        if (!isScreenAvailable || contentView == null) return

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
        this@ViewActivity.window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        this@ViewActivity.window.decorView.isEnabled = true
    }
    private fun initNativeCollab() {
        loadNativeCollabAds(
            getString(R.string.native_cl_detail), binding.flNativeCollab
        )
    }
    override fun initView() {
        initNativeCollab()
        currentImagePath = imagePath
        binding.apply {
            setImageActionBar(actionBar.btnActionBarLeft, R.drawable.back_app)
            loadImage(this@ViewActivity, imagePath, imvImage)
            txtRight.isSelected = true
            txtLeft.isSelected = true
            txtLeftSocial.isSelected = true
            txtRightSocial.isSelected = true

            when (imageType) {
                1 -> {
                    txtLeft.text = getString(R.string.share)
                    setImageActionBar(actionBar.btnActionBarRight, R.drawable.ic_delete)
                    setImageActionBar(actionBar.btnActionBarNextToRight, R.drawable.ic_edit1)
                    txtRight.apply { visible(); text = getString(R.string.download) }
                    txtLeft.visible()
                }
                2 -> {
                    txtLeft.text = getString(R.string.share)
                    setImageActionBar(actionBar.btnActionBarRight, R.drawable.ic_delete)
                    txtRight.apply { visible(); text = getString(R.string.download) }
                    txtLeft.visible()
                }
            }
        }
    }
    private  fun showInter(action: (()-> Unit)){
        Admob.getInstance().showInterAll(this@ViewActivity, object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                action()

            }
        })
    }
    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.onClick { showInter {
                popBack()
            } }


            when (imageType) {
                1 -> {
                    actionBar.btnActionBarRight.onClick1 { confirmDelete() }
                    actionBar.btnActionBarNextToRight.onClick1 {
                        showInter {  navigateToEdit()}
                    }
                    btnBottomLeft.onClick(1500) { shareImage() }
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

                2 -> {
                    actionBar.btnActionBarRight.onClick1 { confirmDelete() }
                    btnBottomLeft.onClick(1500) { shareImage() }
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
        }
    }

    companion object {
        private const val EXTERNAL_SCREEN_RESTORE_DELAY_MS = 500L
    }

    private fun logSocialShareEvent(socialName: String) {
        val avatarPath = appSession.customizedCharacters.value
            .firstOrNull { it.id == idEdit }
            ?.avatar
            .orEmpty()
        val dataName = Uri.parse(avatarPath).pathSegments
            .dropLast(1)
            .lastOrNull()
            .orEmpty()
//
        logEventSocial(
            "click_share_$socialName",
            "click_share_${socialName}_$dataName",
            avatarPath
        )
        Log.d("LogEvenA", "click_share_${socialName}_$dataName -- ${avatarPath}")
    }
    private fun shareToSocialApp(app: SocialShareManager.SocialApp) {
        val path = currentImagePath.takeIf { it.isNotBlank() } ?: imagePath
        when (socialShareManager.shareImage(path, app)) {
            SocialShareManager.ShareResult.Started -> isReturningFromExternalScreen = true
            SocialShareManager.ShareResult.ImageNotFound -> showToast(getString(R.string.image_not_found))
            is SocialShareManager.ShareResult.AppNotAvailable -> showToast(getString(if (app == SocialShareManager.SocialApp.FACEBOOK) R.string.facebook_not_available else R.string.instagram_not_available))
            is SocialShareManager.ShareResult.Failed -> showToast(getString(R.string.share_failed))
        }
    }

    private fun shareImage() {
        if (imagePath.isEmpty()) return
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this@ViewActivity,
            "${this@ViewActivity.packageName}.provider",
            java.io.File(imagePath)
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.share)))
    }
// ViewActivity.kt

    // Thêm vào ViewActivity
    private fun downloadImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performDownload(); return
        }
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            this@ViewActivity.checkPermissions(arrayOf(permission)) -> performDownload()
            permissionViewModel.shouldGoToSettings(isStorage = true) -> {
                isReturningFromExternalScreen = true
                this@ViewActivity.goToSettings()
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
        viewModel.downloadFile(this@ViewActivity, imagePath) { success ->
            showToast(
                if (success) getString(R.string.download_success, getString(R.string.app_name))
                else getString(R.string.download_failed_please_try_again_later)
            )
        }
    }

    private fun confirmDelete() {
        showConfirmDialog(
            title = getString(R.string.delete),
            message = getString(R.string.are_you_sure_want_to_delete_this_item),
            onYes = {
                viewModel.deleteFile(
                    path = imagePath,
                    isAvatar = imageType == 1,
                    idEdit = idEdit,
                    onDone = {
                        popBack()
                    }
                )
            },
            onNo = null
        )
    }

    private fun navigateToEdit() {
        if (idEdit.isEmpty() || imageType != 1) return

        val customized = appSession.customizedCharacters.value
            .firstOrNull { it.id == idEdit }
            ?: run {
                showEditItemNotFoundDialog()
                return
            }

        val templateIndex = appSession.getTemplateIndexForCustomized(idEdit)
            .takeIf { it >= 0 }
            ?: run { showUnstableNetworkDialog(); return }  // ✅ không tìm thấy template → có thể do chưa load online

        val template = appSession.templates.value.getOrNull(templateIndex)

        // Không có template gốc thì không thể khôi phục selections để edit.
        if (template == null) {
            showUnstableNetworkDialog()
            return
        }

        // Template online cần cả Internet và kết nối mạng thực sự.
        if (template.id.startsWith("online_")) {
            val onlineTemplateCount = appSession.templates.value
                .count { it.id.startsWith("online_") }
            if (!isInternetAvailable(this@ViewActivity) ||
                !isNetworkConnected(this@ViewActivity) || onlineTemplateCount == 0
            ) {
                showUnstableNetworkDialog()
                return
            }
        }

        val args = CustomizeActivity.newArgs(
            templateIndex = templateIndex,
            isEdit = true,
            customizedId = idEdit,
            savedSelections = customized.selections.toCleanSelections(),
            isFlipped = customized.isFlipped
        )
            openScreen(Screen.CUSTOMIZE, args)

    }

    private fun showEditItemNotFoundDialog() {
        showOkDialog(
            title = getString(R.string.error),
            message = getString(R.string.errorcontent)
        )
    }

    private fun showToast(msg: String) =
        android.widget.Toast.makeText(this@ViewActivity, msg, android.widget.Toast.LENGTH_SHORT)
            .show()

    override fun observeData() {
        appSession.updatedImagePaths.observe(this) { (characterId, newPath) ->
                if (idEdit.isEmpty() || characterId != idEdit || newPath.isEmpty()) return@observe
                currentImagePath = newPath
                loadImage(this@ViewActivity, currentImagePath, binding.imvImage)
            }
    }

    override fun bindViewModel() {}
}
