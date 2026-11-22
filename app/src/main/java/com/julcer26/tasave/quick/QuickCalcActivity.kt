package com.julcer26.tasave.quick

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julcer26.tasave.MainActivity
import com.julcer26.tasave.analytics.AnalyticsTracker
import com.julcer26.tasave.data.CachedRatesBundle
import com.julcer26.tasave.data.LocalStorageHelper
import com.julcer26.tasave.ui.theme.TasaVeTheme
import com.julcer26.tasave.ultra.PaymentMethodProfile
import com.julcer26.tasave.widget.TasaVeRatesWidgetProvider
import com.julcer26.tasave.widget.TasaVeUltraWidgetProvider
import com.julcer26.tasave.widget.WidgetCalcMath
import java.util.Locale

/**
 * Floating translucent quick-converter and clipboard helper.
 * Launched from:
 * 1) Android text selection toolbar (ACTION_PROCESS_TEXT -> "Tasa VE", supports in-place replacement!)
 * 2) Persistent live-rate notification buttons (Copy rates, Quick calc, Copy Pago Movil)
 * 3) Android Quick Settings Tile ("Tasa VE" in Barra Rápida)
 * 4) Android Share menu (ACTION_SEND text/plain -> "Tasa VE")
 * 5) Ultra Home Screen Widget Copy button (Android 10+ foreground clipboard support)
 *
 * When opened from Notification or Quick Settings Tile without explicit text,
 * automatically inspects the clipboard on window focus so any amount copied
 * from a WhatsApp chat bubble (e.g. "$35") is converted immediately.
 */
class QuickCalcActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_MODE = "extra_open_mode"
        const val MODE_CALC = "calc"
        const val MODE_COPY_RATES = "copy_rates"
        const val MODE_COPY_PM = "copy_pm"
        const val ACTION_OPEN_PLANS = TasaVeUltraWidgetProvider.LAUNCH_PLANS

        fun createIntent(context: Context, mode: String): Intent {
            return Intent(context, QuickCalcActivity::class.java).apply {
                putExtra(EXTRA_OPEN_MODE, mode)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    private var clipboardSnippetState by mutableStateOf<String?>(null)
    private var hasCheckedClipboardOnFocus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (handleDirectActionIfNeeded(intent)) {
            return
        }
        renderQuickOverlay(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        hasCheckedClipboardOnFocus = false
        clipboardSnippetState = null
        if (handleDirectActionIfNeeded(intent)) {
            return
        }
        renderQuickOverlay(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !hasCheckedClipboardOnFocus) {
            hasCheckedClipboardOnFocus = true
            if (extractIncomingText(intent).isNullOrBlank()) {
                val clipText = readClipboardTextSafely()
                if (!clipText.isNullOrBlank() && QuickAmountParser.parse(clipText) != null) {
                    clipboardSnippetState = clipText.trim().take(80)
                }
            }
        }
    }

    private fun extractIncomingText(intent: Intent?): String? {
        return when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND ->
                intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
    }

    private fun readClipboardTextSafely(): String? {
        return runCatching {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0)?.coerceToText(this)?.toString()
            } else {
                null
            }
        }.getOrNull()
    }

    private fun handleDirectActionIfNeeded(intent: Intent?): Boolean {
        val mode = intent?.getStringExtra(EXTRA_OPEN_MODE) ?: return false
        val storage = LocalStorageHelper(this)
        return when (mode) {
            MODE_COPY_RATES -> {
                val summary = PersistentRateNotificationManager.formatPersistentRates(
                    storage.getAllCachedRates()
                )
                copyToClipboard("Tasas Tasa VE", summary.copyClipboardText)
                Toast.makeText(this, "Tasas del día copiadas", Toast.LENGTH_SHORT).show()
                AnalyticsTracker.logAction(this, "notif_copy_rates")
                finish()
                true
            }
            MODE_COPY_PM -> {
                val profiles = storage.getPaymentProfiles()
                if (profiles.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Guarda primero tu Pago Móvil en la pestaña Cobros",
                        Toast.LENGTH_SHORT
                    ).show()
                    openMainApp()
                    finish()
                } else {
                    val idx = storage.resolveActivePaymentProfileIndex(profiles)
                    val display = WidgetCalcMath.formatProfileForWidget(
                        profiles[idx],
                        idx,
                        profiles.size
                    )
                    copyToClipboard(display.title, display.copyText)
                    Toast.makeText(this, "${display.title} copiado", Toast.LENGTH_SHORT).show()
                    AnalyticsTracker.logAction(this, "notif_copy_pm")
                    finish()
                }
                true
            }
            else -> false
        }
    }

    private fun renderQuickOverlay(intent: Intent?) {
        val storage = LocalStorageHelper(this)
        val isDark = storage.getTheme() == "DARK"
        val isUltra = storage.isEffectiveUltraActive()
        val rates = storage.getAllCachedRates()
        val profiles = storage.getPaymentProfiles()
        val initialProfileIndex = storage.resolveActivePaymentProfileIndex(profiles)

        val explicitText = extractIncomingText(intent)
        val isEditableProcessText = intent?.action == Intent.ACTION_PROCESS_TEXT &&
            !intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)

        AnalyticsTracker.logAction(
            this,
            if (explicitText != null) "quick_process_text_opened" else "quick_calc_opened"
        )

        setContent {
            val activeSnippet = explicitText ?: clipboardSnippetState
            val parsed = remember(activeSnippet) { QuickAmountParser.parse(activeSnippet) }
            val initialRateKey = if (isUltra && parsed != null) {
                parsed.suggestedRateKey
            } else {
                "USD_BCV"
            }

            TasaVeTheme(darkTheme = isDark, dynamicColor = false) {
                QuickCalcOverlayContent(
                    isDark = isDark,
                    rates = rates,
                    isUltra = isUltra,
                    rawSnippet = activeSnippet,
                    isFromClipboard = explicitText == null && clipboardSnippetState != null,
                    isEditableTextTarget = isEditableProcessText,
                    initialRateKey = initialRateKey,
                    initialAmount = parsed?.amount?.let { formatCleanInput(it) } ?: "10",
                    initialInputIsVes = parsed?.inputIsVes ?: false,
                    profiles = profiles,
                    initialProfileIndex = initialProfileIndex,
                    onSelectProfileIndex = { idx ->
                        val chosen = profiles.getOrNull(idx)
                        if (chosen != null) {
                            storage.setSelectedPaymentProfileId(chosen.id)
                            TasaVeRatesWidgetProvider.updateAllWidgets(this)
                        }
                    },
                    onCopyResult = { text ->
                        copyToClipboard("Conversión Tasa VE", text)
                        if (isEditableProcessText) {
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                            )
                            Toast.makeText(this, "Insertado en el chat y copiado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Conversión copiada", Toast.LENGTH_SHORT).show()
                        }
                        AnalyticsTracker.logAction(this, "quick_copy_result")
                        finish()
                    },
                    onShareResult = { text ->
                        AnalyticsTracker.logAction(this, "quick_share_result")
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        startActivity(Intent.createChooser(sendIntent, "Compartir conversión Tasa VE"))
                        finish()
                    },
                    onCopyWithPaymentProfile = { conversionLine, chosenIndex ->
                        if (profiles.isEmpty()) {
                            copyToClipboard("Conversión Tasa VE", conversionLine)
                            Toast.makeText(
                                this,
                                "Conversión copiada. Añade un Pago Móvil en Cobros.",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            val idx = chosenIndex.coerceIn(0, profiles.lastIndex)
                            storage.setSelectedPaymentProfileId(profiles[idx].id)
                            TasaVeRatesWidgetProvider.updateAllWidgets(this)
                            val display = WidgetCalcMath.formatProfileForWidget(
                                profiles[idx],
                                idx,
                                profiles.size
                            )
                            val combined = "$conversionLine\n\n${display.copyText}"
                            copyToClipboard("Cobro Tasa VE", combined)
                            if (isEditableProcessText) {
                                setResult(
                                    Activity.RESULT_OK,
                                    Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, combined)
                                )
                                Toast.makeText(
                                    this,
                                    "Insertado y copiado con ${display.title}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Monto + ${display.title} copiado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            AnalyticsTracker.logAction(this, "quick_copy_with_pm")
                            finish()
                        }
                    },
                    onOpenPlans = {
                        AnalyticsTracker.logAction(this, "quick_upgrade_click")
                        openMainApp(openPlans = true)
                        finish()
                    },
                    onOpenApp = {
                        openMainApp(openPlans = false)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun openMainApp(openPlans: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (openPlans) {
                putExtra(TasaVeUltraWidgetProvider.EXTRA_WIDGET_ACTION, TasaVeUltraWidgetProvider.LAUNCH_PLANS)
            }
        }
        startActivity(intent)
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun formatCleanInput(value: Double): String {
        return if (value % 1.0 == 0.0 && value < 1_000_000_000.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}

@Composable
private fun QuickCalcOverlayContent(
    isDark: Boolean,
    rates: CachedRatesBundle,
    isUltra: Boolean,
    rawSnippet: String?,
    isFromClipboard: Boolean,
    isEditableTextTarget: Boolean,
    initialRateKey: String,
    initialAmount: String,
    initialInputIsVes: Boolean,
    profiles: List<PaymentMethodProfile>,
    initialProfileIndex: Int,
    onSelectProfileIndex: (Int) -> Unit,
    onCopyResult: (String) -> Unit,
    onShareResult: (String) -> Unit,
    onCopyWithPaymentProfile: (String, Int) -> Unit,
    onOpenPlans: () -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRateKey by remember(initialRateKey) { mutableStateOf(initialRateKey) }
    var amountText by remember(initialAmount) { mutableStateOf(initialAmount) }
    var inputIsVes by remember(initialInputIsVes) { mutableStateOf(initialInputIsVes) }
    var useTomorrowBcv by remember { mutableStateOf(false) }
    var selectedProfileIdx by remember {
        mutableStateOf(initialProfileIndex.coerceIn(0, (profiles.size - 1).coerceAtLeast(0)))
    }
    var upgradeHint by remember { mutableStateOf<String?>(null) }

    val cardBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val surfaceElevated = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val innerResultBg = if (isDark) Color(0xFF090D16) else Color(0xFFE2E8F0)
    val emerald = Color(0xFF10B981)
    val emeraldText = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
    val gold = Color(0xFFF59E0B)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val subtleBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFCBD5E1)

    val hasTomorrowBcv = rates.nextUsdBcv != null && rates.nextUsdBcv.rate > 0.0
    val effectiveTomorrow = useTomorrowBcv && hasTomorrowBcv && selectedRateKey != "USDT"

    val copPerUsd = rates.copPerUsd.takeIf { it > 0.0 } ?: 4320.0
    val usdRate = if (effectiveTomorrow) {
        rates.nextUsdBcv?.rate ?: rates.usdBcv.rate
    } else {
        rates.usdBcv.rate
    }
    val eurRate = if (effectiveTomorrow) {
        rates.nextEurBcv?.rate ?: rates.eurBcv.rate
    } else {
        rates.eurBcv.rate
    }
    val usdtRate = rates.usdtParalelo.rate
    val copRateVes = if (usdRate > 0.0) usdRate / copPerUsd else 0.0

    val activeRate = when (selectedRateKey) {
        "EUR_BCV" -> eurRate
        "USDT" -> usdtRate
        "COP" -> copRateVes
        else -> usdRate
    }

    val tomorrowSuffix = if (effectiveTomorrow) " Mañana" else ""
    val (rateLabel, unitSymbol) = when (selectedRateKey) {
        "EUR_BCV" -> "EUR BCV$tomorrowSuffix" to "€"
        "USDT" -> "USDT Binance" to "USDT"
        "COP" -> "COP BCV$tomorrowSuffix (${String.format(Locale.US, "%.0f", copPerUsd)}/$)" to "COP"
        else -> "USD BCV$tomorrowSuffix" to "$"
    }

    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val converted = when {
        activeRate <= 0.0 || parsedAmount <= 0.0 -> 0.0
        inputIsVes -> parsedAmount / activeRate
        else -> parsedAmount * activeRate
    }

    val resultDisplay = if (inputIsVes) {
        if (selectedRateKey == "COP") {
            "${String.format(Locale.US, "%,.0f", converted)} COP"
        } else {
            "$unitSymbol ${String.format(Locale.US, "%,.2f", converted)}"
        }
    } else {
        "Bs. ${String.format(Locale.US, "%,.2f", converted)}"
    }

    val conversionCopyLine = if (inputIsVes) {
        "Bs. ${String.format(Locale.US, "%,.2f", parsedAmount)} = $resultDisplay ($rateLabel)"
    } else {
        val left = if (selectedRateKey == "COP") {
            "${String.format(Locale.US, "%,.0f", parsedAmount)} COP"
        } else {
            "$unitSymbol ${String.format(Locale.US, "%,.2f", parsedAmount)}"
        }
        "$left = $resultDisplay ($rateLabel)"
    }

    val activeProfile = profiles.getOrNull(selectedProfileIdx)
    val activeProfileLabel = activeProfile?.let {
        val aliasPart = if (it.alias.isNotBlank()) " (${it.alias})" else ""
        "${it.type.label}$aliasPart"
    } ?: "Pago Móvil"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, emerald.copy(alpha = 0.35f)),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚡ Tasa VE",
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isUltra) gold.copy(alpha = 0.16f)
                                    else emerald.copy(alpha = 0.14f)
                                )
                                .border(
                                    1.dp,
                                    if (isUltra) gold.copy(alpha = 0.45f)
                                    else emerald.copy(alpha = 0.35f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isUltra) "★ Ultra" else "Gratis",
                                color = if (isUltra) gold else emeraldText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (hasTomorrowBcv && selectedRateKey != "USDT") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (effectiveTomorrow) gold.copy(alpha = 0.18f)
                                        else surfaceElevated
                                    )
                                    .border(
                                        1.dp,
                                        if (effectiveTomorrow) gold else subtleBorder,
                                        RoundedCornerShape(50)
                                    )
                                    .clickable { useTomorrowBcv = !useTomorrowBcv }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (effectiveTomorrow) "BCV Mañana" else "BCV Hoy",
                                    color = if (effectiveTomorrow) gold else textSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = textSecondary
                        )
                    }
                }

                if (!rawSnippet.isNullOrBlank()) {
                    val sourceLabel = if (isFromClipboard) {
                        "Detectado del portapapeles"
                    } else {
                        "Texto seleccionado"
                    }
                    Text(
                        text = "$sourceLabel: \"${rawSnippet.trim()}\"",
                        color = if (isFromClipboard) emeraldText else textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isFromClipboard) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Rate Selector Chips
                val options = listOf(
                    "USD_BCV" to "USD BCV",
                    "EUR_BCV" to "EUR BCV",
                    "USDT" to "USDT",
                    "COP" to "COP"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEach { (key, label) ->
                        val selected = selectedRateKey == key
                        val locked = !isUltra && key != "USD_BCV"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (locked) {
                                        upgradeHint = "Cambia a $label al instante en cualquier chat con Tasa VE Ultra."
                                    } else {
                                        upgradeHint = null
                                        selectedRateKey = key
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) emerald.copy(alpha = 0.18f) else surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) emerald else subtleBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (locked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = gold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = label,
                                    color = when {
                                        selected -> emeraldText
                                        locked -> textSecondary
                                        else -> textPrimary
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Amount & Direction Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceElevated)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { raw ->
                                if (raw.length <= 14 && raw.all { it.isDigit() || it == '.' || it == ',' }) {
                                    amountText = raw
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = {
                                Text(
                                    text = if (inputIsVes) "Monto en Bolívares (Bs.)" else "Monto en $unitSymbol",
                                    fontSize = 11.sp
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emerald,
                                unfocusedBorderColor = subtleBorder,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = emeraldText,
                                unfocusedLabelColor = textSecondary
                            )
                        )

                        FilledIconButton(
                            onClick = { inputIsVes = !inputIsVes },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = emerald.copy(alpha = 0.18f),
                                contentColor = emeraldText
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Invertir dirección"
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(innerResultBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (inputIsVes) "Equivalente en $rateLabel" else "Total a cobrar en Bolívares",
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = resultDisplay,
                            color = emeraldText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (activeRate > 0.0) {
                            val rateFormatted = if (selectedRateKey == "COP") {
                                "1 USD = ${String.format(Locale.US, "%.0f", copPerUsd)} COP • BCV Bs. ${String.format(Locale.US, "%.2f", usdRate)}"
                            } else {
                                "Tasa $rateLabel: Bs. ${String.format(Locale.US, "%.2f", activeRate)}"
                            }
                            Text(
                                text = rateFormatted,
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Ultra Payment Profile Selector (when user has 2+ profiles)
                if (isUltra && profiles.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        profiles.forEachIndexed { idx, profile ->
                            val isSelected = idx == selectedProfileIdx
                            val chipLabel = if (profile.alias.isNotBlank()) {
                                "${profile.type.label} • ${profile.alias}"
                            } else {
                                profile.displayTitle()
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedProfileIdx = idx
                                        onSelectProfileIndex(idx)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) gold.copy(alpha = 0.16f) else surfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) gold else subtleBorder
                                )
                            ) {
                                Text(
                                    text = chipLabel,
                                    color = if (isSelected) gold else textSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (upgradeHint != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = gold.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, gold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = upgradeHint!!,
                                color = textPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            TextButton(
                                onClick = onOpenPlans,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Ver Ultra",
                                    color = gold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onCopyResult(conversionCopyLine) },
                            enabled = parsedAmount > 0.0 && activeRate > 0.0,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emerald,
                                contentColor = Color(0xFF042F2E)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEditableTextTarget) "Insertar y copiar" else "Copiar resultado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { onShareResult(conversionCopyLine) },
                            enabled = parsedAmount > 0.0 && activeRate > 0.0,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, subtleBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir conversión",
                                tint = textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (isUltra) {
                                onCopyWithPaymentProfile(conversionCopyLine, selectedProfileIdx)
                            } else {
                                upgradeHint = if (profiles.isNotEmpty()) {
                                    "Copia el monto convertido + tus datos de $activeProfileLabel en 1 toque con Tasa VE Ultra."
                                } else {
                                    "Desbloquea el envío rápido de monto + Pago Móvil en 1 toque con Tasa VE Ultra."
                                }
                            }
                        },
                        enabled = !isUltra || (parsedAmount > 0.0 && activeRate > 0.0),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isUltra) gold.copy(alpha = 0.65f) else subtleBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isUltra) gold else textSecondary
                        )
                    ) {
                        Icon(
                            imageVector = if (isUltra) Icons.Default.Star else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUltra) {
                                if (isEditableTextTarget) {
                                    "Insertar monto + $activeProfileLabel"
                                } else {
                                    "Copiar monto + $activeProfileLabel"
                                }
                            } else {
                                "★ Copiar monto + Pago Móvil (Ultra)"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Privacy & Open App footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Solo procesa el texto que seleccionas o copias",
                        color = textSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Abrir app →",
                        color = emeraldText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onOpenApp)
                    )
                }
            }
        }
    }
}
