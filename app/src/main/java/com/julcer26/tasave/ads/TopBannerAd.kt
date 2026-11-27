package com.julcer26.tasave.ads

import android.content.Context
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * TopBannerAd: Non-intrusive Anchored Adaptive Banner advertising component.
 *
 * Implements Google Play Ads Policy compliance:
 * - Clear disclosure label ("PUBLICIDAD").
 * - Uses full-width Anchored Adaptive Banner for ~20-30% higher eCPM than legacy 320x50.
 * - Automatically disappears when [isUltraUser] or [isAdFreePassActive] is true.
 * - Resolves test vs production unit IDs automatically via [AdUnits].
 */
@Composable
fun TopBannerAd(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    isUltraUser: Boolean,
    isAdFreePassActive: Boolean = false,
    adUnitId: String? = null
) {
    val hideAds = isUltraUser || isAdFreePassActive
    if (hideAds) return

    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val resolvedUnitId = remember(adUnitId, context) {
        adUnitId ?: AdUnits.bannerAdUnitId(context)
    }
    if (!isInPreview && resolvedUnitId == null) return

    var adViewRef by remember { mutableStateOf<AdView?>(null) }
    var adLoadFailed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            adViewRef?.destroy()
            adViewRef = null
        }
    }

    AnimatedVisibility(
        visible = !hideAds && (isInPreview || !adLoadFailed),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "PUBLICIDAD",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isDark) Color(0x0F222226) else Color(0x0A000000)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0x14FFFFFF) else Color(0x14000000),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isInPreview) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Banner Adaptativo AdMob",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                } else {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        factory = { viewContext ->
                            val adSize = resolveAdaptiveBannerSize(viewContext)
                            AdView(viewContext).apply {
                                setAdSize(adSize)
                                this.adUnitId = resolvedUnitId!!
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                adViewRef = this
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        adLoadFailed = false
                                    }
                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        adLoadFailed = true
                                    }
                                }
                                loadAd(AdRequest.Builder().build())
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun resolveAdaptiveBannerSize(context: Context): AdSize {
    val metrics = context.resources.displayMetrics
    val density = metrics.density.takeIf { it > 0f } ?: 1f
    val totalWidthDp = (metrics.widthPixels / density).toInt()
    val containerWidthDp = (totalWidthDp - 32).coerceAtLeast(300)
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, containerWidthDp)
}
