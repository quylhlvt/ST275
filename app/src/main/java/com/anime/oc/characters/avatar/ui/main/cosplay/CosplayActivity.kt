package com.anime.oc.characters.avatar.ui.main.cosplay

import com.anime.oc.characters.avatar.core.extention.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.SpannableString
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.core.navigation.Screen
import com.anime.oc.characters.avatar.ui.main.show.ShowActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isInternetAvailable
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isNetworkConnected
import com.anime.oc.characters.avatar.databinding.ActivityCosplayBinding
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
class CosplayActivity : BaseActivity<ActivityCosplayBinding, CosplayViewModel>(
    ActivityCosplayBinding::inflate,
    CosplayViewModel::class.java
) {
    private var renderJob: Job? = null

    private fun isOnlineTemplate(templateIndex: Int): Boolean {
        return appSession.templates.value.getOrNull(templateIndex)
            ?.id?.startsWith("online_") == true
    }

    private fun checkOnlineNetworkOrShowDialog(templateIndex: Int): Boolean {
        if (!isOnlineTemplate(templateIndex)) return false
        return when {
            !InternetExtension.isInternetAvailable(this@CosplayActivity) -> {
                showUnstableNetworkDialog(); true
            }

            !InternetExtension.isNetworkConnected(this@CosplayActivity) -> {
                showUnstableNetworkDialog(); true
            }

            else -> false
        }
    }

    override fun onContentReady(view: View, savedInstanceState: Bundle?) {
        super.onContentReady(view, savedInstanceState)
        setupBackPressHandler()
    }


    private fun setupBackPressHandler() {
        this@CosplayActivity.onBackPressedDispatcher.addCallback(
            this@CosplayActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    popBack()
                }
            }
        )
    }

    override fun onRestart() {
        super.onRestart()

    }
    override fun setupPreViews() {
        super.setupPreViews()
        Glide.with(binding.imageGif).asGif().load(R.drawable.gif).into(binding.imageGif)
    }

    override fun initView() {
        binding.imvImage.gone()
        binding.imageGif.visible()
        binding.setupActionBar()
        setShowButtonEnabled(false)
        binding.txtRandom.isSelected = true

        // Chỉ randomize lần đầu, nếu chưa có item nào
//        if (viewModel.randomItem.value == null) {
//            viewModel.randomize()
//        }
    }

    private fun ActivityCosplayBinding.setupActionBar() {
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
                val isOnline = isNetworkConnected(this@CosplayActivity) && isInternetAvailable(
                    this@CosplayActivity
                )
                val currentIndex = viewModel.randomItem.value?.templateIndex ?: -1
                if (currentIndex >= 0 && checkOnlineNetworkOrShowDialog(currentIndex)) return@onClick

                        showLoading()
                        viewModel.randomize(isOnline = isOnline)


            }
            actionBar.btnActionBarRight.onClick {
                showGuide.visible()
            }
            closeGuide.onClick {
                showGuide.gone()
            }
            play.onClick {
                if (!play.isEnabled) return@onClick
                val item = viewModel.randomItem.value ?: return@onClick
                if (checkOnlineNetworkOrShowDialog(item.templateIndex)) return@onClick
        val cached = viewModel.cachedBitmap
                if (cached != null && !cached.isRecycled) {
                    appSession.cosplayBitmap = cached
                }
                val args = ShowActivity.newArgshow(
                    templateIndex = item.templateIndex,
                    targetSelections = item.selections
                )
                openScreen(Screen.SHOW, args)
            }
        }
    }


    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isDataReady.collect { ready ->
                        if (ready && viewModel.randomItem.value == null) {
                            val isOnline = isNetworkConnected(this@CosplayActivity) &&
                                isInternetAvailable(this@CosplayActivity)
                            viewModel.randomize(isOnline = isOnline)
                        }
                    }
                }
                launch {
                    viewModel.randomItem.collectLatest { item ->
                        item ?: return@collectLatest
                        val cached = viewModel.cachedBitmap
                        if (cached != null && !cached.isRecycled) showBitmap(cached)
                        else renderCharacter(item)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ Guard: chỉ access viewModel khi Activity đã tạo nội dung
        if (!isScreenAvailable || contentView == null) return
        val cached = viewModel.cachedBitmap
        if (cached != null && !cached.isRecycled) {
            showBitmap(cached)
        }
    }

    private fun renderCharacter(item: CosplayViewModel.RandomItem) {
        renderJob?.cancel()
        setShowButtonEnabled(false)
        renderJob = this@CosplayActivity.lifecycleScope.launch {
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
                                Glide.with(this@CosplayActivity).asBitmap().load(path)
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                    .override(512).submit().get()
                            }.getOrNull()
                        }
                    }.awaitAll()
                }
                bitmaps = loaded.filterNotNull()
                if (bitmaps.size == paths.size) break

                val hasNetwork = InternetExtension.isNetworkConnected(this@CosplayActivity) &&
                        InternetExtension.isInternetAvailable(this@CosplayActivity)
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
        binding.play.isEnabled = enabled
        binding.play.isClickable = enabled
        binding.play.alpha = if (enabled) 1f else 0.5f
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
