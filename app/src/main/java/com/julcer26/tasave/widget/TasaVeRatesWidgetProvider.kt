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

object WidgetCalcMath {
    private const val MAX_INPUT_LEN = 10

    fun appendToken(current: String, token: String): String {
        val cleaned = current.trim()
        if (token == ".") {
            if (cleaned.contains('.')) return cleaned
            val base = if (cleaned.isEmpty()) "0" else cleaned
            return if (base.length >= MAX_INPUT_LEN) base else "$base."
        }
        if (token.length != 1 || !token[0].isDigit()) return cleaned.ifEmpty { "0" }
        val next = when {
            cleaned.isEmpty() || cleaned == "0" -> token
            else -> cleaned + token
        }
        return if (next.length > MAX_INPUT_LEN) cleaned else next
    }

    fun backspace(current: String): String {
        val cleaned = current.trim()
        return if (cleaned.length <= 1) "0" else cleaned.dropLast(1)
    }

    fun formatDisplayPair(
        input: String,
        rate: Double,
        isVesInput: Boolean,
        foreignSymbol: String
    ): Pair<String, String> {
        val safeInput = input.trim().ifEmpty { "0" }
        val amount = safeInput.toDoubleOrNull() ?: 0.0
        val leftLabel = if (isVesInput) "Bs. $safeInput" else "$foreignSymbol $safeInput"
        val rightLabel = if (rate <= 0.0) {
            if (isVesInput) "$foreignSymbol --" else "Bs. --"
        } else if (isVesInput) {
            val converted = amount / rate
            "$foreignSymbol ${String.format(Locale.US, "%,.2f", converted)}"
        } else {
            val converted = amount * rate
            "Bs. ${String.format(Locale.US, "%,.2f", converted)}"
        }
        return leftLabel to rightLabel
    }
}

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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val storage = LocalStorageHelper(context)
        var rateKey = storage.getWidgetCalcRateKey()
        var isVes = storage.isWidgetCalcVesInput()
        var input = storage.getWidgetCalcInput()
        var handled = true

        when (action) {
            ACTION_DIGIT -> {
                val digit = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
                input = WidgetCalcMath.appendToken(input, digit)
            }
            ACTION_BACKSPACE -> {
                input = WidgetCalcMath.backspace(input)
            }
            ACTION_CLEAR -> {
                input = "0"
            }
            ACTION_SWAP -> {
                isVes = !isVes
            }
            ACTION_SELECT_RATE -> {
                val nextKey = intent.getStringExtra(EXTRA_RATE_KEY).orEmpty()
                if (nextKey in listOf("USD_BCV", "EUR_BCV", "USDT")) {
                    rateKey = nextKey
                }
            }
            else -> handled = false
        }

        if (handled) {
            storage.saveWidgetCalcState(rateKey, isVes, input)
            updateAllWidgets(context)
        }
    }

    companion object {
        private const val ACTION_DIGIT = "com.julcer26.tasave.widget.ACTION_DIGIT"
        private const val ACTION_BACKSPACE = "com.julcer26.tasave.widget.ACTION_BACKSPACE"
        private const val ACTION_CLEAR = "com.julcer26.tasave.widget.ACTION_CLEAR"
        private const val ACTION_SWAP = "com.julcer26.tasave.widget.ACTION_SWAP"
        private const val ACTION_SELECT_RATE = "com.julcer26.tasave.widget.ACTION_SELECT_RATE"
        private const val EXTRA_TOKEN = "extra_token"
        private const val EXTRA_RATE_KEY = "extra_rate_key"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TasaVeRatesWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            ids.forEach { id ->
                updateAppWidget(context, manager, id)
            }
        }

        private fun actionBroadcast(
            context: Context,
            requestCode: Int,
            action: String,
            configure: Intent.() -> Unit = {}
        ): PendingIntent {
            val intent = Intent(context, TasaVeRatesWidgetProvider::class.java).apply {
                this.action = action
                configure()
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val storage = LocalStorageHelper(context)
            val (usd, eur, usdt) = storage.getRates()
            val spread = UltraMath.calculateSpreadPercent(usdt.rate, usd.rate)

            val selectedRateKey = storage.getWidgetCalcRateKey()
            val isVesInput = storage.isWidgetCalcVesInput()
            val calcInput = storage.getWidgetCalcInput()

            val (activeRate, activeSymbol) = when (selectedRateKey) {
                "EUR_BCV" -> eur.rate to "€"
                "USDT" -> usdt.rate to "₮"
                else -> usd.rate to "$"
            }
            val (leftDisplay, rightDisplay) = WidgetCalcMath.formatDisplayPair(
                input = calcInput,
                rate = activeRate,
                isVesInput = isVesInput,
                foreignSymbol = activeSymbol
            )

            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                900,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_tasa_ve_rates).apply {
                setOnClickPendingIntent(R.id.widget_header, launchPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_open, launchPendingIntent)

                setTextViewText(R.id.widget_usd_value, String.format(Locale.US, "%.4f", usd.rate))
                setTextViewText(R.id.widget_eur_value, String.format(Locale.US, "%.4f", eur.rate))
                setTextViewText(R.id.widget_usdt_value, String.format(Locale.US, "%.2f", usdt.rate))
                setTextViewText(R.id.widget_updated, usd.dateUpdated)
                setTextViewText(
                    R.id.widget_spread,
                    "• Brecha ${String.format(Locale.US, "%.1f", spread)}%"
                )

                setInt(
                    R.id.widget_card_usd,
                    "setBackgroundResource",
                    if (selectedRateKey == "USD_BCV") R.drawable.widget_card_selected else R.drawable.widget_card_normal
                )
                setInt(
                    R.id.widget_card_eur,
                    "setBackgroundResource",
                    if (selectedRateKey == "EUR_BCV") R.drawable.widget_card_selected else R.drawable.widget_card_normal
                )
                setInt(
                    R.id.widget_card_usdt,
                    "setBackgroundResource",
                    if (selectedRateKey == "USDT") R.drawable.widget_card_selected else R.drawable.widget_card_normal
                )

                setOnClickPendingIntent(
                    R.id.widget_card_usd,
                    actionBroadcast(context, 101, ACTION_SELECT_RATE) { putExtra(EXTRA_RATE_KEY, "USD_BCV") }
                )
                setOnClickPendingIntent(
                    R.id.widget_card_eur,
                    actionBroadcast(context, 102, ACTION_SELECT_RATE) { putExtra(EXTRA_RATE_KEY, "EUR_BCV") }
                )
                setOnClickPendingIntent(
                    R.id.widget_card_usdt,
                    actionBroadcast(context, 103, ACTION_SELECT_RATE) { putExtra(EXTRA_RATE_KEY, "USDT") }
                )

                setTextViewText(R.id.widget_calc_input, leftDisplay)
                setTextViewText(R.id.widget_calc_output, rightDisplay)
                setOnClickPendingIntent(
                    R.id.widget_btn_swap,
                    actionBroadcast(context, 104, ACTION_SWAP)
                )

                val digitButtons = listOf(
                    Triple(R.id.widget_key_0, 200, "0"),
                    Triple(R.id.widget_key_1, 201, "1"),
                    Triple(R.id.widget_key_2, 202, "2"),
                    Triple(R.id.widget_key_3, 203, "3"),
                    Triple(R.id.widget_key_4, 204, "4"),
                    Triple(R.id.widget_key_5, 205, "5"),
                    Triple(R.id.widget_key_6, 206, "6"),
                    Triple(R.id.widget_key_7, 207, "7"),
                    Triple(R.id.widget_key_8, 208, "8"),
                    Triple(R.id.widget_key_9, 209, "9"),
                    Triple(R.id.widget_key_dot, 210, ".")
                )
                digitButtons.forEach { (viewId, reqCode, token) ->
                    setOnClickPendingIntent(
                        viewId,
                        actionBroadcast(context, reqCode, ACTION_DIGIT) { putExtra(EXTRA_TOKEN, token) }
                    )
                }

                setOnClickPendingIntent(
                    R.id.widget_key_back,
                    actionBroadcast(context, 211, ACTION_BACKSPACE)
                )
                setOnClickPendingIntent(
                    R.id.widget_key_clear,
                    actionBroadcast(context, 212, ACTION_CLEAR)
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
