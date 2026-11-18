package com.julcer26.tasave.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.julcer26.tasave.MainActivity
import com.julcer26.tasave.R
import com.julcer26.tasave.data.LocalStorageHelper
import com.julcer26.tasave.ultra.UltraMath
import java.util.Locale

class TasaVeRatesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TasaVeRatesWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            ids.forEach { id ->
                updateAppWidget(context, manager, id)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val storage = LocalStorageHelper(context)
            val (usd, eur, usdt) = storage.getRates()
            val spread = UltraMath.calculateSpreadPercent(usdt.rate, usd.rate)

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_tasa_ve_rates).apply {
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                setTextViewText(R.id.widget_usd_value, "Bs. ${String.format(Locale.US, "%.4f", usd.rate)}")
                setTextViewText(R.id.widget_eur_value, "Bs. ${String.format(Locale.US, "%.4f", eur.rate)}")
                setTextViewText(R.id.widget_usdt_value, "Bs. ${String.format(Locale.US, "%.2f", usdt.rate)}")
                setTextViewText(R.id.widget_updated, usd.dateUpdated)
                setTextViewText(
                    R.id.widget_spread,
                    "Brecha USDT/BCV: ${String.format(Locale.US, "%.1f", spread)}% • Toca para abrir"
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
