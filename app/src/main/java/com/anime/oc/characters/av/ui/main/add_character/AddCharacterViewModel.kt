package com.anime.oc.characters.av.ui.main.add_character

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModel
import com.anime.oc.characters.av.core.custom.Draw
import com.anime.oc.characters.av.core.custom.DrawableDraw
import com.anime.oc.characters.av.data.model.addcharacter.SelectedAddModel
import com.anime.oc.characters.av.utils.DataLocal
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddCharacterViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ========== Init guard ==========
    var isInitialized = false
    var isRestoringDraws = false

    // ========== Adapter Lists ==========
    var backgroundImageList: ArrayList<SelectedAddModel> = arrayListOf()
    var backgroundColorList: ArrayList<SelectedAddModel> = arrayListOf()
    var stickerList: ArrayList<SelectedAddModel> = arrayListOf()
    var speechList: ArrayList<SelectedAddModel> = arrayListOf()
    var textFontList: ArrayList<SelectedAddModel> = arrayListOf()
    var textColorList: ArrayList<SelectedAddModel> = arrayListOf()

    // ========== Navigation ==========
    // -1 = chưa set, Fragment bỏ qua
    private val _typeNavigation = MutableStateFlow(-1)
    val typeNavigation: StateFlow<Int> = _typeNavigation.asStateFlow()

    private val _typeBackground = MutableStateFlow(-1)
    val typeBackground: StateFlow<Int> = _typeBackground.asStateFlow()

    // ========== Background ==========
    private val _backgroundImagePath = MutableStateFlow<String?>(null)
    val backgroundImagePath: StateFlow<String?> = _backgroundImagePath.asStateFlow()
    var selectedBackgroundImagePath: String? = null
    var selectedBackgroundImagePosition: Int = -1

    var savedBackgroundColor: Int? = null

    // ========== Tab state ==========
    // Chỉ dùng để biết tab nào đang active — KHÔNG dùng để control layout
    var isTextTabActive: Boolean = false
    var isSpeechDialogOpen: Boolean = false

    // ========== Draw state ==========
    var currentDraw: Draw? = null
    var drawViewList: ArrayList<DrawableDraw> = arrayListOf()

    // ========== Misc ==========
    var pathDefault = ""

    // ========== Navigation setters ==========

    fun setTypeNavigation(type: Int) {
        if (_typeNavigation.value == type) _typeNavigation.value = -1
        _typeNavigation.value = type
    }

    fun setTypeBackground(type: Int) {
        // StateFlow is the source of truth for the selected tab. Re-emitting via
        // an intermediate -1 made a tap on the active tab rebuild its UI again.
        if (_typeBackground.value == type) return
        _typeBackground.value = type
    }

    fun setBackgroundImage(path: String?) {
        _backgroundImagePath.value = path
    }

    // ========== Data loading ==========

    fun loadDataFromMainViewModel(
        backgrounds: List<String>,
        stickers: List<String>,
        speeches: List<String>
    ) {
        val selectedPath = selectedBackgroundImagePath
        backgroundImageList = arrayListOf(
            SelectedAddModel(
                path = "",
                isSelected = selectedBackgroundImagePosition == NONE_BACKGROUND_POSITION
            ),
            SelectedAddModel(
                path = "",
                isSelected = selectedBackgroundImagePosition == ADD_BACKGROUND_POSITION
            )
        ).apply {
            addAll(backgrounds.map { path ->
                SelectedAddModel(path = path, isSelected = path == selectedPath)
            })
        }
        selectedBackgroundImagePosition = backgroundImageList.indexOfFirst { it.isSelected }

        backgroundColorList.clear()
        backgroundColorList.add(SelectedAddModel()) // Choose custom color
        backgroundColorList.addAll(DataLocal.getBackgroundColorDefault(context))

        stickerList.clear()
        stickerList.addAll(stickers.map { SelectedAddModel(path = it) })

        speechList.clear()
        speechList.addAll(speeches.map { SelectedAddModel(path = it) })

        textFontList.clear()
        textFontList.addAll(DataLocal.getTextFontDefault())
        textFontList.firstOrNull()?.isSelected = true

        textColorList.clear()
        textColorList.addAll(DataLocal.getTextColorDefault(context))
        textColorList.getOrNull(1)?.isSelected = true

    }

    fun loadDataFromQuantity(
        bgQuantity: Int,
        stickerQuantity: Int,
        bgBaseUrl: String,
        speeches: List<String>
    ) {
        val backgrounds = (1..bgQuantity).map { "$bgBaseUrl/Background/$it.png" }
        val stickers = (1..stickerQuantity).map { "$bgBaseUrl/Sticker/$it.png" }
        loadDataFromMainViewModel(backgrounds, stickers, speeches)
    }

    // ========== Selection helpers ==========

    fun updateBackgroundImageSelected(position: Int) {
        selectedBackgroundImagePosition = position
        backgroundColorList.forEach { model ->
            model.isSelected = false
        }
        backgroundImageList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    fun updateBackgroundColorSelected(position: Int) {
        selectedBackgroundImagePosition = -1
        selectedBackgroundImagePath = null
        backgroundImageList.forEach { model ->
            model.isSelected = false
        }
        backgroundColorList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    private companion object {
        const val ADD_BACKGROUND_POSITION = 1
        const val NONE_BACKGROUND_POSITION = 0
    }

    fun updateTextFontSelected(position: Int) {
        textFontList = textFontList
            .map { it.copy(isSelected = false) }
            .toCollection(ArrayList())
        textFontList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    fun updateTextColorSelected(position: Int, selectedColor: Int? = null) {
        textColorList = textColorList
            .mapIndexed { index, item ->
                item.copy(
                    color = if (index == position && selectedColor != null) {
                        selectedColor
                    } else {
                        item.color
                    },
                    isSelected = index == position
                )
            }
            .toCollection(ArrayList())
    }

    // ========== Draw helpers ==========

    fun updateCurrentCurrentDraw(draw: Draw) {
        currentDraw = draw
    }

    fun addDrawView(draw: Draw) {
        if (draw is DrawableDraw) {
            drawViewList.add(draw)
        }
    }

    fun deleteDrawView(draw: Draw) {
        drawViewList.removeIf { it == draw }
    }

    fun resetDraw() {
        drawViewList.clear()
        currentDraw = null
    }

    fun updatePathDefault(path: String) {
        pathDefault = path
    }

    // ========== Drawable / Emoji ==========

    fun loadDrawableEmoji(
        bitmap: Bitmap,
        isCharacter: Boolean = false,
        isText: Boolean = false
    ): DrawableDraw {
        val drawable = bitmap.toDrawable(context.resources)
        val timestamp = SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(Date())
        val drawableEmoji = DrawableDraw(drawable, "$timestamp.png")
        drawableEmoji.isCharacter = isCharacter
        drawableEmoji.isText = isText
        return drawableEmoji
    }

    // ========== Cleanup ==========

    fun clearAllData() {
        backgroundImageList.clear()
        backgroundColorList.clear()
        stickerList.clear()
        speechList.clear()
        textFontList.clear()
        textColorList.clear()
        drawViewList.clear()
        currentDraw = null
        pathDefault = ""
    }
}
