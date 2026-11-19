package com.julcer26.tasave.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialPolicy {
    const val MIN_LAUNCHES_BEFORE_ADS = 3
    const val COOLDOWN_MS = 4 * 60 * 1000L // 4 minutes minimum between interstitials
    const val MAX_PER_SESSION = 3

    fun shouldShow(
        isAdFree: Boolean,
        appLaunchCount: Int,
        sessionShownCount: Int,
        lastShownElapsedMs: Long,
        nowElapsedMs: Long,
        hasLoadedAd: Boolean
    ): Boolean {
        if (isAdFree || !hasLoadedAd) return false
        if (appLaunchCount < MIN_LAUNCHES_BEFORE_ADS) return false
        if (sessionShownCount >= MAX_PER_SESSION) return false
        if (lastShownElapsedMs >= 0L && (nowElapsedMs - lastShownElapsedMs) < COOLDOWN_MS) {
            return false
        }
        return true
    }
}

/**
 * Balanced post-action Interstitial Ad manager.
 *
 * Rules to protect UX while improving AdMob revenue:
 * - Never shown on cold app launch (0 delay when checking rates at checkout).
 * - Grace period on first 2 app opens for new users.
 * - Minimum 4-minute cooldown and max 3 interstitials per session.
 * - Immediately suppressed for Tasa VE Ultra users or 12h Rewarded ad-free pass holders.
 */
class InterstitialAdManager {
    companion object {
        private const val TAG = "InterstitialAdManager"
    }

    private var interstitialAd: InterstitialAd? = null
    private var loadingInFlight = false
    private var sessionShownCount = 0
    private var lastShownElapsedMs = -1L

    fun preload(context: Context, isAdFree: Boolean) {
        if (isAdFree || interstitialAd != null || loadingInFlight) return
        if (sessionShownCount >= InterstitialPolicy.MAX_PER_SESSION) return
        val unitId = AdUnits.interstitialAdUnitId(context) ?: return

        loadingInFlight = true
        InterstitialAd.load(
            context.applicationContext,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInFlight = false
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad preloaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInFlight = false
                    interstitialAd = null
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun maybeShowAfterAction(
        activity: Activity?,
        isAdFree: Boolean,
        appLaunchCount: Int,
        onComplete: () -> Unit = {}
    ) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        val currentAd = interstitialAd
        if (!InterstitialPolicy.shouldShow(
                isAdFree = isAdFree,
                appLaunchCount = appLaunchCount,
                sessionShownCount = sessionShownCount,
                lastShownElapsedMs = lastShownElapsedMs,
                nowElapsedMs = nowElapsed,
                hasLoadedAd = currentAd != null
            )
        ) {
            if (!isAdFree && currentAd == null) {
                preload(activity, isAdFree = false)
            }
            onComplete()
            return
        }

        val adToShow = currentAd ?: run {
            onComplete()
            return
        }

        interstitialAd = null
        sessionShownCount += 1
        lastShownElapsedMs = nowElapsed

        adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload(activity, isAdFree = isAdFree)
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                preload(activity, isAdFree = isAdFree)
                onComplete()
            }
        }
        adToShow.show(activity)
    }
}
