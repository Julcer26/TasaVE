package com.julcer26.tasave.ultra

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.julcer26.tasave.MainActivity
import com.julcer26.tasave.R
import com.julcer26.tasave.data.AppDatabase
import com.julcer26.tasave.data.LocalStorageHelper
import com.julcer26.tasave.data.TasaVeRatesResult
import com.julcer26.tasave.data.TasaVeRepository
import com.julcer26.tasave.widget.TasaVeRatesWidgetProvider
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class RateAlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val storage = LocalStorageHelper(appContext)
        val settings = storage.getAlertSettings()
        if (!settings.enabled) return Result.success()

        val dao = AppDatabase.getDatabase(appContext).conversionHistoryDao
        val repository = TasaVeRepository(appContext, dao)

        val result = repository.fetchRates(forceRefresh = true)
        if (result !is TasaVeRatesResult.Success || result.isOfflineError) {
            return Result.retry()
        }

        TasaVeRatesWidgetProvider.updateAllWidgets(appContext)

        val (prevUsd, prevEur, prevUsdt) = storage.getLastNotifiedRates()
        val currUsd = result.usdBcv.rate
        val currEur = result.eurBcv.rate
        val currUsdt = result.usdtParalelo.rate

        if (prevUsd <= 0.0 || prevEur <= 0.0 || prevUsdt <= 0.0) {
            storage.saveLastNotifiedRates(currUsd, currEur, currUsdt)
            return Result.success()
        }

        val alerts = mutableListOf<String>()
        if (settings.watchUsdBcv && pctChange(currUsd, prevUsd) >= settings.thresholdPercent) {
            alerts.add("USD BCV: Bs. ${String.format(Locale.US, "%.2f", currUsd)}")
        }
        if (settings.watchEurBcv && pctChange(currEur, prevEur) >= settings.thresholdPercent) {
            alerts.add("EUR BCV: Bs. ${String.format(Locale.US, "%.2f", currEur)}")
        }
        if (settings.watchUsdt && pctChange(currUsdt, prevUsdt) >= settings.thresholdPercent) {
            alerts.add("USDT: Bs. ${String.format(Locale.US, "%.2f", currUsdt)}")
        }

        if (alerts.isNotEmpty()) {
            sendAlertNotification(
                context = appContext,
                title = "Cambio en Tasas de Venezuela",
                body = alerts.joinToString(" • ")
            )
            storage.saveLastNotifiedRates(currUsd, currEur, currUsdt)
        }

        return Result.success()
    }

    private fun pctChange(current: Double, previous: Double): Double {
        if (previous <= 0.0) return 0.0
        return abs((current - previous) / previous) * 100.0
    }

    companion object {
        private const val WORK_NAME = "tasa_ve_rate_alert_work"
        const val CHANNEL_ID = "tasa_ve_rate_alerts"

        fun scheduleOrCancel(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<RateAlertWorker>(
                    1, TimeUnit.HOURS
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } else {
                workManager.cancelUniqueWork(WORK_NAME)
            }
        }

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Alertas de Tasa de Cambio",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifica variaciones importantes en Dólar BCV, Euro BCV y USDT."
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        fun sendAlertNotification(context: Context, title: String, body: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }

            ensureNotificationChannel(context)
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(2001, notification)
            return true
        }
    }
}
