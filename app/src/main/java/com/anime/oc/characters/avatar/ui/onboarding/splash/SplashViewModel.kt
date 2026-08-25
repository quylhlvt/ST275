package com.anime.oc.characters.avatar.ui.onboarding.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anime.oc.characters.avatar.data.model.custom.CustomModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {

    private val _dataReadySignal = MutableStateFlow(false)
    val dataReadySignal: StateFlow<Boolean> = _dataReadySignal.asStateFlow()
    private val _navigateSignal = MutableStateFlow(false)
    val navigateSignal: StateFlow<Boolean> = _navigateSignal.asStateFlow()

    private var isTimerRunning = false
    private val initTime = System.currentTimeMillis()

    fun triggerNavigate() { viewModelScope.launch {
        val remaining = 5_000L - (System.currentTimeMillis() - initTime)
        if (remaining > 0) delay(remaining)
        _navigateSignal.value = true
    } }

    fun startSplashTimer(
        hasNetwork: Boolean,
        hasOnlineTemplates: Boolean,
        templatesFlow: StateFlow<List<CustomModel>>,
        imagesReadyFlow: StateFlow<Boolean>,
        localDataReadyFlow: StateFlow<Boolean>
    ) {
        if (isTimerRunning) return
        isTimerRunning = true

        viewModelScope.launch {
            if (!hasNetwork) { delay(5_000L); _navigateSignal.value = true; return@launch }
            val startTime = System.currentTimeMillis()
            withTimeoutOrNull(5_000L) {
                localDataReadyFlow.first { it }
            }
            val dataJob = launch {
                if (!hasOnlineTemplates) {
                    withTimeoutOrNull(8_000L) {
                        templatesFlow.first { list ->
                            list.any { it.id.startsWith("online_") }
                        }
                    }
                }
            }

            val imagesJob = launch {
                withTimeoutOrNull(30_000L) {
                    imagesReadyFlow.first { it }
                }
            }

            dataJob.join()
            imagesJob.join()

            val elapsed = System.currentTimeMillis() - startTime
            val remaining = 3_000L - elapsed  // ✅ khớp với MIN_SPLASH_MS = 3_000L
            if (remaining > 0) delay(remaining)

            isTimerRunning = false
            _dataReadySignal.value = true
        }
    }
}
