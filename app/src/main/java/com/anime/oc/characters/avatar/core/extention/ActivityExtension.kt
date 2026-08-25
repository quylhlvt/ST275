package com.anime.oc.characters.avatar.core.extention
// Extension functions (updated for AndroidX Fragment)
import androidx.fragment.app.Fragment
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import com.anime.oc.characters.avatar.core.helper.RateHelper
import com.anime.oc.characters.avatar.core.helper.SharedPreferencesManager
import com.anime.oc.characters.avatar.utils.state.RateState

fun Fragment.shareApp() {
    ShareCompat.IntentBuilder(requireContext())
        .setType("text/plain")
        .setChooserTitle("Chooser title")
        .setText("http://play.google.com/store/apps/details?id=${requireContext().packageName}")
        .startChooser()
}

fun Fragment.policy() {
    val url = "https://sites.google.com/view/anime-oc-maker-avatar-creator/home"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
}

fun Fragment.rateApp(
    sharePreference: SharedPreferencesManager,
    onRateResult: (RateState) -> Unit = {}
) {
    RateHelper.showRateDialog(requireActivity(), sharePreference, onRateResult)
}