package com.anime.oc.characters.avatar.ui.main.random

import com.anime.oc.characters.avatar.core.extention.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.core.navigation.Screen
import com.anime.oc.characters.avatar.ui.main.customize.CustomizeActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isInternetAvailable
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isNetworkConnected
import com.anime.oc.characters.avatar.databinding.ActivityRandomBinding
import com.anime.oc.characters.avatar.utils.BlockableFrameLayout
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
class RandomActivity : BaseActivity<ActivityRandomBinding, RandomViewModel>(
    ActivityRandomBinding::inflate, RandomViewModel::class.java
) {

    // RandomActivity được tạo mới mỗi lần điều hướng từ Home, nên bộ đếm này
    // tự reset cho mỗi lượt vào màn và vẫn được giữ khi quay lại từ Customize.
    private var randomClickCount = 0

    // Thêm hàm này vào RandomActivity
    private fun isOnlineTemplate(templateIndex: Int): Boolean {
        return appSession.templates.value.getOrNull(templateIndex)?.id?.startsWith("online_") == true
    }



    private fun checkOnlineNetworkOrShowDialog(templateIndex: Int): Boolean {
        if (!isOnlineTemplate(templateIndex)) return false
        return when {
            !InternetExtension.isInternetAvailable(this@RandomActivity) -> {
                showUnstableNetworkDialog(); true
            }

            !InternetExtension.isNetworkConnected(this@RandomActivity) -> {
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
        this@RandomActivity.onBackPressedDispatcher.addCallback(
            this@RandomActivity, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    popBack()
                }
            })
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
        binding.imageGif.gone()
        binding.demoRandom.visible()
        setSaveButtonEnabled(false)
        setRandomButtonEnabled(viewModel.isDataReady.value)
        binding.setupActionBar()
        binding.txtRandom.isSelected = true
    }

    private fun ActivityRandomBinding.setupActionBar() {
        actionBar.apply {
            tvCenter.select()
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)

            setMaterialCardViewActionBar1(
                btnActionBarRightText,
                tvRightText,
                getString(R.string.edit)
            )
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.onClick {
                popBack()

            }
            // ✅ Nút random — check internet nếu template online
            random.onClick {
                randomClickCount++
                val isOnline = isNetworkConnected(this@RandomActivity) && isInternetAvailable(this@RandomActivity)
                val currentIndex = viewModel.randomItem.value?.templateIndex ?: -1

                if (currentIndex < 0 || !checkOnlineNetworkOrShowDialog(currentIndex)) {
                    showLoading()
                    val randomizeAction = {
                        viewModel.randomize(isOnline = isOnline)
                    }

                    if (randomClickCount == 1) {
                        randomizeAction()
                    } else {
                        randomizeAction()
//                        showInter(randomizeAction)
                    }
                } else {
                    return@onClick
                }
            }
            actionBar.btnActionBarRightText.onClick {
                if (!actionBar.btnActionBarRightText.isEnabled) return@onClick
                val item = viewModel.randomItem.value ?: return@onClick
                if (checkOnlineNetworkOrShowDialog(item.templateIndex)) return@onClick
                val templateId = appSession.templates.value.getOrNull(item.templateIndex)?.id ?: ""
                val args = CustomizeActivity.newArgs(
                    templateIndex = item.templateIndex,
                    templateId = templateId,
                    isEdit = false,
                    savedSelections = item.selections
                )

                    openScreen(Screen.CUSTOMIZE, args)

            }
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isDataReady.collect { ready ->
                        if (viewModel.randomItem.value == null) {
                            setRandomButtonEnabled(ready)
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
        // ✅ Khi back về: dùng lại bitmap đã cache
        val cached = viewModel.cachedBitmap
        if (cached != null && !cached.isRecycled) {
            showBitmap(cached)
        }
    }

    private fun renderCharacter(item: RandomViewModel.RandomItem) {
        showLoading()
        this@RandomActivity.lifecycleScope.launch {
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
                                Glide.with(this@RandomActivity).asBitmap().load(path)
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(800).submit().get()
                            }.getOrNull()
                        }
                    }.awaitAll()
                }
                bitmaps = loaded.filterNotNull()
                if (bitmaps.size == paths.size) break

                val hasNetwork =
                    InternetExtension.isNetworkConnected(this@RandomActivity) && InternetExtension.isInternetAvailable(
                        this@RandomActivity
                    )
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
        binding.demoRandom.gone()
        binding.imvImage.apply {
            setImageDrawable(null)
            gone()
        }
        binding.imageGif.visible()
        setSaveButtonEnabled(false)
    }

    private fun showBitmap(bitmap: Bitmap) {
        binding.demoRandom.gone()
        binding.imvImage.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bitmap)
            visible()
        }
        binding.imageGif.gone()
        setSaveButtonEnabled(true)
    }

    private fun setSaveButtonEnabled(enabled: Boolean) {
        binding.actionBar.btnActionBarRightText.isEnabled = enabled
        binding.actionBar.btnActionBarRightText.isClickable = enabled
        binding.actionBar.btnActionBarRightText.alpha = if (enabled) 1f else 0.5f

        setRandomButtonEnabled(enabled)
    }

    private fun setRandomButtonEnabled(enabled: Boolean) {
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
