package com.anime.oc.characters.av

import com.anime.oc.characters.av.core.extention.itemEventName
import com.anime.oc.characters.av.core.extention.withCleanListPath
import com.anime.oc.characters.av.data.datalocal.api.ApiTemplateMapper
import com.anime.oc.characters.av.data.datalocal.api.X10
import com.anime.oc.characters.av.data.model.custom.BodyPartModel
import com.anime.oc.characters.av.data.model.custom.CustomModel
import com.anime.oc.characters.av.data.model.custom.CustomizedCharacterDto
import com.anime.oc.characters.av.data.model.custom.toDto
import com.anime.oc.characters.av.data.model.custom.toModel
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemEventTest {
    private val gson = Gson()

    @Test
    fun apiPositionSurvivesSortingSavingAndReopeningForEdit() {
        val templates = ApiTemplateMapper.map(
            linkedMapOf(
                "avatar42" to listOf(X10("", "1-1", "data9", "1", "2")),
                "avatar7" to listOf(X10("", "1-1", "data3", "1", "1"))
            ),
            "https://example.com/", "templates/", "online_single"
        )
        val template = templates[1]
        assertEquals("click_item_data9", template.itemEventName())
        assertEquals("click_item_data9_done", template.itemEventName("done"))

        val saved = template.copy(
            id = "a73bfbce-27dc-41e0-9a5e-d16cdf0b4383",
            templateId = template.id,
            imageSave = "/saved/avatar.png"
        )
        val reopened = gson.fromJson(
            gson.toJson(saved.toDto()), CustomizedCharacterDto::class.java
        ).toModel()

        assertEquals("data9", reopened.position)
        assertEquals("click_item_data9_edit", reopened.itemEventName("edit"))
    }

    @Test
    fun oldDesignRestoresPositionFromItsAvatarFolderBeforeLogging() {
        val oldDto = gson.fromJson(
            """{"id":"customized_123456","templateId":"online_single_data9",
                "avatar":"https://example.com/templates/data9/avatar.png?version=42"}""",
            CustomizedCharacterDto::class.java
        )
        assertNull(oldDto.position)
        assertNull(oldDto.toModel().itemEventName("edit"))
        assertEquals("click_item_data9_edit", oldDto.toModel().withCleanListPath().itemEventName("edit"))
    }

    @Test
    fun cachedTemplateWithoutPositionUsesPartFolderBeforeAvatarKey() {
        val cached = CustomModel(
            id = "online_single_avatar42",
            avatar = "https://example.com/templates/avatar42/avatar.png",
            listPath = arrayListOf(
                BodyPartModel(nav = "https://example.com/templates/data9/1-1/nav.png")
            )
        ).withCleanListPath()

        assertEquals("data9", cached.position)
        assertEquals("data9", cached.toDto().position)
        assertEquals("click_item_data9_done", cached.itemEventName("done"))
    }

    @Test
    fun missingPositionAndAvatarRestorePositionFromTemplateIdBeforeLogging() {
        val saved = CustomModel(id = "customized_123456", templateId = "online_single_data9")
        assertNull(saved.itemEventName("edit"))
        assertEquals("click_item_data9_edit", saved.withCleanListPath().itemEventName("edit"))
    }

    @Test
    fun missingTemplateIdentityDoesNotUseCustomizedIdForEvent() {
        assertNull(CustomModel(id = "customized_123456").itemEventName("edit"))
    }

    @Test
    fun apiWithoutPositionUsesFullTemplateKey() {
        val template = ApiTemplateMapper.map(
            mapOf("data9" to listOf(X10("", "1-1", "", "1", "1"))),
            "https://example.com/", "templates/", "online_single"
        ).single()
        assertEquals("click_item_data9_done", template.itemEventName("done"))
    }
}
