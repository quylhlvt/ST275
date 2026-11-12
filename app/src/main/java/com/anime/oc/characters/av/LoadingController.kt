package com.anime.oc.characters.av

interface LoadingController {
    fun showGlobalLoading()
    fun hideGlobalLoading()
    fun hideGlobalDialog()
    fun showGlobalConfirmDialog(
        message: String,
        title: String? = null,
        onYes: () -> Unit,
        onNo: (() -> Unit)? = null,
        yesText: String? = null,
        noText: String? = null
    )
    fun showGlobalOkDialog(
        message: String,
        title: String? = null,
        onOk: (() -> Unit)? = null
    )
}

