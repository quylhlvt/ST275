package com.anime.oc.characters.avatar.core.extention
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import com.anime.oc.characters.avatar.core.helper.RateHelper
import com.anime.oc.characters.avatar.core.helper.SharedPreferencesManager
import com.anime.oc.characters.avatar.utils.state.RateState

fun AppCompatActivity.shareApp() {
    ShareCompat.IntentBuilder(this)
        .setType("text/plain")
        .setChooserTitle("Chooser title")
        .setText("http://play.google.com/store/apps/details?id=${this.packageName}")
        .startChooser()
}

fun AppCompatActivity.policy() {
    val url = "https://sites.google.com/view/anime-oc-maker-avatar-creator/home"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
}

fun AppCompatActivity.rateApp(
    sharePreference: SharedPreferencesManager,
    onRateResult: (RateState) -> Unit = {}
) {
    RateHelper.showRateDialog(this, sharePreference, onRateResult)
}