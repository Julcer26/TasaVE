package com.julcer26.tasave.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Non-intrusive Rewarded Ad helper for granting +5 bonus camera scans per day
 * when free users opt in voluntarily.
 * Uses official Google AdMob test Rewarded Ad Unit ID in development builds.
 */
class RewardedAdManager(
    private val adUnitId: String = TEST_REWARDED_AD_UNIT_ID
) {
    companion object {
        private const val TAG = "RewardedAdManager"
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    private var rewardedAd: RewardedAd? = null
    private var loadingInFlight = false

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun preload(context: Context) {
        if (rewardedAd != null || loadingInFlight) return
        loadingInFlight = true
        _isLoading.value = true

        RewardedAd.load(
            context.applicationContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingInFlight = false
                    _isLoading.value = false
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad preloaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingInFlight = false
                    _isLoading.value = false
                    rewardedAd = null
                    Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showOrLoad(
        activity: Activity?,
        onRewarded: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (activity == null) {
            onError("No se pudo iniciar el anuncio en esta pantalla.")
            return
        }

        val currentAd = rewardedAd
        if (currentAd != null) {
            presentAd(activity, currentAd, onRewarded, onError)
            return
        }

        if (loadingInFlight) {
            onError("Cargando anuncio... Intenta en un segundo.")
            return
        }

        loadingInFlight = true
        _isLoading.value = true
        RewardedAd.load(
            activity.applicationContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingInFlight = false
                    _isLoading.value = false
                    rewardedAd = ad
                    presentAd(activity, ad, onRewarded, onError)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingInFlight = false
                    _isLoading.value = false
                    rewardedAd = null
                    onError("No hay anuncios disponibles ahora. Verifica tu conexión.")
                }
            }
        )
    }

    private fun presentAd(
        activity: Activity,
        ad: RewardedAd,
        onRewarded: () -> Unit,
        onError: (String) -> Unit
    ) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                onError("No se pudo mostrar el anuncio. Intenta nuevamente.")
                preload(activity)
            }
        }
        ad.show(activity) {
            onRewarded()
        }
    }
}
