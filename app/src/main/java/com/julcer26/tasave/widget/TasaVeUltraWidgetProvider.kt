package com.julcer26.tasave.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.julcer26.tasave.MainActivity
import com.julcer26.tasave.R
import com.julcer26.tasave.data.LocalStorageHelper
import com.julcer26.tasave.data.TasaVeRepository
import com.julcer26.tasave.ui.VeNumberFormat
import com.julcer26.tasave.ultra.UltraMath
import java.util.Locale

class TasaVeUltraWidgetProvider : AppWidgetProvider() {

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

        if (!storage.isEffectiveUltraActive()) {
            updateAll(context)
            return
        }

        var rateKey = storage.getWidgetCalcRateKey()
        var isVes = storage.isWidgetCalcVesInput()
        var input = storage.getWidgetCalcInput()
        var handled = true

        when (action) {
            ACTION_TAB -> {
                val tab = intent.getStringExtra(EXTRA_TAB).orEmpty()
                if (tab in listOf("RATES", "CALC", "PAY")) {
                    storage.setUltraWidgetTab(tab)
                }
            }
            ACTION_DIGIT -> {
                val token = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
                input = WidgetCalcMath.appendToken(input, token)
                storage.saveWidgetCalcState(rateKey, isVes, input)
            }
            ACTION_BACKSPACE -> {
                input = WidgetCalcMath.backspace(input)
                storage.saveWidgetCalcState(rateKey, isVes, input)
            }
            ACTION_CLEAR -> {
                input = "0"
                storage.saveWidgetCalcState(rateKey, isVes, input)
            }
            ACTION_SWAP -> {
                isVes = !isVes
                storage.saveWidgetCalcState(rateKey, isVes, input)
            }
            ACTION_SELECT_RATE -> {
                val nextKey = intent.getStringExtra(EXTRA_RATE_KEY).orEmpty()
                if (nextKey in listOf("USD_BCV", "EUR_BCV", "USDT")) {
                    rateKey = nextKey
                    storage.saveWidgetCalcState(rateKey, isVes, input)
                }
            }
            ACTION_PAY_PREV -> {
                val profiles = storage.getPaymentProfiles()
                if (profiles.isNotEmpty()) {
                    val cur = storage.resolveActivePaymentProfileIndex(profiles)
                    val prev = if (cur - 1 < 0) profiles.lastIndex else cur - 1
                    storage.setUltraWidgetProfileIndex(prev)
                }
            }
            ACTION_PAY_NEXT -> {
                val profiles = storage.getPaymentProfiles()
                if (profiles.isNotEmpty()) {
                    val cur = storage.resolveActivePaymentProfileIndex(profiles)
                    val next = (cur + 1) % profiles.size
                    storage.setUltraWidgetProfileIndex(next)
                }
            }
            ACTION_PAY_COPY -> {
                val profiles = storage.getPaymentProfiles()
                val idx = storage.resolveActivePaymentProfileIndex(profiles)
                val profile = profiles.getOrNull(idx)
                val data = WidgetCalcMath.formatProfileForWidget(profile, idx, profiles.size)
                if (data.copyText.isNotBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Cobro Tasa VE", data.copyText))
                    Toast.makeText(context, "Datos de cobro copiados", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Agrega primero un método de cobro en la app", Toast.LENGTH_SHORT).show()
                }
                handled = false
            }
            else -> handled = false
        }

        if (handled) {
            updateAll(context)
        }
    }

