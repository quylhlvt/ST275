package com.anime.oc.characters.avatar.ui.main.cosplay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.anime.oc.characters.avatar.core.base.BaseFragment
import com.anime.oc.characters.avatar.core.extention.InternetExtension
import com.anime.oc.characters.avatar.core.extention.gone
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.popBack
import com.anime.oc.characters.avatar.core.extention.select
import com.anime.oc.characters.avatar.core.extention.setImageActionBar
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.ui.main.show.ShowFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isInternetAvailable
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isNetworkConnected
import com.anime.oc.characters.avatar.core.extention.changeText
import com.anime.oc.characters.avatar.core.extention.loadNativeCollabAds
import com.anime.oc.characters.avatar.core.extention.setTextActionBar
import com.anime.oc.characters.avatar.core.extention.showInter
import com.anime.oc.characters.avatar.databinding.FragmentCosplayBinding
import com.anime.oc.characters.avatar.utils.BlockableFrameLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class CosplayFragment : BaseFragment<FragmentCosplayBinding, CosplayViewModel>(
    FragmentCosplayBinding::inflate,
    CosplayViewModel::class.java
) {
    private var renderJob: Job? = null

    private fun isOnlineTemplate(templateIndex: Int): Boolean {
        return viewModelActivity.templates.value.getOrNull(templateIndex)
            ?.id?.startsWith("online_") == true
    }

    private fun checkOnlineNetworkOrShowDialog(templateIndex: Int): Boolean {
        if (!isOnlineTemplate(templateIndex)) return false
        return when {
            !InternetExtension.isInternetAvailable(requireContext()) -> {
                showUnstableNetworkDialog(); true
            }

            !InternetExtension.isNetworkConnected(requireContext()) -> {
                showUnstableNetworkDialog(); true
            }

            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPressHandler()
    }

    private fun initNativeCollab() {
        loadNativeCollabAds(
            getString(R.string.native_cl_cosplay), binding.flNativeCollab
        )
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    popBack()
                }
            }
        )
    }

    override fun onFragmentStart() {
        if (!isAdded || isDetached) return
        (binding.flNativeCollab as? BlockableFrameLayout)?.isBlocked = false
        initNativeCollab()
    }

    override fun onFragmentStop() {
        if (!isAdded || isDetached) return
        (binding.flNativeCollab as? BlockableFrameLayout)?.isBlocked = true
        binding.flNativeCollab.removeAllViews()
    }

    override fun setupPreViews() {
        super.setupPreViews()
        Glide.with(binding.imageGif).asGif().load(R.drawable.gif).into(binding.imageGif)
    }

    override fun initView() {
        initNativeCollab()
        binding.imvImage.gone()
        binding.imageGif.visible()
        binding.setupActionBar()
        setShowButtonEnabled(false)
        binding.txtRandom.isSelected = true
        binding.txtShow.isSelected = true
        val space = SpannableString(" ")
        val parts = listOf(
            changeText(requireContext(), getString(R.string.tvCosplay1), R.color.app_color2, R.font.pacifico_regular),
            space,
            changeText(requireContext(), getString(R.string.tvCosplay2), R.color.app_color2, R.font.pacifico_regular),
            space,
            changeText(requireContext(), getString(R.string.tvCosplay3), R.color.app_color2, R.font.pacifico_regular),
            space,
            changeText(requireContext(), getString(R.string.tvCosplay4), R.color.app_color2, R.font.pacifico_regular),
            space,
            changeText(requireContext(), getString(R.string.tvCosplay5), R.color.app_color2, R.font.pacifico_regular),
            space,
            changeText(requireContext(), getString(R.string.tvCosplay6), R.color.app_color2, R.font.pacifico_regular),
            space,
            changeText(requireContext(), getString(R.string.tvCosplay7), R.color.app_color2, R.font.pacifico_regular),
            space,
            changeText(requireContext(), getString(R.string.tvCosplay8), R.color.app_color2, R.font.pacifico_regular),
        )

        // ✅ Dùng SpannableStringBuilder thay vì TextUtils.concat
        val builder = android.text.SpannableStringBuilder()
        parts.forEach { builder.append(it) }

        binding.txtGuile.setText(builder, TextView.BufferType.SPANNABLE)
        // Chỉ randomize lần đầu, nếu chưa có item nào
//        if (viewModel.randomItem.value == null) {
//            viewModel.randomize()
//        }
    }

    private fun FragmentCosplayBinding.setupActionBar() {
        actionBar.apply {
            tvCenter.select()
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setImageActionBar(btnActionBarRight, R.drawable.guid)
            setTextActionBar(tvCenter, getString(R.string.cosplay))
        }
    }

    override fun viewListener() {
        binding.apply {

            actionBar.btnActionBarLeft.onClick { popBack() }

            random.onClick {
                val isOnline = isNetworkConnected(requireContext()) && isInternetAvailable(
                    requireContext()
                )
                val currentIndex = viewModel.randomItem.value?.templateIndex ?: -1
                if (currentIndex >= 0 && checkOnlineNetworkOrShowDialog(currentIndex)) return@onClick
                showInter {
                    showLoading()
                    viewModel.randomize(isOnline = isOnline)
                }
            }
            actionBar.btnActionBarRight.onClick {
                showGuide.visible()
            }
            closeGuide.onClick {
                showGuide.gone()
            }
            show.onClick {
                if (!show.isEnabled) return@onClick
                val item = viewModel.randomItem.value ?: return@onClick
                if (checkOnlineNetworkOrShowDialog(item.templateIndex)) return@onClick
                val cached = viewModel.cachedBitmap
                if (cached != null && !cached.isRecycled) {
                    viewModelActivity.cosplayBitmap = cached
                }
                val args = ShowFragment.newArgshow(
                    templateIndex = item.templateIndex,
                    targetSelections = item.selections
                )
                findNavController().navigate(R.id.action_cosplay_to_show, args)
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentCosplayBinding = FragmentCosplayBinding.inflate(inflater, container, false)

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isDataReady.collect { ready ->
                if (ready && viewModel.randomItem.value == null) {
                    val isOnline =
                        isNetworkConnected(requireContext()) && isInternetAvailable(requireContext())
                    viewModel.randomize(isOnline = isOnline)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.randomItem.collectLatest { item ->
                item ?: return@collectLatest

                // ✅ Nếu đã có cache bitmap thì không render lại
                val cached = viewModel.cachedBitmap
                if (cached != null && !cached.isRecycled) {
                    showBitmap(cached)
                    return@collectLatest
                }

                renderCharacter(item)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ Guard: chỉ access viewModel khi fragment đã attach xong
        if (!isAdded || view == null) return

        val cached = viewModel.cachedBitmap
        if (cached != null && !cached.isRecycled) {
            showBitmap(cached)
        }
    }

    private fun renderCharacter(item: CosplayViewModel.RandomItem) {
        renderJob?.cancel()
        setShowButtonEnabled(false)
        renderJob = viewLifecycleOwner.lifecycleScope.launch {
            val paths = item.resolvedPaths.filterNotNull()
            if (paths.isEmpty()) return@launch
            showLoading()

            var networkDialogShown = false
            var waitingForNetwork = false
            var bitmaps: List<Bitmap> = emptyList()
            while (isActive) {
                val loaded = withContext(Dispatchers.IO) {
                    paths.map { path ->
                        async {
                            runCatching {
                                Glide.with(requireContext()).asBitmap().load(path)
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                    .override(512).submit().get()
                            }.getOrNull()
                        }
                    }.awaitAll()
                }
                bitmaps = loaded.filterNotNull()
                if (bitmaps.size == paths.size) break

                val hasNetwork = InternetExtension.isNetworkConnected(requireContext()) &&
                        InternetExtension.isInternetAvailable(requireContext())
                if (!hasNetwork) {
                    waitingForNetwork = true
                    if (!networkDialogShown) {
                        showUnstableNetworkDialog()
                        networkDialogShown = true
                    }
                    delay(200)
                    continue
                }
                if (waitingForNetwork) {
                    viewModel.randomize(isOnline = true)
                    return@launch
                }
                delay(200)
            }
            if (!isActive || bitmaps.size != paths.size) return@launch

            val merged = mergeBitmaps(bitmaps)

            // ✅ Lưu vào cache
            viewModel.setCachedBitmap(merged)

            withContext(Dispatchers.Main) {
                showBitmap(merged)
            }
        }
    }

    private fun showLoading() {
        binding.imvImage.apply {
            setImageDrawable(null)
            gone()
        }
        binding.imageGif.visible()
        setShowButtonEnabled(false)
    }

    private fun showBitmap(bitmap: Bitmap) {
        binding.imvImage.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bitmap)
            visible()
        }
        binding.imageGif.gone()
        setShowButtonEnabled(true)
    }

    private fun setShowButtonEnabled(enabled: Boolean) {
        binding.show.isEnabled = enabled
        binding.show.isClickable = enabled
        binding.show.alpha = if (enabled) 1f else 0.5f
        binding.random.isEnabled = enabled
        binding.random.isClickable = enabled
        binding.random.alpha = if (enabled) 1f else 0.5f
    }

    private fun mergeBitmaps(bitmaps: List<Bitmap>): Bitmap {
        val size = 512
        val merged = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(merged)
        bitmaps.forEach { bmp ->
            val scaled = if (bmp.width == size && bmp.height == size) bmp
            else Bitmap.createScaledBitmap(bmp, size, size, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            if (scaled != bmp) scaled.recycle()
        }
        return merged
    }

    override fun bindViewModel() {}
}
