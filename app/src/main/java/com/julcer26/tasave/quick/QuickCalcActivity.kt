package com.julcer26.tasave.quick

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
 * Compact floating translucent quick-converter and clipboard helper.
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
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            }
        }
    }

    private var clipboardSnippetState by mutableStateOf<String?>(null)
    private var clipboardDetectionSeq by mutableStateOf(0)
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun isCallerContextualAction(action: String?): Boolean {
        return action == Intent.ACTION_SEND
    }

    private fun closeOverlay(preserveCallerTask: Boolean = isCallerContextualAction(intent?.action)) {
        mainHandler.removeCallbacksAndMessages(null)
        if (preserveCallerTask) {
            finish()
        } else {
            runCatching {
                finishAndRemoveTask()
            }.onFailure {
                finish()
            }
        }
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        if (handleDirectActionIfNeeded(intent)) {
            return
        }
        renderQuickOverlay(intent)
        scheduleClipboardProbes()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        clipboardSnippetState = null
        if (handleDirectActionIfNeeded(intent)) {
            return
        }
        renderQuickOverlay(intent)
        scheduleClipboardProbes()
    }

    override fun onResume() {
        super.onResume()
        scheduleClipboardProbes()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            scheduleClipboardProbes()
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /**
     * When launched from Notification shade ("Calc. rápida") or Quick Settings Tile,
     * SystemUI takes ~150-350ms to collapse and transfer input focus to our window.
     * Probing at 0, 120, 260, and 450ms guarantees we read ClipboardManager as soon as focus lands.
     */
    private fun scheduleClipboardProbes() {
        if (!extractIncomingText(intent).isNullOrBlank()) return
        val delays = longArrayOf(0L, 120L, 260L, 450L)
        for (delayMs in delays) {
            mainHandler.postDelayed({
                if (clipboardSnippetState == null && extractIncomingText(intent).isNullOrBlank()) {
                    tryReadClipboardAmount(showToastOnFail = false)
                }
            }, delayMs)
        }
    }

    private fun tryReadClipboardAmount(showToastOnFail: Boolean): Boolean {
        val clipText = readClipboardTextSafely()
        if (!clipText.isNullOrBlank()) {
            val parsed = QuickAmountParser.parse(clipText)
            if (parsed != null) {
                clipboardSnippetState = clipText.trim().take(80)
                clipboardDetectionSeq += 1
                return true
            }
        }
        if (showToastOnFail) {
            Toast.makeText(
                this,
                "Copia primero un monto en WhatsApp para pegarlo aquí",
                Toast.LENGTH_SHORT
            ).show()
        }
        return false
    }

    private fun extractIncomingText(intent: Intent?): String? {
        return when (intent?.action) {
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
                closeOverlay()
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
                    closeOverlay()
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
                    closeOverlay()
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

        AnalyticsTracker.logAction(
            this,
            if (explicitText != null) "quick_share_text_opened" else "quick_calc_opened"
        )

        setContent {
            val activeSnippet = explicitText ?: clipboardSnippetState
            val parsed = remember(activeSnippet, clipboardDetectionSeq) {
                QuickAmountParser.parse(activeSnippet)
            }
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
                    snippetVersion = clipboardDetectionSeq,
                    isFromClipboard = explicitText == null && clipboardSnippetState != null,
                    initialRateKey = initialRateKey,
                    initialAmount = parsed?.amount?.let { formatCleanInput(it) } ?: "1",
                    initialInputIsVes = parsed?.inputIsVes ?: false,
                    profiles = profiles,
                    initialProfileIndex = initialProfileIndex,
                    onPasteClipboard = {
                        tryReadClipboardAmount(showToastOnFail = true)
                    },
                    onSelectProfileIndex = { idx ->
                        val chosen = profiles.getOrNull(idx)
                        if (chosen != null) {
                            storage.setSelectedPaymentProfileId(chosen.id)
                            TasaVeRatesWidgetProvider.updateAllWidgets(this)
                        }
                    },
                    onCopyResult = { text ->
                        copyToClipboard("Conversión Tasa VE", text)
                        Toast.makeText(this, "Conversión copiada", Toast.LENGTH_SHORT).show()
                        AnalyticsTracker.logAction(this, "quick_copy_result")
                        closeOverlay()
                    },
                    onShareResult = { text ->
                        AnalyticsTracker.logAction(this, "quick_share_result")
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        startActivity(Intent.createChooser(sendIntent, "Compartir conversión Tasa VE"))
                        closeOverlay()
                    },
                    onCopyWithPaymentProfile = { conversionLine, chosenIndex ->
                        if (profiles.isEmpty()) {
                            copyToClipboard("Conversión Tasa VE", conversionLine)
                            Toast.makeText(
                                this,
                                "Conversión copiada. Añade un Pago Móvil en Cobros.",
                                Toast.LENGTH_SHORT
                            ).show()
                            closeOverlay()
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
                            Toast.makeText(
                                this,
                                "Monto + ${display.title} copiado",
                                Toast.LENGTH_SHORT
                            ).show()
                            AnalyticsTracker.logAction(this, "quick_copy_with_pm")
                            closeOverlay()
                        }
                    },
                    onOpenPlans = {
                        AnalyticsTracker.logAction(this, "quick_upgrade_click")
                        openMainApp(openPlans = true)
                        closeOverlay()
                    },
                    onOpenApp = {
                        openMainApp(openPlans = false)
                        closeOverlay()
                    },
                    onDismiss = { closeOverlay() }
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
    snippetVersion: Int,
    isFromClipboard: Boolean,
    initialRateKey: String,
    initialAmount: String,
    initialInputIsVes: Boolean,
    profiles: List<PaymentMethodProfile>,
    initialProfileIndex: Int,
    onPasteClipboard: () -> Unit,
    onSelectProfileIndex: (Int) -> Unit,
    onCopyResult: (String) -> Unit,
    onShareResult: (String) -> Unit,
    onCopyWithPaymentProfile: (String, Int) -> Unit,
    onOpenPlans: () -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRateKey by remember(initialRateKey, snippetVersion) { mutableStateOf(initialRateKey) }
    var amountText by remember(initialAmount, snippetVersion) { mutableStateOf(initialAmount) }
    var inputIsVes by remember(initialInputIsVes, snippetVersion) { mutableStateOf(initialInputIsVes) }
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
        "COP" -> "COP BCV$tomorrowSuffix" to "COP"
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
    val activeProfileShort = activeProfile?.let {
        val aliasPart = if (it.alias.isNotBlank()) " ${it.alias}" else ""
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, emerald.copy(alpha = 0.35f)),
            tonalElevation = 8.dp,
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Compact Header (Title + Tier + BCV Hoy/Mañana + Paste + Open App + Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "⚡ Tasa VE",
                            color = textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isUltra) gold.copy(alpha = 0.16f)
                                    else emerald.copy(alpha = 0.14f)
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isUltra) "★ Ultra" else "Gratis",
                                color = if (isUltra) gold else emeraldText,
                                fontSize = 10.sp,
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
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (effectiveTomorrow) "Mañana" else "Hoy",
                                    color = if (effectiveTomorrow) gold else textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 1-Tap Paste chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(surfaceElevated)
                                .border(1.dp, subtleBorder, RoundedCornerShape(50))
                                .clickable(onClick = onPasteClipboard)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Pegar monto",
                                tint = emeraldText,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Pegar",
                                color = emeraldText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "App →",
                            color = emeraldText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onOpenApp)
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(surfaceElevated)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Row 2: Slim Rate Selector Pills
                val options = listOf(
                    "USD_BCV" to "USD",
                    "EUR_BCV" to "EUR",
                    "USDT" to "USDT",
                    "COP" to "COP"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    options.forEach { (key, label) ->
                        val selected = selectedRateKey == key
                        val locked = !isUltra && key != "USD_BCV"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (locked) {
                                        upgradeHint = "Cambia a $label en cualquier chat con Ultra."
                                    } else {
                                        upgradeHint = null
                                        selectedRateKey = key
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) emerald.copy(alpha = 0.18f) else surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) emerald else subtleBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 3.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (locked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = gold,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                Text(
                                    text = label,
                                    color = when {
                                        selected -> emeraldText
                                        locked -> textSecondary
                                        else -> textPrimary
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Row 3: Compact Side-by-Side Input ⇄ Result Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(surfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Input Box (Left)
                    Row(
                        modifier = Modifier
                            .weight(0.95f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(innerResultBg)
                            .border(1.dp, emerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (inputIsVes) "Bs." else unitSymbol,
                            color = emeraldText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        BasicTextField(
                            value = amountText,
                            onValueChange = { raw ->
                                if (raw.length <= 12 && raw.all { it.isDigit() || it == '.' || it == ',' }) {
                                    amountText = raw
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            cursorBrush = SolidColor(emerald),
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (amountText.isEmpty()) {
                                    Text("0", color = textSecondary, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (amountText.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar",
                                tint = textSecondary,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { amountText = "" }
                            )
                        }
                    }

                    // Swap Button (Center)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(emerald.copy(alpha = 0.18f))
                            .clickable { inputIsVes = !inputIsVes },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Invertir",
                            tint = emeraldText,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Result Box (Right)
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(innerResultBg)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = resultDisplay,
                            color = emeraldText,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val miniRate = if (selectedRateKey == "COP") {
                            "${String.format(Locale.US, "%.0f", copPerUsd)} COP/$"
                        } else {
                            "Tasa: Bs. ${String.format(Locale.US, "%.2f", activeRate)}"
                        }
                        Text(
                            text = miniRate,
                            color = textSecondary,
                            fontSize = 9.5.sp,
                            maxLines = 1
                        )
                    }
                }

                // Optional Ultra Payment Profile Selector (when user has 2+ profiles)
                if (isUltra && profiles.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedProfileIdx = idx
                                        onSelectProfileIndex(idx)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) gold.copy(alpha = 0.16f) else surfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) gold else subtleBorder
                                )
                            ) {
                                Text(
                                    text = chipLabel,
                                    color = if (isSelected) gold else textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (upgradeHint != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(gold.copy(alpha = 0.12f))
                            .border(1.dp, gold.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = upgradeHint!!,
                            color = textPrimary,
                            fontSize = 10.5.sp,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )
                        Text(
                            text = "Ver Ultra",
                            color = gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable(onClick = onOpenPlans)
                        )
                    }
                }

                // Row 4: Compact Single-Row Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onCopyResult(conversionCopyLine) },
                        enabled = parsedAmount > 0.0 && activeRate > 0.0,
                        modifier = Modifier
                            .weight(0.95f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = emerald,
                            contentColor = Color(0xFF042F2E)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Copiar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (isUltra) {
                                onCopyWithPaymentProfile(conversionCopyLine, selectedProfileIdx)
                            } else {
                                upgradeHint = "Copia el monto + tu $activeProfileShort en 1 toque con Ultra."
                            }
                        },
                        enabled = !isUltra || (parsedAmount > 0.0 && activeRate > 0.0),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
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
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isUltra) "+ $activeProfileShort" else "★ + Pago Móvil",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { onShareResult(conversionCopyLine) },
                        enabled = parsedAmount > 0.0 && activeRate > 0.0,
                        modifier = Modifier
                            .size(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, subtleBorder),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = textPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
