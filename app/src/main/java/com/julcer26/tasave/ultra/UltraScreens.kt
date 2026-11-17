package com.julcer26.tasave.ultra

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.julcer26.tasave.ui.RateSelectorCard
import com.julcer26.tasave.ui.TasaVeUiState
import com.julcer26.tasave.widget.TasaVeRatesWidgetProvider
import java.util.Locale

private val Emerald = Color(0xFF10B981)
private val EmeraldLight = Color(0xFF34D399)
private val AmberUltra = Color(0xFFF59E0B)

@Composable
private fun UltraLockBanner(
    isDark: Boolean,
    featureTitle: String,
    featureDescription: String,
    ultraPrice: String,
    onOpenPlans: () -> Unit
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF141418) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(1.dp, Color(0x4DF59E0B), RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x26F59E0B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = AmberUltra,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = "Función Exclusiva Ultra",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AmberUltra,
                letterSpacing = 0.6.sp
            )
            Text(
                text = featureTitle,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = featureDescription,
                fontSize = 13.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Button(
                onClick = onOpenPlans,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberUltra,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ver Planes • Ultra $ultraPrice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// =========================================================================
// TAB 2: CARRO DE COMPRAS CON ESCÁNER + IVA / IGTF
// =========================================================================
@Composable
fun CartTab(
    isDark: Boolean,
    isUltraUser: Boolean,
    ultraPrice: String,
    uiState: TasaVeUiState,
    cartItems: List<CartItem>,
    applyIva: Boolean,
    applyIgtf: Boolean,
    onAddManualItem: (name: String, amount: Double, isVes: Boolean) -> Unit,
    onScanToCart: () -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onToggleIva: () -> Unit,
    onToggleIgtf: () -> Unit,
    onOpenPlans: () -> Unit
) {
    if (!isUltraUser) {
        UltraLockBanner(
            isDark = isDark,
            featureTitle = "Carrito Inteligente de Supermercado",
            featureDescription = "Escanea precios pasillo por pasillo, suma tus productos automáticamente y calcula IVA (16%) e IGTF (3%) en vivo. Además, Ultra incluye cero anuncios y escáner ilimitado.",
            ultraPrice = ultraPrice,
            onOpenPlans = onOpenPlans
        )
        return
    }

    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF141418) else Color.White
    val borderCol = if (isDark) Color(0x1F94A3B8) else Color(0x19475569)

    val usdRate = uiState.usdBcv?.rate ?: 0.0
    val eurRate = uiState.eurBcv?.rate ?: 0.0
    val usdtRate = uiState.usdtParalelo?.rate ?: 0.0

    val summary = remember(cartItems, applyIva, applyIgtf, usdRate, eurRate, usdtRate) {
        UltraMath.calculateCartSummary(cartItems, applyIva, applyIgtf, usdRate, eurRate, usdtRate)
    }

    var showManualDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Header Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL DEL CARRITO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "$ ${String.format(Locale.US, "%,.2f", summary.totalUsd)}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "En Bolívares (BCV)",
                        fontSize = 10.sp,
                        color = textSecondary
                    )
                    Text(
                        text = "Bs. ${String.format(Locale.US, "%,.2f", summary.totalVes)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                    Text(
                        text = "€ ${String.format(Locale.US, "%.2f", summary.totalEur)}  •  ₮ ${String.format(Locale.US, "%.2f", summary.totalUsdt)}",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                }
            }

            // IVA & IGTF Quick Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = applyIva,
                    onClick = onToggleIva,
                    label = { Text("IVA 16%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (applyIva) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x2610B981),
                        selectedLabelColor = EmeraldLight,
                        selectedLeadingIconColor = EmeraldLight
                    )
                )
                FilterChip(
                    selected = applyIgtf,
                    onClick = onToggleIgtf,
                    label = { Text("IGTF 3%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (applyIgtf) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x26F59E0B),
                        selectedLabelColor = AmberUltra,
                        selectedLeadingIconColor = AmberUltra
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                if (cartItems.isNotEmpty()) {
                    IconButton(onClick = onClearCart) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Vaciar carrito",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }

            if (applyIva || applyIgtf) {
                Text(
                    text = "Subtotal: $${String.format(Locale.US, "%.2f", summary.subtotalUsd)}" +
                        (if (applyIva) " + IVA $${String.format(Locale.US, "%.2f", summary.ivaUsd)}" else "") +
                        (if (applyIgtf) " + IGTF $${String.format(Locale.US, "%.2f", summary.igtfUsd)}" else ""),
                    fontSize = 10.5.sp,
                    color = textSecondary
                )
            }
        }

        // Action Buttons: Scan or Manual Add
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onScanToCart,
                modifier = Modifier
                    .weight(1.3f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Escanear al carrito", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }

            OutlinedButton(
                onClick = { showManualDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, borderCol)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = textPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textPrimary)
            }
        }

        // Cart Items List
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg.copy(alpha = 0.6f))
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCartCheckout,
                        contentDescription = null,
                        tint = textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "Tu carrito está vacío",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Apunta la cámara a las etiquetas del supermercado o agrega montos manualmente para llevar el control exacto de tu compra.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(
                    items = cartItems,
                    key = { it.id }
                ) { item ->
                    val lineUsd = item.unitPriceUsd * item.quantity
                    val lineVes = lineUsd * usdRate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", item.unitPriceUsd)} c/u • Total Bs. ${String.format(Locale.US, "%,.2f", lineVes)}",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onUpdateQuantity(item.id, -1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Restar",
                                    tint = textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "${item.quantity}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                modifier = Modifier.widthIn(min = 20.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = { onUpdateQuantity(item.id, +1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Sumar",
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRemoveItem(item.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Eliminar",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualCartItemDialog(
            isDark = isDark,
            onDismiss = { showManualDialog = false },
            onAdd = { name, amount, isVes ->
                onAddManualItem(name, amount, isVes)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun ManualCartItemDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onAdd: (name: String, amount: Double, isVes: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isVes by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF141418) else Color.White,
        title = {
            Text(
                text = "Agregar producto",
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Descripción (opcional)") },
                    placeholder = { Text("Ej. Harina PAN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (isVes) "Precio en Bolívares (Bs.)" else "Precio en Dólares ($)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isVes,
                        onClick = { isVes = false },
                        label = { Text("En Dólares ($)") }
                    )
                    FilterChip(
                        selected = isVes,
                        onClick = { isVes = true },
                        label = { Text("En Bolívares (Bs.)") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amountStr.replace(",", ".").toDoubleOrNull()
                    if (value != null && value > 0.0) {
                        onAdd(name, value, isVes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
            ) {
                Text("Agregar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// =========================================================================
// TAB 3: MERCADO (BRECHA CAMBIARIA, GRÁFICO LOCAL, ALERTAS Y WIDGET)
// =========================================================================
@Composable
fun MarketTab(
    isDark: Boolean,
    isUltraUser: Boolean,
    ultraPrice: String,
    uiState: TasaVeUiState,
    snapshots: List<RateSnapshot>,
    alertSettings: AlertSettings,
    onUpdateAlerts: (AlertSettings) -> Unit,
    onTestNotification: () -> Boolean,
    onOpenPlans: () -> Unit
) {
    if (!isUltraUser) {
        UltraLockBanner(
            isDark = isDark,
            featureTitle = "Monitor de Brecha, Alertas y Widget",
            featureDescription = "Visualiza la brecha porcentual entre BCV y USDT, recibe notificaciones automáticas cuando suban o bajen las tasas y fija el Widget en tu pantalla de inicio.",
            ultraPrice = ultraPrice,
            onOpenPlans = onOpenPlans
        )
        return
    }

    val context = LocalContext.current
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF141418) else Color.White
    val borderCol = if (isDark) Color(0x1F94A3B8) else Color(0x19475569)

    val usdRate = uiState.usdBcv?.rate ?: 0.0
    val eurRate = uiState.eurBcv?.rate ?: 0.0
    val usdtRate = uiState.usdtParalelo?.rate ?: 0.0

    val spreadUsdtVsBcv = UltraMath.calculateSpreadPercent(usdtRate, usdRate)
    val diffBsUsdtBcv = usdtRate - usdRate
    val spreadEurVsUsd = UltraMath.calculateSpreadPercent(eurRate, usdRate)

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdateAlerts(alertSettings.copy(enabled = true))
            Toast.makeText(context, "Alertas de tasa activadas", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Brecha Cambiaria Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BRECHA CAMBIARIA EN VIVO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberUltra,
                    letterSpacing = 0.6.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (spreadUsdtVsBcv > 15.0) Color(0x26EF4444) else Color(0x2610B981))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", spreadUsdtVsBcv)}% USDT vs BCV",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (spreadUsdtVsBcv > 15.0) Color(0xFFF87171) else EmeraldLight
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF1B1B21) else Color(0xFFF8FAFC))
                        .padding(12.dp)
                ) {
                    Text("Diferencia USDT / BCV", fontSize = 10.sp, color = textSecondary)
                    Text(
                        text = "+Bs. ${String.format(Locale.US, "%.2f", diffBsUsdtBcv)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text("Por cada $1 USD", fontSize = 9.5.sp, color = textSecondary)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF1B1B21) else Color(0xFFF8FAFC))
                        .padding(12.dp)
                ) {
                    Text("Relación EUR / USD BCV", fontSize = 10.sp, color = textSecondary)
                    Text(
                        text = "+${String.format(Locale.US, "%.2f", spreadEurVsUsd)}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF60A5FA)
                    )
                    Text("Bs. ${String.format(Locale.US, "%.2f", eurRate - usdRate)} dif.", fontSize = 9.5.sp, color = textSecondary)
                }
            }
        }

        // 2. Local Rate History Chart (Canvas with drawWithCache)
        val chartPoints = remember(snapshots, usdRate, usdtRate) {
            if (snapshots.size >= 2) snapshots else listOf(
                RateSnapshot(0L, "Base", usdRate * 0.99, eurRate * 0.99, usdtRate * 0.99),
                RateSnapshot(1L, "Hoy", usdRate, eurRate, usdtRate)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TENDENCIA REGISTRADA (${snapshots.size.coerceAtLeast(1)} lecturas)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight,
                    letterSpacing = 0.6.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("● BCV", fontSize = 10.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
                    Text("● USDT", fontSize = 10.sp, color = AmberUltra, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF0E0E12) else Color(0xFFF1F5F9))
                    .padding(12.dp)
                    .drawWithCache {
                        val allValues = chartPoints.flatMap { listOf(it.usdBcv, it.usdt) }.filter { it > 0 }
                        val minVal = (allValues.minOrNull() ?: 0.0) * 0.995
                        val maxVal = ((allValues.maxOrNull() ?: 1.0) * 1.005).coerceAtLeast(minVal + 1.0)
                        val range = maxVal - minVal

                        val bcvPath = Path()
                        val usdtPath = Path()
                        val count = chartPoints.size.coerceAtLeast(2)

                        chartPoints.forEachIndexed { index, snap ->
                            val x = (index.toFloat() / (count - 1).toFloat()) * size.width
                            val yBcv = size.height - (((snap.usdBcv - minVal) / range).toFloat() * size.height)
                            val yUsdt = size.height - (((snap.usdt - minVal) / range).toFloat() * size.height)
                            if (index == 0) {
                                bcvPath.moveTo(x, yBcv)
                                usdtPath.moveTo(x, yUsdt)
                            } else {
                                bcvPath.lineTo(x, yBcv)
                                usdtPath.lineTo(x, yUsdt)
                            }
                        }

                        onDrawBehind {
                            drawLine(
                                color = Color(0x18FFFFFF),
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawPath(
                                path = usdtPath,
                                color = AmberUltra,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = bcvPath,
                                color = EmeraldLight,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
            )
        }

        // 3. Rate Alerts Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alertas de Cambio de Tasa",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Avisa localmente cuando el precio varíe en segundo plano.",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
                Switch(
                    checked = alertSettings.enabled,
                    onCheckedChange = { enable ->
                        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onUpdateAlerts(alertSettings.copy(enabled = enable))
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Emerald, checkedTrackColor = Color(0x4D10B981))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = alertSettings.watchUsdBcv,
                    onClick = { onUpdateAlerts(alertSettings.copy(watchUsdBcv = !alertSettings.watchUsdBcv)) },
                    label = { Text("USD BCV", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = alertSettings.watchEurBcv,
                    onClick = { onUpdateAlerts(alertSettings.copy(watchEurBcv = !alertSettings.watchEurBcv)) },
                    label = { Text("EUR BCV", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = alertSettings.watchUsdt,
                    onClick = { onUpdateAlerts(alertSettings.copy(watchUsdt = !alertSettings.watchUsdt)) },
                    label = { Text("USDT", fontSize = 11.sp) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sensibilidad:", fontSize = 11.sp, color = textSecondary)
                listOf(0.5, 1.0, 2.0).forEach { pct ->
                    FilterChip(
                        selected = alertSettings.thresholdPercent == pct,
                        onClick = { onUpdateAlerts(alertSettings.copy(thresholdPercent = pct)) },
                        label = { Text("${pct}%", fontSize = 11.sp) }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val sent = onTestNotification()
                        Toast.makeText(
                            context,
                            if (sent) "Notificación de prueba enviada" else "Activa las notificaciones primero",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderCol)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldLight)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Probar notificación ahora", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
        }

        // 4. Home Screen Widget Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Widgets, contentDescription = null, tint = EmeraldLight)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Widget de Pantalla de Inicio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = "Mira las 3 tasas y la brecha directo en tu escritorio.",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }
            Button(
                onClick = {
                    TasaVeRatesWidgetProvider.updateAllWidgets(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val manager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, TasaVeRatesWidgetProvider::class.java)
                        if (manager.isRequestPinAppWidgetSupported) {
                            manager.requestPinAppWidget(provider, null, null)
                        } else {
                            Toast.makeText(context, "Agrégalo desde Widgets en tu pantalla de inicio.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Mantén presionada tu pantalla de inicio → Widgets → Tasa VE.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fijar Widget en Inicio", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
        }
    }
}

// =========================================================================
// TAB 4: COBROS (PAGO MIXTO, VUELTO Y RESUMEN WHATSAPP)
// =========================================================================
@Composable
fun CheckoutTab(
    isDark: Boolean,
    isUltraUser: Boolean,
    ultraPrice: String,
    uiState: TasaVeUiState,
    pagoMovilInfo: PagoMovilInfo,
    onSavePagoMovil: (PagoMovilInfo) -> Unit,
    onOpenPlans: () -> Unit
) {
    if (!isUltraUser) {
        UltraLockBanner(
            isDark = isDark,
            featureTitle = "Pago Mixto y Cobros por WhatsApp",
            featureDescription = "Calcula en segundos cuánto falta en Bs. o cuánto vuelto dar cuando pagan parte en efectivo ($) y parte en Pago Móvil, y comparte el resumen en 1 toque.",
            ultraPrice = ultraPrice,
            onOpenPlans = onOpenPlans
        )
        return
    }

    val context = LocalContext.current
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF141418) else Color.White
    val borderCol = if (isDark) Color(0x1F94A3B8) else Color(0x19475569)

    var selectedRateKey by remember { mutableStateOf("USD_BCV") }
    var totalUsdStr by remember { mutableStateOf("") }
    var cashUsdStr by remember { mutableStateOf("") }
    var paidVesStr by remember { mutableStateOf("") }
    var showPagoMovilEditor by remember { mutableStateOf(false) }

    val activeRate = when (selectedRateKey) {
        "EUR_BCV" -> uiState.eurBcv?.rate ?: 1.0
        "USDT" -> uiState.usdtParalelo?.rate ?: 1.0
        else -> uiState.usdBcv?.rate ?: 1.0
    }
    val rateLabel = when (selectedRateKey) {
        "EUR_BCV" -> "Euro BCV"
        "USDT" -> "USDT"
        else -> "Dólar BCV"
    }

    val totalUsd = totalUsdStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val cashUsd = cashUsdStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val paidVes = paidVesStr.replace(",", ".").toDoubleOrNull() ?: 0.0

    val result = remember(totalUsd, cashUsd, paidVes, activeRate) {
        UltraMath.calculateMixedPayment(totalUsd, cashUsd, paidVes, activeRate)
    }

    val shareSummaryText = remember(result, rateLabel, activeRate, pagoMovilInfo) {
        buildString {
            appendLine("*Resumen de Cobro — Tasa VE*")
            appendLine("Total: $${String.format(Locale.US, "%.2f", result.totalUsd)} (Bs. ${String.format(Locale.US, "%,.2f", result.totalVes)})")
            appendLine("Tasa ($rateLabel): Bs. ${String.format(Locale.US, "%.2f", activeRate)}")
            if (result.paidForeignUsd > 0 || result.paidVes > 0) {
                appendLine("Abonado: $${String.format(Locale.US, "%.2f", result.paidForeignUsd)} + Bs. ${String.format(Locale.US, "%,.2f", result.paidVes)}")
            }
            if (result.remainingUsd > 0.005) {
                appendLine("*Restante por pagar:* $${String.format(Locale.US, "%.2f", result.remainingUsd)} / Bs. ${String.format(Locale.US, "%,.2f", result.remainingVes)}")
            } else if (result.changeUsd > 0.005) {
                appendLine("*Vuelto:* $${String.format(Locale.US, "%.2f", result.changeUsd)} / Bs. ${String.format(Locale.US, "%,.2f", result.changeVes)}")
            } else if (result.totalUsd > 0) {
                appendLine("Estado: Pagado completo ✅")
            }
            if (pagoMovilInfo.bank.isNotBlank() || pagoMovilInfo.phone.isNotBlank()) {
                appendLine()
                appendLine("*Datos Pago Móvil:*")
                if (pagoMovilInfo.bank.isNotBlank()) appendLine("Banco: ${pagoMovilInfo.bank}")
                if (pagoMovilInfo.documentId.isNotBlank()) appendLine("CI/RIF: ${pagoMovilInfo.documentId}")
                if (pagoMovilInfo.phone.isNotBlank()) appendLine("Teléfono: ${pagoMovilInfo.phone}")
            }
        }.trim()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rate Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("USD_BCV", uiState.usdBcv, "USD BCV"),
                Triple("EUR_BCV", uiState.eurBcv, "EUR BCV"),
                Triple("USDT", uiState.usdtParalelo, "USDT")
            ).forEach { (key, info, label) ->
                RateSelectorCard(
                    modifier = Modifier.weight(1f),
                    key = key,
                    rateInfo = info,
                    customLabel = label,
                    isDark = isDark,
                    isSelected = selectedRateKey == key,
                    onSelect = { selectedRateKey = key },
                    onCopy = {}
                )
            }
        }

        // Inputs Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CALCULADORA DE PAGO MIXTO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldLight,
                letterSpacing = 0.6.sp
            )

            OutlinedTextField(
                value = totalUsdStr,
                onValueChange = { totalUsdStr = it },
                label = { Text("Total de la cuenta ($)") },
                placeholder = { Text("Ej. 25.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = cashUsdStr,
                    onValueChange = { cashUsdStr = it },
                    label = { Text("Paga en $") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = paidVesStr,
                    onValueChange = { paidVesStr = it },
                    label = { Text("Paga en Bs.") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Result Card
        val isChange = result.changeUsd > 0.005
        val isPaidExact = result.totalUsd > 0 && result.remainingUsd <= 0.005 && !isChange
        val accentColor = when {
            isChange -> AmberUltra
            isPaidExact -> EmeraldLight
            else -> textPrimary
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, if (isChange) Color(0x4DF59E0B) else borderCol, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = when {
                    isChange -> "VUELTO A ENTREGAR"
                    isPaidExact -> "CUENTA SALDADA ✅"
                    else -> "FALTA POR PAGAR"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isChange) AmberUltra else EmeraldLight,
                letterSpacing = 0.6.sp
            )

            val showUsd = if (isChange) result.changeUsd else result.remainingUsd
            val showVes = if (isChange) result.changeVes else result.remainingVes

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ${String.format(Locale.US, "%,.2f", showUsd)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Text(
                    text = "Bs. ${String.format(Locale.US, "%,.2f", showVes)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight
                )
            }
        }

        // Local Pago Móvil Accordion
        val pmArrow by animateFloatAsState(
            targetValue = if (showPagoMovilEditor) 180f else 0f,
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            label = "pmArrow"
        )
        var bank by remember(pagoMovilInfo) { mutableStateOf(pagoMovilInfo.bank) }
        var docId by remember(pagoMovilInfo) { mutableStateOf(pagoMovilInfo.documentId) }
        var phone by remember(pagoMovilInfo) { mutableStateOf(pagoMovilInfo.phone) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                .animateContentSize(tween(220, easing = FastOutSlowInEasing))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPagoMovilEditor = !showPagoMovilEditor },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mis datos de Pago Móvil (locales)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = if (pagoMovilInfo.phone.isNotBlank()) "${pagoMovilInfo.bank} • ${pagoMovilInfo.phone}" else "Opcional: inclúyelos al enviar por WhatsApp",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.graphicsLayer { rotationZ = pmArrow }
                )
            }

            if (showPagoMovilEditor) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("Banco") },
                    placeholder = { Text("Ej. Banesco / BDV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = docId,
                        onValueChange = { docId = it },
                        label = { Text("CI / RIF") },
                        placeholder = { Text("V-12345678") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono") },
                        placeholder = { Text("0414-0000000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSavePagoMovil(PagoMovilInfo(bank, docId, phone))
                        showPagoMovilEditor = false
                        Toast.makeText(context, "Datos guardados localmente", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
                ) {
                    Text("Guardar datos", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Copy & WhatsApp Share Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Resumen Tasa VE", shareSummaryText))
                    Toast.makeText(context, "Resumen copiado", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, borderCol)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = textPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copiar", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textPrimary)
            }

            Button(
                onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareSummaryText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Enviar cobro por WhatsApp"))
                },
                modifier = Modifier
                    .weight(1.3f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enviar por WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
        }
    }
}

// =========================================================================
// COMPARATIVA DE PLANES: GRATIS vs PRO ($2.99 único) vs ULTRA ($1.49/mes)
// =========================================================================
@Composable
fun PlansComparisonDialog(
    isDark: Boolean,
    isProUser: Boolean,
    isUltraUser: Boolean,
    proPrice: String,
    ultraMonthlyPrice: String,
    ultraYearlyPrice: String,
    onPurchasePro: () -> Unit,
    onPurchaseUltraMonthly: () -> Unit,
    onPurchaseUltraYearly: () -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF0E0E11) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF17171D) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Elige tu plan Tasa VE",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Ultra incluye todo lo de Pro sin pagar doble",
                            fontSize = 11.sp,
                            color = EmeraldLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                // Comparison Table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, Color(0x1F94A3B8), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Función", modifier = Modifier.weight(1.6f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        Text("Gratis", modifier = Modifier.weight(0.7f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textSecondary, textAlign = TextAlign.Center)
                        Text("Pro", modifier = Modifier.weight(0.7f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = EmeraldLight, textAlign = TextAlign.Center)
                        Text("Ultra", modifier = Modifier.weight(0.7f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AmberUltra, textAlign = TextAlign.Center)
                    }
                    HorizontalDivider(color = Color(0x1A94A3B8))

                    val rows = listOf(
                        listOf("Calculadora BCV/USDT", "✓", "✓", "✓"),
                        listOf("Sin publicidad", "—", "✓", "✓"),
                        listOf("Escáner cámara", "3/día", "∞", "∞"),
                        listOf("Carrito + IVA/IGTF", "—", "—", "✓"),
                        listOf("Brecha + Alertas", "—", "—", "✓"),
                        listOf("Widget de inicio", "—", "—", "✓"),
                        listOf("Pago Mixto/WhatsApp", "—", "—", "✓")
                    )
                    rows.forEach { cols ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cols[0], modifier = Modifier.weight(1.6f), fontSize = 11.sp, color = textPrimary)
                            Text(cols[1], modifier = Modifier.weight(0.7f), fontSize = 11.sp, color = textSecondary, textAlign = TextAlign.Center)
                            Text(cols[2], modifier = Modifier.weight(0.7f), fontSize = 11.sp, color = EmeraldLight, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text(cols[3], modifier = Modifier.weight(0.7f), fontSize = 11.sp, color = AmberUltra, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }

                // Ultra CTA Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x2AF59E0B), Color(0x12F59E0B))
                            )
                        )
                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isUltraUser) "Tasa VE Ultra Activo ⭐" else "Tasa VE Ultra (Todo incluido)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Incluye cero anuncios + escáner ilimitado (no pagas Pro aparte) + Carrito, Alertas, Brecha, Widget y Pago Mixto. Cancela cuando quieras.",
                        fontSize = 11.sp,
                        color = textSecondary,
                        lineHeight = 15.sp
                    )
                    if (!isUltraUser) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onPurchaseUltraMonthly,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberUltra, contentColor = Color.Black)
                            ) {
                                Text(ultraMonthlyPrice, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                            OutlinedButton(
                                onClick = onPurchaseUltraYearly,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AmberUltra)
                            ) {
                                Text(ultraYearlyPrice, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = AmberUltra)
                            }
                        }
                    }
                }

                // Pro One-Time CTA Card
                if (!isProUser && !isUltraUser) {
                    OutlinedButton(
                        onClick = onPurchasePro,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Emerald)
                    ) {
                        Text(
                            text = "Solo quiero Pro ($proPrice pago único)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = EmeraldLight
                        )
                    }
                }

                TextButton(
                    onClick = onRestorePurchases,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Restaurar compras y suscripciones", fontSize = 11.sp, color = textSecondary)
                }
            }
        }
    }
}
