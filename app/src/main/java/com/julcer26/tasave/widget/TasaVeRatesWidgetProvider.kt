package com.julcer26.tasave.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.julcer26.tasave.MainActivity
import com.julcer26.tasave.R
import com.julcer26.tasave.data.LocalStorageHelper
import com.julcer26.tasave.data.TasaVeRepository
import com.julcer26.tasave.ultra.PaymentMethodProfile
import com.julcer26.tasave.ultra.PaymentMethodType
import com.julcer26.tasave.ultra.UltraMath
import java.util.Locale

data class WidgetPaymentCardData(
    val title: String,
    val displayLines: String,
    val copyText: String
)

object WidgetCalcMath {
    private const val MAX_INPUT_LEN = 10

    fun appendToken(current: String, token: String): String {
        val cleaned = current.trim()
        if (token == ".") {
            if (cleaned.contains('.')) return cleaned
            val base = if (cleaned.isEmpty()) "0" else cleaned
            return if (base.length >= MAX_INPUT_LEN) base else "$base."
        }
        if (token == "00") {
            if (cleaned.isEmpty() || cleaned == "0") return "0"
            return when {
                cleaned.length + 2 <= MAX_INPUT_LEN -> cleaned + "00"
                cleaned.length + 1 <= MAX_INPUT_LEN -> cleaned + "0"
                else -> cleaned
            }
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

    fun formatProfileForWidget(
        profile: PaymentMethodProfile?,
        index: Int = 0,
        totalCount: Int = 0
    ): WidgetPaymentCardData {
        if (profile == null) {
            return WidgetPaymentCardData(
                title = "COBRO EXPRESS",
                displayLines = "Agrega tu Pago Móvil, Cuenta, Zelle o Binance en la pestaña Cobros.",
                copyText = ""
            )
        }
        val counter = if (totalCount > 1) " (${index + 1}/$totalCount)" else ""
        val alias = if (profile.alias.isNotBlank()) " • ${profile.alias}" else ""
        val lines = when (profile.type) {
            PaymentMethodType.PAGO_MOVIL -> listOfNotNull(
                "Banco: ${profile.formattedBank()}",
                profile.formattedDoc().takeIf { it.isNotBlank() }?.let { "CI/RIF: $it" },
                profile.formattedPhone().takeIf { it.isNotBlank() }?.let { "Tel: $it" }
            )
            PaymentMethodType.BANK_ACCOUNT -> listOfNotNull(
                "Banco: ${profile.formattedBank()}",
                profile.accountNumber.takeIf { it.isNotBlank() }?.let { "Cuenta: $it" },
                profile.formattedDoc().takeIf { it.isNotBlank() }?.let { "CI/RIF: $it" },
                profile.accountHolder.takeIf { it.isNotBlank() }?.let { "Titular: $it" }
            )
            PaymentMethodType.ZELLE -> listOfNotNull(
                profile.digitalIdentifier.takeIf { it.isNotBlank() }?.let { "Correo/Tel: $it" },
                profile.accountHolder.takeIf { it.isNotBlank() }?.let { "Titular: $it" }
            )
            PaymentMethodType.BINANCE -> listOfNotNull(
                profile.digitalIdentifier.takeIf { it.isNotBlank() }?.let { "Pay ID/Correo: $it" },
                profile.accountHolder.takeIf { it.isNotBlank() }?.let { "Alias: $it" }
            )
        }
        val header = "${profile.type.label.uppercase(Locale.getDefault())}$alias$counter"
        val copyBlock = buildString {
            appendLine("*${profile.type.label}$alias*")
            lines.forEach { appendLine(it) }
        }.trim()
        return WidgetPaymentCardData(
            title = header,
            displayLines = lines.joinToString("\n").ifBlank { profile.displayTitle() },
            copyText = copyBlock
        )
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val freeComponent = ComponentName(context, TasaVeRatesWidgetProvider::class.java)
            manager.getAppWidgetIds(freeComponent).forEach { id ->
                updateAppWidget(context, manager, id)
            }
            TasaVeUltraWidgetProvider.updateAll(context)
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val storage = LocalStorageHelper(context)
            val bundle = storage.getAllCachedRates()
            val usd = bundle.usdBcv
            val eur = bundle.eurBcv
            val usdt = bundle.usdtParalelo
            val spread = UltraMath.calculateSpreadPercent(usdt.rate, usd.rate)

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                910,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 64)
            val isExpandedHeight = minHeightDp >= 95

            val nextUsd = bundle.nextUsdBcv
            val nextDateIso = bundle.nextBcvDateIso
            val headerMeta = if (!isExpandedHeight && nextUsd != null) {
                "Mañ: ${String.format(Locale.US, "%.2f", nextUsd.rate)} • ${usd.dateUpdated}"
            } else {
                usd.dateUpdated
            }
            val footerText = if (nextUsd != null && nextDateIso != null) {
                val shortDate = TasaVeRepository.formatIsoShortDate(nextDateIso)
                "BCV Mañana ($shortDate): Bs. ${String.format(Locale.US, "%.4f", nextUsd.rate)} • Toca para abrir"
            } else {
                "Toca para abrir calculadora, escáner y cobros"
            }

            val views = RemoteViews(context.packageName, R.layout.widget_tasa_ve_rates).apply {
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                setTextViewText(R.id.widget_usd_value, String.format(Locale.US, "%.4f", usd.rate))
                setTextViewText(R.id.widget_eur_value, String.format(Locale.US, "%.4f", eur.rate))
                setTextViewText(R.id.widget_usdt_value, String.format(Locale.US, "%.2f", usdt.rate))
                setTextViewText(R.id.widget_updated, headerMeta)
                setTextViewText(
                    R.id.widget_spread,
                    "Brecha ${String.format(Locale.US, "%.1f", spread)}%"
                )
                setTextViewText(R.id.widget_footer_note, footerText)
                setViewVisibility(
                    R.id.widget_footer_note,
                    if (isExpandedHeight) View.VISIBLE else View.GONE
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
