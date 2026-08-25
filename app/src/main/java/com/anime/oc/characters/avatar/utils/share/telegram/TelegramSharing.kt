package com.anime.oc.characters.avatar.utils.share.telegram

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.anime.oc.characters.avatar.R

object TelegramSharing {

    fun importToTelegram(context: Context, uriList: List<Uri>) {
        if (uriList.isEmpty()) return

        val list = ArrayList(uriList)

        // Grant permission cho cả Telegram và Telegram X
        val telegramPackages = listOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.plus"
        )

        list.forEach { uri ->
            telegramPackages.forEach { pkg ->
                try {
                    context.grantUriPermission(
                        pkg,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { }
            }
        }

        val intent = Intent("org.telegram.messenger.CREATE_STICKER_PACK").apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
            putExtra("IMPORTER", context.packageName)
            putStringArrayListExtra(
                "STICKER_EMOJIS",
                ArrayList(List(list.size) { "😊" })
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "image/*"

            // Gắn tất cả URI vào ClipData để Android 13+ chuyển quyền đọc
            // cho Telegram đầy đủ, thay vì chỉ dựa vào EXTRA_STREAM.
            clipData = ClipData.newUri(
                context.contentResolver,
                "telegram_stickers",
                list.first()
            ).apply {
                list.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
        }

        // Tìm đúng package Telegram đang cài
        val resolvedPackage = telegramPackages.firstOrNull { pkg ->
            runCatching {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
        }

        if (resolvedPackage != null) {
            intent.setPackage(resolvedPackage)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.no_app_found_to_handle_this_action),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
