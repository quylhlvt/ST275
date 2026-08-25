package com.anime.oc.characters.avatar.core.dialog

import android.content.Context
import android.graphics.Bitmap
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager

import kotlin.apply
import kotlin.text.trim
import kotlin.toString
import androidx.core.view.isVisible
import com.anime.oc.characters.avatar.core.base.BaseDialog
import com.anime.oc.characters.avatar.core.extention.invisible
import com.anime.oc.characters.avatar.core.extention.loadImage
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.setFont
import com.anime.oc.characters.avatar.core.helper.BitmapHelper
import com.anime.oc.characters.avatar.ui.main.add_character.adapter.TextColorAdapter
import com.anime.oc.characters.avatar.ui.main.add_character.adapter.TextFontAdapter
import com.anime.oc.characters.avatar.utils.DataLocal
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.DialogSpeechBinding

class DialogSpeech(
    val mcontext: Context,
    val path: String
) : BaseDialog<DialogSpeechBinding>(mcontext, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_speech
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false
    var onDoneClick: ((Bitmap?) -> Unit) = { }
    private val fontAdapter by lazy { TextFontAdapter(mcontext) }
    private val colorAdapter by lazy { TextColorAdapter() }
    private val fonts = DataLocal.getTextFontDefault()
    private var colors = DataLocal.getTextColorDefault(mcontext)
    private var textSizeProgress = 0.5f
    override fun initView() {
        binding.apply {
            fonts.firstOrNull()?.isSelected = true
            colors.getOrNull(1)?.isSelected = true
            val textFont = fonts.first().color
            val textColor = colors.getOrNull(1)?.color ?: mcontext.getColor(R.color.black)
            val textSizePx = mcontext.resources.getDimension(R.dimen.text_size_20) *
                    (0.5f + textSizeProgress.coerceIn(0f, 1f))
            edtSpeech.setFont(textFont)
            edtSpeech.setTextColor(textColor)
            edtSpeech.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            tvGetText.setFont(textFont)
            tvGetText.setTextColor(textColor)
            tvGetText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            fontAdapter.submitListReset(fonts)
            colorAdapter.submitListReset(colors)
            edtSpeech.isFocusableInTouchMode = true
            edtSpeech.isFocusable = true
            edtSpeech.postDelayed({
                edtSpeech.requestFocus()
                // ✅ Thêm dòng này
                val imm = mcontext.getSystemService(Context.INPUT_METHOD_SERVICE)
                        as InputMethodManager
                imm.showSoftInput(edtSpeech, InputMethodManager.SHOW_FORCED)
            }, 100) // ✅ Tăng delay từ 30 lên 100 để dialog attach xong
            loadImage(mcontext, path, imvBubble)
        }
    }

    override fun initAction() {
        binding.apply {
            fontAdapter.onTextFontClick = { font, position ->
                fonts.forEachIndexed { index, item -> item.isSelected = index == position }
                edtSpeech.setFont(font)
                tvGetText.setFont(font)
                fontAdapter.submitItem(position, fonts)
            }
            colorAdapter.onTextColorClick = { color, position ->
                selectColor(color, position)
            }
            colorAdapter.onChooseColorClick = {
                val dialog = ChooseColorDialog(mcontext)
                dialog.show()
                dialog.onCloseEvent = { dialog.dismiss() }
                dialog.onDoneEvent = { color ->
                    dialog.dismiss()
                    selectColor(color, 0)
                }
            }

            edtSpeech.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                    handleDone()
                    true
                } else {
                    false
                }
            }

            layoutRoot.onClick { handleDone() }

            edtSpeech.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    binding.tvGetText.text = p0.toString()
                }

                override fun afterTextChanged(p0: Editable?) {}
            })
        }
    }

    private fun selectColor(color: Int, position: Int) {
        colors = colors.mapIndexed { index, item ->
            item.copy(
                color = if (index == position) color else item.color,
                isSelected = index == position
            )
        }.toCollection(ArrayList())
        binding.edtSpeech.setTextColor(color)
        binding.tvGetText.setTextColor(color)
        colorAdapter.submitItem(position, colors)
    }

    private fun applyTextSize(progress: Float) {
        val textSizePx = mcontext.resources.getDimension(R.dimen.text_size_20) *
                (0.5f + progress.coerceIn(0f, 1f))
        binding.edtSpeech.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
        binding.tvGetText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
    }

    fun handleDone(){
        binding.apply {
            edtSpeech.clearFocus()
            val imm = mcontext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(edtSpeech.windowToken, 0)

            edtSpeech.invisible()
            tvGetText.isVisible = !TextUtils.isEmpty(edtSpeech.text.toString().trim())
            val bitmap = BitmapHelper.getBitmapFromEditText(layoutBubble)
            onDoneClick.invoke(bitmap)
        }
    }

    override fun onDismissListener() {}
}
