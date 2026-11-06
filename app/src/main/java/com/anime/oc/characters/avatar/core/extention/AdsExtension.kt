package com.anime.oc.characters.avatar.core.extention

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import com.anime.oc.characters.avatar.core.base.BaseActivity
import com.anime.oc.characters.avatar.R
import com.google.android.gms.ads.rewarded.RewardItem
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.callback.RewardCallback
import com.lvt.ads.event.AdmobEvent
import com.lvt.ads.util.Admob
import java.util.concurrent.atomic.AtomicBoolean
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch



private fun BaseActivity<*, *>.isAdCallbackValid(): Boolean =
    isScreenAvailable && !isNavigating

fun BaseActivity<*, *>.showRewardAds1(
    onRewardSuccess: () -> Unit,
    onAdClosed: () -> Unit = {}
) {
    var earned = false
    Admob.getInstance().loadAndShowRewardAds(
        this,
        getString(R.string.reward_random),
        object : RewardCallback() {
            override fun onEarnedReward(rewardItem: RewardItem?) {
                earned = true
            }
            override fun onAdClosed() {
                super.onAdClosed()
                if (!isAdCallbackValid()) return
                lifecycleScope.launch {
                    lifecycle.withResumed {
                        if (isAdCallbackValid()) {
                            if (earned) onRewardSuccess()
                            else Toast.makeText(this@showRewardAds1, getString(R.string.please_watch_full_ads), Toast.LENGTH_SHORT).show()
                            onAdClosed()
                        }
                    }
                }
            }
            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                if (!isAdCallbackValid()) return
                Toast.makeText(this@showRewardAds1, getString(R.string.load_ads_fail), Toast.LENGTH_SHORT).show()
                onAdClosed()
            }
            override fun onAdFailedToShow(codeError: Int) {
                super.onAdFailedToShow(codeError)
                if (!isAdCallbackValid()) return
                Toast.makeText(this@showRewardAds1, getString(R.string.load_ads_fail), Toast.LENGTH_SHORT).show()
                onAdClosed()
            }
        }
    )
}
fun BaseActivity<*, *>.loadNativeCollabAds(id: String, layout: FrameLayout) {
    Admob.getInstance().loadNativeCollap(this, id, layout)
}

fun BaseActivity<*, *>.showInterAll() {
    Admob.getInstance().showInterAll(this, object : InterCallback() {
        override fun onNextAction() {
            super.onNextAction()
        }
    })
}
fun BaseActivity<*, *>.logEventSocial(nameEvent: String, nameEvent1: String, value: String) {
    val bundle = Bundle()
    bundle.putString("link_${nameEvent}", value)
    AdmobEvent.logEvent(this, nameEvent1, bundle)
}
fun BaseActivity<*, *>.logEventSocial(nameEvent: String) {
    AdmobEvent.logEvent(this, nameEvent, null)
}

fun BaseActivity<*, *>.logEvent(nameEvent: String, value: String) {
    val bundle = Bundle()
    bundle.putString("link", value)
    AdmobEvent.logEvent(this, nameEvent, bundle)
}
fun BaseActivity<*, *>.logEvent(nameEvent: String) {
    AdmobEvent.logEvent(this, nameEvent, null)
}
