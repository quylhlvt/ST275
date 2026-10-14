package com.anime.oc.characters.avatar

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anime.oc.characters.avatar.data.datalocal.manager.AppDataManager
import com.anime.oc.characters.avatar.data.model.addcharacter.SpeechCategoryModel
import com.anime.oc.characters.avatar.data.model.addcharacter.StickerCategoryModel
import com.anime.oc.characters.avatar.data.model.custom.CustomModel
import com.anime.oc.characters.avatar.data.model.custom.SelectionIndex
import com.anime.oc.characters.avatar.data.usecase.GetCatalogueUseCase
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.UUID
import javax.inject.Inject
import kotlin.text.isEmpty
import kotlin.text.orEmpty

@HiltViewModel
class ViewModelActivity @Inject constructor(
    private val getCatalogueUseCase: GetCatalogueUseCase,
    val appDataManager: AppDataManager,
    private val networkFlow: Flow<Boolean>,
    @ApplicationContext private val context: Context
) :
    ViewModel() {

    // ── EXPOSED FLOWS ─────────────────────────────────────────────────────────

    val characters:           StateFlow<List<CustomModel>> = appDataManager.characters
    val templates:            StateFlow<List<CustomModel>> = appDataManager.templates
    val customizedCharacters: StateFlow<List<CustomModel>> = appDataManager.customizedCharacters
    val backgrounds:          StateFlow<List<String>>      = appDataManager.backgrounds
    val backgroundTexts:      StateFlow<List<String>>      = appDataManager.backgroundTexts
    val stickers:             StateFlow<List<String>>      = appDataManager.stickers
    val speechs:              StateFlow<List<String>>      = appDataManager.speechs
    val myDesignPaths:        StateFlow<List<String>>      = appDataManager.myDesignPaths
    val isLoading:            StateFlow<Boolean>           = appDataManager.isLoading
    val error:                StateFlow<String?>           = appDataManager.error
    private val _bgStickerFailed = MutableStateFlow(false)
    val bgStickerFailed: StateFlow<Boolean> = _bgStickerFailed.asStateFlow()
    private val _bgStickerReady = MutableStateFlow(false)
    val bgStickerReady: StateFlow<Boolean> = _bgStickerReady.asStateFlow()
    private val _bgLoading = MutableStateFlow(false)
    private val _stickerCategories = MutableStateFlow<List<StickerCategoryModel>>(emptyList())
    val stickerCategories: StateFlow<List<StickerCategoryModel>> = _stickerCategories.asStateFlow()
    private val _speechCategories = MutableStateFlow<List<SpeechCategoryModel>>(emptyList())
    val speechCategories: StateFlow<List<SpeechCategoryModel>> = _speechCategories.asStateFlow()
    var cosplayBitmap: Bitmap? = null
    var userResultBitmap: Bitmap? = null
    var customizeBitmap: Bitmap? = null
    var cosplayPercent: Int = 0
    var shouldRestartShow = false
    val networkOnline: StateFlow<Boolean> = networkFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // ← Eagerly để không miss network event
            initialValue = false,
        )

    // Guard chống gọi fetch trùng từ nhiều fragment
    private val _isFetchingOnline = MutableStateFlow(false)
    val isFetchingOnlineFlow: StateFlow<Boolean> = _isFetchingOnline.asStateFlow()
    private val _imagesReady = MutableStateFlow(false)
    val imagesReady: StateFlow<Boolean> = _imagesReady.asStateFlow()
    private val _localDataReady = MutableStateFlow(false)
    val localDataReady: StateFlow<Boolean> = _localDataReady.asStateFlow()
    fun notifyImagesReady() {
        _imagesReady.value = true
    }
    // ── INIT ──────────────────────────────────────────────────────────────────

    init {
        Log.d("PERF2", "ViewModelActivity created: ${System.currentTimeMillis()}")
        loadInitialData()
    }


    private suspend fun loadUrlList(baseUrl: String, ext: String): List<String> {
        val results = java.util.Collections.synchronizedMap(mutableMapOf<Int, String>())
        var shouldStop = false
        var start = 1

        while (!shouldStop) {
            val end = start + 9
            coroutineScope {
                (start..end).map { i ->
                    async(Dispatchers.IO) {
                        if (shouldStop) return@async
                        val url = "$baseUrl$i.$ext"
                        try {
                            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                            connection.requestMethod = "HEAD"
                            connection.connectTimeout = 5000
                            connection.readTimeout = 5000
                            val code = connection.responseCode
                            connection.disconnect()
                            if (code == 403 || code == 404) shouldStop = true
                            else results[i] = url
                        } catch (e: Exception) {
                            shouldStop = true
                        }
                    }
                }.awaitAll()
            }
            start += 10
        }
        return results.toSortedMap().values.toList()
    }

    private data class BgConfig(
        @SerializedName("background")
        val background: List<CategoryConfig> = emptyList(),
        @SerializedName("sticker")
        val sticker: List<CategoryConfig> = emptyList(),
        @SerializedName("speech bubble")
        val speechBubble: List<CategoryConfig> = emptyList()
    )

    private data class CategoryConfig(
        @SerializedName("category")
        val category: String = "",
        @SerializedName("quantity")
        val quantity: Int = 0
    )

    private fun loadBgConfig(): BgConfig {
        val url = "https://lvtglobal.tech/public/app/ST279_AnimeOCMaker/bg/bg.json"
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.inputStream.bufferedReader().use { reader ->
                Gson().fromJson(reader, BgConfig::class.java)
            }
        } finally {
            connection.disconnect()
        }
    }
    // ✅ Fix — gọi đúng hàm đã có
    fun preloadBackgroundsAndStickers() {
        if (_bgStickerReady.value) return
        loadBackgroundsAndStickers() // ← hàm này đã có trong ViewModelActivity
    }

    fun loadBackgroundsAndStickers() {
        if (_bgLoading.value) return
        if (_bgStickerReady.value && !_bgStickerFailed.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _bgLoading.value = true
            _bgStickerReady.value = false
            try {
                coroutineScope {
                    val config = loadBgConfig()
                    val backgrounds = config.background
                        .filter { it.quantity > 0 }
                        .flatMap { item ->
                            val categoryPath = item.category
                                .trim()
                                .takeIf { it.isNotEmpty() }
                                ?.let { "/$it" }
                                .orEmpty()
                            (1..item.quantity).map { index ->
                                "$BACKGROUND_BASE_URL$categoryPath/$index.png"
                            }
                        }
                    val stickerCategories = config.sticker
                        .filter { it.category.isNotBlank() && it.quantity > 0 }
                        .mapIndexed { index, item ->
                            StickerCategoryModel(
                                category = item.category,
                                quantity = item.quantity,
                                isSelected = index == 0
                            )
                        }
                    val stickers = stickerCategories.firstOrNull()?.imageUrls().orEmpty()
                    val speechCategories = config.speechBubble
                        .filter { it.category.isNotBlank() && it.quantity > 0 }
                        .map { item ->
                            SpeechCategoryModel(
                                category = item.category,
                                quantity = item.quantity
                            )
                        }
                    val speech = speechCategories.flatMap { it.imageUrls() }

                    if (backgrounds.isEmpty() && stickers.isEmpty() && speech.isEmpty()) {
                        Log.w("ViewModelActivity", "⚠️ Empty result, mark as failed")
                        _bgStickerFailed.value = true
                        return@coroutineScope
                    }

                    appDataManager.updateBackgroundsStickersAndSpeech(backgrounds, stickers, speech)
                    _stickerCategories.value = stickerCategories
                    _speechCategories.value = speechCategories
                    _bgStickerFailed.value = false
                    _bgStickerReady.value = true
                    Log.d("ViewModelActivity", "✅ bgs=${backgrounds.size} stickers=${stickers.size}")
                }
            } catch (e: Exception) {
                Log.e("ViewModelActivity", "❌ loadBgSticker: ${e.message}")
                _bgStickerFailed.value = true
            } finally {
                _bgLoading.value = false
            }
        }
    }

    private companion object {
        const val BACKGROUND_BASE_URL =
            "https://lvtglobal.tech/public/app/ST279_AnimeOCMaker/bg/background"
    }
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val hasCache = withContext(Dispatchers.IO) {
                    appDataManager.loadQuickData()
                }
                if (!hasCache) appDataManager.loadInitialData()
                _localDataReady.value = true

                // ← Load song song
                launch { fetchOnlineTemplatesInternal() }
                launch { loadBackgroundsAndStickers() }
                launch { observeNetworkForRetry() }

            } catch (e: Exception) {
                Log.e("ViewModelActivity", "❌ Init error: ${e.message}", e)
            }
        }
    }
    private suspend fun observeNetworkForRetry() {
        networkOnline.collect { isOnline ->
            if (isOnline && _bgStickerFailed.value && !_bgLoading.value) {
                Log.d("ViewModelActivity", "🔄 Network restored, retrying...")
                _bgStickerFailed.value = false
                loadBackgroundsAndStickers()
            }
        }
    }
    // ── FETCH (duy nhất 1 hàm, có guard) ─────────────────────────────────────

    /**
     * Gọi từ mọi nơi đều an toàn — guard đảm bảo chỉ 1 request chạy tại 1 thời điểm.
     * BaseFragment gọi khi vào màn + có mạng + chưa có online data.
     */
    private suspend fun fetchOnlineTemplatesInternal() {
        if (_isFetchingOnline.value) {
            Log.d("ViewModelActivity", "⏭️ Already fetching, skip")
            return
        }
        _isFetchingOnline.value = true
        try {
            Log.d("ViewModelActivity", "📡 fetchOnlineTemplates start")
            val result = getCatalogueUseCase()
            if (result.isSuccess) {
                val newTemplates = result.getOrNull() ?: return
                appDataManager.saveApiCache(newTemplates)
                appDataManager.mergeApiTemplates(newTemplates)
                prefetchTemplateImages(newTemplates)
                Log.d("ViewModelActivity", "✅ Online templates loaded: ${newTemplates.size}")
            } else {
                Log.e("ViewModelActivity", "❌ API failed: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e("ViewModelActivity", "❌ fetchOnlineTemplates error: ${e.message}", e)
        } finally {
            _isFetchingOnline.value = false
            _imagesReady.value = true
        }
    }

    /** Public — BaseFragment và pull-to-refresh gọi */
    fun fetchOnlineTemplates() {
        viewModelScope.launch(Dispatchers.IO){ fetchOnlineTemplatesInternal() }
    }

    fun forceReloadAll() {
        viewModelScope.launch {
            _localDataReady.value = false  // ✅ reset trước
            appDataManager.forceReloadAll()
            _localDataReady.value = true   // ✅ xong
        }
    }
    fun refreshApiData() {
        viewModelScope.launch { appDataManager.refreshFromApi() }
    }

    // ── PREFETCH IMAGES ───────────────────────────────────────────────────────

    private suspend fun prefetchTemplateImages(templates: List<CustomModel>) {
        withContext(Dispatchers.IO) {
            templates.take(5).forEach { template ->
                template.listPath.forEach { bp ->
                    val firstColor = bp.listPath.firstOrNull() ?: return@forEach
                    val firstPath  = firstColor.listPath
                        .firstOrNull { it != "none" && it != "dice" }
                        ?: return@forEach
                    runCatching {
                        Glide.with(context)
                            .asBitmap()
                            .load(firstPath)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .override(256, 256)
                            .preload()
                    }
                }
            }
        }
    }

    // ── QUERIES ───────────────────────────────────────────────────────────────

    fun getCharacterByIndex(index: Int): CustomModel? = appDataManager.getCharacterByIndex(index)
    fun getCharacterById(id: String): CustomModel?    = appDataManager.getCharacterById(id)
    fun isTemplate(id: String): Boolean               = appDataManager.isTemplate(id)
    fun getTemplateIndexByAvt(avt: String): Int       = appDataManager.getTemplateIndexByAvt(avt)
    fun getCharacterIndexById(id: String): Int        = characters.value.indexOfFirst { it.id == id }

    fun getTemplateIndexForCustomized(customizedId: String): Int {
        val customized = customizedCharacters.value.firstOrNull { it.id == customizedId }
            ?: return -1

        // ✅ Ưu tiên templateId
        val byTemplateId = customized.templateId?.let { tplId ->
            templates.value.indexOfFirst { it.id == tplId }.takeIf { it >= 0 }
        }
        if (byTemplateId != null) return byTemplateId

        // ✅ Fallback avatar
        val byAvatar = templates.value.indexOfFirst { it.avatar == customized.avatar }
            .takeIf { it >= 0 }
        if (byAvatar != null) return byAvatar

        Log.w("ViewModelActivity", "⚠️ Template not found for customizedId=$customizedId, templateId=${customized.templateId}")
        return -1
    }

    /**
     * Kiểm tra các index đã lưu của character trước khi mở màn hình Edit.
     * Dữ liệu cũ/corrupt có thể chứa colorIndex hoặc pathIndex không còn nằm
     * trong template hiện tại.
     */
    fun hasValidSelectionsForCustomized(customizedId: String): Boolean {
        val customized = customizedCharacters.value.firstOrNull { it.id == customizedId }
            ?: return false
        val templateIndex = getTemplateIndexForCustomized(customizedId)
        val template = templates.value.getOrNull(templateIndex) ?: return false
        val parts = template.listPath.sortedBy { it.zIndex }

        if (customized.selections.size > parts.size) return false

        return customized.selections.withIndex().all { (index, selection) ->
            val part = parts.getOrNull(index) ?: return false
            val color = part.listPath.getOrNull(selection.colorIndex) ?: return false
            color.listPath.getOrNull(selection.pathIndex) != null
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    suspend fun saveCharacterWithSelections(
        character:  CustomModel,
        selections: List<SelectionIndex>,
        imageSave:  String  = "",
        isFlipped:  Boolean = false
    ) {
        val toSave = if (isTemplate(character.id)) {
            character.copy(
                id         = UUID.randomUUID().toString(),
                templateId = character.id,
                selections = ArrayList(selections),
                imageSave  = imageSave,
                isFlipped  = isFlipped,
                createdAt  = System.currentTimeMillis(),
                updatedAt  = System.currentTimeMillis()
                // ✅ KHÔNG set listPath — giữ nguyên từ template
            )
        } else {
            character.copy(
                selections = ArrayList(selections),
                imageSave  = imageSave,
                isFlipped  = isFlipped,
                updatedAt  = System.currentTimeMillis()
                // ✅ KHÔNG set listPath
            )
        }
        Log.d("ViewModelActivity", "💾 Saving: id=${toSave.id}, templateId=${toSave.templateId}, imageSave=${toSave.imageSave}")
        appDataManager.updateCustomizedCharacter(toSave)
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            if (isTemplate(characterId)) return@launch
            appDataManager.deleteCustomizedCharacter(characterId)
        }
    }

    // ── SELECTION HELPERS ─────────────────────────────────────────────────────

    fun resolvePath(character: CustomModel, sel: SelectionIndex): String? =
        appDataManager.resolvePathFromSelection(character, sel)

    fun resolveAllPaths(character: CustomModel, selections: List<SelectionIndex>): List<Pair<Int, String>> =
        appDataManager.resolveAllPaths(character, selections)

    // ── MY DESIGNS ────────────────────────────────────────────────────────────

    fun addMyDesign(path: String)    { viewModelScope.launch { appDataManager.addMyDesignPath(path) } }
    fun removeMyDesign(path: String) { viewModelScope.launch { appDataManager.removeMyDesignPath(path) } }

    // ── CLEAR ─────────────────────────────────────────────────────────────────

    fun clearData() { appDataManager.clearData() }
}
