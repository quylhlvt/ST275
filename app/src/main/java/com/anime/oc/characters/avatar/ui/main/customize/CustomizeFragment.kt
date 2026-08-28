package com.anime.oc.characters.avatar.ui.main.customize

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.anime.oc.characters.avatar.ViewModelActivity
import com.anime.oc.characters.avatar.core.base.BackPressHandler
import com.anime.oc.characters.avatar.core.base.BaseFragment
import com.anime.oc.characters.avatar.core.extention.InternetExtension
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.saveToFile
import com.anime.oc.characters.avatar.core.extention.safeNavigate
import com.anime.oc.characters.avatar.core.extention.setImageActionBar
import com.anime.oc.characters.avatar.core.extention.setMaterialCardViewActionBar1
import com.anime.oc.characters.avatar.data.model.custom.BodyPartModel
import com.anime.oc.characters.avatar.data.model.custom.LayerTransform
import com.anime.oc.characters.avatar.data.model.custom.SelectionIndex
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.logEvent
import com.anime.oc.characters.avatar.core.extention.showInter
import com.anime.oc.characters.avatar.core.extention.showRewardAds1
import com.anime.oc.characters.avatar.databinding.FragmentCustomizeBinding
import com.anime.oc.characters.avatar.utils.BlockableFrameLayout
import com.lvt.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