    companion object {
        const val EXTRA_WIDGET_ACTION = "widget_action"
        const val LAUNCH_CONVERTER = "OPEN_CONVERTER"
        const val LAUNCH_SCANNER = "OPEN_SCANNER"
        const val LAUNCH_CART = "OPEN_CART"
        const val LAUNCH_CHECKOUT = "OPEN_CHECKOUT"
        const val LAUNCH_PLANS = "OPEN_PLANS"

        private const val ACTION_TAB = "com.julcer26.tasave.widget.ULTRA_TAB"
        private const val ACTION_DIGIT = "com.julcer26.tasave.widget.ULTRA_DIGIT"
        private const val ACTION_BACKSPACE = "com.julcer26.tasave.widget.ULTRA_BACKSPACE"
        private const val ACTION_CLEAR = "com.julcer26.tasave.widget.ULTRA_CLEAR"
        private const val ACTION_SWAP = "com.julcer26.tasave.widget.ULTRA_SWAP"
        private const val ACTION_SELECT_RATE = "com.julcer26.tasave.widget.ULTRA_SELECT_RATE"
        private const val ACTION_PAY_PREV = "com.julcer26.tasave.widget.ULTRA_PAY_PREV"
        private const val ACTION_PAY_NEXT = "com.julcer26.tasave.widget.ULTRA_PAY_NEXT"
        private const val ACTION_PAY_COPY = "com.julcer26.tasave.widget.ULTRA_PAY_COPY"

        private const val EXTRA_TAB = "extra_tab"
        private const val EXTRA_TOKEN = "extra_token"
        private const val EXTRA_RATE_KEY = "extra_rate_key"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TasaVeUltraWidgetProvider::class.java)
            manager.getAppWidgetIds(componentName).forEach { id ->
                updateAppWidget(context, manager, id)
            }
        }

        private fun launchActivityIntent(
            context: Context,
            requestCode: Int,
            widgetAction: String
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, widgetAction)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun actionBroadcast(
            context: Context,
            requestCode: Int,
            action: String,
            configure: Intent.() -> Unit = {}
        ): PendingIntent {
            val intent = Intent(context, TasaVeUltraWidgetProvider::class.java).apply {
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
            val isUltra = storage.isEffectiveUltraActive()
            val views = RemoteViews(context.packageName, R.layout.widget_tasa_ve_ultra)

            if (!isUltra) {
                views.setViewVisibility(R.id.ultra_locked_container, View.VISIBLE)
                views.setViewVisibility(R.id.ultra_unlocked_container, View.GONE)
                val unlockPi = launchActivityIntent(context, 401, LAUNCH_PLANS)
                views.setOnClickPendingIntent(R.id.ultra_locked_container, unlockPi)
                views.setOnClickPendingIntent(R.id.ultra_widget_unlock_btn, unlockPi)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            views.setViewVisibility(R.id.ultra_locked_container, View.GONE)
            views.setViewVisibility(R.id.ultra_unlocked_container, View.VISIBLE)

            val activeTab = storage.getUltraWidgetTab()
            val bundle = storage.getAllCachedRates()
            val usd = bundle.usdBcv
            val eur = bundle.eurBcv
            val usdt = bundle.usdtParalelo
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

            views.apply {
                val openMain = launchActivityIntent(context, 402, LAUNCH_CONVERTER)
                setOnClickPendingIntent(R.id.ultra_widget_logo, openMain)
                setOnClickPendingIntent(R.id.ultra_widget_brand, openMain)

                // Tab pills
                setViewVisibility(R.id.ultra_panel_rates, if (activeTab == "RATES") View.VISIBLE else View.GONE)
                setViewVisibility(R.id.ultra_panel_calc, if (activeTab == "CALC") View.VISIBLE else View.GONE)
                setViewVisibility(R.id.ultra_panel_pay, if (activeTab == "PAY") View.VISIBLE else View.GONE)

                setInt(
                    R.id.ultra_tab_btn_rates,
                    "setBackgroundResource",
                    if (activeTab == "RATES") R.drawable.widget_tab_selected else R.drawable.widget_tab_normal
                )
                setTextColor(
                    R.id.ultra_tab_btn_rates,
                    Color.parseColor(if (activeTab == "RATES") "#34D399" else "#CBD5E1")
                )
                setInt(
                    R.id.ultra_tab_btn_calc,
                    "setBackgroundResource",
                    if (activeTab == "CALC") R.drawable.widget_tab_selected else R.drawable.widget_tab_normal
                )
                setTextColor(
                    R.id.ultra_tab_btn_calc,
                    Color.parseColor(if (activeTab == "CALC") "#34D399" else "#CBD5E1")
                )
                setInt(
                    R.id.ultra_tab_btn_pay,
                    "setBackgroundResource",
                    if (activeTab == "PAY") R.drawable.widget_tab_selected else R.drawable.widget_tab_normal
                )
                setTextColor(
                    R.id.ultra_tab_btn_pay,
                    Color.parseColor(if (activeTab == "PAY") "#34D399" else "#CBD5E1")
                )

                setOnClickPendingIntent(
                    R.id.ultra_tab_btn_rates,
                    actionBroadcast(context, 410, ACTION_TAB) { putExtra(EXTRA_TAB, "RATES") }
                )
                setOnClickPendingIntent(
                    R.id.ultra_tab_btn_calc,
                    actionBroadcast(context, 411, ACTION_TAB) { putExtra(EXTRA_TAB, "CALC") }
                )
                setOnClickPendingIntent(
                    R.id.ultra_tab_btn_pay,
                    actionBroadcast(context, 412, ACTION_TAB) { putExtra(EXTRA_TAB, "PAY") }
                )

                // Panel 1: Rates + Shortcuts
                setTextViewText(R.id.ultra_rates_usd, VeNumberFormat.formatRate(usd.rate, 4))
                setTextViewText(R.id.ultra_rates_eur, VeNumberFormat.formatRate(eur.rate, 4))
                setTextViewText(R.id.ultra_rates_usdt, VeNumberFormat.formatRate(usdt.rate, 2))
                val nextUsd = bundle.nextUsdBcv
                val nextDate = bundle.nextBcvDateIso
                val metaText = if (nextUsd != null && nextDate != null) {
                    "Brecha ${VeNumberFormat.formatPercent(spread, 1)}% • Mañana (${TasaVeRepository.formatIsoShortDate(nextDate)}): ${VeNumberFormat.formatRate(nextUsd.rate, 4)}"
                } else {
                    "Brecha ${VeNumberFormat.formatPercent(spread, 1)}% • Actualizado: ${usd.dateUpdated}"
                }
                setTextViewText(R.id.ultra_rates_meta, metaText)
                setOnClickPendingIntent(R.id.ultra_btn_scanner, launchActivityIntent(context, 420, LAUNCH_SCANNER))
                setOnClickPendingIntent(R.id.ultra_btn_cart, launchActivityIntent(context, 421, LAUNCH_CART))

                // Panel 2: Classic Calculator
                setTextViewText(R.id.ultra_rate_usd, "USD ${VeNumberFormat.formatRate(usd.rate, 2)}")
                setTextViewText(R.id.ultra_rate_eur, "EUR ${VeNumberFormat.formatRate(eur.rate, 2)}")
                setTextViewText(R.id.ultra_rate_usdt, "USDT ${VeNumberFormat.formatRate(usdt.rate, 2)}")

                setInt(
                    R.id.ultra_rate_usd,
                    "setBackgroundResource",
                    if (selectedRateKey == "USD_BCV") R.drawable.widget_card_selected else R.drawable.widget_card_normal
                )
                setTextColor(
                    R.id.ultra_rate_usd,
                    Color.parseColor(if (selectedRateKey == "USD_BCV") "#34D399" else "#CBD5E1")
                )
                setInt(
                    R.id.ultra_rate_eur,
                    "setBackgroundResource",
                    if (selectedRateKey == "EUR_BCV") R.drawable.widget_card_selected else R.drawable.widget_card_normal
                )
                setTextColor(
                    R.id.ultra_rate_eur,
                    Color.parseColor(if (selectedRateKey == "EUR_BCV") "#60A5FA" else "#CBD5E1")
                )
                setInt(
                    R.id.ultra_rate_usdt,
                    "setBackgroundResource",
                    if (selectedRateKey == "USDT") R.drawable.widget_card_selected else R.drawable.widget_card_normal
                )
                setTextColor(
                    R.id.ultra_rate_usdt,
                    Color.parseColor(if (selectedRateKey == "USDT") "#F59E0B" else "#CBD5E1")
                )

                setOnClickPendingIntent(
                    R.id.ultra_rate_usd,
                    actionBroadcast(context, 430, ACTION_SELECT_RATE) { putExtra(EXTRA_RATE_KEY, "USD_BCV") }
                )
                setOnClickPendingIntent(
                    R.id.ultra_rate_eur,
                    actionBroadcast(context, 431, ACTION_SELECT_RATE) { putExtra(EXTRA_RATE_KEY, "EUR_BCV") }
                )
                setOnClickPendingIntent(
                    R.id.ultra_rate_usdt,
                    actionBroadcast(context, 432, ACTION_SELECT_RATE) { putExtra(EXTRA_RATE_KEY, "USDT") }
                )

                setTextViewText(R.id.ultra_calc_input, leftDisplay)
                setTextViewText(R.id.ultra_calc_output, rightDisplay)

                val keyTokens = listOf(
                    Triple(R.id.ultra_key_0, 500, "0"),
                    Triple(R.id.ultra_key_1, 501, "1"),
                    Triple(R.id.ultra_key_2, 502, "2"),
                    Triple(R.id.ultra_key_3, 503, "3"),
                    Triple(R.id.ultra_key_4, 504, "4"),
                    Triple(R.id.ultra_key_5, 505, "5"),
                    Triple(R.id.ultra_key_6, 506, "6"),
                    Triple(R.id.ultra_key_7, 507, "7"),
                    Triple(R.id.ultra_key_8, 508, "8"),
                    Triple(R.id.ultra_key_9, 509, "9"),
                    Triple(R.id.ultra_key_dot, 510, "."),
                    Triple(R.id.ultra_key_00, 511, "00")
                )
                keyTokens.forEach { (viewId, reqCode, token) ->
                    setOnClickPendingIntent(
                        viewId,
                        actionBroadcast(context, reqCode, ACTION_DIGIT) { putExtra(EXTRA_TOKEN, token) }
                    )
                }
                setOnClickPendingIntent(R.id.ultra_key_back, actionBroadcast(context, 512, ACTION_BACKSPACE))
                setOnClickPendingIntent(R.id.ultra_key_clear, actionBroadcast(context, 513, ACTION_CLEAR))
                setOnClickPendingIntent(R.id.ultra_key_swap, actionBroadcast(context, 514, ACTION_SWAP))
                setOnClickPendingIntent(R.id.ultra_key_open, launchActivityIntent(context, 515, LAUNCH_CONVERTER))

                // Panel 3: Cobro Express
                val profiles = storage.getPaymentProfiles()
                val idx = storage.resolveActivePaymentProfileIndex(profiles)
                val profileData = WidgetCalcMath.formatProfileForWidget(
                    profile = profiles.getOrNull(idx),
                    index = idx,
                    totalCount = profiles.size
                )
                setTextViewText(R.id.ultra_pay_title, profileData.title)
                setTextViewText(R.id.ultra_pay_details, profileData.displayLines)
                setViewVisibility(R.id.ultra_pay_prev, if (profiles.size > 1) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.ultra_pay_next, if (profiles.size > 1) View.VISIBLE else View.GONE)
                setOnClickPendingIntent(R.id.ultra_pay_prev, actionBroadcast(context, 520, ACTION_PAY_PREV))
                setOnClickPendingIntent(R.id.ultra_pay_next, actionBroadcast(context, 521, ACTION_PAY_NEXT))
                val copyPmIntent = com.julcer26.tasave.quick.QuickCalcActivity.createIntent(
                    context,
                    com.julcer26.tasave.quick.QuickCalcActivity.MODE_COPY_PM
                )
                val copyPmPendingIntent = PendingIntent.getActivity(
                    context,
                    522,
                    copyPmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.ultra_pay_copy, copyPmPendingIntent)
                setOnClickPendingIntent(R.id.ultra_pay_open, launchActivityIntent(context, 523, LAUNCH_CHECKOUT))
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
