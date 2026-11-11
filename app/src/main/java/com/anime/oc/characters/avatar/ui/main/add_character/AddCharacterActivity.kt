package com.anime.oc.characters.avatar.ui.main.add_character

import com.anime.oc.characters.avatar.core.extention.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.base.BackPressHandler
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.core.custom.Draw
import com.anime.oc.characters.avatar.core.custom.DrawableDraw
import com.anime.oc.characters.avatar.core.custom.listener.listenerdraw.OnDrawListener
import com.anime.oc.characters.avatar.core.dialog.ChooseColorDialog
import com.anime.oc.characters.avatar.core.dialog.DialogSpeech
import com.anime.oc.characters.avatar.core.navigation.Screen
import com.anime.oc.characters.avatar.core.helper.BitmapHelper
import com.anime.oc.characters.avatar.ui.main.add_character.adapter.BackgroundColorAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.anime.oc.characters.avatar.core.custom.DrawView
import com.anime.oc.characters.avatar.data.datalocal.manager.CharacterImageManager
import com.anime.oc.characters.avatar.ui.main.add_character.adapter.BackgroundImageAdapter
import com.anime.oc.characters.avatar.ui.main.add_character.adapter.SpeechAdapter
import com.anime.oc.characters.avatar.ui.main.add_character.adapter.StickerAdapter
import com.anime.oc.characters.avatar.ui.main.add_character.adapter.TextColorAdapter
import com.anime.oc.characters.avatar.ui.main.add_character.adapter.TextFontAdapter
import com.anime.oc.characters.avatar.ui.onboarding.permission.PermissionViewModel
import com.anime.oc.characters.avatar.utils.DataLocal
import com.anime.oc.characters.avatar.utils.key.ValueKey
import com.anime.oc.characters.avatar.databinding.ActivityAddCharacterBinding
import com.anime.oc.characters.avatar.utils.BlockableFrameLayout
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.compareTo

@AndroidEntryPoint
class AddCharacterActivity : BaseActivity<ActivityAddCharacterBinding, AddCharacterViewModel>(
    ActivityAddCharacterBinding::inflate,
    AddCharacterViewModel::class.java
), BackPressHandler {
    @Inject
    lateinit var imageManager: CharacterImageManager
    private val permissionViewModel: PermissionViewModel by viewModels()
    private var keyboardLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    // ── Keyboard state ──────────────────────────────────────────────────────
    // Source of truth duy nhất: layout change listener đo thực tế
    // KHÔNG dùng boolean flag nào trong ViewModel để control layout
    private var isKeyboardOpen = false

    // ── Adapters ─────────────────────────────────────────────────────────────
    private val backgroundImageAdapter by lazy { BackgroundImageAdapter() }

    private val backgroundColorAdapter by lazy { BackgroundColorAdapter() }
    private val stickerAdapter by lazy { StickerAdapter() }
    private val speechAdapter by lazy { SpeechAdapter() }
    private val textFontAdapter by lazy { TextFontAdapter(this@AddCharacterActivity) }
    private val textColorAdapter by lazy { TextColorAdapter() }

    private val imagepath: String by lazy {
        intent.extras?.getString("imagePath") ?: ""
    }

    private fun buttonNavigationList() = arrayListOf(
        binding.btnBackground,
        binding.btnSticker,
        binding.btnSpeech,
        binding.btnText,
    )

    private fun imageNavigationList() = arrayListOf(
        binding.imgBackground,
        binding.imgSticker,
        binding.imgSpeech,
        binding.imgText,
    )

    private fun layoutNavigationList() = arrayListOf(
        binding.lnlBackground.root,
        binding.lnlSticker,
        binding.lnlSpeech,
        binding.lnlText.scvText,
    )

    // ── Launchers ─────────────────────────────────────────────────────────────
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                this@AddCharacterActivity.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                handleSetBackgroundImage(uri.toString(), ADD_BACKGROUND_POSITION)
            }
        }


