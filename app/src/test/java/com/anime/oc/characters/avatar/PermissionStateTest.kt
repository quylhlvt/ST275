package com.anime.oc.characters.av

import com.anime.oc.characters.av.ui.onboarding.permission.PermissionState
import com.anime.oc.characters.av.ui.onboarding.permission.PermissionViewModel
import org.junit.Assert.*
import org.junit.Test

class PermissionStateTest {
    @Test
    fun deniedPermissionStillOffersSettingsOnAnotherScreen() {
        val state = PermissionState()
        val success = PermissionViewModel(state)
        val album = PermissionViewModel(state)
        success.onStorageDenied()
        assertFalse(album.shouldGoToSettings(isStorage = true))
        success.onStorageDenied()
        assertTrue(album.shouldGoToSettings(isStorage = true))
        album.onStorageGranted()
        assertFalse(success.shouldGoToSettings(isStorage = true))
    }

    @Test
    fun notificationDenialsDoNotBlockStoragePermission() {
        val state = PermissionState()
        val onboarding = PermissionViewModel(state)
        val nextScreen = PermissionViewModel(state)
        onboarding.onNotificationDenied()
        onboarding.onNotificationDenied()
        assertTrue(nextScreen.shouldGoToSettings(isStorage = false))
        assertFalse(nextScreen.shouldGoToSettings(isStorage = true))
        nextScreen.onNotificationGranted()
        assertFalse(onboarding.shouldGoToSettings(isStorage = false))
    }
}
