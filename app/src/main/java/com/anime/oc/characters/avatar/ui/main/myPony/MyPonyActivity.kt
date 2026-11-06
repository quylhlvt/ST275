package com.anime.oc.characters.avatar.ui.main.myPony

import com.anime.oc.characters.avatar.core.extention.*
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.core.dialog.CreateNameDialog
import com.anime.oc.characters.avatar.core.navigation.Screen
import com.anime.oc.characters.avatar.data.model.mypony.MyAlbumModel
import com.anime.oc.characters.avatar.ui.main.customize.CustomizeActivity
import com.anime.oc.characters.avatar.ui.main.myPony.adapter.MyAvatarAdapter
import com.anime.oc.characters.avatar.ui.main.myPony.adapter.MyDesignAdapter
import com.anime.oc.characters.avatar.ui.onboarding.permission.PermissionViewModel
import com.anime.oc.characters.avatar.utils.share.whatsapp.WhatsappSharingActivity
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.ActivityMyPonyBinding
import com.anime.oc.characters.avatar.utils.BlockableFrameLayout
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class MyPonyActivity : WhatsappSharingActivity<ActivityMyPonyBinding, MyPonyViewModel>(
    ActivityMyPonyBinding::inflate,
    MyPonyViewModel::class.java
) {
    private lateinit var myAvatarAdapter: MyAvatarAdapter
    private lateinit var myDesignAdapter: MyDesignAdapter
    private var pendingDownloadPaths: ArrayList<String> = arrayListOf()
    private val permissionViewModel: PermissionViewModel by viewModels()

    private val isAvatarTab = MutableStateFlow(true)

    companion object {
        private const val ADD_PACK_REQUEST = 200
        private const val MIN_STICKERS_WHATSAPP = 3
        private const val MAX_STICKERS_WHATSAPP = 30
    }

    private fun performBatchDownload() {
        viewModel.downloadFiles(this@MyPonyActivity, pendingDownloadPaths)
        resetSelection()
    }
    // ── INIT ──────────────────────────────────────────────────────────────────
    private fun initNativeCollab() {
        loadNativeCollabAds(
            getString(R.string.native_cl_myLib), binding.flNativeCollab
        )
    }
    private  fun showInter(action: (()-> Unit)){
        Admob.getInstance().showInterAll(this@MyPonyActivity, object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                action()

            }
        })
    }
    override fun onRestart() {
        super.onRestart()
        initNativeCollab()
    }
    override fun initView() {
        initNativeCollab()
        Admob.getInstance().loadNativeAd(
            this@MyPonyActivity,
            getString(R.string.native_my_lib),
            binding.nativeAds,
            R.layout.ads_native_collap_banner_2
        )
        binding.apply {
            tvWhatApp.isSelected = true
            tvTelegram.isSelected = true

        }
        setupActionBar()
        setupTabs()
        setupRecyclerViews()
        setupBottomButtons()
        setupTouchListenerForResetSelection()
        loadAvatarData()
    }

    private fun setupActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setImageActionBar(btnActionBarNextToRight1, R.drawable.ic_delete_all)
            setTextActionBar(tvCenter, getString(R.string.my_creation))
            setImageActionBar(
                btnActionBarRight1,
                R.drawable.ic_select_all
            )

            btnActionBarRight1.invisible()
            btnActionBarNextToRight1.invisible()
        }
    }

    private fun setupTabs() {
        binding.btnMyAvatar.onClick { switchTab(true) }
        binding.btnMyDesign.onClick { switchTab(false) }
    }

    private fun switchTab(isAvatar: Boolean) {
        isAvatarTab.value = isAvatar
        applyTabUI(isAvatar)
        resetSelection()
    }

    private fun applyTabUI(isAvatar: Boolean) {
        binding.apply {

            if (isAvatar) {
                imgMyponyFor2.setImageResource(R.drawable.bg_btn_type_unselected)
                imgMyponyFor.setImageResource(R.drawable.bg_btn_type_selected)
                tvMyAvatar.setTextColor(ContextCompat.getColor(this@MyPonyActivity, R.color.app_color))
                tvMyDesign.setTextColor(ContextCompat.getColor(this@MyPonyActivity, R.color.app_color2))
                recycleAvatar.visible()
                recycleDesign.gone()
                updateEmptyState(myAvatarAdapter.items.isEmpty())
                // ❌ Bỏ loadAvatarData() — dùng StateFlow
            } else {
                imgMyponyFor2.setImageResource(R.drawable.bg_btn_type_selected)
                imgMyponyFor.setImageResource(R.drawable.bg_btn_type_unselected)
                tvMyDesign.setTextColor(ContextCompat.getColor(this@MyPonyActivity, R.color.app_color))
                tvMyAvatar.setTextColor(ContextCompat.getColor(this@MyPonyActivity, R.color.app_color2))
                recycleAvatar.gone()
                recycleDesign.visible()
                updateEmptyState(myDesignAdapter.items.isEmpty())
                loadDesignData() // Design vẫn load thủ công vì không có StateFlow
            }
            updateSelectionUI()
        }
    }

    private fun setupRecyclerViews() {
        myAvatarAdapter = MyAvatarAdapter(this@MyPonyActivity).apply {
            onItemClick = { item -> handleItemClick(item.path, true, 1, item.idEdit) }
            onLongClick = { position -> handleLongClick(position, true) }
            onItemTick = { position -> toggleSelection(position, true) }
            onEditClick = { idEdit ->
                if (ensureEditItemExists(idEdit)) {
                    showInter {navigateToEdit(idEdit)}
                }
            }
            onDeleteClick = { path -> confirmDelete(arrayListOf(path), true) }
        }
        binding.recycleAvatar.apply {
            layoutManager = GridLayoutManager(this@MyPonyActivity, 2)
            adapter = myAvatarAdapter
            animation = null
        }

        myDesignAdapter = MyDesignAdapter().apply {
            onItemClick = { path -> handleItemClick(path, false, 2, "0") }
            onLongClick = { position -> handleLongClick(position, false) }
            onItemTick = { position -> toggleSelection(position, false) }
            onDeleteClick = { path -> confirmDelete(arrayListOf(path), false) }
        }
        binding.recycleDesign.apply {
            layoutManager = GridLayoutManager(this@MyPonyActivity, 2)
            adapter = myDesignAdapter
            animation = null
        }
    }

    private fun ensureEditItemExists(idEdit: String): Boolean {
        val exists = idEdit.isNotBlank() &&
                appSession.customizedCharacters.value.any { it.id == idEdit }
        if (!exists) showEditItemNotFoundDialog()
        return exists
    }

    private fun showEditItemNotFoundDialog() {
    showOkDialog(
            title = getString(R.string.error),
            message = getString(R.string.errorcontent)
        )
    }

    private fun setupBottomButtons() {
        binding.apply {
            btnWhatsapp.onClick(1000) { handleWhatsAppShare() }
            btnTelegram.onClick(1000) { handleTelegramShare() }
            btnDownload.onClick(1000) { handleDownload() }
            btnShare.onClick(1000) { handleShare() }
            actionBar.btnActionBarNextToRight1.onClick { handleDeleteSelected() }
            actionBar.btnActionBarRight1.onClick { handleSelectAll() }

        }
    }

    private fun handleShare() {
        val selected = getSelectedItems()
        if (selected.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }
        val paths = selected.map { it.path }.filter { it.isNotEmpty() }
        if (paths.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }

        val uris = ArrayList(paths.map { path ->
            FileProvider.getUriForFile(
                this@MyPonyActivity,
                "${this@MyPonyActivity.packageName}.provider",
                File(path)
            )
        })

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share)))
        resetSelection()
    }

    private fun setupTouchListenerForResetSelection() {
        val touchListener = object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_UP && rv.findChildViewUnder(e.x, e.y) == null) {
                    resetSelection()
                    return true
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallow: Boolean) {}
        }
        binding.recycleAvatar.addOnItemTouchListener(touchListener)
        binding.recycleDesign.addOnItemTouchListener(touchListener)
    }

    // ── OBSERVE ───────────────────────────────────────────────────────────────
    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ✅ Chỉ dùng 1 nguồn duy nhất cho avatar
                launch {
                    appSession.customizedCharacters.collect { customized ->
                        val list = customized
                            .filter {
                                it.imageSave.isNotEmpty() && File(it.imageSave).exists()
                            }
                            .sortedByDescending { it.createdAt } // ← dùng createdAt đã fix
                            .map { MyAlbumModel(path = it.imageSave, idEdit = it.id, type = 1) }

                        myAvatarAdapter.submitList(list)
                        if (isAvatarTab.value) updateEmptyState(list.isEmpty())
                        updateSelectionUI()
                    }
                }

                // Design giữ nguyên
                launch {
                    viewModel.myDesignList.collect { list ->
                        myDesignAdapter.submitList(list)
                        if (!isAvatarTab.value) updateEmptyState(list.isEmpty())
                        updateSelectionUI()
                    }
                }

                launch {
                    viewModel.downloadState.collect { state ->
                        when (state) {
                            MyPonyViewModel.DownloadState.SUCCESS -> {
                                showToast(getString(
                                        R.string.download_success,
                                        getString(R.string.app_name)
                                    )
                                )
                                viewModel.resetDownloadState()
                            }

                            MyPonyViewModel.DownloadState.ERROR -> {
                                showToast(R.string.download_failed_please_try_again_later)
                                viewModel.resetDownloadState()
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    // ── UI HELPERS ────────────────────────────────────────────────────────────

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.noItem.isVisible = isEmpty
    }

    private fun updateSelectionUI() {
        val currentList = if (isAvatarTab.value) myAvatarAdapter.items else myDesignAdapter.items
        val hasSelection = currentList.any { it.isShowSelection }
        val allSelected = currentList.isNotEmpty() && currentList.all { it.isSelected }



        binding.apply {
            if (isAvatarTab.value) {
                val hasAvatars = myAvatarAdapter.items.isNotEmpty()

                if (!hasAvatars) {
                    // ✅ Không có item → ẩn hết
                    lnlBottom.gone()
                    return
                }

                lnlBottom.visible()

                if (hasSelection) {
                    // ✅ Long click: hiện cả 4 nút
                    lnlBottomTop.visible()  // WhatsApp + Telegram
                    lnlBottomTopShadown.visible()
                    llBottomShadown.visible()
                    llBottom.visible()      // Share + Download
                    actionBar.apply {
                        btnActionBarNextToRight1.visible()
                        btnActionBarRight1.visible()
                        btnActionBarRight1.setImageResource(
                            if (allSelected) R.drawable.ic_select_all else R.drawable.ic_not_select_all
                        )
                    }
                    // Share + Download
                } else {
                    // ✅ Bình thường có item: chỉ WhatsApp + Telegram
                    lnlBottomTop.visible()
                    llBottom.gone()
                    lnlBottomTopShadown.visible()
                    llBottomShadown.gone()
                    actionBar.apply {
                        btnActionBarRight1.invisible()
                        btnActionBarNextToRight1.invisible()
                    }
                }

            } else {
                // Design tab
                if (hasSelection) {
                    // ✅ Long click: chỉ Share + Download
                    lnlBottom.visible()
                    actionBar.apply {
                        btnActionBarRight1.visible()
                        btnActionBarNextToRight1.visible()
                        btnActionBarRight1.setImageResource(
                            if (allSelected) R.drawable.ic_select_all else R.drawable.ic_not_select_all
                        )
                    }
                    lnlBottomTop.gone()
                    llBottom.visible()
                    lnlBottomTopShadown.gone()
                    llBottomShadown.visible()
                } else {
                    // ✅ Bình thường hoặc không có item: ẩn hết
                    lnlBottom.gone()
                    llBottom.gone()
                    llBottomShadown.gone()
                    actionBar.apply {
                        btnActionBarRight1.invisible()
                        btnActionBarNextToRight1.invisible()
                    }
                }
            }
        }
    }

    // ── DATA LOADING ──────────────────────────────────────────────────────────

    private fun loadAvatarData() = viewModel.loadMyAvatar(this@MyPonyActivity, true)
    private fun loadDesignData() = viewModel.loadMyDesign(this@MyPonyActivity)

    // ── SELECTION ─────────────────────────────────────────────────────────────

    private fun handleItemClick(path: String, isAvatar: Boolean, type: Int, idEdit: String) {
//        val currentList = if (isAvatar) myAvatarAdapter.items else myDesignAdapter.items
//        if (currentList.any { it.isShowSelection }) {
//            val position = currentList.indexOfFirst { it.path == path }
//            if (position >= 0) toggleSelection(position, isAvatar)
//        } else {
//            navigateToView(path, type, idEdit)
//        }
        showInter { navigateToView(path, type, idEdit)}
    }
// MyAvatarAdapter — long click gọi về Fragment


    // Fragment nhận và gọi ViewModel

    private fun handleLongClick(position: Int, isAvatar: Boolean) {
        val currentList = if (isAvatar) myAvatarAdapter.items else myDesignAdapter.items
        val updatedList = currentList.mapIndexed { index, item ->
            if (index == position) {
                item.copy(isSelected = true, isShowSelection = true)
            } else {
                item.copy(isShowSelection = true)
            }
        }
        if (isAvatar) {
            myAvatarAdapter.submitList(updatedList)
            setRecyclerBottomMargin(binding.recycleAvatar, 100)
        } else {
            myDesignAdapter.submitList(updatedList)
            setRecyclerBottomMargin(binding.recycleDesign, 50)
        }
        updateSelectionUI()
    }

    private fun setRecyclerBottomMargin(view: RecyclerView, dpValue: Int) {
        val px = (dpValue * resources.displayMetrics.density).toInt()
        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            bottomMargin = px
            view.layoutParams = this
        }
    }

    private fun toggleSelection(position: Int, isAvatar: Boolean) {
        val currentList = if (isAvatar) myAvatarAdapter.items else myDesignAdapter.items
        val updatedList = currentList.mapIndexed { index, item ->
            if (index == position) item.copy(isSelected = !item.isSelected) else item
        }
        if (isAvatar) myAvatarAdapter.submitList(updatedList)
        else myDesignAdapter.submitList(updatedList)

        updateSelectionUI()
        if (updatedList.none { it.isSelected }) resetSelection()
    }

    private fun handleDeleteSelected() {
        val selected = getSelectedItems()
        if (selected.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }
        val paths = ArrayList(selected.map { it.path })
        confirmDelete(paths, isAvatarTab.value)
    }

    private fun handleSelectAll() {
        val currentList = if (isAvatarTab.value) myAvatarAdapter.items else myDesignAdapter.items
        val shouldSelectAll = !currentList.all { it.isSelected }
        val updatedList =
            currentList.map { it.copy(isSelected = shouldSelectAll, isShowSelection = true) }
        if (isAvatarTab.value) myAvatarAdapter.submitList(updatedList)
        else myDesignAdapter.submitList(updatedList)
        updateSelectionUI()
    }

    private fun resetSelection() {
        val avatarReset =
            myAvatarAdapter.items.map { it.copy(isSelected = false, isShowSelection = false) }
        val designReset =
            myDesignAdapter.items.map { it.copy(isSelected = false, isShowSelection = false) }
        myAvatarAdapter.submitList(avatarReset)
        myDesignAdapter.submitList(designReset)

        // Reset margin về 0
        setRecyclerBottomMargin(binding.recycleAvatar, 0)
        setRecyclerBottomMargin(binding.recycleDesign, 0)

        updateSelectionUI()
    }

    private fun getSelectedItems(): List<MyAlbumModel> {
        val currentList = if (isAvatarTab.value) myAvatarAdapter.items else myDesignAdapter.items
        val inSelectionMode = currentList.any { it.isShowSelection }
        val selected = currentList.filter { it.isSelected }

        return when {
            // Đang ở chế độ selection (đã long click) → tôn trọng lựa chọn, kể cả rỗng → sẽ toast
            inSelectionMode -> selected
            // Không ở chế độ selection → fallback lấy toàn bộ list
            else -> currentList
        }
    }
//    private fun getSelectedItems(): List<MyAlbumModel> {
//        val currentList = if (isAvatarTab.value) myAvatarAdapter.items else myDesignAdapter.items
//        val selected = currentList.filter { it.isSelected }
//        // ✅ Nếu không chọn gì → trả về toàn bộ list
//        return if (selected.isEmpty()) currentList else selected
//    }
    // ── NAVIGATION ────────────────────────────────────────────────────────────

    private fun navigateToView(path: String, type: Int, idEdit: String) {
        openScreen(Screen.VIEW, Bundle().apply {
            putString("imagePath", path)
            putString("idEdit", idEdit)
            putInt("imageType", type)
        })
    }

    /**
     * Navigate sang CustomizeActivity ở chế độ Edit.
     *
     * Vấn đề: khi save từ template, AppSession.saveCharacterWithSelections() copy
     * character với id = UUID mới. Không có field "templateId" nào được lưu lại.
     *
     * Giải pháp: dùng [CustomModel.avatar] của customized character để tìm template gốc
     * có cùng avatar (template gốc KHÔNG thay đổi avatar, chỉ customized mới có imageSave riêng).
     *
     * Nếu project có field templateId trong CustomModel thì dùng trực tiếp field đó thay thế.
     */
    private fun navigateToEdit(idEdit: String) {
        val customized = appSession.customizedCharacters.value
            .firstOrNull { it.id == idEdit }
            ?: run {
                showEditItemNotFoundDialog()
                return
            }

        if (!appSession.hasValidSelectionsForCustomized(idEdit)) {
            showEditItemNotFoundDialog()
            return
        }

        val templateIndex = appSession.getTemplateIndexForCustomized(idEdit)
            .takeIf { it >= 0 }
            ?: run { showUnstableNetworkDialog(); return }

        val template = appSession.templates.value.getOrNull(templateIndex)

        // ✅ Thêm check: online template + mất mạng hoặc data chưa đủ
        if (template?.id?.startsWith("online_") == true) {
            val onlineTemplateCount = appSession.templates.value
                .count { it.id.startsWith("online_") }
            if (!InternetExtension.isNetworkConnected(this@MyPonyActivity) || onlineTemplateCount < 2) {
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

    // ── ACTIONS ───────────────────────────────────────────────────────────────

    private fun confirmDelete(paths: ArrayList<String>, isAvatar: Boolean) {
        showConfirmDialog(
            title = getString(R.string.delete),
            message = getString(R.string.are_you_sure_want_to_delete_this_item),
            onYes = {
                if (isAvatar) viewModel.deleteItem(this@MyPonyActivity, paths)
                else viewModel.deleteItemDesign(paths, this@MyPonyActivity)
                resetSelection()
            },
            onNo = null
        )
    }

    private fun handleDownload() {
        val selected = getSelectedItems()
        if (selected.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }
        pendingDownloadPaths = ArrayList(selected.map { it.path })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performBatchDownload(); return
        }

        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            this@MyPonyActivity.checkPermissions(arrayOf(permission)) -> performBatchDownload()
            permissionViewModel.shouldGoToSettings(isStorage = true) -> this@MyPonyActivity.goToSettings()
            else -> downloadPermissionLauncher.launch(arrayOf(permission))
        }
    }

    private val downloadPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                permissionViewModel.onStorageGranted()
                performBatchDownload()
            } else {
                permissionViewModel.onStorageDenied()
                // ✅ Chỉ toast, KHÔNG check goToSettings ở đây
                showToast(R.string.download_failed_please_try_again_later)
            }
        }
    // ── SHARE: chỉ dùng imageSave (ảnh render) ───────────────────────────────

    /**
     * Lấy đúng path để share.
     * - Avatar tab: dùng [MyAlbumModel.path] = customized.imageSave (ảnh render đã lưu)
     * - Design tab: dùng path trực tiếp
     * KHÔNG dùng customized.avatar (đó là thumbnail template gốc từ assets)
     */
    private fun getSharePaths(): List<String> =
        getSelectedItems().map { it.path }.filter { it.isNotEmpty() }

    // ── WHATSAPP ──────────────────────────────────────────────────────────────

    private fun handleWhatsAppShare() {
        val paths = getSharePaths()
        when {
            paths.isEmpty() -> {
                showToast(R.string.please_select_an_image); return
            }

            paths.size < MIN_STICKERS_WHATSAPP -> {
                showToast(R.string.limit_3_items); return
            }

            paths.size > MAX_STICKERS_WHATSAPP -> {
                showToast(R.string.limit_30_items); return
            }
        }
        // ✅ Dùng CreateNameDialog thay AlertDialog
        val dialog = CreateNameDialog(this@MyPonyActivity)
        dialog.show()
        dialog.onYesClick = { packName ->
            dialog.dismiss()
            viewModel.addToWhatsapp(this@MyPonyActivity, packName, ArrayList(paths)) { pack ->
                if (pack != null) {
                    addToWhatsapp(pack)
                    resetSelection()
                } else showToast("Failed to create sticker pack")
            }
        }
        dialog.onNoClick = { dialog.dismiss() }
        dialog.onDismissClick = { dialog.dismiss() }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != ADD_PACK_REQUEST) return
        when (resultCode) {
            Activity.RESULT_OK -> showToast("Sticker pack added successfully")
            Activity.RESULT_CANCELED -> {
                val err = data?.getStringExtra("validation_error")
                if (err != null) {
                    Log.e("MyPonyActivity", "Validation: $err"); showToast("Failed: $err")
                } else showToast("Cancelled")
            }
        }
    }

    // ── TELEGRAM ──────────────────────────────────────────────────────────────

    private fun handleTelegramShare() {
        val paths = getSharePaths()
        if (paths.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }

        // ✅ Debug: kiểm tra file có tồn tại và đúng định dạng không
        paths.forEach { path ->
            val file = File(path)
            Log.d(
                "TelegramDebug",
                "path=$path | exists=${file.exists()} | size=${file.length()} | ext=${file.extension}"
            )
        }

        viewModel.addToTelegram(this@MyPonyActivity, ArrayList(paths))
        resetSelection()
    }

    // ── UTILITY ───────────────────────────────────────────────────────────────

    private fun showToast(resId: Int) =
        Toast.makeText(this@MyPonyActivity, resId, Toast.LENGTH_SHORT)
            .show()

    private fun showToast(msg: String) =
        Toast.makeText(this@MyPonyActivity, msg, Toast.LENGTH_SHORT)
            .show()

    // ── BASE OVERRIDES ────────────────────────────────────────────────────────

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.setOnClickListener {
            if (myAvatarAdapter.items.any { it.isShowSelection } ||
                myDesignAdapter.items.any { it.isShowSelection }
            ) {
                resetSelection()  // Thoát selection mode, KHÔNG navigate
            } else {
                popBack()
            }
        }
    }

    override fun bindViewModel() {}


    override fun onResume() {
        super.onResume()
        applyTabUI(isAvatarTab.value)
        if (!isAvatarTab.value) {
            loadDesignData()
        }
    }
}
