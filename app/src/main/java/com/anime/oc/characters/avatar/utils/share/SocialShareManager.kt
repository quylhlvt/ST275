package com.anime.oc.characters.avatar.utils.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class SocialShareManager(private val context: Context) {
    enum class SocialApp(val packageName: String) {
        FACEBOOK("com.facebook.katana"),
        INSTAGRAM("com.instagram.android")
    }

    sealed interface ShareResult {
        data object Started : ShareResult
        data object ImageNotFound : ShareResult
        data class AppNotAvailable(val app: SocialApp) : ShareResult
        data class Failed(val error: Throwable) : ShareResult
    }

    fun shareImage(imagePath: String, app: SocialApp): ShareResult {
        val file = File(imagePath)
        if (imagePath.isBlank() || !file.isFile) return ShareResult.ImageNotFound

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                setPackage(app.packageName)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("shared_image", uri)
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
                )
            }
            context.grantUriPermission(
                app.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            context.startActivity(intent)
            ShareResult.Started
        } catch (_: ActivityNotFoundException) {
            ShareResult.AppNotAvailable(app)
        } catch (error: Exception) {
            ShareResult.Failed(error)
        }
    }
}
