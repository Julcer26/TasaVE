package com.julcer26.tasave.quick

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
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
import com.julcer26.tasave.widget.TasaVeUltraWidgetProvider
import com.julcer26.tasave.widget.WidgetCalcMath
import java.util.Locale

/**
 * Floating translucent quick-converter and clipboard helper.
 * Launched from:
 * 1) Android text selection toolbar (ACTION_PROCESS_TEXT -> "Tasa VE")
 * 2) Persistent live-rate notification buttons (Copy rates, Quick calc, Copy Pago Movil)
 * 3) Android Quick Settings Tile (Ultra)
 */
class QuickCalcActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_MODE = "extra_open_mode"
        const val MODE_CALC = "calc"
        const val MODE_COPY_RATES = "copy_rates"
        const val MODE_COPY_PM = "copy_pm"
        const val ACTION_OPEN_PLANS = "open_plans"

        fun createIntent(context: Context, mode: String): Intent {
            return Intent(context, QuickCalcActivity::class.java).apply {
                putExtra(EXTRA_OPEN_MODE, mode)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

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
        if (handleDirectActionIfNeeded(intent)) {
            return
        }
        renderQuickOverlay(intent)
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
                        "Guarda primero tu Pago Móvil en la pestaña Cobrar",
                        Toast.LENGTH_SHORT
                    ).show()
                    openMainApp()
                    finish()
                } else {
                    val idx = storage.getUltraWidgetProfileIndex().coerceIn(0, profiles.lastIndex)
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
        val isUltra = storage.isEffectiveUltraActive()
        val rates = storage.getAllCachedRates()
        val rawProcessText = if (intent?.action == Intent.ACTION_PROCESS_TEXT) {
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        } else {
            null
        }
        val parsed = QuickAmountParser.parse(rawProcessText)

        AnalyticsTracker.logAction(
            this,
            if (rawProcessText != null) "quick_process_text_opened" else "quick_calc_opened"
        )

        setContent {
            TasaVeTheme(darkTheme = true, dynamicColor = false) {
                QuickCalcOverlayContent(
                    rates = rates,
                    isUltra = isUltra,
                    rawSnippet = rawProcessText,
                    initialAmount = parsed?.amount?.let { formatCleanInput(it) } ?: "10",
                    initialInputIsVes = parsed?.inputIsVes ?: false,
                    hasSavedProfile = storage.getPaymentProfiles().isNotEmpty(),
                    onCopyResult = { text ->
                        copyToClipboard("Conversión Tasa VE", text)
                        Toast.makeText(this, "Conversión copiada", Toast.LENGTH_SHORT).show()
                        AnalyticsTracker.logAction(this, "quick_copy_result")
                        finish()
                    },
                    onCopyWithPaymentProfile = { conversionLine ->
                        val profiles = storage.getPaymentProfiles()
                        if (profiles.isEmpty()) {
                            copyToClipboard("Conversión Tasa VE", conversionLine)
                            Toast.makeText(
                                this,
                                "Conversión copiada. Añade un Pago Móvil en Cobrar.",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            val idx = storage.getUltraWidgetProfileIndex()
                                .coerceIn(0, profiles.lastIndex)
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (openPlans) {
                putExtra(TasaVeUltraWidgetProvider.EXTRA_WIDGET_ACTION, ACTION_OPEN_PLANS)
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
    rates: CachedRatesBundle,
    isUltra: Boolean,
    rawSnippet: String?,
    initialAmount: String,
    initialInputIsVes: Boolean,
    hasSavedProfile: Boolean,
    onCopyResult: (String) -> Unit,
    onCopyWithPaymentProfile: (String) -> Unit,
    onOpenPlans: () -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRateKey by remember { mutableStateOf("USD_BCV") }
    var amountText by remember { mutableStateOf(initialAmount) }
    var inputIsVes by remember { mutableStateOf(initialInputIsVes) }
    var upgradeHint by remember { mutableStateOf<String?>(null) }

    val cardBg = Color(0xFF0F172A)
    val surfaceElevated = Color(0xFF1E293B)
    val emerald = Color(0xFF10B981)
    val gold = Color(0xFFF59E0B)
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)

    val copPerUsd = rates.copPerUsd.takeIf { it > 0.0 } ?: 4320.0
    val usdRate = rates.usdBcv.rate
    val eurRate = rates.eurBcv.rate
    val usdtRate = rates.usdtParalelo.rate
    val copRateVes = if (usdRate > 0.0) usdRate / copPerUsd else 0.0

    val activeRate = when (selectedRateKey) {
        "EUR_BCV" -> eurRate
        "USDT" -> usdtRate
        "COP" -> copRateVes
        else -> usdRate
    }

    val (rateLabel, unitSymbol) = when (selectedRateKey) {
        "EUR_BCV" -> "EUR BCV" to "€"
        "USDT" -> "USDT Binance" to "USDT"
        "COP" -> "COP BCV (${String.format(Locale.US, "%.0f", copPerUsd)}/$)" to "COP"
        else -> "USD BCV" to "$"
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 16.dp, vertical = 24.dp),
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                color = if (isUltra) gold else emerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
                    Text(
                        text = "Texto seleccionado: \"${rawSnippet.trim()}\"",
                        color = textSecondary,
                        fontSize = 11.sp,
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
                            color = if (selected) emerald.copy(alpha = 0.2f) else surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) emerald else Color.White.copy(alpha = 0.08f)
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
                                        selected -> emerald
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
                                unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = emerald,
                                unfocusedLabelColor = textSecondary
                            )
                        )

                        FilledIconButton(
                            onClick = { inputIsVes = !inputIsVes },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = emerald.copy(alpha = 0.18f),
                                contentColor = emerald
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
                            .background(cardBg.copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (inputIsVes) "Equivalente en $rateLabel" else "Total a cobrar en Bolívares",
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = resultDisplay,
                            color = emerald,
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
                    Button(
                        onClick = { onCopyResult(conversionCopyLine) },
                        enabled = parsedAmount > 0.0 && activeRate > 0.0,
                        modifier = Modifier.fillMaxWidth(),
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copiar resultado", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (isUltra) {
                                onCopyWithPaymentProfile(conversionCopyLine)
                            } else {
                                upgradeHint = if (hasSavedProfile) {
                                    "Copia el monto convertido + tus datos de Pago Móvil en 1 toque con Tasa VE Ultra."
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
                            if (isUltra) gold.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.16f)
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
                            text = if (isUltra) "Copiar monto + Pago Móvil" else "★ Copiar monto + Pago Móvil (Ultra)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
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
                        text = "Solo procesa el texto que seleccionas",
                        color = textSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Abrir app →",
                        color = emerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onOpenApp)
                    )
                }
            }
        }
    }
}
