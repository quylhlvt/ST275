package com.anime.oc.characters.av.ui.onboarding.intro

import androidx.annotation.StringRes
import com.anime.oc.characters.av.data.model.intro.IntroModel
import com.anime.oc.characters.av.R

class IntroContact {
}
data class IntroUiState(
    val pagesSplash: List<IntroModel>? = emptyList(),
    val page: Int = 0,
    @StringRes val textButtonRes: Int = R.string.next
)

sealed class IntroSingleEvent {
    data object NavigateToNextScreen : IntroSingleEvent()
}