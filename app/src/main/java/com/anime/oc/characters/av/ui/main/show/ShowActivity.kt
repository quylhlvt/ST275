package com.anime.oc.characters.av.ui.main.show

import com.anime.oc.characters.av.core.extention.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.anime.oc.characters.av.MyApplication
import com.anime.oc.characters.av.core.base.BaseActivity
import com.anime.oc.characters.av.core.navigation.Screen
import com.anime.oc.characters.av.data.model.custom.BodyPartModel
import com.anime.oc.characters.av.data.model.custom.SelectionIndex
import com.anime.oc.characters.av.ui.main.customize.ColorAdapter
import com.anime.oc.characters.av.ui.main.customize.NavAdapter
import com.anime.oc.characters.av.ui.main.customize.PartAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.anime.oc.characters.av.R
import com.anime.oc.characters.av.databinding.ActivityShowBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

@AndroidEntryPoint
class ShowActivity : BaseActivity<ActivityShowBinding, ShowViewModel>(
    ActivityShowBinding::inflate,
    ShowViewModel::class.java
) {
    // ── Layer views (giống CustomizeActivity) ─────────────────────────────────
    private val layerViews = arrayListOf<AppCompatImageView>()
    private val navToLayerIndex = mutableMapOf<String, Int>()
    private val arrShowColor = mutableListOf<Boolean>()

    private val adapterNav by lazy { NavAdapter() }
    private val adapterColor by lazy { ColorAdapter() }
    private val adapterPart by lazy { PartAdapter() }

    private val pendingLoads = AtomicInteger(0)
    private var timerJob: Job? = null
    private var countDownJob: Job? = null
    private val totalSeconds = 1 * 6
    private var remainingSeconds = totalSeconds

    private var hasNavigatedToSuccess = false
    private var scrollPartAfterRandom = false

    // ── INFLATE ───────────────────────────────────────────────────────────────

    private fun isOnlineTemplate(): Boolean {
        val templateIndex = intent.extras?.getInt(ARG_TEMPLATE_INDEX, 0) ?: 0
        return appSession.templates.value
            .getOrNull(templateIndex)?.id?.startsWith("online_") == true
    }

    private fun checkOnlineNetworkOrShowDialog(): Boolean {
        if (!isOnlineTemplate()) return false
        return when {
            !InternetExtension.isInternetAvailable(this@ShowActivity) -> {
                showUnstableNetworkDialog(); true
            }
            !InternetExtension.isNetworkConnected(this@ShowActivity) -> {
                showUnstableNetworkDialog(); true
            }
            else -> false
        }
    }
    // ── INIT ──────────────────────────────────────────────────────────────────

    override fun onRestart() {
        super.onRestart()
    }
    override fun initView() {
        binding.apply {
            actionBar.apply {
                setImageActionBar(cvLogo, R.drawable.back_app)
//            setImageActionBar(btnActionBarRight, R.drawable.next_app)
//            setTextActionBar( tvCenter, "05:00" )
            }
        }

        setupAdapters()
        readArgsAndInit()
        startCountDown()

        val bitmap = appSession.cosplayBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            binding.imvImage2.setImageBitmap(bitmap)
            binding.imvImage.setImageBitmap(bitmap)
            binding.imvImage2.visibility = View.VISIBLE
        } else {
            binding.imvImage2.visibility = View.GONE
        }
    }
    private fun updateTimerUI(minutes: Int, seconds: Int) {
        binding.actionBar.tvCenter.text = String.format(
            Locale.US,
            "%02d:%02d",
            minutes,
            seconds
        )
    }
    private fun startTimer(fromSeconds: Int = totalSeconds) {
        if (hasNavigatedToSuccess) return
        timerJob?.cancel()
        remainingSeconds = fromSeconds

        timerJob = this@ShowActivity.lifecycleScope.launch {
            while (remainingSeconds >= 0) {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                updateTimerUI(minutes, seconds)

                if (remainingSeconds == 0) {
                    navigateToSuccess()
                    break
                }

                delay(1000)
                remainingSeconds--  // ← cập nhật liên tục
            }
        }
    }
    private fun startCountDown() {
        countDownJob?.cancel()
        binding.actionBar.cvLogo.isEnabled = false
        binding.actionBar.btnActionBarRight.isEnabled = false
        val colors = listOf(
            ContextCompat.getColor(this@ShowActivity, R.color.app_color5), // 3
            ContextCompat.getColor(this@ShowActivity, R.color.app_color6), // 2
            ContextCompat.getColor(this@ShowActivity, R.color.orange_F6)   // 1
        )

        binding.countDown.visibility = View.VISIBLE

        countDownJob = this@ShowActivity.lifecycleScope.launch {
            for (count in 3 downTo 1) {
                val colorIndex = 3 - count
                binding.tvCountDown.setTextColor(colors[colorIndex])
                binding.tvCountDown.text = count.toString()

                // reset state trước khi animate in
                binding.tvCountDown.scaleX = 0.4f
                binding.tvCountDown.scaleY = 0.4f
                binding.tvCountDown.alpha = 0f

                // pop in — chờ xong
                suspendCancellableCoroutine { cont ->
                    binding.tvCountDown.animate()
                        .scaleX(1f).scaleY(1f)
                        .alpha(1f)
                        .setDuration(350)
                        .withEndAction { cont.resume(Unit) {} }
                        .start()
                }

                // giữ 500ms
                delay(500)

                // pop out — chờ xong
                suspendCancellableCoroutine { cont ->
                    binding.tvCountDown.animate()
                        .scaleX(1.5f).scaleY(1.5f)
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction { cont.resume(Unit) {} }
                        .start()
                }
            }

            // Ẩn overlay, bắt đầu timer
            binding.countDown.visibility = View.GONE
            binding.actionBar.cvLogo.isEnabled = true
            binding.actionBar.btnActionBarRight.isEnabled = true
            binding.tvCountDown.alpha = 1f
            binding.tvCountDown.scaleX = 1f
            binding.tvCountDown.scaleY = 1f
            startTimer()
        }
    }
    private var waitingForCatalogue = false

    private fun readArgsAndInit() {
        if (!appSession.localDataReady.value) {
            if (!waitingForCatalogue) {
                waitingForCatalogue = true
                lifecycleScope.launch {
                    appSession.localDataReady.first { it }
                    waitingForCatalogue = false
                    if (isScreenAvailable) readArgsAndInit()
                }
            }
            return
        }
        val templateIndex = intent.extras?.getInt(ARG_TEMPLATE_INDEX, 0) ?: 0
        val targetSelections: ArrayList<SelectionIndex> =
            intent.extras?.getParcelableArrayList(ARG_SELECTIONS) ?: return

        viewModel.init(templateIndex, targetSelections)
    }

    private fun setupAdapters() {
        binding.rcvNav.adapter = adapterNav
        binding.rcvColor.apply {
            adapter = adapterColor
            itemAnimator = null
        }
        binding.rcvPart.apply {
            (layoutManager as? GridLayoutManager)?.spanCount =
                if (MyApplication.isTablet) TABLET_GRID_SPAN_COUNT else PHONE_GRID_SPAN_COUNT
            adapter = adapterPart
        }
    }
    private fun navigateToSuccess() {
        if (!isScreenAvailable || isDestroyed) return
        if (hasNavigatedToSuccess) return
        hasNavigatedToSuccess = true
        timerJob?.cancel()
        countDownJob?.cancel()

        // Nếu vẫn còn đang load ảnh → đợi
        if (pendingLoads.get() > 0) {
            this@ShowActivity.lifecycleScope.launch {
                while (pendingLoads.get() > 0) {
                    delay(50)
                }
                doNavigateToSuccess()
            }
        } else {
            doNavigateToSuccess()
        }
    }

    private fun doNavigateToSuccess() {
        if (!isScreenAvailable || isDestroyed) return
        val bitmap = renderLayersToBitmap()
        if (bitmap != null) appSession.userResultBitmap = bitmap
        appSession.cosplayPercent = viewModel.state.value.matchPercent

        openScreen(Screen.SUCCESS_COSPLAY)
    }
    private fun renderLayersToBitmap(): Bitmap? {
        val root = binding.rlCharacter
        if (root.width == 0 || root.height == 0) return null
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        layerViews.forEach { iv ->
            if (iv.visibility != View.VISIBLE) return@forEach
            val drawable = iv.drawable ?: return@forEach
            canvas.save()
            if (iv.scaleX < 0) canvas.scale(-1f, 1f, root.width / 2f, 0f)
            drawable.setBounds(0, 0, root.width, root.height)
            drawable.draw(canvas)
            canvas.restore()
        }
        return bitmap
    }

    // ── LISTENERS ─────────────────────────────────────────────────────────────

    override fun viewListener() {
        binding.apply {
            actionBar.cvLogo.onClick { popBack() }
            materialSmall.onClick { imgShowBig.visible() }
            close.onClick { imgShowBig.gone() }
        }

        // ── Nav ───────────────────────────────────────────────────────────────────
        adapterNav.onClick = {
            if (!checkOnlineNetworkOrShowDialog()) viewModel.selectNav(it)       // ← guard
        }

        // ── Color ─────────────────────────────────────────────────────────────────
        adapterColor.onClick = {
            if (!checkOnlineNetworkOrShowDialog()) viewModel.selectColor(it)     // ← guard
        }

        // ── Part ──────────────────────────────────────────────────────────────────
        adapterPart.onClick = { idx, type ->
            if (!checkOnlineNetworkOrShowDialog()) {                              // ← guard
                when (type) {
                    "none" -> viewModel.selectNone()
                    "dice" -> {
                        scrollPartAfterRandom = true
                        viewModel.selectDiceCurrent()
                    }
                    else   -> viewModel.selectPath(idx)
                }
            }
        }
    }

    // ── OBSERVE ───────────────────────────────────────────────────────────────

    // ShowActivity.observeData() — THÊM guard này
    // ✅ FIX — thêm flag giống CustomizeActivity
    private var hasTriggeredReInit = false

    override fun observeData() {

        this@ShowActivity.lifecycleScope.launch {
            this@ShowActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    if (state.listData.isEmpty()) {
                        if (!hasTriggeredReInit) {
                            hasTriggeredReInit = true
                            readArgsAndInit()
                        }
                        return@collectLatest
                    }
                    hasTriggeredReInit = false  // reset khi có data

                    if (layerViews.size != state.listData.size) {
                        buildLayerViews(state.listData)
                    }
                    renderLayers(state)
                    updateAdapters(state)
                    val scale = if (state.isFlipped) -1f else 1f
                    layerViews.forEach { it.scaleX = scale }
                    updateMatchUI(state.matchPercent)
                    if (state.matchPercent >= 100) {
                        navigateToSuccess()
                    }
                }
            }
        }
    }

    // ── BUILD LAYER VIEWS (giống CustomizeActivity.buildLayerViews) ───────────

    private fun buildLayerViews(parts: List<BodyPartModel>) {
        layerViews.clear()
        navToLayerIndex.clear()
        binding.rlCharacter.removeAllViews()

        parts.sortedBy { it.position }.forEachIndexed { layerIdx, bp ->
            val iv = AppCompatImageView(this@ShowActivity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            binding.rlCharacter.addView(iv)
            layerViews.add(iv)
            navToLayerIndex[bp.nav] = layerIdx
        }
    }

    // ── RENDER LAYERS (giống CustomizeActivity.renderLayers) ─────────────────

    private fun renderLayers(state: ShowState) {
        val pathsToLoad = state.listData.mapIndexedNotNull { i, bp ->
            val path = viewModel.resolveUserPathAt(i)
            val layerIndex = navToLayerIndex[bp.nav] ?: return@mapIndexedNotNull null
            val view = layerViews.getOrNull(layerIndex) ?: return@mapIndexedNotNull null

            if (path == null) {
                if (view.visibility != View.GONE) {
                    view.visibility = View.GONE
                    view.tag = null
                    Glide.with(binding.rlCharacter).clear(view)
                }
                return@mapIndexedNotNull null
            }
            if (view.tag == path && view.visibility == View.VISIBLE) return@mapIndexedNotNull null
            Triple(view, path, layerIndex)
        }

        if (pathsToLoad.isEmpty()) {
            viewModel.onLoadingComplete()
            return
        }

        pendingLoads.set(pathsToLoad.size)

        pathsToLoad.forEach { (view, path, _) ->
            view.tag = path
            view.visibility = View.VISIBLE
            view.scaleX = if (viewModel.state.value.isFlipped) -1f else 1f
            loadImageIntoView(view, path)
        }
    }

    private fun loadImageIntoView(view: ImageView, path: String) {
        Glide.with(binding.rlCharacter)
            .load(path)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .priority(Priority.IMMEDIATE)
            .skipMemoryCache(false)
            .dontAnimate()
            .dontTransform()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<Drawable>?, isFirstResource: Boolean
                ): Boolean {
                    onLoadFinished(); return false
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any?,
                    target: Target<Drawable>?, dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    onLoadFinished(); return false
                }
            })
            .into(view)
    }

    private fun onLoadFinished() {
        if (pendingLoads.decrementAndGet() <= 0) {
            pendingLoads.set(0)
            contentView?.post { viewModel.onLoadingComplete() }
        }
    }

    // ── ADAPTERS (giống CustomizeActivity.updateAdapters) ────────────────────

    private fun updateAdapters(state: ShowState) {
        adapterNav.setPos(state.currentNavIndex)
        adapterNav.submitList(state.listData)

        adapterColor.setPos(state.currentColorIndex)

        // Khởi tạo arrShowColor
        if (arrShowColor.size != state.listData.size) {
            arrShowColor.clear()
            repeat(state.listData.size) { arrShowColor.add(true) }
        }

        val navPos = state.currentNavIndex

        if (state.hasMultipleColors) {
            if (adapterColor.items != state.currentColors) {
                adapterColor.submitList(state.currentColors)
            }
            setColorPanelVisible(navPos < arrShowColor.size && arrShowColor[navPos])
        } else {
            setColorPanelVisible(false)
        }

        val bp = state.listData.getOrNull(state.currentNavIndex)
        val thumb = buildThumbList(bp, state.currentPaths)
        adapterPart.listThumb = thumb
        adapterPart.setPos(state.currentPathIndex)
        adapterPart.submitList(state.currentPaths)
        if (scrollPartAfterRandom) {
            scrollPartAfterRandom = false
            binding.rcvPart.post {
                binding.rcvPart.scrollToPosition(state.currentPathIndex.coerceAtLeast(0))
            }
        }
    }

    /**
     * Hides frame_rando_all when the current layer has no color list and shows it
     * when it does. The panel fades while frameLayer (constrained below it) slides,
     * so the part list never snaps. Only runs when the visibility actually changes.
     */
    private fun setColorPanelVisible(show: Boolean) {
        val panel = binding.frameRandoAll
        val targetVisibility = if (show) View.VISIBLE else View.GONE
        if (panel.visibility == targetVisibility) return

        val sceneRoot = panel.parent as? ViewGroup
        if (sceneRoot != null && sceneRoot.isLaidOut) {
            TransitionManager.endTransitions(sceneRoot)
            val transition = TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(Fade().addTarget(panel))
                .addTransition(ChangeBounds().addTarget(panel).addTarget(binding.frameLayer))
                .setDuration(COLOR_PANEL_ANIM_DURATION)
                .setInterpolator(FastOutSlowInInterpolator())
            TransitionManager.beginDelayedTransition(sceneRoot, transition)
        }
        panel.animate().cancel()
        panel.alpha = 1f
        panel.visibility = targetVisibility
    }

    private fun buildThumbList(bp: BodyPartModel?, paths: List<String>): List<String> {
        val thumbs = bp?.listThumbPath ?: return paths
        if (thumbs.isEmpty()) return paths
        var idx = 0
        return paths.map { path ->
            when (path) {
                "none", "dice" -> path
                else -> thumbs.getOrElse(idx++) { path }
            }
        }
    }

    private fun updateMatchUI(percent: Int) {
        binding.tvPercent.text = "${percent.coerceIn(0, 100)}%"
    }

    // onPause — lưu remainingSeconds thực tế
    override fun onPause() {
        super.onPause()
        timerJob?.cancel()
        // remainingSeconds đã được cập nhật liên tục trong startTimer
    }
    // ── RESUME ────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        pendingLoads.set(0)

        // ← Resume timer nếu đang đếm (chưa win/fail)
        if (remainingSeconds in 1 until totalSeconds
            && !hasNavigatedToSuccess
            && binding.countDown.visibility != View.VISIBLE
        ) {
            startTimer(remainingSeconds)
        }

        if (appSession.shouldRestartShow) {
            appSession.shouldRestartShow = false

            // Reset hoàn toàn
            timerJob?.cancel()
            countDownJob?.cancel()
            binding.tvCountDown.animate().cancel()
            hasTriggeredReInit = false
            remainingSeconds = totalSeconds
            scrollPartAfterRandom = false
            binding.imgShowBig.gone()
            appSession.userResultBitmap = null
            appSession.cosplayPercent = 0

            // Clear tất cả layer
            layerViews.forEach { Glide.with(binding.rlCharacter).clear(it) }
            layerViews.clear()
            navToLayerIndex.clear()
            arrShowColor.clear()
            binding.rlCharacter.removeAllViews()
            pendingLoads.set(0)
            viewModel.reset()
            hasNavigatedToSuccess = false
            binding.rcvNav.scrollToPosition(0)
            binding.rcvColor.scrollToPosition(0)
            binding.rcvPart.scrollToPosition(0)

            // Cập nhật ảnh cosplay mới
            val bitmap = appSession.cosplayBitmap
            if (bitmap != null && !bitmap.isRecycled) {
                binding.imvImage2.setImageBitmap(bitmap)
                binding.imvImage.setImageBitmap(bitmap)
                binding.imvImage2.visibility = View.VISIBLE
            }

            // Restart
            binding.actionBar.tvCenter.text = "10:00"
            startCountDown()
            readArgsAndInit()
            return
        }

        val currentState = viewModel.state.value
        if (currentState.listData.isEmpty()) {
            if (!hasTriggeredReInit) {
                hasTriggeredReInit = true
                readArgsAndInit()
            }
            return
        }

        val needRebuild = layerViews.isEmpty() ||
                layerViews.firstOrNull()?.isAttachedToWindow == false

        if (needRebuild) {
            buildLayerViews(currentState.listData)
            renderLayers(currentState)
            updateAdapters(currentState)
            val scale = if (currentState.isFlipped) -1f else 1f
            layerViews.forEach { it.scaleX = scale }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        countDownJob?.cancel()
    }
    override fun bindViewModel() {}

    // ── COMPANION ─────────────────────────────────────────────────────────────

    companion object {
        private const val COLOR_PANEL_ANIM_DURATION = 220L
        private const val PHONE_GRID_SPAN_COUNT = 5
        private const val TABLET_GRID_SPAN_COUNT = 7
        const val ARG_TEMPLATE_INDEX = "template_index"
        const val ARG_SELECTIONS = "selections"

        fun newArgshow(
            templateIndex: Int,
            targetSelections: ArrayList<SelectionIndex>
        ) = Bundle().apply {
            putInt(ARG_TEMPLATE_INDEX, templateIndex)
            putParcelableArrayList(ARG_SELECTIONS, targetSelections)
        }
    }
}
