package com.anime.oc.characters.av.core.extention

import com.anime.oc.characters.av.data.model.custom.BodyPartModel
import com.anime.oc.characters.av.data.model.custom.ColorModel
import com.anime.oc.characters.av.data.model.custom.CustomModel
import com.google.gson.Gson

private val extensionGson = Gson()

fun CustomModel.itemPosition(): String? {
    position?.takeIf { it.isNotBlank() }?.let { return it }

    // Older cached templates and designs did not store position.
    val partPosition = listPath.firstOrNull()?.nav
        ?.substringBefore('?')?.substringBefore('#')
        ?.substringBeforeLast('/', "")?.substringBeforeLast('/', "")?.substringAfterLast('/')
    if (!partPosition.isNullOrBlank()) return partPosition

    val avatarFolder = avatar.substringBefore('?').substringBefore('#')
        .trimEnd('/').substringBeforeLast('/', "").substringAfterLast('/')
    if (avatarFolder.isNotBlank()) return avatarFolder

    val sourceId = templateId ?: id.takeIf { isTemplate() } ?: return null
    return sourceId.removePrefix("online_single_").removePrefix("online_")
        .removePrefix("template_").takeIf { it.isNotBlank() }
}

fun CustomModel.itemEventName(action: String? = null): String? = position?.takeIf { it.isNotBlank() }?.let {
    "click_item_$it" + (action?.let { suffix -> "_$suffix" } ?: "")
}

fun CustomModel.withCleanListPath(): CustomModel {
    val cleanListPath = listPath.map { bp ->
        val bpJson = extensionGson.toJson(bp)
        val bodyPart = extensionGson.fromJson(bpJson, BodyPartModel::class.java)
        val cleanColors = bodyPart.listPath.map { color ->
            val colorJson = extensionGson.toJson(color)
            extensionGson.fromJson(colorJson, ColorModel::class.java)
        }
        bodyPart.copy(listPath = ArrayList(cleanColors))
    }
    return this.copy(position = itemPosition(), listPath = ArrayList(cleanListPath))
}
