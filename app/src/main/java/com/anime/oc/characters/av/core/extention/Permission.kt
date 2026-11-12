package com.anime.oc.characters.av.core.extention

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.anime.oc.characters.av.R
import androidx.core.net.toUri
import androidx.appcompat.app.AppCompatActivity
import com.anime.oc.characters.av.utils.key.RequestKey


fun Context.checkPermissions(listPermission: Array<String>): Boolean {
    return listPermission.all {
        ContextCompat.checkSelfPermission( this,it) == PackageManager.PERMISSION_GRANTED
    }
}

fun Activity.requestPermission(permissions: Array<String>, requestCode: Int) {
    ActivityCompat.requestPermissions(this, permissions, requestCode)
}
fun AppCompatActivity.openImagePicker() {
    val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*"
    startActivityForResult(intent, RequestKey.PICK_IMAGE_REQUEST_CODE)
}
fun Activity.goToSettings() {
    val dialog = AlertDialog.Builder(this)
        .setTitle(R.string.go_to_setting_message)
        .setMessage(R.string.go_to_setting_message)
        .setPositiveButton(R.string.settings) { dialog, _ ->
            // Đóng dialog trước khi rời Activity. Nếu để dialog còn tồn tại
            // trong lúc mở Settings, window của dialog có thể giữ touch/focus
            // khi người dùng quay lại app.
            dialog.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${this@goToSettings.packageName}".toUri()
            }
            this.startActivity(intent)
            // Không thay đổi system UI sau startActivity(): lúc này cửa sổ đang
            // mất focus cho Settings. MainActivity sẽ khôi phục immersive mode
            // khi cửa sổ app thực sự nhận lại focus.
        }
        .setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            hideNavigation(true)
        }
        .setCancelable(false)
        .create()

    dialog.show()
    // Keep the app's follow-up Settings dialog open if the user taps outside.
    dialog.setCanceledOnTouchOutside(false)
    val positiveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
    val negativeButton: Button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
    positiveButton.setTextColor(getColor(R.color.app_color))
    negativeButton.setTextColor(getColor(R.color.black))
}