@AndroidEntryPoint
class CustomizeFragment : BaseFragment<FragmentCustomizeBinding, CustomizeViewModel>(
    FragmentCustomizeBinding::inflate,
    CustomizeViewModel::class.java
), BackPressHandler {
    private val arrShowColor = mutableListOf<Boolean>()
    private var isScaleActive = false

    private var isColorVisible = true

    // viewModel đã được inject sẵn bởi BaseFragment — không cần khai báo lại
    // sharedViewModel dùng viewModelActivity từ BaseFragment
    private val sharedViewModel: ViewModelActivity get() = viewModelActivity

    private val layerViews = arrayListOf<AppCompatImageView>()
    private val navToLayerIndex = mutableMapOf<String, Int>()
    private var visibleNavIndices: List<Int> = emptyList()
    private val adapterNav by lazy { NavAdapter() }
    private val adapterColor by lazy { ColorAdapter() }
    private val adapterPart by lazy { PartAdapter() }

    private val pendingLoads = AtomicInteger(0)
    private var canSave = false
    private var hasTriggeredReInit = false
    private var holdActionJob: Job? = null
    private var scrollPartAfterRandom = false

    // ── INFLATE ───────────────────────────────────────────────────────────────
    private fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(
            requireContext(), getString(R.string.native_cl_custom), binding.flNativeCollab
        )
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentCustomizeBinding = FragmentCustomizeBinding.inflate(inflater, container, false)


    // ── INIT ──────────────────────────────────────────────────────────────────
    private fun isOnlineTemplate(): Boolean {
        val templateIndex = arguments?.getInt(ARG_TEMPLATE_INDEX, 0) ?: 0
        val templateId = arguments?.getString(ARG_TEMPLATE_ID)
        val templates = sharedViewModel.templates.value
        val resolvedIndex = if (templateId != null) {
            templates.indexOfFirst { it.id == templateId }.takeIf { it >= 0 } ?: templateIndex
        } else templateIndex
        return templates.getOrNull(resolvedIndex)?.id?.startsWith("online_") == true
    }

    /** Trả về true nếu đã show dialog → caller nên block action */
    private fun checkOnlineNetworkOrShowDialog(): Boolean {
        if (!isOnlineTemplate()) return false
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

    override fun onFragmentStart() {
        if (!isAdded || isDetached) return
        (binding.flNativeCollab as? BlockableFrameLayout)?.isBlocked = false
        initNativeCollab()
    }

    override fun onFragmentStop() {
        if (!isAdded || isDetached) return
        (binding.flNativeCollab as? BlockableFrameLayout)?.isBlocked = true
//        binding.flNativeCollab.removeAllViews()
    }

    override fun initView() {
        initNativeCollab()
        applyTabletLayout()

        binding.apply {
            txtReset.isSelected = true
        }
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setImageActionBar(btnActionBarCenter2, R.drawable.ic_reset_all_custom)
            setImageActionBar(btnActionBarCenter, R.drawable.ic_flip_all_custom)
            setMaterialCardViewActionBar1(
                btnActionBarRightText,
                tvRightText,
                getString(R.string.next)
            )
        }
        setupAdapters()

        readArgsAndInit()
    }

    private fun applyTabletLayout() {
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
//        binding.view17.visibility = if (isTablet) View.GONE else View.VISIBLE
    }

    // CustomizeFragment.kt - readArgsAndInit() — FIX chính ở đây
    private fun readArgsAndInit() {
        val templateIndex = arguments?.getInt(ARG_TEMPLATE_INDEX, 0) ?: 0
        val templateId = arguments?.getString(ARG_TEMPLATE_ID) // ✅ id để verify
        val isEdit = arguments?.getBoolean(ARG_IS_EDIT, false) ?: false
        val isFlipped = arguments?.getBoolean(ARG_IS_FLIPPED, false) ?: false
        val customizedId = arguments?.getString(ARG_CUSTOMIZED_ID)

        val savedSelections: ArrayList<SelectionIndex>? =
            arguments?.getParcelableArrayList(ARG_SELECTIONS)

        val templates = sharedViewModel.templates.value

        // ✅ Resolve index đúng bằng id nếu có
        val resolvedIndex = if (templateId != null) {
            val byId = templates.indexOfFirst { it.id == templateId }
            if (byId >= 0) byId else templateIndex // fallback về index nếu không tìm được
        } else {
            templateIndex
        }

        // ✅ Guard cuối
        if (resolvedIndex < 0 || resolvedIndex >= templates.size) {
            showToast(getString(R.string.download_failed_please_try_again_later))
            findNavController().navigateUp()
            return
        }

        when {
            isEdit && savedSelections != null -> {
                viewModel.initEditWithCustomizedId(
                    templateIndex = resolvedIndex,
                    customizedId = customizedId ?: "",
                    savedSelections = savedSelections,
                    isFlipped = isFlipped
                )
            }

            savedSelections != null -> {
                viewModel.initWithSelections(resolvedIndex, savedSelections)
            }

            else -> {
                viewModel.initNew(resolvedIndex)
            }
        }
    }

    private fun setupAdapters() {
        binding.rcvNav.apply {
            adapter = adapterNav
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(8)
        }
        binding.rcvColor.apply {
            adapter = adapterColor
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(8)
        }
        binding.rcvPart.apply {
            adapter = adapterPart
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(10)
        }
    }

    private fun closeScalePanel(animate: Boolean = false) {
        binding.frameScale.animate().cancel()
        isScaleActive = false
        if (animate && binding.frameScale.visibility == View.VISIBLE) {
            binding.frameScale.animate().alpha(0f).setDuration(200).withEndAction {
                binding.frameScale.visibility = View.GONE
                binding.rcvPart.visibility = View.VISIBLE
                binding.frameScale.alpha = 1f
            }.start()
        } else {
            binding.frameScale.alpha = 1f
            binding.frameScale.visibility = View.GONE
            binding.rcvPart.visibility = View.VISIBLE
        }
    }

    private fun toggleScalePanel() {
        binding.frameScale.animate().cancel()
        isScaleActive = !isScaleActive
        if (isScaleActive) {
            binding.imgScale.setImageResource(R.drawable.ic_scale_cus_open)
            binding.frameScale.visibility = View.VISIBLE
            binding.rcvPart.visibility = View.INVISIBLE
            binding.frameScale.alpha = 0f
            binding.frameScale.animate().alpha(1f).setDuration(200).start()
        } else {
            binding.imgScale.setImageResource(R.drawable.ic_scale_cus)
            closeScalePanel(animate = true)
        }
    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────
    override fun viewListener() {
        binding.imgScale.onClick {
            if (viewModel.resolvePathAt(viewModel.state.value.currentNavIndex) == null) return@onClick
            toggleScalePanel()
        }
//        binding.imgScale.onClick {
//            closeScalePanel()
//        }

        binding.ratioRight.onClickAndHold {
            changeCurrentTransform {
                it.copy(
                    rotation = normalizeRotation(
                        it.rotation + 5f
                    )
                )
            }
        }
        binding.ratioLeft.onClickAndHold {
            changeCurrentTransform {
                it.copy(
                    rotation = normalizeRotation(
                        it.rotation - 5f
                    )
                )
            }
        }
        binding.transitionLeft.onClickAndHold { changeCurrentTransform { it.copy(translationX = it.translationX - 20f) } }
        binding.transitionRight.onClickAndHold { changeCurrentTransform { it.copy(translationX = it.translationX + 20f) } }
        binding.transitionTop.onClickAndHold { changeCurrentTransform { it.copy(translationY = it.translationY - 20f) } }
        binding.transitionBottom.onClickAndHold { changeCurrentTransform { it.copy(translationY = it.translationY + 20f) } }
        binding.scalePlus.onClickAndHold { changeCurrentTransform { it.copy(scale = it.scale + 0.05f) } }
        binding.scaleMinus.onClickAndHold { changeCurrentTransform { it.copy(scale = it.scale - 0.05f) } }
        binding.btnResetScale.onClick {
            viewModel.resetTransform(viewModel.state.value.currentNavIndex)
            applyTransformsToAllLayers(viewModel.state.value)
            updateScaleControls()
        }
        adapterNav.onClick = { index ->
            closeScalePanel()
            if (!checkOnlineNetworkOrShowDialog()) {
                // Update the focus immediately; the state collector may also be
                // rendering a newly selected layer before its next UI pass.
                adapterNav.setPos(index)
                syncNavSelection(index)
            }
        }
        adapterColor.onClick = { index ->
            if (!checkOnlineNetworkOrShowDialog()) {
                adapterColor.setPos(index)
                viewModel.selectColor(index)
            }
        }
        adapterPart.onClick = { idx, type ->
            if (!checkOnlineNetworkOrShowDialog()) {
                when (type) {
                    "none" -> {
                        adapterPart.setPos(idx)
                        viewModel.selectNone()
                    }

                    "dice" -> {
                        scrollPartAfterRandom = true
                        viewModel.selectDiceCurrent()
                    }

                    else -> {
                        adapterPart.setPos(idx)
                        viewModel.selectPath(idx)
                    }
                }
            }
        }
        binding.apply {
            imgChangColor.onClick {
                val navPos = viewModel.state.value.currentNavIndex
                if (!viewModel.state.value.hasMultipleColors) return@onClick
                if (llColor.isVisible) {
                    if (navPos < arrShowColor.size) arrShowColor[navPos] = false
                    llColor.animate().alpha(0f).setDuration(200).withEndAction {
                        llColor.visibility = View.INVISIBLE
                    }.start()
                } else {
                    if (navPos < arrShowColor.size) arrShowColor[navPos] = true
                    llColor.visibility = View.VISIBLE
                    llColor.alpha = 0f
                    llColor.animate().alpha(1f).setDuration(200).start()
                }
            }
            imgRandom.onClick {
                if (!checkOnlineNetworkOrShowDialog()) {
                    showConfirmDialog(
                        message = getString(R.string.watch_a_short_ad_to_generate_a_random_anime),
                        title = getString(R.string.random),
                        yesText = getString(R.string.cancel),
                        noText = getString(R.string.ok),
                        onNo = {
                            showRewardAds1(
                                onRewardSuccess = {
                                    if (!checkOnlineNetworkOrShowDialog()) {
                                        viewModel.randomizeAll()
                                    }
                                },
                                onAdClosed = {}
                            )
                        },
                        onYes = { hideLoadingSafe() }
                    )
                }
            }
            actionBar.btnActionBarCenter.setOnClickListener {
                viewModel.toggleFlip()
            }
            actionBar.btnActionBarCenter2.setOnClickListener {
                showConfirmDialog(
                    title = getString(R.string.reset),
                    message = getString(R.string.do_you_want_to_reset_all),
                    onYes = {
                        arrShowColor.fill(true)
                        viewModel.resetAll()
                        // resetAll may leave CustomizeState equal to its previous value.
                        // In that case StateFlow does not emit, so reset the layer views here too.
                        applyTransformsToAllLayers(viewModel.state.value)
                        updateScaleControls()
                        closeScalePanel()
                        showInter {
                            // RecyclerView keeps its own scroll state; resetting the selected
                            // nav index does not move the list (and StateFlow may not re-emit).
                            binding.rcvNav.stopScroll()
                            binding.rcvNav.post {
                                binding.rcvNav.scrollToPosition(0)
                            }
                        }
                    }
                )
            }
            actionBar.btnActionBarLeft.onClick { confirmExit() }
            actionBar.btnActionBarRightText.onClick { if (canSave) performSave() }
            actionBar.btnActionBarRightText.setOnClickListener {
                if (!canSave) return@setOnClickListener
                if (checkOnlineNetworkOrShowDialog()) return@setOnClickListener

                performSave()

            }
        }
    }

    // ── OBSERVE ───────────────────────────────────────────────────────────────
    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    if (state.listData.isEmpty()) {
                        if (!hasTriggeredReInit) {
                            hasTriggeredReInit = true
                            readArgsAndInit()
                        }
                        return@collectLatest
                    }
                    hasTriggeredReInit = false  // reset khi state đã có data

                    if (layerViews.size != state.listData.size) {
                        buildLayerViews(state.listData)
                    }
                    updateAdapters(state)
                    renderLayers(state)
                    applyTransformsToAllLayers(state)
                }
            }
        }
    }

    // ── LAYER VIEWS ───────────────────────────────────────────────────────────

    private fun buildLayerViews(parts: List<BodyPartModel>) {
        val currentFlipped = viewModel.state.value.isFlipped  // ✅ lấy flip state hiện tại
        layerViews.clear()
        navToLayerIndex.clear()
        binding.rlCharacter.removeAllViews()

        parts.sortedBy { it.position }.forEachIndexed { layerIdx, bp ->
            val iv = AppCompatImageView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                scaleX = if (currentFlipped) -1f else 1f  // ✅ apply flip ngay khi tạo view
            }
            binding.rlCharacter.addView(iv)
            layerViews.add(iv)
            navToLayerIndex[bp.nav] = layerIdx
        }
    }

    // Reset pendingLoads mỗi khi bắt đầu render lại
    private fun renderLayers(state: CustomizeState) {
        // ✅ Reset counter trước khi đếm lại
        val pathsToLoad = state.listData.mapIndexedNotNull { i, bp ->
            val path = viewModel.resolvePathAt(i)
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
            // Không có gì cần load → enable save ngay
            setSaveEnabled(true)
            return
        }

        // Reset counter chính xác theo số ảnh thực sự cần load
        pendingLoads.set(pathsToLoad.size)
        setSaveEnabled(false)

        pathsToLoad.forEach { (view, path, _) ->
            view.tag = path
            view.visibility = View.VISIBLE
            loadImageIntoView(view, path, skipCount = true) // skipCount vì đã set ở trên
        }
    }

    // Thêm param skipCount để tránh double increment
    private fun loadImageIntoView(view: ImageView, path: String, skipCount: Boolean = false) {
        if (!skipCount) {
            pendingLoads.incrementAndGet()
            setSaveEnabled(false)
        }

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
            view?.post {
                setSaveEnabled(true)
                viewModel.onLoadingComplete()
            }
        }
    }

    // ✅ Thêm vào onResume: reset pendingLoads khi quay lại
    override fun onResume() {
        super.onResume()
        pendingLoads.set(0)

        val state = viewModel.state.value
        if (state.listData.isEmpty()) {
            if (!hasTriggeredReInit) {
                hasTriggeredReInit = true
                readArgsAndInit()
            }
            return
        }
        // ✅ Nếu state empty thì để observeData xử lý re-init

        val needRebuild = layerViews.isEmpty() ||
                layerViews.firstOrNull()?.isAttachedToWindow == false

        if (needRebuild) {
            layerViews.clear()
            navToLayerIndex.clear()
            binding.rlCharacter.removeAllViews()
            buildLayerViews(state.listData)
            renderLayers(state)
            updateAdapters(state)
            applyTransformsToAllLayers(state)
        } else {
            // Chỉ reload layer thực sự đã mất drawable. Xóa tag của toàn bộ
            // layer ở mỗi onResume làm Glide tải lại cả nhân vật.
            layerViews.forEach { layer ->
                if (layer.visibility == View.VISIBLE && layer.drawable == null) {
                    layer.tag = null
                }
            }
            renderLayers(state)
        }
    }

    private fun setSaveEnabled(enabled: Boolean) {
        canSave = enabled
        binding.actionBar.btnActionBarRight.alpha = if (enabled) 1f else 0.5f
        binding.actionBar.btnActionBarRight.isEnabled = enabled
    }

    // ── ADAPTERS ──────────────────────────────────────────────────────────────

    private fun updateAdapters(state: CustomizeState) {
        visibleNavIndices = state.listData.indices.toList()
        val visibleNavItems = visibleNavIndices.mapNotNull { state.listData.getOrNull(it) }
        val visibleNavPosition = visibleNavIndices.indexOf(state.currentNavIndex)
            .takeIf { it >= 0 } ?: 0

        if (adapterNav.items != visibleNavItems) {
            adapterNav.submitList(visibleNavItems)
        }
        adapterNav.setPos(visibleNavPosition.coerceIn(0, maxOf(0, visibleNavItems.lastIndex)))
//        binding.imgChangColor.isVisible = state.hasMultipleColors

        // ── Color ──────────────────────────────────────────────────────────────
        adapterColor.setPos(state.currentColorIndex)

        // Khởi tạo arrShowColor khi data load lần đầu
        if (arrShowColor.size != state.listData.size) {
            arrShowColor.clear()
            repeat(state.listData.size) { arrShowColor.add(true) }
        }

        val navPos = state.currentNavIndex

        if (state.hasMultipleColors) {
            if (adapterColor.items != state.currentColors) {
                adapterColor.submitList(state.currentColors)
            }
            updateColorSectionVisibility(
                showColors = true,
                expanded = navPos < arrShowColor.size && arrShowColor[navPos]
            )
        } else {
            updateColorSectionVisibility(showColors = false, expanded = false)
        }

        // ── Part ───────────────────────────────────────────────────────────────
        val bp = state.listData.getOrNull(state.currentNavIndex)
        val thumb = buildThumbList(bp, state.currentPaths)
        adapterPart.setPos(state.currentPathIndex)

        val targetPartIndex = state.currentPathIndex.coerceAtLeast(0)
        val partContentChanged = adapterPart.items != state.currentPaths ||
                adapterPart.listThumb != thumb
        adapterPart.listThumb = thumb
        if (partContentChanged) {
            adapterPart.submitList(state.currentPaths)
        }
        if (scrollPartAfterRandom) {
            scrollPartAfterRandom = false
            ensurePositionVisible(binding.rcvPart, targetPartIndex)
        }
        updateScaleControls()
    }

    private fun ensurePositionVisible(recyclerView: RecyclerView, position: Int) {
        if (position < 0) return
        recyclerView.post {
            if (view == null) return@post
            if (recyclerView.layoutManager?.findViewByPosition(position) == null) {
                recyclerView.scrollToPosition(position)
            }
        }
    }

    private fun updateColorSectionVisibility(showColors: Boolean, expanded: Boolean) {
        val showPanel = showColors && expanded
        binding.imgChangColor.visibility = if (showColors) View.VISIBLE else View.GONE

        if (showPanel &&
            (binding.llColor.visibility != View.VISIBLE || binding.llColor.alpha != 1f)
        ) {
            binding.llColor.animate().cancel()
            if (binding.llColor.visibility != View.VISIBLE) {
                binding.llColor.visibility = View.VISIBLE
                binding.llColor.alpha = 0f
            }
            binding.llColor.animate().alpha(1f).setDuration(150).start()
        } else if (!showPanel && binding.llColor.visibility == View.VISIBLE) {
            binding.llColor.animate().cancel()
            binding.llColor.animate().alpha(0f).setDuration(150).withEndAction {
                binding.llColor.visibility = View.GONE
            }.start()
        }
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

    private fun syncNavSelection(localNavIndex: Int) {
        val globalNavIndex = visibleNavIndices.getOrNull(localNavIndex) ?: return
        viewModel.selectNav(globalNavIndex)
    }

    private fun applyTransformsToAllLayers(state: CustomizeState) {
        state.listData.forEachIndexed { navIndex, bp ->
            val image = layerViews.getOrNull(navToLayerIndex[bp.nav] ?: return@forEachIndexed)
                ?: return@forEachIndexed
            val transform = viewModel.getTransform(navIndex)
            val targetScaleX = transform.scaleX * transform.scale *
                    if (state.isFlipped) -1f else 1f
            if (image.scaleX != targetScaleX) image.scaleX = targetScaleX
            if (image.scaleY != transform.scale) image.scaleY = transform.scale
            if (image.translationX != transform.translationX) {
                image.translationX = transform.translationX
            }
            if (image.translationY != transform.translationY) {
                image.translationY = transform.translationY
            }
            if (image.rotation != transform.rotation) image.rotation = transform.rotation
        }
    }

    private fun changeCurrentTransform(change: (LayerTransform) -> LayerTransform) {
        val index = viewModel.state.value.currentNavIndex
        val requested = change(viewModel.getTransform(index))
        val maxX = binding.rlCharacter.width.coerceAtLeast(1) * MAX_TRANSLATION_X_FRACTION
        val maxY = binding.rlCharacter.height.coerceAtLeast(1) / 2f
        viewModel.updateTransform(
            index, requested.copy(
                scale = requested.scale.coerceIn(MIN_LAYER_SCALE, MAX_LAYER_SCALE),
                translationX = requested.translationX.coerceIn(-maxX, maxX),
                translationY = requested.translationY.coerceIn(-maxY, maxY)
            )
        )
        applyTransformsToAllLayers(viewModel.state.value)
        updateScaleControls()
    }

    private fun updateScaleControls() {
        val index = viewModel.state.value.currentNavIndex
        val hasLayer = viewModel.resolvePathAt(index) != null
        val transform = viewModel.getTransform(index)
        val scale = transform.scale
        val canScaleUp = hasLayer && scale < MAX_LAYER_SCALE - SCALE_EPSILON
        val canScaleDown = hasLayer && scale > MIN_LAYER_SCALE + SCALE_EPSILON
        val maxX = binding.rlCharacter.width.coerceAtLeast(1) * MAX_TRANSLATION_X_FRACTION
        val maxY = binding.rlCharacter.height.coerceAtLeast(1) / 2f
        val canMoveLeft = hasLayer && transform.translationX > -maxX + TRANSFORM_EPSILON
        val canMoveRight = hasLayer && transform.translationX < maxX - TRANSFORM_EPSILON
        val canMoveUp = hasLayer && transform.translationY > -maxY + TRANSFORM_EPSILON
        val canMoveDown = hasLayer && transform.translationY < maxY - TRANSFORM_EPSILON

        binding.imgScale.isEnabled = hasLayer
        binding.imgScale.setImageResource(if (hasLayer) R.drawable.ic_scale_cus else R.drawable.ic_scale_cus_none)
        binding.scalePlus.isEnabled = canScaleUp
        binding.scalePlus.alpha = if (canScaleUp) 1f else 0.4f
        binding.scaleMinus.isEnabled = canScaleDown
        binding.scaleMinus.alpha = if (canScaleDown) 1f else 0.4f
        binding.transitionLeft.isEnabled = canMoveLeft
        binding.transitionLeft.alpha = if (canMoveLeft) 1f else 0.4f
        binding.transitionRight.isEnabled = canMoveRight
        binding.transitionRight.alpha = if (canMoveRight) 1f else 0.4f
        binding.transitionTop.isEnabled = canMoveUp
        binding.transitionTop.alpha = if (canMoveUp) 1f else 0.4f
        binding.transitionBottom.isEnabled = canMoveDown
        binding.transitionBottom.alpha = if (canMoveDown) 1f else 0.4f
        binding.btnResetScale.isEnabled = hasLayer && !viewModel.isTransformDefault(index)
        binding.btnResetScale.alpha = if (binding.btnResetScale.isEnabled) 1f else 0.4f
        if (!hasLayer) {
            closeScalePanel()
        }
    }

    private fun normalizeRotation(value: Float): Float = when {
        value >= 360f -> value - 360f
        value <= -360f -> value + 360f
        else -> value
    }

    private fun View.onClickAndHold(action: () -> Unit) {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isEnabled) return@setOnTouchListener false
                    holdActionJob?.cancel()
                    holdActionJob = null
                    action()
                    holdActionJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(400)
                        while (isEnabled) {
                            action()
                            delay(80)
                        }
                        holdActionJob = null
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holdActionJob?.cancel()
                    holdActionJob = null
                    performClick()
                    true
                }

                else -> false
            }
        }
    }

    // ── SAVE ──────────────────────────────────────────────────────────────────

    private fun performSave() {
        if (!canSave) return
        closeScalePanel(animate = true)
        setSaveEnabled(false)
        showLoadingSafe()

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = renderLayersToBitmap()
            if (bitmap == null) {
                setSaveEnabled(true)
                hideLoadingSafe()
                return@launch
            }
            viewModelActivity.customizeBitmap = bitmap
            val savedPath = withContext(Dispatchers.IO) {
                bitmap.saveToFile(requireActivity(), "avatar")
            }

            if (savedPath == null) {
                setSaveEnabled(true)
                hideLoadingSafe()
                return@launch
            }

            val result = viewModel.onSaveComplete(savedPath)
            val avatarUrl = result?.first?.avatar.orEmpty()
            result?.let { (template, selections) ->
                sharedViewModel.saveCharacterWithSelections(
                    character = template,
                    selections = selections,
                    imageSave = savedPath,
                    isFlipped = viewModel.state.value.isFlipped
                )
            }

            val isEdit = arguments?.getBoolean(ARG_IS_EDIT, false) ?: false
            val savedCharacter = result?.first
            if (savedCharacter == null) {
                setSaveEnabled(true)
                hideLoadingSafe()
                return@launch
            }
            // Khi edit, savedCharacter.id là ID của bản customized (UUID/timestamp),
            // không phải ID template. Lấy số từ thư mục avatar, ví dụ data_2 -> 2,
            // để event edit đồng nhất với event done lúc tạo mới.
            val dataFolder = Uri.parse(savedCharacter.avatar)
                .pathSegments
                .dropLast(1)
                .lastOrNull()
                .orEmpty()
            val number = dataFolder.filter { it.isDigit() }
                .ifEmpty {
                    savedCharacter.templateId
                        ?.filter { it.isDigit() }
                        .orEmpty()
                }
                .ifEmpty { savedCharacter.id.filter { it.isDigit() } }
            val eventName = "click_item_${number}_${if (isEdit) "edit" else "done"}"
            logEvent(eventName, savedCharacter.avatar)
            Log.d("LogEvenA","${eventName} -- ${savedCharacter.avatar}")

            if (isEdit) {
                runCatching {
                    findNavController()
                        .getBackStackEntry(R.id.viewFragment)
                        .savedStateHandle["updated_image_path"] = savedPath
                }
            }

            // ✅ Navigate trực tiếp trên Main thread, KHÔNG wrap thêm withContext
            if (isAdded && !isDetached) {
                findNavController().safeNavigate(
                    R.id.action_customizeFragment_to_addFragment,
                    Bundle().apply {
                        putString("imagePath", savedPath)
                        putString("avatarUrl", avatarUrl)
                    }
                )
            }
        }
    }

    private fun renderLayersToBitmap(): Bitmap? {
        val root = binding.rlCharacter
        if (root.width == 0 || root.height == 0) return null

        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        root.draw(canvas)

        return bitmap
    }
    // ── BASE OVERRIDES ────────────────────────────────────────────────────────

    override fun bindViewModel() {}

    //--------------------------------Backpress
    override fun onBackPressed(): Boolean {
        confirmExit()
        return true
    }

    // ── COMPANION ─────────────────────────────────────────────────────────────
    private fun confirmExit() {
        showConfirmDialog(
            message = getString(R.string.haven_t_saved_it_yet_do_you_want_to_exit),
            title = getString(R.string.exit),
            onYes = {
                showInter {
                    hideLoadingSafe()
                    findNavController().navigateUp()
                }
            },
            onNo = { hideLoadingSafe() }
        )
    }

    companion object {
        private const val MIN_LAYER_SCALE = 0.3f
        private const val MAX_LAYER_SCALE = 2f
        private const val MAX_TRANSLATION_X_FRACTION = 0.65f
        private const val SCALE_EPSILON = 0.0001f
        private const val TRANSFORM_EPSILON = 0.01f
        const val ARG_TEMPLATE_INDEX = "template_index"
        const val ARG_TEMPLATE_ID = "template_id"
        const val ARG_IS_EDIT = "is_edit"
        const val ARG_IS_FLIPPED = "is_flipped"
        const val ARG_SELECTIONS = "selections"
        const val ARG_CUSTOMIZED_ID = "customized_id"

        fun newArgs(
            templateIndex: Int,
            templateId: String? = null,
            isEdit: Boolean = false,
            customizedId: String? = null,
            savedSelections: ArrayList<SelectionIndex>? = null,
            isFlipped: Boolean = false
        ) = Bundle().apply {
            putInt(ARG_TEMPLATE_INDEX, templateIndex)
            templateId?.let { putString(ARG_TEMPLATE_ID, it) }
            putBoolean(ARG_IS_EDIT, isEdit)
            putBoolean(ARG_IS_FLIPPED, isFlipped)
            customizedId?.let { putString(ARG_CUSTOMIZED_ID, it) }
            savedSelections?.let { putParcelableArrayList(ARG_SELECTIONS, it) }
        }
    }
}
