package com.anime.oc.characters.avatar.data.model.addcharacter

data class SpeechCategoryModel(
    val category: String,
    val quantity: Int,
    var isSelected: Boolean = false
) {
    fun imageUrls(): List<String> = (1..quantity).map { index ->
        "$SPEECH_BASE_URL/$category/$index.png"
    }

    companion object {
        private const val SPEECH_BASE_URL =
            "https://lvtglobal.tech/public/app/ST279_AnimeOCMaker/bg/speech%20bubble"
    }
}
