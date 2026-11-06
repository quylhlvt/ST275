package com.anime.oc.characters.avatar.ui.onboarding.permission

import androidx.lifecycle.ViewModel
import com.anime.oc.characters.avatar.core.helper.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
/** Permission denial history follows the app session across separate Activities. */
@Singleton
class PermissionState @Inject constructor() {
    internal val storageDenyCount = MutableStateFlow(0)
    internal val notificationDenyCount = MutableStateFlow(0)
}

@HiltViewModel
class PermissionViewModel @Inject constructor(state: PermissionState) : ViewModel() {
    private val _storageDenyCount = state.storageDenyCount
    private val _notificationDenyCount = state.notificationDenyCount

    val storageDenyCount: StateFlow<Int> = _storageDenyCount.asStateFlow()
    val notificationDenyCount: StateFlow<Int> = _notificationDenyCount.asStateFlow()

    fun onStorageDenied()      { _storageDenyCount.value++ }
    fun onStorageGranted()     { _storageDenyCount.value = 0 }
    fun onNotificationDenied() { _notificationDenyCount.value++ }
    fun onNotificationGranted(){ _notificationDenyCount.value = 0 }

    fun shouldGoToSettings(isStorage: Boolean): Boolean {
        val count = if (isStorage) _storageDenyCount.value else _notificationDenyCount.value
        return count >= 2
    }

    fun getStoragePermissions()      = PermissionHelper.storagePermission
    fun getNotificationPermissions() = PermissionHelper.notificationPermission
}