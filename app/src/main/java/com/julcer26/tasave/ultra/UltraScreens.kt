package com.julcer26.tasave.ultra

import android.Manifest
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon as AndroidIcon
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.julcer26.tasave.R
import com.julcer26.tasave.quick.TasaVeQuickTileService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.julcer26.tasave.ads.TopBannerAd
import com.julcer26.tasave.data.RateInfo
import com.julcer26.tasave.share.ShareCardGenerator
import com.julcer26.tasave.ui.RateSelectorCard
import com.julcer26.tasave.ui.TasaVeUiState
import com.julcer26.tasave.widget.TasaVeRatesWidgetProvider
import com.julcer26.tasave.widget.TasaVeUltraWidgetProvider
import java.util.Locale
import kotlin.math.roundToInt

private val Emerald = Color(0xFF10B981)
private val EmeraldLight = Color(0xFF34D399)
private val BlueEuro = Color(0xFF60A5FA)
private val AmberUltra = Color(0xFFF59E0B)
private val PurpleZelle = Color(0xFFA855F7)

// =========================================================================
// TAB 2: CARRO DE COMPRAS CON ESCÁNER, EDICIÓN + IVA / IGTF
// =========================================================================
@Composable
fun CartTab(
    isDark: Boolean,
    isUltraUser: Boolean,
    isAdFreePassActive: Boolean = false,
    ultraPrice: String,
    uiState: TasaVeUiState,
    cartItems: List<CartItem>,
    applyIva: Boolean,
    applyIgtf: Boolean,
    paymentProfiles: List<PaymentMethodProfile> = emptyList(),
    selectedProfileId: Long? = null,
    customBrandName: String = "",
    customBrandLogoRevision: Long = 0L,
    onAddManualItem: (name: String, amount: Double, isVes: Boolean, quantity: Int) -> Boolean,
    onEditItem: (id: Long, name: String, amount: Double, isVes: Boolean, quantity: Int) -> Unit,
    onScanToCart: () -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onToggleIva: () -> Boolean,
    onToggleIgtf: () -> Boolean,
    onOpenPlans: () -> Unit
) {
    val context = LocalContext.current
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF141418) else Color.White
    val borderCol = if (isDark) Color(0x1F94A3B8) else Color(0x19475569)

    val usdRate = uiState.usdBcv?.rate ?: 0.0
    val eurRate = uiState.eurBcv?.rate ?: 0.0
    val usdtRate = uiState.usdtParalelo?.rate ?: 0.0

    val effectiveApplyIva = isUltraUser && applyIva
    val effectiveApplyIgtf = isUltraUser && applyIgtf

    val summary = remember(cartItems, effectiveApplyIva, effectiveApplyIgtf, usdRate, eurRate, usdtRate) {
        UltraMath.calculateCartSummary(
            items = cartItems,
            applyIva = effectiveApplyIva,
            applyIgtf = effectiveApplyIgtf,
            usdRate = usdRate,
            eurRate = eurRate,
            usdtRate = usdtRate
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<CartItem?>(null) }
    val cartFullForFree = !isUltraUser && !UltraMath.canAddCartItem(cartItems.size, isUltraUser)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopBannerAd(isDark = isDark, isUltraUser = isUltraUser, isAdFreePassActive = isAdFreePassActive)

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TOTAL DEL CARRITO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight,
                            letterSpacing = 0.6.sp
                        )
                        if (!isUltraUser) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (cartFullForFree) Color(0x26F59E0B) else Color(0x1F10B981))
                                    .clickable { onOpenPlans() }
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${cartItems.size}/${UltraMath.FREE_CART_MAX_ITEMS} gratis",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cartFullForFree) AmberUltra else EmeraldLight
                                )
                            }
                        }
                    }
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

            // IVA & IGTF Quick Toggles (Ultra Exclusive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = effectiveApplyIva,
                    onClick = {
                        if (!isUltraUser || !onToggleIva()) {
                            Toast.makeText(
                                context,
                                "IVA automático es exclusivo de Tasa VE Ultra ($ultraPrice)",
                                Toast.LENGTH_SHORT
                            ).show()
                            onOpenPlans()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    label = {
                        Text(
                            text = if (isUltraUser) "IVA 16%" else "IVA 16% • ULTRA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when {
                                !isUltraUser -> Icons.Default.Lock
                                effectiveApplyIva -> Icons.Default.CheckCircle
                                else -> Icons.Default.AddCircleOutline
                            },
                            contentDescription = null,
                            tint = if (!isUltraUser) AmberUltra else LocalContentColor.current,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x2610B981),
                        selectedLabelColor = EmeraldLight,
                        selectedLeadingIconColor = EmeraldLight
                    )
                )
                FilterChip(
                    selected = effectiveApplyIgtf,
                    onClick = {
                        if (!isUltraUser || !onToggleIgtf()) {
                            Toast.makeText(
                                context,
                                "IGTF automático es exclusivo de Tasa VE Ultra ($ultraPrice)",
                                Toast.LENGTH_SHORT
                            ).show()
                            onOpenPlans()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    label = {
                        Text(
                            text = if (isUltraUser) "IGTF 3%" else "IGTF 3% • ULTRA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when {
                                !isUltraUser -> Icons.Default.Lock
                                effectiveApplyIgtf -> Icons.Default.CheckCircle
                                else -> Icons.Default.AddCircleOutline
                            },
                            contentDescription = null,
                            tint = if (!isUltraUser) AmberUltra else LocalContentColor.current,
                            modifier = Modifier.size(15.dp)
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

            if (effectiveApplyIva || effectiveApplyIgtf) {
                Text(
                    text = "Subtotal: $${String.format(Locale.US, "%.2f", summary.subtotalUsd)}" +
                        (if (effectiveApplyIva) " + IVA $${String.format(Locale.US, "%.2f", summary.ivaUsd)}" else "") +
                        (if (effectiveApplyIgtf) " + IGTF $${String.format(Locale.US, "%.2f", summary.igtfUsd)}" else ""),
                    fontSize = 10.5.sp,
                    color = textSecondary
                )
            }

            if (cartItems.isNotEmpty()) {
                val activeProfile = remember(paymentProfiles, selectedProfileId) {
                    paymentProfiles.firstOrNull { it.id == selectedProfileId }
                        ?: paymentProfiles.firstOrNull()
                }
                val customLogoFile = remember(customBrandLogoRevision, isUltraUser) {
                    if (isUltraUser) ShareCardGenerator.getCustomBrandLogoFile(context) else null
                }
                val quoteText = remember(
                    cartItems,
                    summary,
                    usdRate,
                    effectiveApplyIva,
                    effectiveApplyIgtf,
                    activeProfile,
                    isUltraUser,
                    customBrandName
                ) {
                    ShareCardGenerator.formatCartQuoteText(
                        items = cartItems,
                        summary = summary,
                        usdRate = usdRate,
                        applyIva = effectiveApplyIva,
                        applyIgtf = effectiveApplyIgtf,
                        profile = activeProfile,
                        customBrandName = if (isUltraUser) customBrandName else null
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ShareCardGenerator.shareCartQuoteCard(
                                context = context,
                                isDark = isDark,
                                items = cartItems,
                                summary = summary,
                                usdRate = usdRate,
                                applyIva = effectiveApplyIva,
                                applyIgtf = effectiveApplyIgtf,
                                profile = activeProfile,
                                captionText = quoteText,
                                customBrandName = if (isUltraUser) customBrandName else null,
                                customBrandLogoFile = customLogoFile
                            )
                        },
                        modifier = Modifier
                            .weight(1.25f)
                            .heightIn(min = 40.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x2610B981),
                            contentColor = EmeraldLight
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Tarjeta (IMG)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            sharePlainText(context, "Compartir presupuesto", quoteText)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = textPrimary
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "WhatsApp",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Presupuesto Tasa VE", quoteText))
                            Toast.makeText(context, "Presupuesto copiado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.heightIn(min = 40.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar presupuesto",
                            modifier = Modifier.size(15.dp),
                            tint = textPrimary
                        )
                    }
                }
            }
        }

        // Action Buttons: Scan or Manual Add
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (cartFullForFree) {
                        Toast.makeText(
                            context,
                            "Límite de 5 productos gratis. Pásate a Ultra para artículos ilimitados.",
                            Toast.LENGTH_SHORT
                        ).show()
                        onOpenPlans()
                    } else {
                        onScanToCart()
                    }
                },
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
                onClick = {
                    if (cartFullForFree) {
                        Toast.makeText(
                            context,
                            "Límite de 5 productos gratis. Pásate a Ultra para artículos ilimitados.",
                            Toast.LENGTH_SHORT
                        ).show()
                        onOpenPlans()
                    } else {
                        showAddDialog = true
                    }
                },
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
                        text = "Apunta la cámara a las etiquetas del supermercado o agrega productos manualmente. Toca cualquier producto para editar su nombre o precio.",
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                            .clickable { editingItem = item }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar producto",
                                    tint = EmeraldLight.copy(alpha = 0.85f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", item.unitPriceUsd)} c/u • Total Bs. ${String.format(Locale.US, "%,.2f", lineVes)}",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { onUpdateQuantity(item.id, -1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Restar",
                                    tint = textSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Text(
                                text = "${item.quantity}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                modifier = Modifier.widthIn(min = 20.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = { onUpdateQuantity(item.id, +1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Sumar",
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRemoveItem(item.id) },
                                modifier = Modifier.size(30.dp)
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

    if (showAddDialog) {
        CartItemEditorDialog(
            isDark = isDark,
            initialItem = null,
            usdRate = usdRate,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, isVes, qty ->
                val added = onAddManualItem(name, amount, isVes, qty)
                showAddDialog = false
                if (!added) {
                    Toast.makeText(
                        context,
                        "Límite de 5 productos gratis. Pásate a Ultra para artículos ilimitados.",
                        Toast.LENGTH_SHORT
                    ).show()
                    onOpenPlans()
                }
            }
        )
    }

    editingItem?.let { item ->
        CartItemEditorDialog(
            isDark = isDark,
            initialItem = item,
            usdRate = usdRate,
            onDismiss = { editingItem = null },
            onConfirm = { name, amount, isVes, qty ->
                onEditItem(item.id, name, amount, isVes, qty)
                editingItem = null
            }
        )
    }
}

@Composable
private fun CartItemEditorDialog(
    isDark: Boolean,
    initialItem: CartItem?,
    usdRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, isVes: Boolean, quantity: Int) -> Unit
) {
    val isEdit = initialItem != null
    var name by remember(initialItem) { mutableStateOf(initialItem?.name ?: "") }
    var isVes by remember(initialItem) { mutableStateOf(false) }
    var amountStr by remember(initialItem) {
        mutableStateOf(
            if (initialItem != null) String.format(Locale.US, "%.2f", initialItem.unitPriceUsd) else ""
        )
    }
    var qty by remember(initialItem) { mutableStateOf(initialItem?.quantity ?: 1) }

    val dialogBg = if (isDark) Color(0xFF141418) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val borderCol = if (isDark) Color(0x2694A3B8) else Color(0x1F475569)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .border(1.dp, borderCol, RoundedCornerShape(24.dp)),
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
                    Text(
                        text = if (isEdit) "Editar producto" else "Agregar producto",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del producto") },
                    placeholder = { Text("Ej. Harina PAN") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (isVes) "Precio unitario (Bs.)" else "Precio unitario ($)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isVes,
                        onClick = {
                            if (isVes && usdRate > 0.0) {
                                val current = amountStr.replace(",", ".").toDoubleOrNull()
                                if (current != null && current > 0) {
                                    amountStr = String.format(Locale.US, "%.2f", current / usdRate)
                                }
                            }
                            isVes = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x2610B981),
                            selectedLabelColor = EmeraldLight
                        ),
                        label = {
                            Text(
                                text = "En Dólares ($)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                    FilterChip(
                        selected = isVes,
                        onClick = {
                            if (!isVes && usdRate > 0.0) {
                                val current = amountStr.replace(",", ".").toDoubleOrNull()
                                if (current != null && current > 0) {
                                    amountStr = String.format(Locale.US, "%.2f", current * usdRate)
                                }
                            }
                            isVes = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x2610B981),
                            selectedLabelColor = EmeraldLight
                        ),
                        label = {
                            Text(
                                text = "En Bolívares (Bs.)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF1B1B21) else Color(0xFFF8FAFC))
                        .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cantidad",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0x1F94A3B8) else Color(0x14475569))
                                .clickable { if (qty > 1) qty -= 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos", tint = textPrimary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "$qty",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            modifier = Modifier.widthIn(min = 20.dp),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x2610B981))
                                .clickable { qty += 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Más", tint = EmeraldLight, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.SemiBold, color = textPrimary)
                    }
                    Button(
                        onClick = {
                            val value = amountStr.replace(",", ".").toDoubleOrNull()
                            if (value != null && value > 0.0) {
                                onConfirm(name, value, isVes, qty)
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
                    ) {
                        Text(if (isEdit) "Guardar cambios" else "Agregar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// =========================================================================
// TAB 3: ÚTILES (HERRAMIENTAS DEL DÍA A DÍA, ALERTAS Y WIDGETS)
// =========================================================================
private enum class UtilTool(val title: String, val subtitle: String, val analyticsKey: String) {
    COMPARE("¿Con qué pago?", "USDT vs Efectivo/$", "compare_payment"),
    SPLIT("Dividir cuenta", "Entre amigos o grupo", "split_bill")
}

private fun formatDec2(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun formatCopAmount(value: Double): String =
    if (value % 1.0 == 0.0) String.format(Locale.US, "%,.0f", value)
    else String.format(Locale.US, "%,.2f", value)

private fun formatSplitCurrencyValue(value: Double, currency: SplitCurrency): String =
    when (currency) {
        SplitCurrency.USD -> "$${formatDec2(value)}"
        SplitCurrency.EUR -> "€${formatDec2(value)}"
        SplitCurrency.VES -> "Bs. ${formatDec2(value)}"
        SplitCurrency.USDT -> "${formatDec2(value)} USDT"
        SplitCurrency.COP -> "${formatCopAmount(value)} COP"
    }

private fun sharePlainText(context: Context, title: String, body: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(sendIntent, title))
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun MarketTab(
    isDark: Boolean,
    isUltraUser: Boolean,
    isAdFreePassActive: Boolean = false,
    ultraPrice: String,
    uiState: TasaVeUiState,
    snapshots: List<RateSnapshot>,
    isHistoryLoading: Boolean,
    alertSettings: AlertSettings,
    persistentNotifEnabled: Boolean = false,
    onTrackUtilTool: (String) -> Unit = {},
    onRefreshHistory: () -> Unit,
    onUpdateAlerts: (AlertSettings) -> Unit,
    onTogglePersistentNotif: (Boolean) -> Boolean = { false },
    onTestNotification: () -> Boolean,
    onOpenPlans: () -> Unit
) {
    val context = LocalContext.current
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF141418) else Color.White
    val borderCol = if (isDark) Color(0x1F94A3B8) else Color(0x19475569)
    val surfaceMuted = if (isDark) Color(0xFF1B1C22) else Color(0xFFF1F5F9)

    val usdRate = uiState.usdBcv?.rate ?: 0.0
    val eurRate = uiState.eurBcv?.rate?.takeIf { it > 0.0 } ?: usdRate
    val usdtRate = uiState.usdtParalelo?.rate ?: 0.0
    val copPerUsd = uiState.copPerUsd.takeIf { it > 0.0 } ?: 4320.0
    val spreadUsdtVsBcv = UltraMath.calculateSpreadPercent(usdtRate, usdRate)

    var selectedTool by remember { mutableStateOf<UtilTool?>(null) }

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

    val persistentNotifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (onTogglePersistentNotif(true)) {
                Toast.makeText(context, "Notificación fija de tasas activada", Toast.LENGTH_SHORT).show()
            }
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
        TopBannerAd(isDark = isDark, isUltraUser = isUltraUser, isAdFreePassActive = isAdFreePassActive)

        if (selectedTool != null) {
            // Cabecera ← Volver a Útiles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                    .clickable { selectedTool = null }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver a Útiles",
                    tint = EmeraldLight,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedTool!!.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Volver a todas las herramientas",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                }
            }

            when (selectedTool!!) {
                UtilTool.COMPARE -> {
                    var priceText by remember { mutableStateOf("100") }
                    var discountText by remember { mutableStateOf("0") }
                    var isFixedUsdDiscount by remember { mutableStateOf(false) }

                    val price = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val discountVal = discountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val res = UltraMath.calculatePaymentComparison(
                        priceUsd = price,
                        bcvRate = usdRate,
                        usdtRate = usdtRate,
                        cashDiscountPercent = if (isFixedUsdDiscount) 0.0 else discountVal,
                        cashDiscountFixedUsd = if (isFixedUsdDiscount) discountVal else 0.0
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Compara en segundos si te rinde más cambiar USDT a bolívares (pagando a BCV) o pagar directo en efectivo \$ / Zelle.",
                            fontSize = 11.5.sp,
                            color = textSecondary
                        )

                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { if (it.length <= 10) priceText = it },
                            label = { Text("Monto a pagar ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "¿Descuento en efectivo / Zelle?",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(false to "% Porc.", true to "\$ Monto").forEach { (isFixed, lbl) ->
                                        val active = isFixedUsdDiscount == isFixed
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (active) Emerald.copy(alpha = 0.2f) else surfaceMuted)
                                                .border(
                                                    1.dp,
                                                    if (active) Emerald else borderCol,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { isFixedUsdDiscount = isFixed }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = lbl,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) EmeraldLight else textSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            val presets = if (isFixedUsdDiscount) {
                                listOf("0" to "Sin desc.", "5" to "-\$5", "10" to "-\$10", "20" to "-\$20")
                            } else {
                                listOf("0" to "Sin desc.", "5" to "-5%", "10" to "-10%", "15" to "-15%")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.forEach { (valStr, labelStr) ->
                                    val active = discountText.trim() == valStr
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (active) Emerald.copy(alpha = 0.2f) else surfaceMuted)
                                            .border(
                                                1.dp,
                                                if (active) Emerald else borderCol,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { discountText = valStr }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = labelStr,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) EmeraldLight else textPrimary
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { if (it.length <= 10) discountText = it },
                                label = {
                                    Text(
                                        if (isFixedUsdDiscount) "Descuento libre en dólares ($)"
                                        else "Descuento libre en porcentaje (%)"
                                    )
                                },
                                placeholder = {
                                    Text(if (isFixedUsdDiscount) "Ej: 8.50" else "Ej: 7.5")
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (res.recommendUsdt) Color(0x1810B981) else surfaceMuted)
                                    .border(1.5.dp, if (res.recommendUsdt) Emerald else borderCol, RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "1. Cambiando USDT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.recommendUsdt) EmeraldLight else textPrimary
                                )
                                Text(
                                    text = "${formatDec2(res.usdtNeededToCoverBs)} USDT",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Para cubrir Bs. ${formatDec2(res.bsAmount)}",
                                    fontSize = 10.5.sp,
                                    color = textSecondary
                                )
                            }

                            val discountDesc = when {
                                discountVal <= 0.0 -> "Pago directo en divisas"
                                isFixedUsdDiscount -> "Con -$${formatDec2(res.cashDiscountUsd)} de desc."
                                else -> "Con -${formatDec2(res.cashDiscountPercent).removeSuffix(".00")}% de desc."
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (!res.recommendUsdt) Color(0x1860A5FA) else surfaceMuted)
                                    .border(1.5.dp, if (!res.recommendUsdt) BlueEuro else borderCol, RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "2. Efectivo \$ / Zelle",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!res.recommendUsdt) BlueEuro else textPrimary
                                )
                                Text(
                                    text = "$${formatDec2(res.cashOrZelleEffectiveUsd)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textPrimary
                                )
                                Text(
                                    text = discountDesc,
                                    fontSize = 10.5.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (res.recommendUsdt) Color(0x2210B981) else Color(0x2260A5FA))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (res.recommendUsdt) EmeraldLight else BlueEuro,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (res.recommendUsdt) {
                                    "Conviene cambiar USDT: ahorras $${formatDec2(res.usdtSavingsVsCashUsd)} frente al pago directo."
                                } else {
                                    "Conviene pagar en Efectivo \$ / Zelle: ahorras $${formatDec2(res.cashSavingsVsUsdtUsd)} frente a cambiar USDT."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                        }

                        Button(
                            onClick = {
                                val rec = if (res.recommendUsdt) {
                                    "Conviene cambiar ${formatDec2(res.usdtNeededToCoverBs)} USDT (ahorro $${formatDec2(res.usdtSavingsVsCashUsd)})"
                                } else {
                                    "Conviene pagar $${formatDec2(res.cashOrZelleEffectiveUsd)} directo (ahorro $${formatDec2(res.cashSavingsVsUsdtUsd)})"
                                }
                                val discNote = if (res.cashDiscountUsd > 0.0) {
                                    " (desc. -$${formatDec2(res.cashDiscountUsd)} / -${formatDec2(res.cashDiscountPercent).removeSuffix(".00")}%)"
                                } else ""
                                sharePlainText(
                                    context,
                                    "Compartir comparación",
                                    "¿Con qué pago $${formatDec2(res.priceUsd)}?\n• Cambiando USDT: ${formatDec2(res.usdtNeededToCoverBs)} USDT (Bs. ${formatDec2(res.bsAmount)})\n• Efectivo/Zelle: $${formatDec2(res.cashOrZelleEffectiveUsd)}$discNote\n👉 $rec\nCalculado con Tasa VE"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartir resultado", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                UtilTool.SPLIT -> {
                    var selectedCurrency by remember { mutableStateOf(SplitCurrency.USD) }
                    var totalBillText by remember { mutableStateOf("60") }
                    var people by remember { mutableIntStateOf(4) }

                    val billTotal = totalBillText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val splitRes = UltraMath.calculateSplitBillMultiCurrency(
                        totalAmount = billTotal,
                        currency = selectedCurrency,
                        peopleCount = people,
                        usdRate = usdRate,
                        eurRate = eurRate,
                        usdtRate = usdtRate,
                        copPerUsd = copPerUsd
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Moneda de la cuenta",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SplitCurrency.entries.forEach { cur ->
                                    val active = selectedCurrency == cur
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (active) Emerald.copy(alpha = 0.2f) else surfaceMuted)
                                            .border(
                                                1.dp,
                                                if (active) Emerald else borderCol,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedCurrency = cur }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cur.chipLabel,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) EmeraldLight else textPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = totalBillText,
                            onValueChange = { if (it.length <= 12) totalBillText = it },
                            label = { Text(selectedCurrency.inputLabel) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Personas ($people)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedIconButton(onClick = { if (people > 2) people-- }, modifier = Modifier.size(34.dp)) {
                                    Text("-", fontWeight = FontWeight.Bold, color = textPrimary)
                                }
                                OutlinedIconButton(onClick = { if (people < 30) people++ }, modifier = Modifier.size(34.dp)) {
                                    Text("+", fontWeight = FontWeight.Bold, color = textPrimary)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 4, 6, 8).forEach { cnt ->
                                val active = people == cnt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) Emerald.copy(alpha = 0.2f) else surfaceMuted)
                                        .border(1.dp, if (active) Emerald else borderCol, RoundedCornerShape(10.dp))
                                        .clickable { people = cnt }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$cnt", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (active) EmeraldLight else textPrimary)
                                }
                            }
                        }

                        val primaryPerPersonText = when (selectedCurrency) {
                            SplitCurrency.VES -> "Bs. ${formatDec2(splitRes.perPersonBs)}  •  $${formatDec2(splitRes.perPersonUsd)} BCV"
                            else -> "${formatSplitCurrencyValue(splitRes.perPersonInCurrency, selectedCurrency)}  •  Bs. ${formatDec2(splitRes.perPersonBs)}"
                        }
                        val equivParts = buildList {
                            if (selectedCurrency != SplitCurrency.USD && selectedCurrency != SplitCurrency.VES) {
                                add("$${formatDec2(splitRes.perPersonUsd)} BCV")
                            }
                            if (selectedCurrency != SplitCurrency.USDT) {
                                add("${formatDec2(splitRes.perPersonUsdt)} USDT")
                            }
                            if (selectedCurrency != SplitCurrency.EUR) {
                                add("€${formatDec2(splitRes.perPersonEur)}")
                            }
                            if (selectedCurrency != SplitCurrency.COP) {
                                add("${formatCopAmount(splitRes.perPersonCop)} COP")
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1810B981))
                                .border(1.dp, Color(0x4D10B981), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "CADA PERSONA PAGA (${splitRes.peopleCount} PERS. • ${selectedCurrency.code})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                            Text(
                                text = primaryPerPersonText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                            Text(
                                text = "Equiv. c/u: ${equivParts.joinToString("  •  ")}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldLight
                            )
                            Text(
                                text = if (selectedCurrency == SplitCurrency.VES) {
                                    "Cuenta total: Bs. ${formatDec2(splitRes.totalBs)} ($${formatDec2(splitRes.totalUsd)} BCV)"
                                } else {
                                    "Cuenta total: ${formatSplitCurrencyValue(splitRes.totalInCurrency, selectedCurrency)} (Bs. ${formatDec2(splitRes.totalBs)})"
                                },
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }

                        Button(
                            onClick = {
                                val totalFormatted = formatSplitCurrencyValue(splitRes.totalInCurrency, selectedCurrency)
                                val perPersonFormatted = formatSplitCurrencyValue(splitRes.perPersonInCurrency, selectedCurrency)
                                val shareBody = buildString {
                                    append("Cuenta dividida entre ${splitRes.peopleCount} personas (${selectedCurrency.code}):\n")
                                    if (selectedCurrency == SplitCurrency.VES) {
                                        append("• Total: Bs. ${formatDec2(splitRes.totalBs)} ($${formatDec2(splitRes.totalUsd)} BCV)\n")
                                        append("👉 Cada uno paga: Bs. ${formatDec2(splitRes.perPersonBs)} ($${formatDec2(splitRes.perPersonUsd)} BCV / ${formatDec2(splitRes.perPersonUsdt)} USDT / ${formatCopAmount(splitRes.perPersonCop)} COP)\n")
                                    } else {
                                        append("• Total: $totalFormatted / Bs. ${formatDec2(splitRes.totalBs)}\n")
                                        append("👉 Cada uno paga: $perPersonFormatted o Bs. ${formatDec2(splitRes.perPersonBs)} (${equivParts.joinToString(" / ")})\n")
                                    }
                                    append("Calculado con Tasa VE")
                                }
                                sharePlainText(
                                    context,
                                    "Compartir cuenta dividida",
                                    shareBody
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mandar al grupo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // Resumen compacto de referencia BCV vs USDT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BCV Bs. ${formatDec2(usdRate)}  •  USDT Bs. ${formatDec2(usdtRate)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Text(
                    text = "Dif. ${if (spreadUsdtVsBcv >= 0) "+" else ""}${formatDec2(spreadUsdtVsBcv)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberUltra
                )
            }

            // Hub de 4 herramientas del día a día
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "HERRAMIENTAS DEL DÍA A DÍA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight,
                    letterSpacing = 0.6.sp
                )
                UtilTool.entries.chunked(2).forEach { rowTools ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTools.forEach { tool ->
                            val icon = when (tool) {
                                UtilTool.COMPARE -> Icons.AutoMirrored.Filled.CompareArrows
                                UtilTool.SPLIT -> Icons.Default.Group
                            }
                            val accent = when (tool) {
                                UtilTool.COMPARE -> EmeraldLight
                                UtilTool.SPLIT -> PurpleZelle
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cardBg)
                                    .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                                    .clickable {
                                        selectedTool = tool
                                        onTrackUtilTool(tool.analyticsKey)
                                    }
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tool.title,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = tool.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tool.subtitle,
                                    fontSize = 10.5.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Abrir →",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }

        // 3. Rate Alerts Card (Exclusivo Ultra)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(
                    1.dp,
                    if (isUltraUser) borderCol else Color(0x4DF59E0B),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Alertas de Cambio de Tasa",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        if (!isUltraUser) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x26F59E0B))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "EXCLUSIVO ULTRA",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AmberUltra
                                )
                            }
                        }
                    }
                    Text(
                        text = "Avisa localmente cuando el precio varíe en segundo plano.",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
                Switch(
                    checked = isUltraUser && alertSettings.enabled,
                    onCheckedChange = { enable ->
                        if (!isUltraUser) {
                            Toast.makeText(
                                context,
                                "Las alertas automáticas son exclusivas de Tasa VE Ultra.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onOpenPlans()
                        } else if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
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
                    onClick = {
                        if (!isUltraUser) onOpenPlans()
                        else onUpdateAlerts(alertSettings.copy(watchUsdBcv = !alertSettings.watchUsdBcv))
                    },
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("USD BCV", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = alertSettings.watchEurBcv,
                    onClick = {
                        if (!isUltraUser) onOpenPlans()
                        else onUpdateAlerts(alertSettings.copy(watchEurBcv = !alertSettings.watchEurBcv))
                    },
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("EUR BCV", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = alertSettings.watchUsdt,
                    onClick = {
                        if (!isUltraUser) onOpenPlans()
                        else onUpdateAlerts(alertSettings.copy(watchUsdt = !alertSettings.watchUsdt))
                    },
                    shape = RoundedCornerShape(14.dp),
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
                        onClick = {
                            if (!isUltraUser) onOpenPlans()
                            else onUpdateAlerts(alertSettings.copy(thresholdPercent = pct))
                        },
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("${pct}%", fontSize = 11.sp) }
                    )
                }
            }

            if (isUltraUser) {
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
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldLight)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probar notificación ahora", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
            } else {
                Button(
                    onClick = onOpenPlans,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberUltra, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Desbloquear con Ultra ($ultraPrice)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Accesos Rápidos (Notificación Fija + Menú WhatsApp + Barra Rápida)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = EmeraldLight)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Accesos Rápidos (Sin abrir la app)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Calcula directo desde WhatsApp, notificaciones o la barra superior.",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }

            // 1) Notificación fija de tasas (Ultra opt-in)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceMuted)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Notificación fija de tasas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        if (!isUltraUser) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x26F59E0B))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ULTRA",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AmberUltra
                                )
                            }
                        }
                    }
                    Text(
                        text = "Barra silenciosa con USD, EUR, USDT, copiar tasas, calc. y Pago Móvil.",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
                Switch(
                    checked = isUltraUser && persistentNotifEnabled,
                    onCheckedChange = { enable ->
                        if (!isUltraUser) {
                            Toast.makeText(
                                context,
                                "La barra fija de tasas es exclusiva de Tasa VE Ultra.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onOpenPlans()
                        } else if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            persistentNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val ok = onTogglePersistentNotif(enable)
                            if (ok) {
                                Toast.makeText(
                                    context,
                                    if (enable) "Notificación fija activada" else "Notificación fija desactivada",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Emerald,
                        checkedTrackColor = Color(0x4D10B981)
                    )
                )
            }

            // 2) Conversor flotante en WhatsApp (burbujas copiadas o texto seleccionado)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceMuted)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Conversor flotante en WhatsApp",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x2610B981))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isUltraUser) "MULTI-TASA + PM" else "BCV GRATIS",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldLight
                        )
                    }
                }
                Text(
                    text = "• Si te enviaron una burbuja (ej. \"\$35\"): cópiala en WhatsApp y toca \"Calc. rápida\" en la notificación fija o el botón \"Tasa VE\" en tu Barra Rápida.\n• Si estás escribiendo en \"Mensaje\": escribe 35 → selecciónalo → toca ⋮ → \"Tasa VE\" y reemplázalo en el chat en 1 toque.",
                    fontSize = 11.sp,
                    color = textSecondary,
                    lineHeight = 15.sp
                )
                OutlinedButton(
                    onClick = {
                        onTrackUtilTool("test_floating_calc")
                        context.startActivity(
                            com.julcer26.tasave.quick.QuickCalcActivity.createIntent(
                                context,
                                com.julcer26.tasave.quick.QuickCalcActivity.MODE_CALC
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmeraldLight
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Abrir conversor flotante ahora",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }

            // 3) Botón en Barra Rápida de Android (Quick Settings Tile)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceMuted)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Botón en Barra Rápida",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = if (isUltraUser) {
                                "Abre el conversor flotante directo sobre cualquier app."
                            } else {
                                "Abre Tasa VE desde el panel superior (conversor flotante en Ultra)."
                            },
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        onTrackUtilTool("add_quick_tile")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                            statusBarManager?.requestAddTileService(
                                ComponentName(context, TasaVeQuickTileService::class.java),
                                "Tasa VE",
                                AndroidIcon.createWithResource(context, R.drawable.ic_launcher_monochrome),
                                ContextCompat.getMainExecutor(context)
                            ) { resultCode ->
                                val msg = when (resultCode) {
                                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                                        "Botón añadido a tu Barra Rápida"
                                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                                        "Tasa VE ya está en tu Barra Rápida"
                                    else ->
                                        "Puedes añadir Tasa VE editando tu Barra Rápida"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Desliza la barra superior → Editar (lápiz) → arrastra \"Tasa VE\"",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmeraldLight
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Añadir a Barra Rápida",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }
        }

        // Home Screen Widgets Card (Tasas Gratis + Calculadora y Cobro Express Ultra)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(
                    1.dp,
                    if (isUltraUser) borderCol else Color(0x4D10B981),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Widgets, contentDescription = null, tint = EmeraldLight)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Widgets de Pantalla de Inicio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = "Tasas en Vivo gratis para todos • Calculadora y Cobro Express en Ultra.",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }

            // Free Widget Pin Button (Available to everyone)
            OutlinedButton(
                onClick = {
                    TasaVeRatesWidgetProvider.updateAllWidgets(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val manager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, TasaVeRatesWidgetProvider::class.java)
                        if (manager.isRequestPinAppWidgetSupported) {
                            manager.requestPinAppWidget(provider, null, null)
                        } else {
                            Toast.makeText(context, "Agrégalo desde Widgets → Tasa VE • Tasas en Vivo.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Mantén presionada tu pantalla de inicio → Widgets → Tasa VE.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0x4D10B981))
            ) {
                Icon(Icons.Default.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(17.dp), tint = EmeraldLight)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fijar Widget Tasas en Vivo (Gratis)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldLight)
            }

            // Ultra Pro Widget Pin / Unlock Button
            if (isUltraUser) {
                Button(
                    onClick = {
                        TasaVeRatesWidgetProvider.updateAllWidgets(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val manager = AppWidgetManager.getInstance(context)
                            val provider = ComponentName(context, TasaVeUltraWidgetProvider::class.java)
                            if (manager.isRequestPinAppWidgetSupported) {
                                manager.requestPinAppWidget(provider, null, null)
                            } else {
                                Toast.makeText(context, "Agrégalo desde Widgets → Tasa VE Ultra • Calculadora y Cobros.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Mantén presionada tu pantalla de inicio → Widgets → Tasa VE Ultra.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fijar Widget Calculadora y Cobros (Ultra)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onOpenPlans,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberUltra, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Desbloquear Widget Calc. + Cobro ($ultraPrice)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
        }
        }
    }
}

// =========================================================================
// TAB 4: COBROS (PAGO MIXTO, MÚLTIPLES MÉTODOS, TARJETA PNG Y WHATSAPP)
// =========================================================================
@Composable
fun CheckoutTab(
    isDark: Boolean,
    isUltraUser: Boolean,
    isAdFreePassActive: Boolean = false,
    ultraPrice: String,
    uiState: TasaVeUiState,
    effectiveUsdBcv: RateInfo? = uiState.usdBcv,
    effectiveEurBcv: RateInfo? = uiState.eurBcv,
    paymentProfiles: List<PaymentMethodProfile>,
    selectedProfileId: Long?,
    onSelectProfile: (Long?) -> Unit,
    onSaveProfile: (PaymentMethodProfile) -> Boolean,
    onDeleteProfile: (Long) -> Unit,
    customBrandName: String = "",
    customBrandLogoRevision: Long = 0L,
    onSaveCustomBrandName: (String) -> Unit = {},
    onImportCustomBrandLogo: (Uri) -> Boolean = { false },
    onRemoveCustomBrandLogo: () -> Unit = {},
    onOpenPlans: () -> Unit
) {
    val context = LocalContext.current
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF141418) else Color.White
    val borderCol = if (isDark) Color(0x1F94A3B8) else Color(0x19475569)

    var selectedRateKey by remember { mutableStateOf("USD_BCV") }
    var totalUsdStr by remember { mutableStateOf("") }
    var cashUsdStr by remember { mutableStateOf("") }
    var paidVesStr by remember { mutableStateOf("") }

    var editingProfile by remember { mutableStateOf<PaymentMethodProfile?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showBrandDialog by remember { mutableStateOf(false) }

    val customLogoFile = remember(customBrandLogoRevision, isUltraUser) {
        if (isUltraUser) ShareCardGenerator.getCustomBrandLogoFile(context) else null
    }
    val customLogoPreviewBitmap = remember(customLogoFile?.absolutePath, customBrandLogoRevision) {
        runCatching {
            customLogoFile?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        }.getOrNull()
    }

    val selectedProfile = remember(paymentProfiles, selectedProfileId) {
        paymentProfiles.firstOrNull { it.id == selectedProfileId }
    }

    val activeRate = when (selectedRateKey) {
        "EUR_BCV" -> effectiveEurBcv?.rate ?: 1.0
        "USDT" -> uiState.usdtParalelo?.rate ?: 1.0
        else -> effectiveUsdBcv?.rate ?: 1.0
    }
    val rateLabel = when (selectedRateKey) {
        "EUR_BCV" -> "Euro BCV"
        "USDT" -> "USDT"
        else -> "Dólar BCV"
    }
    val rateFmt = if (selectedRateKey == "USDT") "%.2f" else "%.4f"

    val totalUsd = totalUsdStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val cashUsd = cashUsdStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val paidVes = paidVesStr.replace(",", ".").toDoubleOrNull() ?: 0.0

    val result = remember(totalUsd, cashUsd, paidVes, activeRate) {
        UltraMath.calculateMixedPayment(totalUsd, cashUsd, paidVes, activeRate)
    }

    val shareSummaryText = remember(result, rateLabel, rateFmt, activeRate, selectedProfile, isUltraUser, customBrandName) {
        buildString {
            if (result.totalUsd > 0.005) {
                val brandHeader = if (isUltraUser && customBrandName.isNotBlank()) customBrandName.trim() else "Tasa VE"
                appendLine("*$brandHeader • Resumen de Cobro*")
                appendLine("Total: $${String.format(Locale.US, "%.2f", result.totalUsd)} • Bs. ${String.format(Locale.US, "%,.2f", result.totalVes)}")
                appendLine("Tasa ($rateLabel): Bs. ${String.format(Locale.US, rateFmt, activeRate)}")
                if (result.paidForeignUsd > 0 || result.paidVes > 0) {
                    appendLine("Abonado: $${String.format(Locale.US, "%.2f", result.paidForeignUsd)} + Bs. ${String.format(Locale.US, "%,.2f", result.paidVes)}")
                }
                if (result.remainingUsd > 0.005) {
                    appendLine("Por pagar: $${String.format(Locale.US, "%.2f", result.remainingUsd)} / Bs. ${String.format(Locale.US, "%,.2f", result.remainingVes)}")
                } else if (result.changeUsd > 0.005) {
                    appendLine("Vuelto: $${String.format(Locale.US, "%.2f", result.changeUsd)} / Bs. ${String.format(Locale.US, "%,.2f", result.changeVes)}")
                } else {
                    appendLine("Estado: Pagado completo ✅")
                }
            }
            selectedProfile?.let { profile ->
                if (result.totalUsd > 0.005) appendLine()
                val aliasSuffix = if (profile.alias.isNotBlank()) " (${profile.alias})" else ""
                when (profile.type) {
                    PaymentMethodType.PAGO_MOVIL -> {
                        appendLine("*Pago Móvil$aliasSuffix*")
                        appendLine("Banco: ${profile.formattedBank()}")
                        if (profile.docNumber.isNotBlank()) appendLine("CI/RIF: ${profile.formattedDoc()}")
                        if (profile.phoneNumber.isNotBlank()) appendLine("Teléfono: ${profile.formattedPhone()}")
                    }
                    PaymentMethodType.BANK_ACCOUNT -> {
                        appendLine("*Cuenta Bancaria$aliasSuffix*")
                        appendLine("Banco: ${profile.formattedBank()}")
                        if (profile.accountNumber.isNotBlank()) appendLine("Cuenta: ${profile.accountNumber}")
                        if (profile.docNumber.isNotBlank()) appendLine("CI/RIF: ${profile.formattedDoc()}")
                        if (profile.accountHolder.isNotBlank()) appendLine("Titular: ${profile.accountHolder}")
                    }
                    PaymentMethodType.ZELLE -> {
                        appendLine("*Datos Zelle$aliasSuffix*")
                        if (profile.digitalIdentifier.isNotBlank()) appendLine("Correo / Tel: ${profile.digitalIdentifier}")
                        if (profile.accountHolder.isNotBlank()) appendLine("Titular: ${profile.accountHolder}")
                    }
                    PaymentMethodType.BINANCE -> {
                        appendLine("*Binance Pay$aliasSuffix*")
                        if (profile.digitalIdentifier.isNotBlank()) appendLine("Pay ID / Correo: ${profile.digitalIdentifier}")
                        if (profile.accountHolder.isNotBlank()) appendLine("Titular: ${profile.accountHolder}")
                    }
                }
            }
            if (isNotEmpty()) appendLine()
            append(ShareCardGenerator.buildDownloadLinksBlock())
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
        TopBannerAd(isDark = isDark, isUltraUser = isUltraUser, isAdFreePassActive = isAdFreePassActive)

        // Rate Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("USD_BCV", effectiveUsdBcv, "USD BCV"),
                Triple("EUR_BCV", effectiveEurBcv, "EUR BCV"),
                Triple("USDT", uiState.usdtParalelo, "USDT")
            ).forEach { (key, info, label) ->
                RateSelectorCard(
                    modifier = Modifier.weight(1f),
                    key = key,
                    rateInfo = info,
                    customLabel = label,
                    isDark = isDark,
                    isSelected = selectedRateKey == key,
                    onSelect = { selectedRateKey = key }
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
                shape = RoundedCornerShape(14.dp),
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
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = paidVesStr,
                    onValueChange = { paidVesStr = it },
                    label = { Text("Paga en Bs.") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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

        // Multi-Profile Pago Móvil, Bank Accounts, Zelle & Binance Pay Section
        val profileLimitReached = !isUltraUser && !UltraMath.canAddPaymentProfile(paymentProfiles.size, isUltraUser)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUltraUser) {
                            "Mis Métodos de Cobro (${paymentProfiles.size})"
                        } else {
                            "Mis Métodos de Cobro (${paymentProfiles.size}/${UltraMath.FREE_PAYMENT_PROFILES_MAX} gratis)"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Pago Móvil, Bancos, Zelle o Binance • Toca para incluir",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                }
                FilledTonalButton(
                    onClick = {
                        if (profileLimitReached) {
                            Toast.makeText(
                                context,
                                "Gratis permite 1 perfil. Ultra desbloquea perfiles ilimitados.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onOpenPlans()
                        } else {
                            editingProfile = null
                            showProfileDialog = true
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (profileLimitReached) Icons.Default.Lock else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (profileLimitReached) "Ultra ∞" else "Nueva",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (paymentProfiles.isEmpty()) {
                OutlinedButton(
                    onClick = {
                        editingProfile = null
                        showProfileDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldLight)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Agregar Pago Móvil, Banco, Zelle o Binance", fontSize = 11.5.sp, color = textPrimary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    paymentProfiles.forEach { profile ->
                        val isSelected = profile.id == selectedProfile?.id
                        val badgeBg = when (profile.type) {
                            PaymentMethodType.PAGO_MOVIL -> Color(0x2610B981)
                            PaymentMethodType.BANK_ACCOUNT -> Color(0x2660A5FA)
                            PaymentMethodType.ZELLE -> Color(0x26A855F7)
                            PaymentMethodType.BINANCE -> Color(0x26F59E0B)
                        }
                        val badgeTextCol = when (profile.type) {
                            PaymentMethodType.PAGO_MOVIL -> EmeraldLight
                            PaymentMethodType.BANK_ACCOUNT -> BlueEuro
                            PaymentMethodType.ZELLE -> PurpleZelle
                            PaymentMethodType.BINANCE -> AmberUltra
                        }
                        val badgeLabel = when (profile.type) {
                            PaymentMethodType.PAGO_MOVIL -> "PAGO MÓVIL"
                            PaymentMethodType.BANK_ACCOUNT -> "CUENTA"
                            PaymentMethodType.ZELLE -> "ZELLE"
                            PaymentMethodType.BINANCE -> "BINANCE PAY"
                        }
                        val sub = when (profile.type) {
                            PaymentMethodType.PAGO_MOVIL ->
                                "${profile.formattedBank()} • ${profile.formattedDoc()} • ${profile.formattedPhone()}"
                            PaymentMethodType.BANK_ACCOUNT ->
                                "${profile.formattedBank()} • ${profile.accountNumber}"
                            PaymentMethodType.ZELLE ->
                                listOf(profile.digitalIdentifier, profile.accountHolder)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • ")
                                    .ifBlank { "Cuenta Zelle" }
                            PaymentMethodType.BINANCE ->
                                listOf(profile.digitalIdentifier, profile.accountHolder)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • ")
                                    .ifBlank { "Binance Pay" }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) {
                                        if (isDark) Color(0x1F10B981) else Color(0x1410B981)
                                    } else {
                                        if (isDark) Color(0xFF1B1B21) else Color(0xFFF8FAFC)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Emerald else borderCol,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    onSelectProfile(if (isSelected) null else profile.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) EmeraldLight else textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextCol
                                        )
                                    }
                                    Text(
                                        text = profile.displayTitle(),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = sub,
                                    fontSize = 10.5.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingProfile = profile
                                    showProfileDialog = true
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteProfile(profile.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
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

        // Custom Brand Header Card for Checkout PNG Cards (Ultra / Business)
        val hasCustomBrandConfigured = isUltraUser && (customLogoPreviewBitmap != null || customBrandName.isNotBlank())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(
                    width = 1.dp,
                    color = if (hasCustomBrandConfigured) Color(0x4D10B981) else borderCol,
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable {
                    if (isUltraUser) {
                        showBrandDialog = true
                    } else {
                        Toast.makeText(
                            context,
                            "Personaliza tu logo en tarjetas con Tasa VE Ultra.",
                            Toast.LENGTH_SHORT
                        ).show()
                        onOpenPlans()
                    }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isDark) Color(0xFF070C16) else Color(0xFFF1F5F9))
                    .border(1.dp, Color(0x4D10B981), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (customLogoPreviewBitmap != null) {
                    Image(
                        bitmap = customLogoPreviewBitmap,
                        contentDescription = "Logo personalizado",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = if (isUltraUser) EmeraldLight else AmberUltra,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (hasCustomBrandConfigured) {
                            customBrandName.ifBlank { "Logo propio activo" }
                        } else {
                            "Logo propio en Tarjeta PNG"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isUltraUser) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2EF59E0B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ULTRA",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberUltra
                            )
                        }
                    }
                }
                Text(
                    text = if (hasCustomBrandConfigured) {
                        "Se muestra arriba en la tarjeta • Pie Tasa VE abajo"
                    } else if (isUltraUser) {
                        "Sube tu logo (PNG/JPG 1:1 ≥ 512 px) y nombre comercial"
                    } else {
                        "Muestra tu marca arriba manteniendo el pie de Tasa VE"
                    },
                    fontSize = 10.5.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (isUltraUser) Icons.Default.Edit else Icons.Default.Lock,
                contentDescription = "Configurar marca",
                tint = if (isUltraUser) EmeraldLight else AmberUltra,
                modifier = Modifier.size(18.dp)
            )
        }

        // Copy, Visual PNG Card & WhatsApp Share Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                Text("Copiar texto", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
            }

            OutlinedButton(
                onClick = {
                    val statusTitle = when {
                        isChange -> "VUELTO A ENTREGAR"
                        isPaidExact -> "CUENTA SALDADA"
                        else -> "FALTA POR PAGAR"
                    }
                    ShareCardGenerator.shareCheckoutCard(
                        context = context,
                        isDark = isDark,
                        totalUsd = result.totalUsd,
                        totalVes = result.totalVes,
                        rateLabel = rateLabel,
                        rateValue = activeRate,
                        paidUsd = result.paidForeignUsd,
                        paidVes = result.paidVes,
                        statusTitle = statusTitle,
                        statusUsd = if (isChange) result.changeUsd else result.remainingUsd,
                        statusVes = if (isChange) result.changeVes else result.remainingVes,
                        profile = selectedProfile,
                        captionText = shareSummaryText,
                        customBrandName = if (isUltraUser) customBrandName.takeIf { it.isNotBlank() } else null,
                        customBrandLogoFile = if (isUltraUser) customLogoFile else null
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0x4D10B981))
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldLight)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tarjeta (IMG)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldLight)
            }
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
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Enviar por WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        }
    }

    if (showBrandDialog) {
        CustomBrandDialog(
            isDark = isDark,
            initialBrandName = customBrandName,
            logoPreviewBitmap = customLogoPreviewBitmap,
            onDismiss = { showBrandDialog = false },
            onSaveName = { name ->
                onSaveCustomBrandName(name)
                showBrandDialog = false
                Toast.makeText(context, "Marca de tarjeta actualizada", Toast.LENGTH_SHORT).show()
            },
            onPickLogo = { uri ->
                val imported = onImportCustomBrandLogo(uri)
                Toast.makeText(
                    context,
                    if (imported) "Logo guardado (512×512 px)" else "No se pudo leer esa imagen",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onRemoveLogo = {
                onRemoveCustomBrandLogo()
                Toast.makeText(context, "Logo personalizado eliminado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showProfileDialog) {
        PaymentProfileEditorDialog(
            isDark = isDark,
            initial = editingProfile,
            onDismiss = { showProfileDialog = false },
            onSave = { profile ->
                val saved = onSaveProfile(profile)
                showProfileDialog = false
                if (saved) {
                    Toast.makeText(context, "Método de cobro guardado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Gratis permite 1 perfil. Ultra desbloquea perfiles ilimitados.",
                        Toast.LENGTH_SHORT
                    ).show()
                    onOpenPlans()
                }
            }
        )
    }
}

@Composable
private fun CustomBrandDialog(
    isDark: Boolean,
    initialBrandName: String,
    logoPreviewBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onPickLogo: (Uri) -> Unit,
    onRemoveLogo: () -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF141418) else Color.White
    val subCardBg = if (isDark) Color(0xFF1B1C22) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    var brandName by remember(initialBrandName) { mutableStateOf(initialBrandName) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickLogo(uri)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x3310B981), RoundedCornerShape(24.dp))
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Marca en Tarjeta de Cobro",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Reemplaza la cabecera superior; el pie de Tasa VE se mantiene abajo.",
                            fontSize = 10.5.sp,
                            color = textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                // Live Header Preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(subCardBg)
                        .border(1.dp, Color(0x2610B981), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070C16))
                            .border(1.dp, Color(0x5510B981), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoPreviewBitmap != null) {
                            Image(
                                bitmap = logoPreviewBitmap,
                                contentDescription = "Vista previa logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initials = ShareCardGenerator.extractBrandInitials(
                                brandName.ifBlank { "Tasa VE" }
                            )
                            Text(
                                text = initials,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldLight
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = brandName.trim().ifBlank {
                            if (logoPreviewBitmap != null) "Solo logo centrado" else "Tasa VE (predeterminado)"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedTextField(
                    value = brandName,
                    onValueChange = { if (it.length <= 40) brandName = it },
                    label = { Text("Nombre de tu negocio o marca") },
                    placeholder = { Text("Ej. Bodega La Esquina") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x4D10B981))
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldLight)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (logoPreviewBitmap != null) "Cambiar logo" else "Subir logo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }

                    if (logoPreviewBitmap != null) {
                        OutlinedButton(
                            onClick = onRemoveLogo,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0x4DEF4444))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Quitar logo", modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                        }
                    }
                }

                // Clear specifications box so user logos look crisp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x1410B981) else Color(0x0F059669))
                        .border(1.dp, Color(0x3310B981), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "RECOMENDACIONES PARA TU LOGO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "• Formato ideal: PNG con fondo transparente (también JPG o WebP).",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                    Text(
                        text = "• Proporción cuadrada 1:1 (se recorta al centro automáticamente).",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                    Text(
                        text = "• Tamaño mínimo recomendado: 512 × 512 px para máxima nitidez.",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                    Text(
                        text = "• Evita textos diminutos; un isotipo limpio luce mejor.",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            brandName = ""
                            onRemoveLogo()
                            onSaveName("")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restablecer", color = textSecondary, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onSaveName(brandName) },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
                    ) {
                        Text("Guardar marca", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentProfileEditorDialog(
    isDark: Boolean,
    initial: PaymentMethodProfile?,
    onDismiss: () -> Unit,
    onSave: (PaymentMethodProfile) -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF141418) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    var methodType by remember(initial) { mutableStateOf(initial?.type ?: PaymentMethodType.PAGO_MOVIL) }
    var alias by remember(initial) { mutableStateOf(initial?.alias ?: "") }
    var selectedBank by remember(initial) {
        mutableStateOf(
            VENEZUELAN_BANKS.firstOrNull { it.code == initial?.bankCode }
                ?: VENEZUELAN_BANKS.first { it.code == "0134" }
        )
    }
    var docPrefix by remember(initial) { mutableStateOf(initial?.docPrefix ?: "V") }
    var docNumber by remember(initial) { mutableStateOf(initial?.docNumber ?: "") }
    var phonePrefix by remember(initial) { mutableStateOf(initial?.phonePrefix ?: "0414") }
    var phoneNumber by remember(initial) { mutableStateOf(initial?.phoneNumber ?: "") }
    var accountNumber by remember(initial) { mutableStateOf(initial?.accountNumber ?: "") }
    var accountHolder by remember(initial) { mutableStateOf(initial?.accountHolder ?: "") }
    var digitalIdentifier by remember(initial) { mutableStateOf(initial?.digitalIdentifier ?: "") }

    var showBankPicker by remember { mutableStateOf(false) }
    var expandDocMenu by remember { mutableStateOf(false) }
    var expandPhoneMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .border(1.dp, Color(0x2694A3B8), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initial == null) "Nuevo Método de Cobro" else "Editar Método de Cobro",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                // Type Toggle: 2 rows of 2 chips (Pago Móvil / Cuenta Bancaria / Zelle / Binance Pay)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        listOf(PaymentMethodType.PAGO_MOVIL, PaymentMethodType.BANK_ACCOUNT),
                        listOf(PaymentMethodType.ZELLE, PaymentMethodType.BINANCE)
                    ).forEach { rowTypes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTypes.forEach { t ->
                                FilterChip(
                                    selected = methodType == t,
                                    onClick = {
                                        methodType = t
                                        if (t == PaymentMethodType.BANK_ACCOUNT && accountNumber.isBlank()) {
                                            accountNumber = selectedBank.code
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    label = {
                                        Text(
                                            text = t.label,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0x2610B981),
                                        selectedLabelColor = EmeraldLight
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Nombre o Alias (opcional)") },
                    placeholder = {
                        Text(
                            when (methodType) {
                                PaymentMethodType.ZELLE -> "Ej. Zelle Personal / Tienda"
                                PaymentMethodType.BINANCE -> "Ej. Binance Comercio"
                                else -> "Ej. Personal Banesco / Tienda"
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (methodType == PaymentMethodType.PAGO_MOVIL || methodType == PaymentMethodType.BANK_ACCOUNT) {
                    // Venezuelan Bank Dropdown Selector (0134 - Banesco, etc.)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Banco (Código + Nombre)", fontSize = 11.sp, color = textSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0x4D94A3B8), RoundedCornerShape(14.dp))
                                .clickable { showBankPicker = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedBank.display(),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Elegir banco",
                                tint = EmeraldLight
                            )
                        }
                    }

                    // Fixed Document Prefix (V / E / J / G / P / C) + Number
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Cédula / RIF", fontSize = 11.sp, color = textSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .width(76.dp)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Color(0x4D94A3B8), RoundedCornerShape(14.dp))
                                        .clickable { expandDocMenu = true }
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(docPrefix, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = EmeraldLight)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp), tint = textSecondary)
                                }
                                DropdownMenu(
                                    expanded = expandDocMenu,
                                    onDismissRequest = { expandDocMenu = false }
                                ) {
                                    DOC_PREFIXES.forEach { prefix ->
                                        DropdownMenuItem(
                                            text = { Text(prefix, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                docPrefix = prefix
                                                expandDocMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = docNumber,
                                onValueChange = { input -> docNumber = input.filter { it.isDigit() }.take(12) },
                                placeholder = { Text("12345678") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (methodType == PaymentMethodType.PAGO_MOVIL) {
                        // Fixed Phone Prefix (0412 / 0414 / 0424 / 0416 / 0426 / 0422) + 7 digits
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Teléfono (7 dígitos)", fontSize = 11.sp, color = textSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(1.dp, Color(0x4D94A3B8), RoundedCornerShape(14.dp))
                                            .clickable { expandPhoneMenu = true }
                                            .padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(phonePrefix, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = EmeraldLight)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp), tint = textSecondary)
                                    }
                                    DropdownMenu(
                                        expanded = expandPhoneMenu,
                                        onDismissRequest = { expandPhoneMenu = false }
                                    ) {
                                        PHONE_PREFIXES.forEach { prefix ->
                                            DropdownMenuItem(
                                                text = { Text(prefix, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    phonePrefix = prefix
                                                    expandPhoneMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { input -> phoneNumber = input.filter { it.isDigit() }.take(7) },
                                    placeholder = { Text("1234567") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { input -> accountNumber = input.filter { it.isDigit() }.take(20) },
                            label = { Text("Número de Cuenta (20 dígitos)") },
                            placeholder = { Text("${selectedBank.code}0000000000000000") },
                            supportingText = { Text("${accountNumber.length}/20 dígitos") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = accountHolder,
                            onValueChange = { accountHolder = it },
                            label = { Text("Titular de la cuenta (opcional)") },
                            placeholder = { Text("Nombre o razón social") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (methodType == PaymentMethodType.ZELLE) {
                    OutlinedTextField(
                        value = digitalIdentifier,
                        onValueChange = { digitalIdentifier = it },
                        label = { Text("Correo o Teléfono Zelle") },
                        placeholder = { Text("ej. pagos@miempresa.com o +1...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountHolder,
                        onValueChange = { accountHolder = it },
                        label = { Text("Titular de la cuenta") },
                        placeholder = { Text("Nombre exacto del titular") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = digitalIdentifier,
                        onValueChange = { digitalIdentifier = it },
                        label = { Text("Binance Pay ID o Correo") },
                        placeholder = { Text("Ej. 123456789 o correo@ejemplo.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountHolder,
                        onValueChange = { accountHolder = it },
                        label = { Text("Alias / Titular (opcional)") },
                        placeholder = { Text("Nickname de Binance o nombre") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x2694A3B8))
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.SemiBold, color = textPrimary)
                    }
                    Button(
                        onClick = {
                            onSave(
                                PaymentMethodProfile(
                                    id = initial?.id ?: 0L,
                                    alias = alias.trim(),
                                    type = methodType,
                                    bankCode = selectedBank.code,
                                    bankName = selectedBank.name,
                                    docPrefix = docPrefix,
                                    docNumber = docNumber.trim(),
                                    phonePrefix = phonePrefix,
                                    phoneNumber = phoneNumber.trim(),
                                    accountNumber = accountNumber.trim(),
                                    accountHolder = accountHolder.trim(),
                                    digitalIdentifier = digitalIdentifier.trim()
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.Black)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showBankPicker) {
        BankPickerDialog(
            isDark = isDark,
            selectedCode = selectedBank.code,
            onSelect = { bank ->
                val oldCode = selectedBank.code
                selectedBank = bank
                if (methodType == PaymentMethodType.BANK_ACCOUNT) {
                    accountNumber = if (accountNumber.isBlank() || accountNumber == oldCode) {
                        bank.code
                    } else if (accountNumber.startsWith(oldCode)) {
                        bank.code + accountNumber.removePrefix(oldCode)
                    } else {
                        accountNumber
                    }
                }
                showBankPicker = false
            },
            onDismiss = { showBankPicker = false }
        )
    }
}

@Composable
private fun BankPickerDialog(
    isDark: Boolean,
    selectedCode: String,
    onSelect: (VenezuelanBank) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val dialogBg = if (isDark) Color(0xFF141418) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val filteredBanks = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) VENEZUELAN_BANKS
        else VENEZUELAN_BANKS.filter {
            it.code.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .border(1.dp, Color(0x2694A3B8), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selecciona el Banco",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar por código (0134) o nombre...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredBanks, key = { it.code }) { bank ->
                        val selected = bank.code == selectedCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) Color(0x2610B981)
                                    else if (isDark) Color(0xFF1B1B21) else Color(0xFFF8FAFC)
                                )
                                .clickable { onSelect(bank) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x2610B981))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = bank.code,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldLight
                                )
                            }
                            Text(
                                text = bank.name,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// COMPARATIVA DE PLANES: GRATIS vs TASA VE ULTRA ($0.99/mes o $7.99/año)
// =========================================================================
@Composable
fun PlansComparisonDialog(
    isDark: Boolean,
    isUltraUser: Boolean,
    isAdFreePassActive: Boolean = false,
    isRewardedAdLoading: Boolean = false,
    ultraMonthlyPrice: String,
    ultraYearlyPrice: String,
    onPurchaseUltraMonthly: () -> Unit,
    onPurchaseUltraYearly: () -> Unit,
    onWatchAdFreePass: (() -> Unit)? = null,
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
                            text = "Tasa VE Ultra",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Sin publicidad y todas las funciones ilimitadas",
                            fontSize = 11.sp,
                            color = EmeraldLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                // Comparison Table (Gratis vs Ultra)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, Color(0x1F94A3B8), RoundedCornerShape(20.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Función", modifier = Modifier.weight(1.8f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        Text("Gratis", modifier = Modifier.weight(0.8f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textSecondary, textAlign = TextAlign.Center)
                        Text("Ultra", modifier = Modifier.weight(0.8f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AmberUltra, textAlign = TextAlign.Center)
                    }
                    HorizontalDivider(color = Color(0x1A94A3B8))

                    val rows = listOf(
                        listOf("Calculadora exacta BCV y USDT + COP", "✓", "✓"),
                        listOf("BCV Mañana + Calendario SUDEBAN", "✓", "✓"),
                        listOf("Útiles (¿Con qué pago?, Dividir cuenta)", "✓", "✓"),
                        listOf("Pago Mixto + Tarjeta PNG (Cobro y Carrito)", "✓", "✓"),
                        listOf("Perfiles (PM/Banco/Zelle/Bin.)", "1", "∞"),
                        listOf("Carrito inteligente", "5 prod.", "∞ + IVA/IGTF"),
                        listOf("Escáner cámara OCR", "3/día +5 Ad", "∞"),
                        listOf("Sin publicidad", "12h/Video", "✓ Siempre"),
                        listOf("Widget Tasas en Vivo", "✓", "✓"),
                        listOf("Widget Calculadora y Cobros", "—", "✓"),
                        listOf("Alertas de tasa", "—", "✓"),
                        listOf("Menú rápido WhatsApp", "BCV USD", "Todos + PM"),
                        listOf("Barra fija y Barra Rápida", "—", "✓"),
                        listOf("Logo propio en Tarjeta PNG", "—", "✓")
                    )
                    rows.forEach { cols ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cols[0], modifier = Modifier.weight(1.8f), fontSize = 11.sp, color = textPrimary)
                            Text(cols[1], modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = textSecondary, textAlign = TextAlign.Center)
                            Text(cols[2], modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = AmberUltra, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }

                // Ultra CTA Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x2AF59E0B), Color(0x12F59E0B))
                            )
                        )
                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isUltraUser) "Tasa VE Ultra Activo ⭐" else "Activa Tasa VE Ultra",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Cero anuncios, notificación fija de tasas, conversor flotante en WhatsApp/Barra Rápida con Pago Móvil, escáner ∞, carrito ∞ + IVA/IGTF, perfiles ∞, logo propio, Alertas y Widgets. Cancela cuando quieras.",
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
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberUltra, contentColor = Color.Black)
                            ) {
                                Text(ultraMonthlyPrice, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                            OutlinedButton(
                                onClick = onPurchaseUltraYearly,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, AmberUltra)
                            ) {
                                Text(ultraYearlyPrice, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = AmberUltra)
                            }
                        }
                    }
                }

                if (!isUltraUser && onWatchAdFreePass != null) {
                    if (isAdFreePassActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1F10B981))
                                .border(1.dp, Color(0x4D10B981), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Pase 12h sin anuncios activo en este dispositivo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldLight
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onWatchAdFreePass,
                            enabled = !isRewardedAdLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0x4D10B981))
                        ) {
                            if (isRewardedAdLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = EmeraldLight
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cargando video...", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = EmeraldLight)
                            } else {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldLight)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "12h sin banners ni pausas (Ver 1 video)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onRestorePurchases,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Restaurar suscripción", fontSize = 11.sp, color = textSecondary)
                }

                Text(
                    text = "La suscripción Ultra se renueva automáticamente por el período elegido a través de Google Play salvo cancelación al menos 24 h antes del vencimiento desde Google Play > Suscripciones. Reembolsos sujetos a las políticas de Google Play.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = textSecondary.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
