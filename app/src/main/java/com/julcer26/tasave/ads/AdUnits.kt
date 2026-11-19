package com.julcer26.tasave.ads

import android.content.Context
import com.julcer26.tasave.BuildConfig
import com.julcer26.tasave.R

/**
 * Resolves AdMob Ad Unit IDs safely between DEBUG (Google official test units)
 * and RELEASE (production unit IDs configured in strings.xml).
 */
object AdUnits {
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    fun bannerAdUnitId(context: Context): String? {
        if (BuildConfig.DEBUG) return TEST_BANNER_AD_UNIT_ID
        return sanitizeProdId(context.getString(R.string.admob_banner_ad_unit_id))
    }

    fun interstitialAdUnitId(context: Context): String? {
        if (BuildConfig.DEBUG) return TEST_INTERSTITIAL_AD_UNIT_ID
        return sanitizeProdId(context.getString(R.string.admob_interstitial_ad_unit_id))
    }

    fun rewardedAdUnitId(context: Context): String? {
        if (BuildConfig.DEBUG) return TEST_REWARDED_AD_UNIT_ID
        return sanitizeProdId(context.getString(R.string.admob_rewarded_ad_unit_id))
    }

    internal fun sanitizeProdId(raw: String): String? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        if (cleaned.startsWith("ca-app-pub-3940256099942544")) return null
        return cleaned
    }
}
