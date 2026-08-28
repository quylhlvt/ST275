package com.anime.oc.characters.avatar.ui.main.success

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
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.base.BaseFragment
import com.anime.oc.characters.avatar.core.extention.checkPermissions
import com.anime.oc.characters.avatar.core.extention.goToSettings
import com.anime.oc.characters.avatar.core.extention.loadImage
import com.anime.oc.characters.avatar.core.extention.loadNativeCollabAds
import com.anime.oc.characters.avatar.core.extention.logEventSocial
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.onClick1
import com.anime.oc.characters.avatar.core.extention.safeNavigate
import com.anime.oc.characters.avatar.core.extention.setImageActionBar
import com.anime.oc.characters.avatar.core.extention.setTextActionBar
import com.anime.oc.characters.avatar.core.extention.showInter
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.core.helper.PermissionRequestHelper
import com.anime.oc.characters.avatar.databinding.FragmentSuccessBinding
import com.anime.oc.characters.avatar.ui.onboarding.permission.PermissionViewModel
import com.anime.oc.characters.avatar.utils.share.SocialShareManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class SuccessFragment : BaseFragment<FragmentSuccessBinding, SuccessViewModel>(
    FragmentSuccessBinding::inflate,
    SuccessViewModel::class.java
) {
    private val storageHelper = PermissionRequestHelper()

    private val permissionViewModel: PermissionViewModel by activityViewModels()
    private val socialShareManager by lazy(LazyThreadSafetyMode.NONE) {
        SocialShareManager(requireContext())
    }
    private var currentImagePath: String = ""
    private var isReturningFromExternalScreen = false
    private val imagePath: String by lazy { arguments?.getString("imagePath") ?: "" }
    private val avatarUrl: String by lazy { arguments?.getString("avatarUrl") ?: "" }

    private val imageType: Int    by lazy { arguments?.getInt("imageType", 0) ?: 0 }
    private val idEdit: String    by lazy { arguments?.getString("idEdit") ?: "" }

    companion object {
        private const val EXTERNAL_SCREEN_RESTORE_DELAY_MS = 500L
    }
    private fun initNativeCollab() {
        loadNativeCollabAds(
            getString(R.string.native_success), binding.flNativeCollab
        )
    }
    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSuccessBinding = FragmentSuccessBinding.inflate(inflater, container, false)

    override fun onResume() {
        super.onResume()
        hideLoadingSafe()
        hideGlobalDialogSafe()
        if (!isReturningFromExternalScreen || view == null) return

        restoreWindowInteractions()
        restoreViewInteractions()
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

    override fun initView() {
        initNativeCollab()
        currentImagePath = imagePath
        binding.apply {
            setImageActionBar(actionBar.btnActionBarLeft, R.drawable.back_app)
            loadImage(requireContext(), imagePath, imvImage)
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

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.onClick1 { findNavController().navigateUp() }

            // Home
            actionBar.btnActionBarRight.onClick1 {
                showInter {
                    findNavController().safeNavigate(
                        R.id.action_success_to_home, null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true).build()
                    )
                }
            }
            // Share
            actionBar.btnActionBarNextToRight.onClick( 1500) { shareImage() }

            // MyCreation
            btnBottomLeft.onClick1 {
                showInter {
                    findNavController().safeNavigate(
                        R.id.action_success_to_myPony, null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, false).build()
                    )
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
            SocialShareManager.ShareResult.Started -> isReturningFromExternalScreen = true
            SocialShareManager.ShareResult.ImageNotFound -> showToast(getString(R.string.image_not_found))
            is SocialShareManager.ShareResult.AppNotAvailable -> showToast(getString(if (app == SocialShareManager.SocialApp.FACEBOOK) R.string.facebook_not_available else R.string.instagram_not_available))
            is SocialShareManager.ShareResult.Failed -> showToast(getString(R.string.share_failed))
        }
    }
    private fun shareImage() {
        if (imagePath.isEmpty()) return
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            File(imagePath)
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        isReturningFromExternalScreen = true
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }
// ViewFragment.kt

    // Thêm vào ViewFragment
    private fun downloadImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { performDownload(); return }
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
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



    private fun showToast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

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
