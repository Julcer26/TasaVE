package com.julcer26.tasave.quick

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.julcer26.tasave.MainActivity
import com.julcer26.tasave.analytics.AnalyticsTracker
import com.julcer26.tasave.data.LocalStorageHelper
import java.util.Locale

/**
 * Android Quick Settings Tile ("Tasa VE") for one-tap access from the status bar.
 * - Ultra users: opens the floating QuickCalcActivity directly over their active app.
 * - Free users: opens MainActivity with cached BCV/USDT rates.
 */
class TasaVeQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val storage = LocalStorageHelper(this)
        val usd = storage.getAllCachedRates().usdBcv.rate

        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Tasa VE"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (usd > 0.0) {
                    "BCV ${String.format(Locale.US, "%.2f", usd)}"
                } else {
                    "Ver tasas"
                }
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val isUltra = LocalStorageHelper(this).isEffectiveUltraActive()
        val targetIntent = if (isUltra) {
            QuickCalcActivity.createIntent(this, QuickCalcActivity.MODE_CALC)
        } else {
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        AnalyticsTracker.logAction(
            this,
            if (isUltra) "quick_tile_ultra" else "quick_tile_free"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this,
                304,
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(targetIntent)
        }
    }
}
