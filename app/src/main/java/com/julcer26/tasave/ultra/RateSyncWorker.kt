package com.julcer26.tasave.ultra

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.julcer26.tasave.data.AppDatabase
import com.julcer26.tasave.data.LocalStorageHelper
import com.julcer26.tasave.data.TasaVeRatesResult
import com.julcer26.tasave.data.TasaVeRepository
import com.julcer26.tasave.quick.PersistentRateNotificationManager
import com.julcer26.tasave.widget.TasaVeRatesWidgetProvider
import java.util.concurrent.TimeUnit

/**
 * Periodically refreshes cached rates in the background when the user has enabled
 * the optional Ultra persistent live-rate notification bar, keeping both the
 * notification bar and home-screen widgets up to date.
 */
class RateSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val storage = LocalStorageHelper(appContext)
        if (!storage.isPersistentRateNotifEnabled()) return Result.success()

        val dao = AppDatabase.getDatabase(appContext).conversionHistoryDao
        val repository = TasaVeRepository(appContext, dao)

        val result = repository.fetchRates(forceRefresh = true)
        if (result !is TasaVeRatesResult.Success || result.isOfflineError) {
            return Result.retry()
        }

        TasaVeRatesWidgetProvider.updateAllWidgets(appContext)
        PersistentRateNotificationManager.sync(appContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "tasa_ve_rate_sync_work"
        private const val LEGACY_ALERT_WORK_NAME = "tasa_ve_rate_alert_work"
        private const val LEGACY_ALERT_CHANNEL_ID = "tasa_ve_rate_alerts"

        fun scheduleOrCancel(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(LEGACY_ALERT_WORK_NAME)
            cleanupLegacyAlertChannel(context)
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<RateSyncWorker>(
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

        private fun cleanupLegacyAlertChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.deleteNotificationChannel(LEGACY_ALERT_CHANNEL_ID)
                }
            }
        }
    }
}
