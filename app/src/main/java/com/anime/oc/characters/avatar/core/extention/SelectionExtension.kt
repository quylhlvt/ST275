package com.anime.oc.characters.avatar.core.extention

// SelectionExtension.kt
import com.anime.oc.characters.avatar.data.model.custom.SelectionIndex
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun List<*>.toCleanSelections(): ArrayList<SelectionIndex> {
    val gson = Gson()
    val json = gson.toJson(this)
    val type = object : TypeToken<ArrayList<SelectionIndex>>() {}.type
    return gson.fromJson(json, type)
}