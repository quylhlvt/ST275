package com.anime.oc.characters.avatar.ui.main.view

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.anime.oc.characters.avatar.core.base.BaseFragment
import com.anime.oc.characters.avatar.core.extention.InternetExtension
import com.anime.oc.characters.avatar.core.extention.checkPermissions
import com.anime.oc.characters.avatar.core.extention.goToSettings
import com.anime.oc.characters.avatar.core.extention.gone
import com.anime.oc.characters.avatar.core.extention.loadImage
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.onClick1
import com.anime.oc.characters.avatar.core.extention.safeNavigate
import com.anime.oc.characters.avatar.core.extention.setImageActionBar
import com.anime.oc.characters.avatar.core.extention.toCleanSelections
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.core.helper.PermissionRequestHelper
import com.anime.oc.characters.avatar.ui.main.customize.CustomizeFragment
import com.anime.oc.characters.avatar.ui.onboarding.permission.PermissionViewModel
import com.anime.oc.characters.avatar.utils.share.SocialShareManager
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isInternetAvailable
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isNetworkConnected
import com.anime.oc.characters.avatar.core.extention.loadNativeCollabAds
import com.anime.oc.characters.avatar.core.extention.logEventSocial
import com.anime.oc.characters.avatar.core.extention.showInter
import com.anime.oc.characters.avatar.databinding.FragmentViewBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlin.compareTo

@AndroidEntryPoint
class ViewFragment : BaseFragment<FragmentViewBinding, ViewViewModel>(
    FragmentViewBinding::inflate,
    ViewViewModel::class.java
) {
    private val storageHelper = PermissionRequestHelper()

    private val permissionViewModel: PermissionViewModel by activityViewModels()
    private val socialShareManager by lazy(LazyThreadSafetyMode.NONE) {
        SocialShareManager(requireContext())
    }
    private var currentImagePath: String = ""
    private var isReturningFromExternalScreen = false
    private val imagePath: String by lazy { arguments?.getString("imagePath") ?: "" }
    private val imageType: Int by lazy { arguments?.getInt("imageType", 0) ?: 0 }
    private val idEdit: String by lazy { arguments?.getString("idEdit") ?: "" }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentViewBinding = FragmentViewBinding.inflate(inflater, container, false)

    override fun onResume() {
        super.onResume()
        hideLoadingSafe()
        hideGlobalDialogSafe()
        if (!isReturningFromExternalScreen) return

        restoreWindowInteractions()
        binding.root.post {
            if (!isAdded || view == null) return@post
            restoreWindowInteractions()
            restoreViewInteractions()
        }
        binding.root.postDelayed({
            if (!isAdded || view == null) return@postDelayed
            restoreWindowInteractions()
            restoreViewInteractions()
            isReturningFromExternalScreen = false
        }, EXTERNAL_SCREEN_RESTORE_DELAY_MS)
    }

    private fun restoreViewInteractions() {
        if (!isAdded || view == null) return

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
        requireActivity().window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        requireActivity().window.decorView.isEnabled = true
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
            loadImage(requireContext(), imagePath, imvImage)
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

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.onClick { showInter {
                findNavController().navigateUp()
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
        val avatarPath = viewModelActivity.customizedCharacters.value
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
        Log.d("click_share", "click_share_${socialName}_$dataName -- ${avatarPath}")
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
            requireContext(),
            "${requireContext().packageName}.provider",
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
// ViewFragment.kt

    // Thêm vào ViewFragment
    private fun downloadImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performDownload(); return
        }
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            requireContext().checkPermissions(arrayOf(permission)) -> performDownload()
            permissionViewModel.shouldGoToSettings(isStorage = true) -> {
                isReturningFromExternalScreen = true
                activity?.goToSettings()
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
        viewModel.downloadFile(requireContext(), imagePath) { success ->
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
                        findNavController().navigateUp()
                    }
                )
            },
            onNo = null
        )
    }

    private fun navigateToEdit() {
        if (idEdit.isEmpty() || imageType != 1) return

        val customized = viewModelActivity.customizedCharacters.value
            .firstOrNull { it.id == idEdit }
            ?: run {
                showEditItemNotFoundDialog()
                return
            }

        val templateIndex = viewModelActivity.getTemplateIndexForCustomized(idEdit)
            .takeIf { it >= 0 }
            ?: run { showUnstableNetworkDialog(); return }  // ✅ không tìm thấy template → có thể do chưa load online

        val template = viewModelActivity.templates.value.getOrNull(templateIndex)

        // Không có template gốc thì không thể khôi phục selections để edit.
        if (template == null) {
            showUnstableNetworkDialog()
            return
        }

        // Template online cần cả Internet và kết nối mạng thực sự.
        if (template.id.startsWith("online_")) {
            val onlineTemplateCount = viewModelActivity.templates.value
                .count { it.id.startsWith("online_") }
            if (!isInternetAvailable(requireContext()) ||
                !isNetworkConnected(requireContext()) || onlineTemplateCount == 0
            ) {
                showUnstableNetworkDialog()
                return
            }
        }

        val args = CustomizeFragment.newArgs(
            templateIndex = templateIndex,
            isEdit = true,
            customizedId = idEdit,
            savedSelections = customized.selections.toCleanSelections(),
            isFlipped = customized.isFlipped
        )
            findNavController().safeNavigate(R.id.action_view_to_customize, args)

    }

    private fun showEditItemNotFoundDialog() {
        showOkDialog(
            title = getString(R.string.error),
            message = getString(R.string.errorcontent)
        )
    }

    private fun showToast(msg: String) =
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT)
            .show()

    override fun observeData() {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("updated_image_path")
            ?.observe(viewLifecycleOwner) { newPath ->
                if (newPath.isNullOrEmpty()) return@observe
                currentImagePath = newPath
                loadImage(requireContext(), currentImagePath, binding.imvImage)
            }
    }

    override fun bindViewModel() {}
}