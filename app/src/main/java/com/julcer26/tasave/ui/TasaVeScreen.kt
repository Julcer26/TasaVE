package com.julcer26.tasave.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.julcer26.tasave.ads.TopBannerAd
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julcer26.tasave.R
import androidx.compose.ui.res.painterResource
import com.julcer26.tasave.data.ConversionHistory
import com.julcer26.tasave.data.RateInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasaVeScreen(
    viewModel: TasaVeViewModel,
    activity: Activity? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
    val proPrice by viewModel.proProductPrice.collectAsStateWithLifecycle()
    val purchaseStatus by viewModel.purchaseStatusMessage.collectAsStateWithLifecycle()
    val historyList by viewModel.historyState.collectAsStateWithLifecycle()

    val isDark = themeMode == "DARK"

    // Settings & History Modal States
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    LaunchedEffect(purchaseStatus) {
        purchaseStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearPurchaseStatus()
        }
    }

    // Focus state for inputs
    var topFocused by remember { mutableStateOf(false) }
    var bottomFocused by remember { mutableStateOf(false) }

    // Copying tick indicator feedback
    var isConvertedCopied by remember { mutableStateOf(false) }

    // Palette Colors for Sleek Theme
    // Base Background: Dark Obsidian Velvet (#0A0A0B) vs Clean Light Slate (#F8FAFC)
    val bgStart = if (isDark) Color(0xFF0A0A0B) else Color(0xFFF8FAFC)
    val bgEnd = if (isDark) Color(0xFF121214) else Color(0xFFF1F5F9)
    
    // Background Orbs (Emerald / Blue Blur Glows)
    val orbColor1 = if (isDark) Color(0x1210B981) else Color(0x1C34D399) // Soft Emerald (10% opacity)
    val orbColor2 = if (isDark) Color(0x122563EB) else Color(0x1460A5FA) // Soft Blue (10% opacity)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Background Base Color
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(bgStart, bgEnd)
                    )
                )
                // Top-Left Emerald blurred static orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbColor1, Color.Transparent),
                        center = Offset(0f, 200.dp.toPx())
                    ),
                    radius = 280.dp.toPx()
                )
                // Bottom-Right Blue blurred static orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbColor2, Color.Transparent),
                        center = Offset(size.width, size.height * 0.7f)
                    ),
                    radius = 320.dp.toPx()
                )
            },
        containerColor = Color.Transparent, // Transparent scaffold content area to show backdrops
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Custom logo container displaying the beautiful internal app logo (with custom emerald-green and amber-yellow vector arrows)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x1F10B981) else Color(0x0F10B981))
                                .border(
                                    width = 1.6.dp,
                                    color = if (isDark) Color(0x5910B981) else Color(0x4010B981),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "Tasa VE Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(
                            text = "Tasa VE",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.5).sp
                        )

                        if (isProUser) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF59E0B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Pro Buy Button (if not already pro)
                    if (!isProUser) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                    )
                                )
                                .clickable {
                                    if (activity != null) {
                                        viewModel.purchasePro(activity)
                                    } else {
                                        showSettings = true
                                    }
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Pro",
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "PRO $proPrice",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Conversion History Button
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1F94A3B8) else Color(0x0F000000))
                            .clickable(onClick = { showHistory = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historial",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Modern Circle Refresh Button
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1F94A3B8) else Color(0x0F000000))
                            .clickable(
                                onClick = { viewModel.fetchRates(forceRefresh = true) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refrescar",
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // Modern Circle Settings Trigger
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1F94A3B8) else Color(0x0F000000))
                            .clickable(
                                onClick = { showSettings = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Non-intrusive Top Banner Ad (hidden for Pro users)
            TopBannerAd(
                isDark = isDark,
                isProUser = isProUser
            )

            // Resilience & Offline alerts banner
            if (uiState.isOfflineError || (uiState.usdBcv?.isLocalOffline == true)) {
                GlassCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = Color(0xFFEF4444)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Sin conexión",
                            tint = Color(0xFFEF4444)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Fuera de Línea",
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Mostrando tasas de cambio guardadas localmente.",
                                color = textSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Text(
                    text = "Error de Red: $error. Reintentando...",
                    fontSize = 11.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // 1. DATE HEADER & RATE SELECTOR
            val representativeDate = uiState.usdBcv?.dateUpdated 
                ?: uiState.eurBcv?.dateUpdated 
                ?: uiState.usdtParalelo?.dateUpdated

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tasas de Cambio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    representativeDate?.let { date ->
                        Text(
                            text = "Actualizado: $date",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ratesList = listOf(
                        Triple("USD_BCV", uiState.usdBcv, "USD - BCV"),
                        Triple("EUR_BCV", uiState.eurBcv, "EUR - BCV"),
                        Triple("USDT", uiState.usdtParalelo, "USDT Binance")
                    )
                    
                    ratesList.forEach { (key, rateInfo, customLabel) ->
                        val isSelected = viewModel.selectedRateKey == key
                        RateSelectorCard(
                            modifier = Modifier.weight(1f),
                            key = key,
                            rateInfo = rateInfo,
                            customLabel = customLabel,
                            isDark = isDark,
                            isSelected = isSelected,
                            onSelect = { viewModel.selectRate(key) },
                            onCopy = { rateValue ->
                                copyToClipboard(context, rateValue.toString())
                                Toast.makeText(context, "$customLabel copiado: Bs. $rateValue", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            val currentRateInfo = viewModel.getSelectedRateInfo()

            // 2. STUNNING DIGITAL CONVERSION CONVERTER (GLASSMORPHIC BACKDROP)
            
            GlassCard(
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_card"),
                glowColor = if (isDark) Color(0x0DFFFFFF) else Color(0x05000000)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // Head of Calculator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculadora Inteligente",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        
                        currentRateInfo?.let {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isDark) Color(0x2610B981) else Color(0x1F059669)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Tasa actual: Bs. ${String.format(Locale.US, "%.2f", it.rate)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                                )
                            }
                        }
                    }

                    val topLabel = if (viewModel.isVesAsInput) "Bolívares (VES)" else "Divisa (${currentRateInfo?.label ?: "Moneda Extranjera"})"
                    val topSymbol = if (viewModel.isVesAsInput) "Bs." else (currentRateInfo?.symbol ?: "$")
                    val topValue = if (viewModel.isVesAsInput) viewModel.vesAmount else viewModel.foreignAmount
                    val onTopValueChange: (String) -> Unit = { if (viewModel.isVesAsInput) viewModel.updateVesAmount(it) else viewModel.updateForeignAmount(it) }
                    val topTestTag = if (viewModel.isVesAsInput) "ves_input" else "foreign_input"
                    val topTextColor = if (viewModel.isVesAsInput) {
                        if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                    } else textPrimary

                    val bottomLabel = if (viewModel.isVesAsInput) "Divisa (${currentRateInfo?.label ?: "Moneda Extranjera"})" else "Bolívares (VES)"
                    val bottomSymbol = if (viewModel.isVesAsInput) (currentRateInfo?.symbol ?: "$") else "Bs."
                    val bottomValue = if (viewModel.isVesAsInput) viewModel.foreignAmount else viewModel.vesAmount
                    val onBottomValueChange: (String) -> Unit = { if (viewModel.isVesAsInput) viewModel.updateForeignAmount(it) else viewModel.updateVesAmount(it) }
                    val bottomTestTag = if (viewModel.isVesAsInput) "foreign_input" else "ves_input"
                    val bottomTextColor = if (viewModel.isVesAsInput) {
                        textPrimary
                    } else {
                        if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                    }

                    // Panel 1: Top Input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topLabel,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Monto de entrada",
                                fontSize = 9.sp,
                                color = textSecondary.copy(alpha = 0.8f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (topFocused) {
                                        if (isDark) Color(0x1F10B981) else Color(0x0F10B981)
                                    } else {
                                        if (isDark) Color(0x0FFFFFFF) else Color(0x05000000)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (topFocused) {
                                        Color(0xFF10B981)
                                    } else {
                                        if (isDark) Color(0x0DFFFFFF) else Color(0x0D000000)
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topSymbol,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = if (topFocused) {
                                    Color(0xFF10B981)
                                } else if (viewModel.isVesAsInput) {
                                    if (isDark) Color(0x9E10B981) else Color(0x9E059669)
                                } else {
                                    if (isDark) Color(0xFF64748B) else Color(0xFF475569)
                                },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            
                            BasicTextField(
                                value = topValue,
                                onValueChange = onTopValueChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { topFocused = it.isFocused }
                                    .testTag(topTestTag),
                                textStyle = TextStyle(
                                    color = topTextColor,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Default
                                ),
                                cursorBrush = SolidColor(if (isDark) Color(0xFF10B981) else Color(0xFF059669)),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (topValue.isEmpty()) {
                                            Text(
                                                text = "0.00",
                                                color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (topValue.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0x1F94A3B8) else Color(0x0F000000))
                                        .clickable { onTopValueChange("") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar",
                                        tint = textSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Floating Switch Button between sheet blocks (Shadow & Emerald custom glow)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(
                                    width = 4.dp,
                                    color = if (isDark) Color(0xFF0A0A0B) else Color(0xFFF8FAFC),
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.swapAmounts()
                                    Toast.makeText(context, "Montos intercambiados", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Conversión recíproca",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                             )
                        }
                    }

                    // Panel 2: Bottom Output
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bottomLabel,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Monto convertido",
                                fontSize = 9.sp,
                                color = textSecondary.copy(alpha = 0.8f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (bottomFocused) {
                                        if (isDark) Color(0x1F10B981) else Color(0x0F10B981)
                                    } else {
                                        if (isDark) Color(0x0FFFFFFF) else Color(0x05000000)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (bottomFocused) {
                                        Color(0xFF10B981)
                                    } else {
                                        if (isDark) Color(0x0DFFFFFF) else Color(0x0D000000)
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bottomSymbol,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = if (bottomFocused) {
                                    Color(0xFF10B981)
                                } else if (!viewModel.isVesAsInput) {
                                    if (isDark) Color(0x9E10B981) else Color(0x9E059669)
                                } else {
                                    if (isDark) Color(0xFF64748B) else Color(0xFF475569)
                                },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            
                            BasicTextField(
                                value = bottomValue,
                                onValueChange = onBottomValueChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { bottomFocused = it.isFocused }
                                    .testTag(bottomTestTag),
                                textStyle = TextStyle(
                                    color = bottomTextColor,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Default
                                ),
                                cursorBrush = SolidColor(if (isDark) Color(0xFF10B981) else Color(0xFF059669)),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (bottomValue.isEmpty()) {
                                            Text(
                                                text = "0.00",
                                                color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (bottomValue.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Copiar resultado de conversión
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isConvertedCopied) {
                                                    if (isDark) Color(0x1F10B981) else Color(0x1F059669)
                                                } else {
                                                    if (isDark) Color(0x1F94A3B8) else Color(0x0F000000)
                                                }
                                            )
                                            .clickable {
                                                copyToClipboard(context, bottomValue)
                                                isConvertedCopied = true
                                                coroutineScope.launch {
                                                    delay(2000)
                                                    isConvertedCopied = false
                                                }
                                                Toast.makeText(context, "$bottomSymbol $bottomValue copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isConvertedCopied) Icons.Default.Done else Icons.Default.ContentCopy,
                                            contentDescription = "Copiar resultado",
                                            tint = if (isConvertedCopied) Color(0xFF10B981) else textSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    // Botón limpiar
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0x1F94A3B8) else Color(0x0F000000))
                                            .clickable { onBottomValueChange("") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar",
                                            tint = textSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Actions: Guardar en Historial y Ver Historial
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val saved = viewModel.saveConversionToHistory()
                                if (saved) {
                                    Toast.makeText(context, "Conversión guardada en el historial", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Ingresa un monto para guardar el cálculo", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0x1F10B981) else Color(0x1410B981),
                                contentColor = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                            ),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Guardar cálculo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showHistory = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0x2694A3B8) else Color(0x26475569)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = textPrimary
                            ),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Ver historial", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Live Multi-Rate Comparison Panel
                    val parsedVes = viewModel.vesAmount.toDoubleOrNull() ?: 0.0
                    val parsedForeign = viewModel.foreignAmount.toDoubleOrNull() ?: 0.0
                    
                    if (parsedVes > 0.0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0x0F222226) else Color(0x0C000000))
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color(0x0DFFFFFF) else Color(0x0D000000),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EQUIVALENCIAS EN OTRA TASA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Bs. ${String.format(Locale.US, "%,.2f", parsedVes)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(if (isDark) Color(0x0DFFFFFF) else Color(0x0D000000))
                            )
                            
                            val bcvUsdRate = uiState.usdBcv?.rate ?: 0.0
                            val bcvEurRate = uiState.eurBcv?.rate ?: 0.0
                            val parallelUsdtRate = uiState.usdtParalelo?.rate ?: 0.0
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. USD Equivalent
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Dólar BCV",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val usdVal = if (bcvUsdRate > 0) parsedVes / bcvUsdRate else 0.0
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", usdVal)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Tasa: ${String.format(Locale.US, "%.2f", bcvUsdRate)}",
                                        fontSize = 8.sp,
                                        color = textSecondary.copy(alpha = 0.7f)
                                    )
                                }
                                
                                // Divider vertical
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(36.dp)
                                        .background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000))
                                )
                                
                                // 2. EUR Equivalent
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Euro BCV",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val eurVal = if (bcvEurRate > 0) parsedVes / bcvEurRate else 0.0
                                    Text(
                                        text = "€${String.format(Locale.US, "%.2f", eurVal)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Tasa: ${String.format(Locale.US, "%.2f", bcvEurRate)}",
                                        fontSize = 8.sp,
                                        color = textSecondary.copy(alpha = 0.7f)
                                    )
                                }
                                
                                // Divider vertical
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(36.dp)
                                        .background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000))
                                )
                                
                                // 3. USDT Equivalent
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "USDT Bin.",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val usdtVal = if (parallelUsdtRate > 0) parsedVes / parallelUsdtRate else 0.0
                                    Text(
                                        text = "₮${String.format(Locale.US, "%.2f", usdtVal)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Tasa: ${String.format(Locale.US, "%.2f", parallelUsdtRate)}",
                                        fontSize = 8.sp,
                                        color = textSecondary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            if (parsedForeign > 0.0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(if (isDark) Color(0x0DFFFFFF) else Color(0x0D000000))
                                )
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val symbol = currentRateInfo?.symbol ?: "$"
                                    Text(
                                        text = "CON ${String.format(Locale.US, "%,.2f", parsedForeign)} $symbol EN CADA MONEDA:",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = "USD BCV", fontSize = 8.5.sp, color = textSecondary)
                                            Text(
                                                text = "Bs. ${String.format(Locale.US, "%,.2f", parsedForeign * bcvUsdRate)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary
                                            )
                                        }
                                        Column {
                                            Text(text = "EUR BCV", fontSize = 8.5.sp, color = textSecondary)
                                            Text(
                                                text = "Bs. ${String.format(Locale.US, "%,.2f", parsedForeign * bcvEurRate)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary
                                            )
                                        }
                                        Column {
                                            Text(text = "USDT Bin.", fontSize = 8.5.sp, color = textSecondary)
                                            Text(
                                                text = "Bs. ${String.format(Locale.US, "%,.2f", parsedForeign * parallelUsdtRate)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }

            // Bottom Navigation sync and state layout conforming to Sleek footer design
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Green static active point indicative of real-time sync
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = if (uiState.isOfflineError) "Sincronizado (Local)" else "Motor en tiempo real activo",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                    }
                    Text(
                        text = "Tasa VE • Desarrollado localmente ❤️",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                }
            }
        }
    }

    // 4. DIALOGS: HISTORY AND SETTINGS
    if (showHistory) {
        HistoryDialog(
            isDark = isDark,
            historyList = historyList,
            onDismiss = { showHistory = false },
            onLoadItem = { item ->
                viewModel.loadConversion(item)
                showHistory = false
                Toast.makeText(context, "Conversión cargada en la calculadora", Toast.LENGTH_SHORT).show()
            },
            onClearHistory = {
                viewModel.clearHistory()
                Toast.makeText(context, "Historial vaciado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSettings) {
        SleekSettingsDialog(
            isDark = isDark,
            isProUser = isProUser,
            proPrice = proPrice,
            onDismiss = { showSettings = false },
            onToggleTheme = { viewModel.toggleTheme() },
            onPurchasePro = {
                if (activity != null) {
                    viewModel.purchasePro(activity)
                } else {
                    Toast.makeText(context, "Servicio de Google Play no disponible en este momento.", Toast.LENGTH_SHORT).show()
                }
            },
            onRestorePurchases = {
                viewModel.restorePurchases()
            }
        )
    }
}

@Composable
fun RateSelectorCard(
    modifier: Modifier = Modifier,
    key: String,
    rateInfo: RateInfo?,
    customLabel: String,
    isDark: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCopy: (Double) -> Unit
) {
    // Gentle floating scale representation on hover/selection
    val scale = animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.00f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "select_scale"
    )

    // Tag Custom Colors matching the Sleek layout
    val tagColor = when (key) {
        "USD_BCV" -> Color(0xFF10B981) // Emerald Green
        "EUR_BCV" -> Color(0xFF3B82F6) // Azure Blue
        else -> Color(0xFFF59E0B) // Amber USDT
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                onSelect()
                if (rateInfo != null) {
                    onCopy(rateInfo.rate)
                }
            }
            .background(
                if (isSelected) {
                    if (isDark) Color(0x1F222226) else Color(0x3DFFFFFF)
                } else {
                    if (isDark) Color(0x0F222226) else Color(0x1FFFFFFF)
                }
            )
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) {
                    tagColor.copy(alpha = 0.8f)
                } else {
                    if (isDark) Color(0x1AFFFFFF) else Color(0x1F000000)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Custom label formatted uppercase text
            Text(
                text = customLabel.uppercase(Locale.getDefault()),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = tagColor,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (rateInfo == null) {
                Box(
                    modifier = Modifier.padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = tagColor
                    )
                }
            } else {
                Text(
                    text = String.format(Locale.US, "%.2f", rateInfo.rate),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    letterSpacing = (-0.5).sp
                )
            }
        }
    }
}

@Composable
fun HistoryDialog(
    isDark: Boolean,
    historyList: List<ConversionHistory>,
    onDismiss: () -> Unit,
    onLoadItem: (ConversionHistory) -> Unit,
    onClearHistory: () -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF0E0E10) else Color(0xFFF1F5F9)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(12.dp)
                .border(
                    BorderStroke(1.dp, if (isDark) Color(0x1F94A3B8) else Color(0x1F475569)),
                    RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Historial de Conversiones",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000))
                            .clickable { onDismiss() },
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

                Divider(color = if (isDark) Color(0x1F94A3B8) else Color(0x1F475569))

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Sin conversiones guardadas aún",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "Usa 'Guardar cálculo' en la calculadora para almacenar tus conversiones aquí.",
                                fontSize = 11.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        historyList.forEach { item ->
                            val dateStr = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault()).format(Date(item.timestamp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(0x0FFFFFFF) else Color(0x0A000000))
                                    .border(
                                        width = 1.dp,
                                        color = if (isDark) Color(0x0DFFFFFF) else Color(0x0D000000),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onLoadItem(item) }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "${String.format(Locale.US, "%,.2f", item.amountFrom)} ${item.currencyFrom} = ${String.format(Locale.US, "%,.2f", item.amountTo)} ${item.currencyTo}",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "Tasa: Bs. ${String.format(Locale.US, "%.2f", item.rate)} • $dateStr",
                                            fontSize = 10.sp,
                                            color = textSecondary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0x1F10B981) else Color(0x1410B981))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Cargar",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Botón para vaciar historial
                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = BorderStroke(1.dp, Color(0x40EF4444))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Borrar todo el historial", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SleekSettingsDialog(
    isDark: Boolean,
    isProUser: Boolean,
    proPrice: String,
    onDismiss: () -> Unit,
    onToggleTheme: () -> Unit,
    onPurchasePro: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    var expandPrivacy by remember { mutableStateOf(false) }
    var expandTerms by remember { mutableStateOf(false) }

    val dialogBg = if (isDark) Color(0xFF0E0E10) else Color(0xFFF1F5F9)
    val dividerColor = if (isDark) Color(0x1F94A3B8) else Color(0x1F475569)

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(12.dp)
                .border(
                    BorderStroke(1.dp, if (isDark) Color(0x1F94A3B8) else Color(0x1F475569)),
                    RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header of dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configuración",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Divider(color = dividerColor)

                // PRO UPGRADE / IAP SECTION ($2.99 USD)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProUser) {
                            if (isDark) Color(0x1F10B981) else Color(0x1410B981)
                        } else {
                            if (isDark) Color(0x14F59E0B) else Color(0x0FF59E0B)
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isProUser) Color(0x4D10B981) else Color(0x4DF59E0B)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isProUser) Icons.Default.Verified else Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isProUser) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isProUser) "Tasa VE Pro Activo ⭐" else "Tasa VE Pro ($proPrice)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }

                        Text(
                            text = if (isProUser) {
                                "¡Gracias por tu apoyo! Los anuncios están desactivados permanentemente y cuentas con funciones premium."
                            } else {
                                "Pago único de $proPrice (sin suscripciones recurrentes). Elimina para siempre todos los anuncios publicitarios y respalda las mejoras del proyecto."
                            },
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            lineHeight = 16.sp
                        )

                        if (!isProUser) {
                            Button(
                                onClick = onPurchasePro,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Quitar Publicidad ($proPrice)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(onClick = onRestorePurchases) {
                                    Text(
                                        text = "Restaurar compra anterior",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }

                // Interfaz / Switch Mode Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x0FFFFFFF) else Color(0x0A000000))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Visual switch icon",
                            tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Modo de Interfaz",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = if (isDark) "Tema Oscuro Activo" else "Tema Claro Activo",
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        }
                    }

                    Switch(
                        checked = isDark,
                        onCheckedChange = { onToggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0x4D10B981),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }

                // Privacy Disclosure con Local storage y Google Play compliance
                SleekDisclosurePanel(
                    title = "Política de Privacidad de Datos",
                    isExpanded = expandPrivacy,
                    isDark = isDark,
                    onToggle = { expandPrivacy = !expandPrivacy },
                    content = "Tasa VE respeta profundamente tu privacidad. Las configuraciones del usuario, caché de cotizaciones e historial de conversiones se almacenan únicamente de forma local en tu dispositivo móvil. Los servicios de Google Mobile Ads (AdMob) y Google Play Billing procesan identificadores técnicos estándar para la entrega no invasiva de anuncios y la verificación de compras in-app conforme a las directrices de privacidad de Google Play. No vendemos ni compartimos datos personales a terceros."
                )

                // Terms and Conditions Disclosure (Data Disclaimer)
                SleekDisclosurePanel(
                    title = "Términos y Descargo de Responsabilidad",
                    isExpanded = expandTerms,
                    isDark = isDark,
                    onToggle = { expandTerms = !expandTerms },
                    content = "Esta herramienta tiene un carácter estrictamente informativo, educativo y de cálculo matemático. Los datos de cotizaciones se obtienen de fuentes públicas de acceso libre provistas por DolarAPI y el Banco Central de Venezuela (BCV). Tasa VE no fija, manipula ni influye en el valor oficial ni paralelo de divisas, no presta servicios financieros, captación ni intermediación cambiaria. Se recomienda verificar cotizaciones oficiales antes de transacciones bancarias."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sleek Footer version detail
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color(0x1410B981) else Color(0x0A10B981))
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0x2610B981) else Color(0x1F10B981),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Tasa VE Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Tasa VE • Versión 1.0.0",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = if (isProUser) "Versión Pro • Sin Anuncios ⭐" else "Versión Gratuita con Anuncios • Pro Disponible",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun SleekDisclosurePanel(
    title: String,
    isExpanded: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0x0FFFFFFF) else Color(0x0A000000))
            .clickable { onToggle() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Cerrar" else "Abrir",
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = content,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun GlassCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    // Ultra luxurious translucent card reflecting the "Sleek Interface" web design
    val cardBackground = if (isDark) Color(0x0F222226) else Color(0x9EFFFFFF)
    val borderStroke = if (isDark) Color(0x1F94A3B8) else Color(0x19475569)

    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (glowColor != Color.Transparent) glowColor.copy(alpha = 0.35f) else borderStroke,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Tasa VE", text)
    clipboard.setPrimaryClip(clip)
}


