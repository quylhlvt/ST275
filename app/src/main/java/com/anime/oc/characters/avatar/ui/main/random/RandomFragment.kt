package com.anime.oc.characters.avatar.ui.main.random

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
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
import com.anime.oc.characters.avatar.ui.main.customize.CustomizeFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isInternetAvailable
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isNetworkConnected
import com.anime.oc.characters.avatar.core.extention.setTextActionBar
import com.anime.oc.characters.avatar.databinding.FragmentRandomBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.forEach

@AndroidEntryPoint
class RandomFragment : BaseFragment<FragmentRandomBinding, RandomViewModel>(
    FragmentRandomBinding::inflate,
    RandomViewModel::class.java
) {
    private var count = 0

    // Thêm hàm này vào RandomFragment
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

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentRandomBinding = FragmentRandomBinding.inflate(inflater, container, false)

    override fun onFragmentStart() {
        if (!isAdded || isDetached) return

    }

    override fun onFragmentStop() {
        if (!isAdded || isDetached) return

    }

    override fun setupPreViews() {
        super.setupPreViews()
        Glide.with(binding.imageGif).asGif().load(R.drawable.gif).into(binding.imageGif)
    }

    override fun initView() {
        binding.imvImage.gone()
        binding.imageGif.visible()
        setSaveButtonEnabled(false)
        binding.setupActionBar()
        binding.txtRandom.isSelected = true
        binding.txtEdit.isSelected = true
    }

    private fun FragmentRandomBinding.setupActionBar() {
        actionBar.apply {
            tvCenter.select()
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setTextActionBar(tvCenter, getString(R.string.random))
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.onClick {
                popBack()

            }
            // ✅ Nút random — check internet nếu template online
            random.onClick {
                count++

                val action = {
                    val isOnline = isNetworkConnected(requireContext()) &&
                            isInternetAvailable(requireContext())

                    val currentIndex = viewModel.randomItem.value?.templateIndex ?: -1

                    if (currentIndex < 0 || !checkOnlineNetworkOrShowDialog(currentIndex)) {
                        showLoading()
                        viewModel.randomize(isOnline = isOnline)
                    }
                }

                if (count > 1) {

                    action()

                } else {
                    action()
                }
            }
            btnEdit.onClick {
                if (!btnEdit.isEnabled) return@onClick
                val item = viewModel.randomItem.value ?: return@onClick
                if (checkOnlineNetworkOrShowDialog(item.templateIndex)) return@onClick
                val templateId = viewModelActivity.templates.value
                    .getOrNull(item.templateIndex)?.id ?: ""
                val args = CustomizeFragment.newArgs(
                    templateIndex = item.templateIndex,
                    templateId = templateId,
                    isEdit = false,
                    savedSelections = item.selections
                )
                findNavController().navigate(R.id.action_random_to_custom, args)
            }
        }
    }

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
        // ✅ Khi back về: dùng lại bitmap đã cache
        val cached = viewModel.cachedBitmap
        if (cached != null && !cached.isRecycled) {
            showBitmap(cached)
        }
    }

    private fun renderCharacter(item: RandomViewModel.RandomItem) {
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
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
                                    .override(800).submit().get()
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
        setSaveButtonEnabled(false)
    }

    private fun showBitmap(bitmap: Bitmap) {
        binding.imvImage.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bitmap)
            visible()
        }
        binding.imageGif.gone()
        setSaveButtonEnabled(true)
    }

    private fun setSaveButtonEnabled(enabled: Boolean) {
        binding.btnEdit.isEnabled = enabled
        binding.btnEdit.isClickable = enabled
        binding.btnEdit.alpha = if (enabled) 1f else 0.5f

        binding.random.isEnabled = enabled
        binding.random.isClickable = enabled
        binding.random.alpha = if (enabled) 1f else 0.5f
    }

    private fun mergeBitmaps(bitmaps: List<Bitmap>): Bitmap {
        val size = 800
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
