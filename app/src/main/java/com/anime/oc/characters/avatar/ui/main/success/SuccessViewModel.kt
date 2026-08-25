package com.anime.oc.characters.avatar.ui.main.success

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anime.oc.characters.avatar.core.helper.DownloadHelper
import com.anime.oc.characters.avatar.data.datalocal.manager.AppDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject



// ViewViewModel.kt
@HiltViewModel
class SuccessViewModel @Inject constructor(private val appDataManager: AppDataManager) : ViewModel() {

    fun downloadFile(context: Context, path: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = DownloadHelper.downloadToGallery(context, path)
            onResult(success)
        }
    }

    fun deleteFile(
        path: String,
        isAvatar: Boolean,
        idEdit: String = "",
        onDone: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // ✅ Nếu là avatar, xóa khỏi customizedCharacters
                if (isAvatar && idEdit.isNotEmpty()) {
                    appDataManager.deleteCustomizedCharacter(idEdit)
                }
                // ✅ Nếu là design, xóa khỏi myDesignPaths
                if (!isAvatar) {
                    val current = appDataManager.myDesignPaths.value.toMutableList()
                    current.remove(path)
                    appDataManager.saveMyDesignToJson(current)
                }
                // Xóa file vật lý
                File(path).delete()
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}