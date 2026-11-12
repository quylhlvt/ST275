package com.anime.oc.characters.av.ui.main.createPony

import androidx.lifecycle.ViewModel
import com.anime.oc.characters.av.data.datalocal.manager.AppDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ── VIEWMODEL ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ChoosePonyViewModel @Inject constructor(
    appDataManager: AppDataManager
) : ViewModel() {
    // State của màn Category nằm trong ViewModel và dùng chung nguồn cache local.
    // Fragment bị destroy/recreate khi đi Custom rồi quay lại cũng không mất list.
    val templates = appDataManager.templates
}

// ── ADAPTER ───────────────────────────────────────────────────────────────────


// ── FRAGMENT ──────────────────────────────────────────────────────────────────