//    private fun launchImagePicker() {
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = "image/*"
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
//        }
//        imagePickerLauncher.launch(intent)
//    }

    // ── Inflate ───────────────────────────────────────────────────────────────

    override fun onRestart() {
        super.onRestart()
    }

    // ── Observe ───────────────────────────────────────────────────────────────
    override fun observeData() {
        this@AddCharacterActivity.lifecycleScope.launch {
            this@AddCharacterActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.typeNavigation.collect { type ->
                        if (type != -1) setupTypeNavigation(type)
                        this@AddCharacterActivity.hideNavigation(true)
                    }
                }
                launch {
                    viewModel.typeBackground.collect { type ->
                        if (type != -1) setupTypeBackground(type)
                    }
                }
                launch {
                    viewModel.backgroundImagePath.collect { path ->
                        path?.let { loadImage(this@AddCharacterActivity, it, binding.imvBackground) }
                    }
                }
                launch {
                    appSession.stickerCategories.collectLatest { categories ->
                        if (categories.isNotEmpty()) {
                            viewModel.setStickerCategories(categories)
                            stickerAdapter.submitList(viewModel.stickerList)
                        }
                    }
                }
                launch {
                    appSession.speechCategories.collectLatest { categories ->
                        if (categories.isNotEmpty()) {
                            viewModel.setSpeechCategories(categories)
                            val selected = viewModel.speechCategoryList.indexOfFirst { it.isSelected }
                            if (selected >= 0) viewModel.selectSpeechCategory(selected)
                            speechAdapter.submitList(viewModel.speechList)
                        }
                    }
                }
                launch {
                    appSession.backgrounds.collectLatest { bgs ->
                        if (bgs.isNotEmpty()) {
                            viewModel.loadDataFromMainViewModel(
                                bgs,
                                appSession.stickers.value,
                                appSession.speechs.value
                            )
                            backgroundImageAdapter.submitList(viewModel.backgroundImageList)
                            stickerAdapter.submitList(viewModel.stickerList, true)
                            speechAdapter.submitList(viewModel.speechList)
                        }
                    }
                }
            }
        }
    }

    override fun bindViewModel() {}

    // ── Listeners ─────────────────────────────────────────────────────────────
    override fun viewListener() {
        binding.apply {
            // Action bar
            actionBar.btnActionBarLeft.onClick { confirmExit() }
            actionBar.btnActionBarCenter1.onClick { confirmReset() }
            actionBar.btnActionBarRightText.onClick {
                handleSave()
            }

            // Background tabs
            lnlBackground.btnBackgroundImage.onClick {
                viewModel.setTypeBackground(ValueKey.IMAGE_BACKGROUND)
            }
            lnlBackground.btnBackgroundColor.onClick {
                viewModel.setTypeBackground(ValueKey.COLOR_BACKGROUND)
            }

            // Bottom navigation
            btnBackground.onClick {
                clearFocus()
                viewModel.isTextTabActive = false
                viewModel.setTypeNavigation(ValueKey.BACKGROUND_NAVIGATION)
            }
            btnSticker.onClick {
                clearFocus()
                viewModel.isTextTabActive = false
                viewModel.setTypeNavigation(ValueKey.STICKER_NAVIGATION)
            }
            btnSpeech.onClick {
                clearFocus()
                viewModel.isTextTabActive = false
                viewModel.setTypeNavigation(ValueKey.SPEECH_NAVIGATION)
            }
            btnText.onClick {
                viewModel.isTextTabActive = true
                viewModel.setTypeNavigation(ValueKey.TEXT_NAVIGATION)
            }

            // EditText
            lnlText.edtText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tvGetText.text = s.toString()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            lnlText.edtText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    clearFocus()
                    true
                } else false
            }

            lnlText.btnDoneText.onClick {
                handleDoneText()
                clearFocus(true)
            }

            // Click ngoài → đóng keyboard
            main.onClick { clearFocus() }

            // Adapters
            backgroundImageAdapter.onAddImageClick = { launchImagePicker() }
            backgroundImageAdapter.onNoneImageClick = { handleRemoveBackground() }
            backgroundImageAdapter.onBackgroundImageClick = { path, position ->
                if (checkNetworkBeforeRemoteAsset(path)) {
                    handleSetBackgroundImage(path, position)
                }
            }
            backgroundColorAdapter.onChooseColorClick = { handleChooseColor() }
            backgroundColorAdapter.onBackgroundColorClick = { color, position ->
                handleSetBackgroundColor(color, position)
            }
            stickerAdapter.onItemClick = {
                if (checkNetworkBeforeRemoteAsset(it)) addDrawable(it)
            }

            speechAdapter.onItemClick = { path ->
                if (checkNetworkBeforeRemoteAsset(path)) handleSpeech(path)
            }
            textFontAdapter.onTextFontClick = { font, position -> handleFontClick(font, position) }
            textColorAdapter.onChooseColorClick = { handleChooseColor(isTextColor = true) }
            textColorAdapter.onTextColorClick = { color, position ->
                handleTextColorClick(color, position)
            }
        }

        initActionBar()
        this@AddCharacterActivity.hideNavigation(true)
    }

    /** Chặn tải asset online khi thiết bị không có mạng. */
    private fun checkNetworkBeforeRemoteAsset(path: String): Boolean {
        if (!path.startsWith("http://") && !path.startsWith("https://")) return true
        if (!InternetExtension.isInternetAvailable(this@AddCharacterActivity) || !InternetExtension.isNetworkConnected(
                this@AddCharacterActivity
            )
        ) {
            showUnstableNetworkDialog()
            return false
        }
        return true
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    override fun initView() {
        this@AddCharacterActivity.hideNavigation(true)

        setupKeyboardListener()
        binding.tvGetText.setTextColor(this@AddCharacterActivity.getColor(R.color.black))

        initRcv()
        initDrawView()

        if (!viewModel.isInitialized) {
            initData()
            viewModel.isInitialized = true
        } else {
            hideLoadingSafe()
            restoreUIState()
        }

    }

    // ── Keyboard ──────────────────────────────────────────────────────────────

    /**
     * Source of truth duy nhất cho keyboard state và flFunction position.
     *
     * Logic:
     * - Keyboard lên (heightDiff > THRESHOLD):
     *     → Tab Text + speech dialog không mở → set bottomMargin = -170dp (cố định)
     *     → Các tab khác hoặc speech dialog đang mở → giữ nguyên (margin = 0)
     * - Keyboard xuống (heightDiff < -THRESHOLD):
     *     → Luôn reset margin = 0, bất kể tab nào
     */
    private fun setupKeyboardListener() {
        // Android 10+ dùng WindowInsets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
                val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (imeVisible && imeHeight > 0) onKeyboardOpen()
                else onKeyboardClose()
                insets
            }
        } else {
            // Android 9 trở xuống: dùng GlobalLayout
            setupKeyboardListenerLegacy()
        }
    }

    private fun setupKeyboardListenerLegacy() {
        val threshold = 150.dp(this@AddCharacterActivity)

        keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight > threshold) {
                onKeyboardOpen()
            } else {
                onKeyboardClose()
            }
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)
    }

    private fun onKeyboardOpen() {
        isKeyboardOpen = true
        if (viewModel.isTextTabActive && !viewModel.isSpeechDialogOpen) {
            binding.flFunction.translationY = (-170).dp(this@AddCharacterActivity).toFloat()
            binding.lnlBottom.translationY = (-170).dp(this@AddCharacterActivity).toFloat()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        keyboardLayoutListener?.let {
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        keyboardLayoutListener = null
    }

    private fun onKeyboardClose() {
        // ✅ Android 9-: ignore nếu speech dialog đang mở
        // vì GlobalLayoutListener fire false-close khi dialog transition
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            && viewModel.isSpeechDialogOpen
        ) return

        isKeyboardOpen = false
        binding.lnlBottom.translationY = 0f
        binding.flFunction.translationY = 0f
    }

    // ĐỔI TÊN + ĐỔI bottomMargin → topMargin
    private fun setFlFunctionTopMargin(margin: Int) {
        (binding.flFunction.layoutParams as ViewGroup.MarginLayoutParams).topMargin = margin
        (binding.lnlBottom.layoutParams as ViewGroup.MarginLayoutParams).topMargin = margin
    }

    /**
     * Đóng keyboard và reset view.
     * Dùng ở mọi nơi cần dismiss keyboard — backpress, click ngoài, done text, tab switch.
     */
    private fun collapseKeyboard() {
        binding.lnlText.edtText.clearFocus()
        binding.drawView.hideSelect()
        hideSoftKeyboard()
        // Reset view ngay lập tức, không đợi layout change
        setFlFunctionTopMargin(0)
    }

    private fun clearFocus(check: Boolean = false) {
        if (!check)
            binding.drawView.hideSelect()
        hideSoftKeyboard()
        setFlFunctionTopMargin(0)
        lifecycleScope.launch {
            delay(50)
            binding.lnlText.edtText.clearFocus()
        }
    }

    // ── Data ──────────────────────────────────────────────────────────────────
    private fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setImageActionBar(btnActionBarCenter1, R.drawable.ic_reset_all_bg)
            setMaterialCardViewActionBar1(
                btnActionBarRightText,
                tvRightText,
                getString(R.string.save)
            )
        }
    }

    private fun initRcv() {
        binding.apply {
            lnlBackground.rcvBackgroundImage.apply {
                adapter = backgroundImageAdapter; itemAnimator = null
                setHasFixedSize(true); setItemViewCacheSize(10)
            }
            lnlBackground.rcvBackgroundColor.apply {
                adapter = backgroundColorAdapter; itemAnimator = null
            }
            rcvSticker.apply {
                adapter = stickerAdapter; itemAnimator = null
                setHasFixedSize(true); setItemViewCacheSize(10)
            }

            rcvSpeech.apply {
                adapter = speechAdapter; itemAnimator = null
                setHasFixedSize(true); setItemViewCacheSize(10)
            }
            lnlText.rcvFont.apply { adapter = textFontAdapter; itemAnimator = null }
            lnlText.rcvTextColor.apply { adapter = textColorAdapter; itemAnimator = null }
        }
        this@AddCharacterActivity.hideNavigation(true)
    }

    private fun initData() {
        showLoadingSafe()
        this@AddCharacterActivity.lifecycleScope.launch {
            // Character/background canvas can be shown immediately. Remote
            // catalogues update their adapters later through observeData();
            // blocking here made Customize -> Background wait up to 15 seconds.
            viewModel.loadDataFromMainViewModel(
                appSession.backgrounds.value,
                appSession.stickers.value,
                appSession.speechs.value
            )
            // Màn hình mới chưa có background: chọn None trong danh sách ảnh.
            // Tab màu không còn mục None nên không chọn item nào ở đó.
            if (viewModel.selectedBackgroundImagePosition < 0 &&
                viewModel.selectedBackgroundImagePath == null &&
                viewModel.savedBackgroundColor == null
            ) {
                viewModel.updateBackgroundImageSelected(NONE_BACKGROUND_POSITION)
            }
            submitAllAdapters()
            applySelectedTextStyle()
            backgroundImageAdapter.selectItem(
                viewModel.backgroundImageList.indexOfFirst { it.isSelected }
            )
            backgroundColorAdapter.selectItem(
                viewModel.backgroundColorList.indexOfFirst { it.isSelected }
            )
            viewModel.setTypeNavigation(ValueKey.BACKGROUND_NAVIGATION)
            viewModel.setTypeBackground(ValueKey.IMAGE_BACKGROUND)

            val customizeBitmap = appSession.customizeBitmap
            if (customizeBitmap != null && !customizeBitmap.isRecycled) {
                binding.drawView.addDraw(
                    viewModel.loadDrawableEmoji(customizeBitmap, isCharacter = true)
                )
                appSession.customizeBitmap = null
            } else if (imagepath.isNotEmpty()) {
                addDrawable(imagepath, isCharacter = true) { hideLoadingAfterFirstFrame() }
                return@launch
            }
            hideLoadingAfterFirstFrame()
        }
    }

    /**
     * Giữ loading từ màn Custom cho tới khi màn Background đã sẵn sàng vẽ.
     * post sau pre-draw bảo đảm frame chứa character/background được render trước
     * khi dialog loading biến mất.
     */
    private fun hideLoadingAfterFirstFrame() {
        if (!isScreenAvailable || isDestroyed || contentView == null) return
        binding.root.doOnPreDraw {
            binding.root.post {
                if (isScreenAvailable && !isDestroyed && contentView != null) hideLoadingSafe()
            }
        }
        binding.root.invalidate()
    }

    private fun submitAllAdapters() {
        backgroundImageAdapter.submitList(viewModel.backgroundImageList)
        backgroundColorAdapter.submitList(viewModel.backgroundColorList, true)
        stickerAdapter.submitList(viewModel.stickerList, true)
        speechAdapter.submitList(viewModel.speechList)
        textFontAdapter.submitListReset(viewModel.textFontList)
        textColorAdapter.submitListReset(viewModel.textColorList)
    }

    private fun restoreUIState() {
        submitAllAdapters()
        applySelectedTextStyle()

        backgroundImageAdapter.selectItem(
            viewModel.backgroundImageList.indexOfFirst { it.isSelected }
        )
        backgroundColorAdapter.selectItem(
            viewModel.backgroundColorList.indexOfFirst { it.isSelected }
        )

        val currentNav = viewModel.typeNavigation.value
        if (currentNav != -1) setupTypeNavigation(currentNav)

        val currentBg = viewModel.typeBackground.value
        if (currentBg != -1) setupTypeBackground(currentBg)

        val imagePath = viewModel.backgroundImagePath.value
        val savedColor = viewModel.savedBackgroundColor
        when {
            imagePath != null -> {
                binding.imvBackground.setBackgroundColor(this@AddCharacterActivity.getColor(R.color.transparent))
                loadImage(this@AddCharacterActivity, imagePath, binding.imvBackground)
            }

            savedColor != null -> {
                binding.imvBackground.setImageBitmap(null)
                binding.imvBackground.setBackgroundColor(savedColor)
            }
        }

        if (viewModel.drawViewList.isNotEmpty()) {
            viewModel.isRestoringDraws = true
            binding.drawView.fillData(viewModel.drawViewList)
            viewModel.isRestoringDraws = false
        }
    }

    private fun applySelectedTextStyle() {
        viewModel.textFontList.firstOrNull { it.isSelected }?.color?.let { font ->
            binding.lnlText.edtText.setFont(font)
            binding.tvGetText.setFont(font)
        }
        viewModel.textColorList.firstOrNull { it.isSelected }?.color?.let { color ->
            binding.lnlText.edtText.setTextColor(color)
            binding.tvGetText.setTextColor(color)
        }
    }

    // ── DrawView ──────────────────────────────────────────────────────────────
    private fun initDrawView() {
        this@AddCharacterActivity.hideNavigation(true)
        binding.drawView.apply {
            setConstrained(true)
            setLocked(false)
            setOnDrawListener(object : OnDrawListener {
                override fun onAddedDraw(draw: Draw) {
                    if (!viewModel.isRestoringDraws) {
                        viewModel.updateCurrentCurrentDraw(draw)
                        viewModel.addDrawView(draw)
                    }
                }

                override fun onClickedDraw(draw: Draw) {}
                override fun onDeletedDraw(draw: Draw) {
                    viewModel.deleteDrawView(draw)
                }

                override fun onDragFinishedDraw(draw: Draw) {}
                override fun onTouchedDownDraw(draw: Draw) {
                    viewModel.updateCurrentCurrentDraw(draw)
                }

                override fun onZoomFinishedDraw(draw: Draw) {}
                override fun onFlippedDraw(draw: Draw) {}
                override fun onDoubleTappedDraw(draw: Draw) {}
                override fun onHideOptionIconDraw() {}
                override fun onUndoDeleteDraw(draw: List<Draw?>) {}
                override fun onUndoUpdateDraw(draw: List<Draw?>) {}
                override fun onUndoDeleteAll() {}
                override fun onRedoAll() {}
                override fun onReplaceDraw(draw: Draw) {}
                override fun onEditText(draw: DrawableDraw) {}
                override fun onReplace(draw: Draw) {}
            })
        }
    }

    private fun addDrawable(
        path: String,
        isCharacter: Boolean = false,
        bitmapText: Bitmap? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (bitmapText != null) {
            binding.drawView.addDraw(viewModel.loadDrawableEmoji(bitmapText, isCharacter))
            onDone?.invoke()
            return
        }
        Glide.with(this)
            .asBitmap()
            .load(path)
            .override(512, 512)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .format(DecodeFormat.PREFER_ARGB_8888)
            .disallowHardwareConfig()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    binding.drawView.addDraw(viewModel.loadDrawableEmoji(resource, isCharacter))
                    this@AddCharacterActivity.hideNavigation(true)
                    onDone?.invoke()
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    showToast("Don't Download sticker")
                    onDone?.invoke()
                }
            })
    }

    // ── UI setup ──────────────────────────────────────────────────────────────
    private fun setupTypeBackground(type: Int) {
        binding.apply {
            val isImageSelected = type == ValueKey.IMAGE_BACKGROUND

            lnlBackground.btnBackgroundImage.setBackgroundResource(
                if (isImageSelected) R.drawable.frame_select_add else R.drawable.frame_un_select_add
            )

            lnlBackground.btnBackgroundColor.setBackgroundResource(
                if (isImageSelected)R.drawable.frame_un_select_add else R.drawable.frame_select_add
            )

            lnlBackground.btnBackgroundImageTv.isSelected = isImageSelected
            lnlBackground.btnBackgroundColorTv.isSelected = !isImageSelected

            when (type) {
                ValueKey.IMAGE_BACKGROUND -> {
                    lnlBackground.rcvBackgroundColor.gone()
                    lnlBackground.tabImage.visible()
                }

                ValueKey.COLOR_BACKGROUND -> {
                    lnlBackground.rcvBackgroundColor.visible()
                    lnlBackground.tabImage.gone()
                }
            }
        }
    }

    private fun setupTypeNavigation(type: Int) {
        val buttons = buttonNavigationList()
        val images = imageNavigationList()
        val layouts = layoutNavigationList()

        buttons.forEachIndexed { index, _ ->
            val isSelected = index == type
            val iconRes = if (isSelected) {
                DataLocal.bottomNavigationSelected.getOrNull(index)

            } else {
                DataLocal.bottomNavigationNotSelect.getOrNull(index)
            }
            iconRes?.let { images.getOrNull(index)?.setImageResource(it) }
            layouts.getOrNull(index)?.isVisible = isSelected
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────
    private fun confirmExit() {
        clearFocus()
        showConfirmDialog(
            message = getString(R.string.haven_t_saved_it_yet_do_you_want_to_exit),
            title = getString(R.string.exit),
            onYes = { hideLoadingSafe(); popBack() },
            onNo = { hideLoadingSafe() }
        )
    }

    private fun confirmReset() {
        clearFocus()
        showConfirmDialog(
            message = getString(R.string.do_you_want_to_reset_all),
            title = getString(R.string.reset),
            onYes = {
                showLoadingSafe()
                viewModel.loadDataFromMainViewModel(
                    appSession.backgrounds.value,
                    appSession.stickers.value,
                    appSession.speechs.value
                )
                textFontAdapter.submitListReset(viewModel.textFontList)
                textColorAdapter.submitListReset(viewModel.textColorList)

                val defaultFont = viewModel.textFontList.firstOrNull()?.color
                val defaultColor = viewModel.textColorList.getOrNull(1)?.color
                defaultFont?.let {
                    binding.lnlText.edtText.setFont(it)
                    binding.tvGetText.setFont(it)
                }
                defaultColor?.let {
                    binding.lnlText.edtText.setTextColor(it)
                    binding.tvGetText.setTextColor(it)
                }
                viewModel.resetDraw()
                viewModel.setBackgroundImage(null)
                viewModel.selectedBackgroundImagePath = null
                viewModel.updateBackgroundImageSelected(NONE_BACKGROUND_POSITION)
                viewModel.savedBackgroundColor = null
                binding.drawView.removeAllDraw()
                binding.imvBackground.setImageBitmap(null)
                binding.imvBackground.setBackgroundColor(this@AddCharacterActivity.getColor(R.color.transparent))
                backgroundImageAdapter.submitList(viewModel.backgroundImageList)
                backgroundImageAdapter.clearSelection()
                backgroundImageAdapter.selectItem(NONE_BACKGROUND_POSITION)
                backgroundColorAdapter.submitList(viewModel.backgroundColorList)
                backgroundColorAdapter.clearSelection()
                hideLoadingSafe()

                // ✅ Ưu tiên bitmap đã cache, fallback về imagepath
                val cachedBitmap = appSession.customizeBitmap
                if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                    binding.drawView.addDraw(
                        viewModel.loadDrawableEmoji(cachedBitmap, isCharacter = true)
                    )
                    hideLoadingSafe()
                } else if (imagepath.isNotEmpty()) {
                    addDrawable(imagepath, isCharacter = true)
                }
            },
            onNo = { hideLoadingSafe() }
        )
    }

    private fun handleSetBackgroundImage(path: String, position: Int) {
        viewModel.setBackgroundImage(path)
        viewModel.selectedBackgroundImagePath = path
        viewModel.savedBackgroundColor = null
        binding.imvBackground.setBackgroundColor(this@AddCharacterActivity.getColor(R.color.transparent))
        loadImage(this@AddCharacterActivity, path, binding.imvBackground)
        viewModel.updateBackgroundImageSelected(position)
        backgroundColorAdapter.clearSelection()
        backgroundImageAdapter.selectItem(position)
    }

    private fun handleSetBackgroundColor(color: Int, position: Int) {
        binding.imvBackground.setImageBitmap(null)
        binding.imvBackground.setBackgroundColor(color)
        viewModel.savedBackgroundColor = color
        viewModel.setBackgroundImage(null)
        viewModel.selectedBackgroundImagePath = null
        viewModel.selectedBackgroundImagePosition = -1
        viewModel.updateBackgroundColorSelected(position)
        backgroundImageAdapter.clearSelection()
        backgroundColorAdapter.selectItem(position)
    }

    private fun handleRemoveBackground() {
        viewModel.setBackgroundImage(null)
        viewModel.selectedBackgroundImagePath = null
        viewModel.selectedBackgroundImagePosition = NONE_BACKGROUND_POSITION
        viewModel.savedBackgroundColor = null
        Glide.with(this).clear(binding.imvBackground)
        binding.imvBackground.setImageDrawable(null)
        binding.imvBackground.setBackgroundColor(
            this@AddCharacterActivity.getColor(R.color.transparent)
        )

        viewModel.updateBackgroundImageSelected(NONE_BACKGROUND_POSITION)
        backgroundImageAdapter.selectItem(NONE_BACKGROUND_POSITION)
        backgroundColorAdapter.clearSelection()
    }


    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        imagePickerLauncher.launch(intent)
    }

    private companion object {
        const val ADD_BACKGROUND_POSITION = 0
        const val NONE_BACKGROUND_POSITION = 1
        const val CUSTOM_BACKGROUND_COLOR_POSITION = 0
    }

    private fun handleChooseColor(isTextColor: Boolean = false) {
        val dialog = ChooseColorDialog(this@AddCharacterActivity)
        dialog.show()
        dialog.onCloseEvent = { dialog.dismiss() }
        dialog.onDoneEvent = { color ->
            dialog.dismiss()
            when {
                isTextColor -> handleTextColorClick(color, 0)
                else -> handleSetBackgroundColor(color, CUSTOM_BACKGROUND_COLOR_POSITION)
            }
        }
    }

    private fun handleSpeech(path: String) {
        viewModel.isSpeechDialogOpen = true
        binding.lnlText.edtText.clearFocus()
        hideSoftKeyboard()

        val dialog = DialogSpeech(
            this@AddCharacterActivity,
            path
        )

        // ✅ Android 9-: SOFT_INPUT_STATE_VISIBLE để keyboard tự hiện
        // BaseDialog đã set ADJUST_RESIZE, chỉ cần OR thêm state
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            )
        }

        dialog.show()

        dialog.onDoneClick = { bitmap ->
            dialog.dismiss()
            this@AddCharacterActivity.hideNavigation(true)
            if (bitmap != null) addDrawable("", bitmapText = bitmap)
        }

        dialog.setOnDismissListener {
            viewModel.isSpeechDialogOpen = false
            setFlFunctionTopMargin(0)
            this@AddCharacterActivity.hideNavigation(true)
        }
    }

    private fun handleFontClick(font: Int, position: Int) {
        binding.lnlText.edtText.setFont(font)
        binding.tvGetText.setFont(font)
        viewModel.updateTextFontSelected(position)
        textFontAdapter.submitItem(position, viewModel.textFontList)
    }

    private fun handleTextColorClick(color: Int, position: Int) {
        binding.lnlText.edtText.setTextColor(color)
        binding.tvGetText.setTextColor(color)
        // Với màu custom (item 0), cần lưu cả giá trị màu vào model để
        // DialogSpeech đọc lại đúng màu đang được chọn.
        viewModel.updateTextColorSelected(position, color)
        textColorAdapter.submitItem(position, viewModel.textColorList)
    }


    @SuppressLint("SimpleDateFormat")
    private fun handleDoneText() {
        clearFocus()
        binding.apply {
            val text = lnlText.edtText.text.toString().trim()
            if (text.isEmpty()) {
                showToast(getString(R.string.null_edt))
                return
            }
            tvGetText.text = text
            val bitmap = BitmapHelper.getBitmapFromEditText(tvGetText)
            drawView.addDraw(viewModel.loadDrawableEmoji(bitmap, isText = true))

            // Reset text tab
            val font = viewModel.textFontList.first().color
            val color = viewModel.textColorList[1].color
            lnlText.edtText.text = null
            lnlText.edtText.setFont(font)
            lnlText.edtText.setTextColor(color)
            viewModel.updateTextFontSelected(0)
            viewModel.updateTextColorSelected(1)
            textFontAdapter.submitListReset(viewModel.textFontList)
            textColorAdapter.submitListReset(viewModel.textColorList)
            tvGetText.text = ""
            tvGetText.setFont(font)
            tvGetText.setTextColor(color)
        }
    }

    private fun handleSave() {
        clearFocus()
        this@AddCharacterActivity.lifecycleScope.launch {
            showLoadingSafe()
            try {
                val bitmap = binding.flSave.drawToBitmap()
                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val designId = "design_$timestamp"

                val savedImagePath = withContext(Dispatchers.IO) {
                    imageManager.deleteOldImage(designId)
                    val path = imageManager.saveBitmap(bitmap, designId)
                    if (path != null) appSession.appDataManager.addMyDesignPath(path)
                    path
                }
                hideLoadingSafe()

                if (savedImagePath != null) {

                            openScreen(
                                Screen.SUCCESS,
                                Bundle().apply {
                                    putString("imagePath", savedImagePath)
                                    putString("avatarUrl", intent.extras?.getString("avatarUrl").orEmpty())
                                    putString("idEdit", "")
                                    putInt("imageType", 0)
                                }
                            )

                } else {
                    Toast.makeText(this@AddCharacterActivity, "Lưu thất bại!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                hideLoadingSafe()
                Toast.makeText(this@AddCharacterActivity, "Có lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Back press ────────────────────────────────────────────────────────────
    /**
     * Logic backpress:
     * - Keyboard đang mở → đóng keyboard, KHÔNG back
     * - Keyboard đóng → hiện confirm dialog
     */
    override fun onScreenBackPressed(): Boolean {
        return if (isKeyboardOpen) {
            collapseKeyboard()
            true // consumed
        } else {
            confirmExit()
            true // consumed
        }
    }
}
