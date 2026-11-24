package com.julcer26.tasave.quick

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.julcer26.tasave.R
import com.julcer26.tasave.data.CachedRatesBundle
import com.julcer26.tasave.data.LocalStorageHelper
import com.julcer26.tasave.ui.VeNumberFormat

data class PersistentRateSummary(
    val title: String,
    val compactLine: String,
    val bigText: String,
    val copyClipboardText: String
)

/**
 * Manages the optional Ultra persistent live-rate notification bar.
 * Strictly opt-in (OFF by default), silent (IMPORTANCE_LOW), and synced
 * when rates update without using a foreground service.
 */
object PersistentRateNotificationManager {

    const val CHANNEL_ID = "tasave_live_rates_bar"
    const val NOTIFICATION_ID = 2002

    fun canShowPersistentNotification(
        isUltraActive: Boolean,
        isOptedIn: Boolean,
        hasPermission: Boolean
    ): Boolean = isUltraActive && isOptedIn && hasPermission

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun formatPersistentRates(bundle: CachedRatesBundle): PersistentRateSummary {
        val usd = bundle.usdBcv.rate
        val eur = bundle.eurBcv.rate
        val usdt = bundle.usdtParalelo.rate
        val nextUsd = bundle.nextUsdBcv?.rate

        val usdStr = if (usd > 0.0) VeNumberFormat.formatRate(usd, 2) else "--"
        val eurStr = if (eur > 0.0) VeNumberFormat.formatRate(eur, 2) else "--"
        val usdtStr = if (usdt > 0.0) VeNumberFormat.formatRate(usdt, 2) else "--"
        val usdExact = if (usd > 0.0) VeNumberFormat.formatRate(usd, 4) else "--"
        val eurExact = if (eur > 0.0) VeNumberFormat.formatRate(eur, 4) else "--"

        val title = "Tasa VE • BCV Bs. $usdStr"
        val compactLine = "USD $usdStr • EUR $eurStr • USDT $usdtStr"

        val lines = mutableListOf(
            "USD BCV: Bs. $usdExact",
            "EUR BCV: Bs. $eurExact",
            "USDT Binance: Bs. $usdtStr"
        )
        if (nextUsd != null && nextUsd > 0.0) {
            val nextLabel = bundle.nextBcvDateIso?.takeIf { it.isNotBlank() } ?: "Próx."
            lines += "BCV Mañana ($nextLabel): Bs. ${VeNumberFormat.formatRate(nextUsd, 4)}"
        }

        val bigText = lines.joinToString("\n")
        val copyText = buildString {
            appendLine("Tasas del día • Tasa VE")
            lines.forEach { appendLine("• $it") }
        }.trim()

        return PersistentRateSummary(
            title = title,
            compactLine = compactLine,
            bigText = bigText,
            copyClipboardText = copyText
        )
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Barra fija de tasas (Ultra)",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Muestra las tasas del día en el panel de notificaciones con botones rápidos."
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun sync(context: Context) {
        val storage = LocalStorageHelper(context)
        val shouldShow = canShowPersistentNotification(
            isUltraActive = storage.isEffectiveUltraActive(),
            isOptedIn = storage.isPersistentRateNotifEnabled(),
            hasPermission = hasNotificationPermission(context)
        )

        val nm = NotificationManagerCompat.from(context)
        if (!shouldShow) {
            nm.cancel(NOTIFICATION_ID)
            return
        }

        ensureChannel(context)
        val summary = formatPersistentRates(storage.getAllCachedRates())

        val copyRatesPendingIntent = PendingIntent.getActivity(
            context,
            301,
            QuickCalcActivity.createIntent(context, QuickCalcActivity.MODE_COPY_RATES),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calcPendingIntent = PendingIntent.getActivity(
            context,
            302,
            QuickCalcActivity.createIntent(context, QuickCalcActivity.MODE_CALC),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val copyPmPendingIntent = PendingIntent.getActivity(
            context,
            303,
            QuickCalcActivity.createIntent(context, QuickCalcActivity.MODE_COPY_PM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(summary.title)
            .setContentText(summary.compactLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary.bigText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(calcPendingIntent)
            .addAction(0, "Copiar tasas", copyRatesPendingIntent)
            .addAction(0, "Calc. rápida", calcPendingIntent)
            .addAction(0, "Pago Móvil", copyPmPendingIntent)
            .build()

        try {
            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission may have been revoked between check and notify
        }
    }
}
