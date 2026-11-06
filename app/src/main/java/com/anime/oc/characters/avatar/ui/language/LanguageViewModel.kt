package com.anime.oc.characters.avatar.ui.language

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.anime.oc.characters.avatar.data.model.language.LanguageModel
import com.anime.oc.characters.avatar.utils.DataLocal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor( sharedPreferences: SharedPreferences) : ViewModel() {
    private val _languageList = MutableStateFlow<List<LanguageModel>>(emptyList())
    val languageList: StateFlow<List<LanguageModel>> = _languageList.asStateFlow()

    private val _codeLang = MutableStateFlow("")
    val codeLang: StateFlow<String> = _codeLang.asStateFlow()

    private val _isFirstLanguage = MutableStateFlow(false)
    val isFirstLanguage: StateFlow<Boolean> = _isFirstLanguage.asStateFlow()

    fun loadLanguages(currentLang: String) {
        val list = DataLocal.getLanguageList().map {
            it.copy(activate = !isFirstLanguage.value && it.code == currentLang)
        }.toMutableList()
        val index = list.indexOfFirst { it.code == currentLang }
        if (index >= 0) {
            list.add(0, list.removeAt(index))
        }
        _codeLang.value = currentLang.takeIf { index >= 0 }.orEmpty()
        _languageList.value = list
    }

    fun selectLanguage(code: String) {
        _codeLang.value = code
        val updatedList = _languageList.value.map {
            it.copy(activate = it.code == code)
        }
        _languageList.value = updatedList
    }

    fun setFirstLanguage(isFirst: Boolean){
        _isFirstLanguage.value = isFirst
    }
}
