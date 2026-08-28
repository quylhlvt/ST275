package com.anime.oc.characters.avatar.core.extention

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.anime.oc.characters.avatar.R
import com.google.android.gms.ads.rewarded.RewardItem
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.callback.RewardCallback
import com.lvt.ads.event.AdmobEvent
import com.lvt.ads.util.Admob
import java.util.concurrent.atomic.AtomicBoolean


fun Fragment.showInter(action: (() -> Unit)) {
    val originDestinationId = runCatching {
        findNavController().currentDestination?.id
    }.getOrNull()
    val hasHandledCallback = AtomicBoolean(false)

    Admob.getInstance().showInterAll(requireActivity(), object : InterCallback() {
        override fun onNextAction() {
            super.onNextAction()

            // Some ad flows can deliver a late or duplicated callback. Running an action
            // captured by an old Fragment destination can navigate with an invalid action ID.
            if (!hasHandledCallback.compareAndSet(false, true)) return
            if (!isAdCallbackValid(originDestinationId)) {
                Log.w(
                    "AdsNavigation",
                    "Skipped stale interstitial callback from destination $originDestinationId"
                )
                return
            }
            action()
        }
    })
}

private fun Fragment.isAdCallbackValid(originDestinationId: Int?): Boolean {
    if (!isAdded || isDetached || view == null) return false
    if (originDestinationId == null) return true

    return runCatching {
        findNavController().currentDestination?.id == originDestinationId
    }.getOrDefault(false)
}
fun Fragment.showRewardAds1(
    onRewardSuccess: () -> Unit,
    onAdClosed: () -> Unit = {}
) {
    var earned = false
    Admob.getInstance().loadAndShowRewardAds(
        requireActivity(),
        getString(R.string.reward_random),
        object : RewardCallback() {
            override fun onEarnedReward(rewardItem: RewardItem?) {
                earned = true
            }
            override fun onAdClosed() {
                super.onAdClosed()
                if (earned) {
                    onRewardSuccess()
                } else {
                    Toast.makeText(requireActivity(), getString(R.string.please_watch_full_ads), Toast.LENGTH_SHORT).show()
                }
                onAdClosed()
            }
            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                Toast.makeText(requireActivity(), getString(R.string.load_ads_fail), Toast.LENGTH_SHORT).show()
                onAdClosed()
            }
            override fun onAdFailedToShow(codeError: Int) {
                super.onAdFailedToShow(codeError)
                Toast.makeText(requireActivity(), getString(R.string.load_ads_fail), Toast.LENGTH_SHORT).show()
                onAdClosed()
            }
        }
    )
}
fun Fragment.loadNativeCollabAds(id: String, layout: FrameLayout) {
    Admob.getInstance().loadNativeCollap(requireActivity(), id, layout)
}

fun Fragment.showInterAll() {
    Admob.getInstance().showInterAll(requireActivity(), object : InterCallback() {
        override fun onNextAction() {
            super.onNextAction()
        }
    })
}
fun Fragment.logEventSocial(nameEvent: String, nameEvent1: String, value: String) {
    val bundle = Bundle()
    bundle.putString("link_${nameEvent}", value)
    AdmobEvent.logEvent(requireActivity(), nameEvent1, bundle)
}
fun Fragment.logEventSocial(nameEvent: String) {
    AdmobEvent.logEvent(requireActivity(), nameEvent, null)
}

fun Fragment.logEvent(nameEvent: String, value: String) {
    val bundle = Bundle()
    bundle.putString("link", value)
    AdmobEvent.logEvent(requireActivity(), nameEvent, bundle)
}
fun Fragment.logEvent(nameEvent: String) {
    AdmobEvent.logEvent(requireActivity(), nameEvent, null)
}
