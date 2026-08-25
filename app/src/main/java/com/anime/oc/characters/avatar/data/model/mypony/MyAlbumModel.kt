package com.anime.oc.characters.avatar.data.model.mypony

data class MyAlbumModel(
    val path: String,
    var isShowSelection: Boolean = false,
    var isSelected: Boolean = false,
    var idEdit:String,
    var type:Int=2
)
