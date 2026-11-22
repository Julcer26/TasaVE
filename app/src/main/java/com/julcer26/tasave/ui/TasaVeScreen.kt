package com.julcer26.tasave.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.julcer26.tasave.ads.InterstitialAdManager
import com.julcer26.tasave.ads.RewardedAdManager
import com.julcer26.tasave.ads.TopBannerAd
import com.julcer26.tasave.data.TasaVeRepository
import com.julcer26.tasave.feedback.FeedbackReporter
import com.julcer26.tasave.feedback.FeedbackSubmission
import com.julcer26.tasave.feedback.FeedbackSubmitResult
import com.julcer26.tasave.feedback.FeedbackType
import com.julcer26.tasave.scanner.PriceScannerDialog
import com.julcer26.tasave.share.ShareCardGenerator
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
import com.julcer26.tasave.BuildConfig
import com.julcer26.tasave.R
import androidx.compose.ui.res.painterResource
import com.julcer26.tasave.analytics.AnalyticsTracker
import com.julcer26.tasave.calendar.BcvCalendarHelper
import com.julcer26.tasave.data.ConversionHistory
import com.julcer26.tasave.data.RateInfo
import com.julcer26.tasave.ultra.AppTab
import com.julcer26.tasave.ultra.CartTab
import com.julcer26.tasave.ultra.CheckoutTab
import com.julcer26.tasave.ultra.MarketTab
import com.julcer26.tasave.ultra.PlansComparisonDialog
import com.julcer26.tasave.ultra.RateSnapshot
import com.julcer26.tasave.ultra.UltraMath
import com.julcer26.tasave.ultra.UserTier
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasaVeScreen(
    viewModel: TasaVeViewModel,
    activity: Activity? = null,
    widgetAction: String? = null,
    onConsumeWidgetAction: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isUltraUser by viewModel.isUltraUser.collectAsStateWithLifecycle()
    val debugTier by viewModel.debugTier.collectAsStateWithLifecycle()
    val ultraMonthlyPrice by viewModel.ultraMonthlyPrice.collectAsStateWithLifecycle()
    val ultraYearlyPrice by viewModel.ultraYearlyPrice.collectAsStateWithLifecycle()
    val purchaseStatus by viewModel.purchaseStatusMessage.collectAsStateWithLifecycle()
    val historyList by viewModel.historyState.collectAsStateWithLifecycle()
    val remainingFreeScans by viewModel.remainingFreeScans.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartIvaEnabled by viewModel.cartIvaEnabled.collectAsStateWithLifecycle()
    val cartIgtfEnabled by viewModel.cartIgtfEnabled.collectAsStateWithLifecycle()
    val rateSnapshots by viewModel.rateSnapshots.collectAsStateWithLifecycle()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsStateWithLifecycle()
    val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()
    val paymentProfiles by viewModel.paymentProfiles.collectAsStateWithLifecycle()
    val selectedPaymentProfileId by viewModel.selectedPaymentProfileId.collectAsStateWithLifecycle()
    val customBrandName by viewModel.customBrandName.collectAsStateWithLifecycle()
    val customBrandLogoRevision by viewModel.customBrandLogoRevision.collectAsStateWithLifecycle()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()

    val adFreePassUntilMs by viewModel.adFreePassUntilMs.collectAsStateWithLifecycle()
    val isAdFreePassActive = adFreePassUntilMs > System.currentTimeMillis()
    val isAdsDisabled = isUltraUser || isAdFreePassActive

    val rewardedAdManager = remember { RewardedAdManager() }
    val isRewardedAdLoading by rewardedAdManager.isLoading.collectAsStateWithLifecycle()
    val interstitialAdManager = remember { InterstitialAdManager() }

    LaunchedEffect(isAdsDisabled) {
        if (!isAdsDisabled) {
            rewardedAdManager.preload(context)
            interstitialAdManager.preload(context, isAdFree = false)
        }
    }

    val isDark = themeMode == "DARK"

    // Navigation & Modal States
    var currentTab by remember { mutableStateOf(AppTab.CONVERTER) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var scannerForCart by remember { mutableStateOf(false) }
    var scannedDuringDialog by remember { mutableStateOf(false) }
    var showPlansDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    LaunchedEffect(widgetAction) {
        when (widgetAction) {
            com.julcer26.tasave.widget.TasaVeUltraWidgetProvider.LAUNCH_CONVERTER -> {
                currentTab = AppTab.CONVERTER
                onConsumeWidgetAction()
            }
            com.julcer26.tasave.widget.TasaVeUltraWidgetProvider.LAUNCH_CART -> {
                currentTab = AppTab.CART
                onConsumeWidgetAction()
            }
            com.julcer26.tasave.widget.TasaVeUltraWidgetProvider.LAUNCH_CHECKOUT -> {
                currentTab = AppTab.CHECKOUT
                onConsumeWidgetAction()
            }
            com.julcer26.tasave.widget.TasaVeUltraWidgetProvider.LAUNCH_SCANNER -> {
                viewModel.refreshRemainingFreeScans()
                scannerForCart = false
                showScanner = true
                onConsumeWidgetAction()
            }
            com.julcer26.tasave.widget.TasaVeUltraWidgetProvider.LAUNCH_PLANS -> {
                showPlansDialog = true
                onConsumeWidgetAction()
            }
        }
    }

    val triggerWatchAdFreePass: () -> Unit = {
        rewardedAdManager.showOrLoad(
            activity = activity,
            onRewarded = {
                viewModel.grantAdFreePass12Hours()
                Toast.makeText(
                    context,
                    "¡Pase de 12 horas sin anuncios activado!",
                    Toast.LENGTH_LONG
                ).show()
            },
            onError = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable { showPlansDialog = true }
                    ) {
                        // Custom logo container displaying the internal app logo
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF070C16))
                                .border(
                                    width = 1.5.dp,
                                    color = if (isDark) Color(0x5910B981) else Color(0x6610B981),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_brand_mark),
                                contentDescription = "Tasa VE Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Tasa VE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.4).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isUltraUser) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                                        )
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "ULTRA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                actions = {
                    val hasUpcomingHoliday = remember { BcvCalendarHelper.hasUpcomingBankHolidaySoon() }
                    // SUDEBAN Bank Calendar & Daily Historical Rate Lookup
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1F94A3B8) else Color(0x0F000000))
                            .clickable {
                                showCalendarDialog = true
                                AnalyticsTracker.logCalendarOpened(context)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendario bancario y tasas",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        if (hasUpcomingHoliday) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B))
                            )
                        }
                    }

                    // Camera Price Scanner Quick Trigger
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x2610B981) else Color(0x1F059669))
                            .clickable {
                                viewModel.refreshRemainingFreeScans()
                                scannerForCart = false
                                showScanner = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Escanear precio",
                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Conversion History Button
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
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
                            .size(36.dp)
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
                            .padding(end = 10.dp)
                            .size(36.dp)
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
        },
        bottomBar = {
            val navItems = listOf(
                Triple(AppTab.CONVERTER, Icons.Default.CurrencyExchange, AppTab.CONVERTER.label),
                Triple(AppTab.CART, Icons.Default.ShoppingCart, AppTab.CART.label),
                Triple(AppTab.MARKET, Icons.Default.GridView, AppTab.MARKET.label),
                Triple(AppTab.CHECKOUT, Icons.Default.PointOfSale, AppTab.CHECKOUT.label)
            )
            val dockContainerColor = if (isDark) Color(0xEB0D131D) else Color(0xFAFFFFFF)
            val dockBorderColor = if (isDark) Color(0x3834D399) else Color(0x1E0F172A)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = dockContainerColor,
                    border = BorderStroke(1.dp, dockBorderColor),
                    shadowElevation = if (isDark) 16.dp else 12.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { (tab, icon, label) ->
                            val selected = currentTab == tab
                            val pillBg by animateColorAsState(
                                targetValue = if (selected) {
                                    if (isDark) Color(0x2B10B981) else Color(0x1F059669)
                                } else {
                                    Color.Transparent
                                },
                                animationSpec = tween(durationMillis = 200),
                                label = "floatingNavPillBg"
                            )
                            val pillBorder = if (selected) {
                                if (isDark) Color(0x5234D399) else Color(0x40059669)
                            } else {
                                Color.Transparent
                            }
                            val itemTint by animateColorAsState(
                                targetValue = if (selected) {
                                    if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                                } else {
                                    textSecondary
                                },
                                animationSpec = tween(durationMillis = 200),
                                label = "floatingNavItemTint"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(pillBg)
                                    .border(1.dp, pillBorder, RoundedCornerShape(22.dp))
                                    .clickable {
                                        if (currentTab != tab) {
                                            currentTab = tab
                                            AnalyticsTracker.logTabOpened(context, tab.label)
                                            if (tab != AppTab.CONVERTER) {
                                                interstitialAdManager.maybeShowAfterAction(
                                                    activity = activity,
                                                    isAdFree = isAdsDisabled,
                                                    appLaunchCount = viewModel.appLaunchCount
                                                )
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (tab == AppTab.CART && cartItems.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = Color(0xFF10B981),
                                                    contentColor = Color.Black
                                                ) {
                                                    Text(
                                                        text = "${cartItems.sumOf { it.quantity }}",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = itemTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = itemTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = itemTint,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            animationSpec = tween(durationMillis = 180),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "tabCrossfade"
        ) { activeTab ->
            when (activeTab) {
                AppTab.CART -> {
                    CartTab(
                        isDark = isDark,
                        isUltraUser = isUltraUser,
                        isAdFreePassActive = isAdFreePassActive,
                        ultraPrice = ultraMonthlyPrice,
                        uiState = uiState,
                        cartItems = cartItems,
                        applyIva = cartIvaEnabled,
                        applyIgtf = cartIgtfEnabled,
                        onAddManualItem = { name, amount, isVes, quantity ->
                            viewModel.addCartItem(name, amount, isVes, quantity = quantity)
                        },
                        onEditItem = { id, name, amount, isVes, quantity ->
                            viewModel.editCartItem(id, name, amount, isVes, quantity)
                        },
                        onScanToCart = {
                            viewModel.refreshRemainingFreeScans()
                            scannerForCart = true
                            showScanner = true
                        },
                        onUpdateQuantity = { id, delta -> viewModel.updateCartItemQuantity(id, delta) },
                        onRemoveItem = { id -> viewModel.removeCartItem(id) },
                        onClearCart = { viewModel.clearCart() },
                        onToggleIva = { viewModel.toggleCartIva() },
                        onToggleIgtf = { viewModel.toggleCartIgtf() },
                        onOpenPlans = { showPlansDialog = true }
                    )
                }
                AppTab.MARKET -> {
                    MarketTab(
                        isDark = isDark,
                        isUltraUser = isUltraUser,
                        isAdFreePassActive = isAdFreePassActive,
                        ultraPrice = ultraMonthlyPrice,
                        uiState = uiState,
                        snapshots = rateSnapshots,
                        isHistoryLoading = isHistoryLoading,
                        alertSettings = alertSettings,
                        onTrackUtilTool = { toolKey -> AnalyticsTracker.logUtilToolOpened(context, toolKey) },
                        onRefreshHistory = { viewModel.fetchHistoricalRates(forceRefresh = true) },
                        onUpdateAlerts = { viewModel.updateAlertSettings(it) },
                        onTestNotification = { viewModel.triggerTestAlertNotification() },
                        onOpenPlans = { showPlansDialog = true }
                    )
                }
                AppTab.CHECKOUT -> {
                    CheckoutTab(
                        isDark = isDark,
                        isUltraUser = isUltraUser,
                        isAdFreePassActive = isAdFreePassActive,
                        ultraPrice = ultraMonthlyPrice,
                        uiState = uiState,
                        effectiveUsdBcv = viewModel.getEffectiveUsdBcv(),
                        effectiveEurBcv = viewModel.getEffectiveEurBcv(),
                        paymentProfiles = paymentProfiles,
                        selectedProfileId = selectedPaymentProfileId,
                        onSelectProfile = { viewModel.selectPaymentProfile(it) },
                        onSaveProfile = { viewModel.savePaymentProfile(it) },
                        onDeleteProfile = { viewModel.deletePaymentProfile(it) },
                        customBrandName = customBrandName,
                        customBrandLogoRevision = customBrandLogoRevision,
                        onSaveCustomBrandName = { viewModel.saveCustomBrandName(it) },
                        onImportCustomBrandLogo = { viewModel.importCustomBrandLogo(it) },
                        onRemoveCustomBrandLogo = { viewModel.removeCustomBrandLogo() },
                        onOpenPlans = { showPlansDialog = true }
                    )
                }
                AppTab.CONVERTER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Non-intrusive Top Banner Ad (hidden for Ultra users or 12h ad-free pass holders)
            TopBannerAd(
                isDark = isDark,
                isUltraUser = isUltraUser,
                isAdFreePassActive = isAdFreePassActive
            )

            // Relocated sleek Ultra banner
            if (!isUltraUser) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isDark) Color(0x1AF59E0B) else Color(0x1FF59E0B)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0x4DF59E0B),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { showPlansDialog = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0x2EF59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Tasa VE Ultra",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tasa VE Ultra • $ultraMonthlyPrice",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Cero anuncios, escáner ∞, carrito ∞ + IVA/IGTF, alertas y perfiles ∞",
                                fontSize = 10.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "ULTRA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                }
            }

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
            val effectiveUsdBcv = viewModel.getEffectiveUsdBcv()
            val effectiveEurBcv = viewModel.getEffectiveEurBcv()
            val representativeDate = effectiveUsdBcv?.dateUpdated
                ?: effectiveEurBcv?.dateUpdated
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

                // BCV Hoy vs Mañana segmented selector when BCV has published tomorrow's rate
                val nextUsd = uiState.nextUsdBcv
                val nextDateIso = uiState.nextBcvDateIso
                val isCopSelected = viewModel.selectedRateKey == "COP"
                val copRateInfo = viewModel.getEffectiveCopRate()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (nextUsd != null && nextDateIso != null) {
                        FilterChip(
                            selected = !viewModel.previewTomorrowBcv,
                            onClick = { viewModel.setTomorrowBcvPreview(false) },
                            shape = RoundedCornerShape(14.dp),
                            label = {
                                Text(
                                    text = "BCV Hoy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        FilterChip(
                            selected = viewModel.previewTomorrowBcv,
                            onClick = { viewModel.setTomorrowBcvPreview(true) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x33F59E0B),
                                selectedLabelColor = Color(0xFFF59E0B)
                            ),
                            label = {
                                Text(
                                    text = "Mañana (${TasaVeRepository.formatIsoShortDate(nextDateIso)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    } else {
                        Text(
                            text = "Tasas del día",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                    }

                    FilterChip(
                        selected = isCopSelected,
                        onClick = {
                            if (isCopSelected) {
                                viewModel.selectRate("USD_BCV")
                            } else {
                                viewModel.selectRate("COP")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x33F59E0B),
                            selectedLabelColor = Color(0xFFF59E0B)
                        ),
                        label = {
                            Text(
                                text = "🇨🇴 COP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )

                    if (nextUsd != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x26F59E0B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Próx: Bs. ${String.format(Locale.US, "%.4f", nextUsd.rate)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ratesList = listOf(
                        Triple("USD_BCV", effectiveUsdBcv, "USD - BCV"),
                        Triple("EUR_BCV", effectiveEurBcv, "EUR - BCV"),
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
                            onSelect = { viewModel.selectRate(key) }
                        )
                    }
                }

                if (isCopSelected && copRateInfo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1FF59E0B))
                            .border(1.dp, Color(0x4DF59E0B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🇨🇴 Modo Frontera: 1 USD = ${String.format(Locale.US, "%,.0f", uiState.copPerUsd)} COP",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Text(
                            text = "1,000 COP = Bs. ${String.format(Locale.US, "%.2f", copRateInfo.rate * 1000.0)}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
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
                            val badgeText = if (it.key == "COP") {
                                "1,000 COP = Bs. ${String.format(Locale.US, "%.2f", it.rate * 1000.0)}"
                            } else {
                                val decimals = if (it.key == "USDT") "%.2f" else "%.4f"
                                "Tasa actual: Bs. ${String.format(Locale.US, decimals, it.rate)}"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isDark) Color(0x2610B981) else Color(0x1F059669)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = badgeText,
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
                                .clip(RoundedCornerShape(14.dp))
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
                                    shape = RoundedCornerShape(14.dp)
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
                                .clip(RoundedCornerShape(14.dp))
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
                                    shape = RoundedCornerShape(14.dp)
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

                    val parsedVes = viewModel.vesAmount.toDoubleOrNull() ?: 0.0
                    val parsedForeign = viewModel.foreignAmount.toDoubleOrNull() ?: 0.0

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
                                    interstitialAdManager.maybeShowAfterAction(
                                        activity = activity,
                                        isAdFree = isAdsDisabled,
                                        appLaunchCount = viewModel.appLaunchCount
                                    )
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

                    val bcvUsdRate = effectiveUsdBcv?.rate ?: 0.0
                    val bcvEurRate = effectiveEurBcv?.rate ?: 0.0
                    val parallelUsdtRate = uiState.usdtParalelo?.rate ?: 0.0

                    // Visual PNG Share Card Button
                    if (parsedVes > 0.0) {
                        OutlinedButton(
                            onClick = {
                                val rateDecimals = if (currentRateInfo?.key == "USDT") "%.2f" else "%.4f"
                                val sym = currentRateInfo?.symbol ?: "$"
                                val foreignStr = "$sym ${String.format(Locale.US, "%,.2f", parsedForeign)}"
                                val vesStr = "Bs. ${String.format(Locale.US, "%,.2f", parsedVes)}"

                                val eqList = buildList {
                                    if (bcvUsdRate > 0) add("Dólar BCV" to "$${String.format(Locale.US, "%.2f", parsedVes / bcvUsdRate)}")
                                    if (bcvEurRate > 0) add("Euro BCV" to "€${String.format(Locale.US, "%.2f", parsedVes / bcvEurRate)}")
                                    if (parallelUsdtRate > 0) add("USDT Binance" to "₮${String.format(Locale.US, "%.2f", parsedVes / parallelUsdtRate)}")
                                }

                                ShareCardGenerator.shareCalculationCard(
                                    context = context,
                                    isDark = isDark,
                                    inputLabel = if (viewModel.isVesAsInput) "Bolívares (VES)" else (currentRateInfo?.label ?: "Divisa"),
                                    inputDisplay = if (viewModel.isVesAsInput) vesStr else foreignStr,
                                    outputLabel = if (viewModel.isVesAsInput) (currentRateInfo?.label ?: "Divisa") else "Bolívares (VES)",
                                    outputDisplay = if (viewModel.isVesAsInput) foreignStr else vesStr,
                                    rateLabel = currentRateInfo?.label ?: "Tasa BCV",
                                    rateDisplay = "Bs. ${String.format(Locale.US, rateDecimals, currentRateInfo?.rate ?: 0.0)}",
                                    updatedAt = currentRateInfo?.dateUpdated,
                                    modifierText = null,
                                    adjustedTotalText = null,
                                    equivalences = eqList,
                                    isTomorrowBcv = viewModel.previewTomorrowBcv
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0x4D10B981)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                            ),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Compartir Tarjeta (IMG)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Live Multi-Rate Comparison Panel
                    if (parsedVes > 0.0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0x0F222226) else Color(0x0C000000))
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color(0x0DFFFFFF) else Color(0x0D000000),
                                    shape = RoundedCornerShape(14.dp)
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
                                        text = "Tasa: ${String.format(Locale.US, "%.4f", bcvUsdRate)}",
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
                                        text = "Tasa: ${String.format(Locale.US, "%.4f", bcvEurRate)}",
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

                            val copVesRate = viewModel.getEffectiveCopRate()?.rate ?: 0.0
                            if (copVesRate > 0.0) {
                                val copEquiv = parsedVes / copVesRate
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Color(0x14F59E0B) else Color(0x12F59E0B))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🇨🇴 Frontera (TRM ${String.format(Locale.US, "%,.0f", uiState.copPerUsd)}):",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", copEquiv)} COP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
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
                        text = "Tasa VE v1.2 • Local ❤️",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                }
            }
        }
                }
            }
        }
    }

    // 4. DIALOGS: HISTORY, SETTINGS, PLANS AND SCANNER
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
            isUltraUser = isUltraUser,
            isAdFreePassActive = isAdFreePassActive,
            isRewardedAdLoading = isRewardedAdLoading,
            analyticsEnabled = analyticsEnabled,
            onToggleAnalytics = { enabled -> viewModel.setAnalyticsEnabled(enabled) },
            debugTier = debugTier,
            ultraMonthlyPrice = ultraMonthlyPrice,
            onDismiss = { showSettings = false },
            onToggleTheme = { viewModel.toggleTheme() },
            onSelectDebugTier = { tier -> viewModel.setDebugTier(tier) },
            onOpenPlans = {
                showSettings = false
                showPlansDialog = true
            },
            onWatchAdFreePass = triggerWatchAdFreePass,
            onRestorePurchases = {
                viewModel.restorePurchases()
            }
        )
    }

    if (showCalendarDialog) {
        BcvCalendarDialog(
            isDark = isDark,
            snapshots = rateSnapshots,
            currentUsd = viewModel.getEffectiveUsdBcv()?.rate ?: uiState.usdBcv?.rate ?: 0.0,
            currentEur = viewModel.getEffectiveEurBcv()?.rate ?: uiState.eurBcv?.rate ?: 0.0,
            currentUsdt = uiState.usdtParalelo?.rate ?: 0.0,
            onApplyQuickAmount = { amountUsd ->
                currentTab = AppTab.CONVERTER
                viewModel.selectRate("USD_BCV")
                viewModel.updateForeignAmount(amountUsd)
                showCalendarDialog = false
            },
            onDismiss = { showCalendarDialog = false }
        )
    }

    if (showPlansDialog) {
        PlansComparisonDialog(
            isDark = isDark,
            isUltraUser = isUltraUser,
            isAdFreePassActive = isAdFreePassActive,
            isRewardedAdLoading = isRewardedAdLoading,
            ultraMonthlyPrice = ultraMonthlyPrice,
            ultraYearlyPrice = ultraYearlyPrice,
            onPurchaseUltraMonthly = {
                if (activity != null) {
                    viewModel.purchaseUltra(activity, yearly = false)
                } else {
                    Toast.makeText(context, "Servicio de Google Play no disponible en este momento.", Toast.LENGTH_SHORT).show()
                }
            },
            onPurchaseUltraYearly = {
                if (activity != null) {
                    viewModel.purchaseUltra(activity, yearly = true)
                } else {
                    Toast.makeText(context, "Servicio de Google Play no disponible en este momento.", Toast.LENGTH_SHORT).show()
                }
            },
            onWatchAdFreePass = triggerWatchAdFreePass,
            onRestorePurchases = {
                viewModel.restorePurchases()
            },
            onDismiss = { showPlansDialog = false }
        )
    }

    if (showScanner) {
        PriceScannerDialog(
            isUltraUser = isUltraUser,
            remainingFreeScans = remainingFreeScans,
            ultraPrice = ultraMonthlyPrice,
            selectedRateKey = viewModel.selectedRateKey,
            usdBcv = viewModel.getEffectiveUsdBcv(),
            eurBcv = viewModel.getEffectiveEurBcv(),
            usdtParalelo = uiState.usdtParalelo,
            isRewardedAdLoading = isRewardedAdLoading,
            onWatchRewardedAd = {
                rewardedAdManager.showOrLoad(
                    activity = activity,
                    onRewarded = {
                        val next = viewModel.grantRewardedScans(UltraMath.REWARDED_SCAN_BONUS)
                        Toast.makeText(
                            context,
                            "¡+5 escaneos desbloqueados! ($next disponibles)",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onSelectRate = { key -> viewModel.selectRate(key) },
            onApplyPrice = { amount, isVes, rateKey ->
                if (scannerForCart) {
                    val added = viewModel.addCartItem("Producto escaneado", amount, isVes, rateKey, fromScanner = true)
                    if (added) {
                        scannedDuringDialog = true
                        Toast.makeText(context, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()
                    } else {
                        scannedDuringDialog = false
                        showScanner = false
                        Toast.makeText(context, "Límite de 5 productos gratis en el carrito", Toast.LENGTH_SHORT).show()
                        showPlansDialog = true
                    }
                } else {
                    scannedDuringDialog = true
                    viewModel.applyScannedPrice(amount, isVes, rateKey)
                    Toast.makeText(context, "Precio escaneado aplicado", Toast.LENGTH_SHORT).show()
                }
            },
            onUpgradeUltra = {
                scannedDuringDialog = false
                showScanner = false
                showPlansDialog = true
            },
            onDismiss = {
                showScanner = false
                if (scannedDuringDialog) {
                    scannedDuringDialog = false
                    interstitialAdManager.maybeShowAfterAction(
                        activity = activity,
                        isAdFree = isAdsDisabled,
                        appLaunchCount = viewModel.appLaunchCount
                    )
                }
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
    onSelect: () -> Unit
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
            .clickable { onSelect() }
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
            verticalArrangement = Arrangement.spacedBy(3.dp),
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
                val decimals = if (key == "USDT") "%.2f" else "%.4f"
                Text(
                    text = String.format(Locale.US, decimals, rateInfo.rate),
                    fontSize = if (key == "USDT") 17.sp else 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    letterSpacing = (-0.5).sp,
                    maxLines = 1
                )

                val pct = rateInfo.changePercent
                if (pct != null && kotlin.math.abs(pct) >= 0.005) {
                    val isUp = pct > 0.0
                    val pctColor = if (isUp) Color(0xFF10B981) else Color(0xFFEF4444)
                    val pctSign = if (isUp) "+" else ""
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(pctColor.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "$pctSign${String.format(Locale.US, "%.2f", pct)}%",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = pctColor
                        )
                    }
                }
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
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0x0FFFFFFF) else Color(0x0A000000))
                                    .border(
                                        width = 1.dp,
                                        color = if (isDark) Color(0x0DFFFFFF) else Color(0x0D000000),
                                        shape = RoundedCornerShape(14.dp)
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
    isUltraUser: Boolean,
    isAdFreePassActive: Boolean = false,
    isRewardedAdLoading: Boolean = false,
    analyticsEnabled: Boolean = true,
    onToggleAnalytics: (Boolean) -> Unit = {},
    debugTier: UserTier,
    ultraMonthlyPrice: String,
    onDismiss: () -> Unit,
    onToggleTheme: () -> Unit,
    onSelectDebugTier: (UserTier) -> Unit,
    onOpenPlans: () -> Unit,
    onWatchAdFreePass: (() -> Unit)? = null,
    onRestorePurchases: () -> Unit
) {
    val context = LocalContext.current
    var legalDialogTab by remember { mutableStateOf<Int?>(null) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    val dialogBg = if (isDark) Color(0xFF0E0E10) else Color(0xFFF1F5F9)
    val dividerColor = if (isDark) Color(0x1F94A3B8) else Color(0x1F475569)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

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
                        color = textPrimary,
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
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                HorizontalDivider(color = dividerColor)

                // DEBUG BUILD: Plan Switcher so user can test FREE / ULTRA without Play Store
                if (BuildConfig.DEBUG) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0x1F3B82F6) else Color(0x142563EB)
                        ),
                        border = BorderStroke(1.dp, Color(0x4D3B82F6))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "MODO PRUEBA v1.2 (SIMULAR PLAN)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Alterna libremente entre Gratis y Ultra para probar anuncios, límites y funciones.",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    UserTier.FREE to "Gratis",
                                    UserTier.ULTRA to "Ultra"
                                ).forEach { (tier, label) ->
                                    FilterChip(
                                        selected = debugTier == tier,
                                        onClick = { onSelectDebugTier(tier) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // PLAN ULTRA CARD
                val planBorderColor = if (isUltraUser) Color(0x6610B981) else Color(0x4DF59E0B)
                val planContainerColor = if (isUltraUser) {
                    if (isDark) Color(0x1F10B981) else Color(0x1410B981)
                } else {
                    if (isDark) Color(0x14F59E0B) else Color(0x0FF59E0B)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = planContainerColor),
                    border = BorderStroke(width = 1.dp, color = planBorderColor)
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
                                imageVector = if (isUltraUser) Icons.Default.WorkspacePremium else Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isUltraUser) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isUltraUser) "Tasa VE Ultra Activo ⭐" else "Tasa VE Ultra ($ultraMonthlyPrice)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }

                        Text(
                            text = if (isUltraUser) {
                                "Tienes acceso total: cero anuncios, escáner ilimitado, Carrito ∞ + IVA/IGTF, Brecha y Alertas, Widget y perfiles ilimitados."
                            } else {
                                "Elimina toda la publicidad y desbloquea escáner ilimitado, carrito ilimitado + IVA/IGTF, alertas de tasa, Widget y perfiles de cobro ilimitados."
                            },
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = onOpenPlans,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUltraUser) Color(0xFF10B981) else Color(0xFFF59E0B),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUltraUser) "Ver detalle de tu Plan Ultra" else "Ver Plan Ultra ($ultraMonthlyPrice)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!isUltraUser && onWatchAdFreePass != null) {
                            if (isAdFreePassActive) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1F10B981))
                                        .border(1.dp, Color(0x4D10B981), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Pase 12h sin anuncios activo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onWatchAdFreePass,
                                    enabled = !isRewardedAdLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0x4D10B981))
                                ) {
                                    if (isRewardedAdLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF34D399)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Cargando video...",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF34D399)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "12h sin anuncios (Ver 1 video)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }
                        }

                        if (!isUltraUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(onClick = onRestorePurchases) {
                                    Text(
                                        text = "Restaurar suscripción",
                                        fontSize = 11.sp,
                                        color = textSecondary
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
                        .clip(RoundedCornerShape(14.dp))
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
                                color = textPrimary
                            )
                            Text(
                                text = if (isDark) "Tema Oscuro Activo" else "Tema Claro Activo",
                                fontSize = 10.sp,
                                color = textSecondary
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

                // Community, Official Website & Social Media Section (Real Icons Side-by-Side)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0x0FFFFFFF) else Color.White)
                        .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WEB Y REDES (@tasa.ve)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        letterSpacing = 0.7.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://tasave.net"))
                                        )
                                    }.onFailure {
                                        Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_social_web),
                                contentDescription = "Página Web Oficial tasave.net",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/tasa.ve/"))
                                        )
                                    }.onFailure {
                                        Toast.makeText(context, "No se pudo abrir Instagram", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_social_instagram),
                                contentDescription = "Instagram @tasa.ve",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@tasa.ve"))
                                        )
                                    }.onFailure {
                                        Toast.makeText(context, "No se pudo abrir TikTok", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_social_tiktok),
                                contentDescription = "TikTok @tasa.ve",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }

                // Support & Suggestions Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0x0FFFFFFF) else Color.White)
                        .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SOPORTE Y MEJORAS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        letterSpacing = 0.7.sp
                    )

                    LegalMenuRow(
                        icon = Icons.Default.Feedback,
                        title = "Reportar error o sugerir función",
                        subtitle = "¿Falló algo o qué te gustaría ver en Tasa VE?",
                        isDark = isDark,
                        accentColor = Color(0xFFF59E0B),
                        onClick = { showFeedbackDialog = true }
                    )
                }

                // Legal & Privacy Section (Title-only clean entries)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0x0FFFFFFF) else Color.White)
                        .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LEGAL Y PRIVACIDAD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        letterSpacing = 0.7.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Estadísticas anónimas de uso",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Solo qué funciones abres (sin montos ni datos personales).",
                                fontSize = 10.sp,
                                color = textSecondary
                            )
                        }
                        Switch(
                            checked = analyticsEnabled,
                            onCheckedChange = onToggleAnalytics,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }

                    LegalMenuRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Política de Privacidad",
                        subtitle = null,
                        isDark = isDark,
                        onClick = { legalDialogTab = 0 }
                    )

                    LegalMenuRow(
                        icon = Icons.Default.Gavel,
                        title = "Términos de Uso y Descargo",
                        subtitle = null,
                        isDark = isDark,
                        onClick = { legalDialogTab = 1 }
                    )

                    LegalMenuRow(
                        icon = Icons.Default.OpenInBrowser,
                        title = "Ver Política Web Completa",
                        subtitle = null,
                        isDark = isDark,
                        onClick = {
                            runCatching {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://tasave.net/privacy-policy.html")
                                )
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sleek Footer version detail
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF070C16))
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0x4010B981) else Color(0x5510B981),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_brand_mark),
                            contentDescription = "Tasa VE Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Tasa VE • Versión 1.2.0",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = if (isUltraUser) "Plan Ultra • Todo Desbloqueado ⭐" else "Plan Gratuito • Ultra Disponible",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }

    if (showFeedbackDialog) {
        FeedbackReportDialog(
            isDark = isDark,
            isUltraUser = isUltraUser,
            onDismiss = { showFeedbackDialog = false }
        )
    }

    legalDialogTab?.let { initialTab ->
        LegalCenterDialog(
            isDark = isDark,
            initialTab = initialTab,
            onOpenWeb = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://tasave.net/privacy-policy.html"))
                    )
                }.onFailure {
                    Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { legalDialogTab = null }
        )
    }
}

@Composable
private fun LegalMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    isDark: Boolean,
    accentColor: Color = Color(0xFF10B981),
    onClick: () -> Unit
) {
    val rowBg = if (isDark) Color(0x12FFFFFF) else Color(0xFFF8FAFC)
    val rowBorder = if (isDark) Color(0x1F94A3B8) else Color(0x19475569)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = if (subtitle.isNullOrBlank()) 10.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedbackReportDialog(
    isDark: Boolean,
    isUltraUser: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dialogBg = if (isDark) Color(0xFF141418) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    var selectedType by remember { mutableStateOf(FeedbackType.SUGGESTION) }
    var selectedModule by remember { mutableStateOf(FeedbackReporter.MODULES.first()) }
    var description by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var fallbackErrorMessage by remember { mutableStateOf<String?>(null) }

    val buildSubmission = {
        FeedbackSubmission(
            type = selectedType,
            module = selectedModule,
            description = description.trim(),
            contact = contact.trim(),
            planLabel = if (isUltraUser) "Ultra" else "Gratis"
        )
    }

    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            modifier = Modifier
                .fillMaxWidth(0.92f)
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
                            text = "Reportes y Sugerencias",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Tu opinión se registra directamente para mejorar Tasa VE.",
                            fontSize = 10.5.sp,
                            color = textSecondary
                        )
                    }
                    IconButton(
                        onClick = { if (!isSending) onDismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                // Type Toggle: Bug vs Suggestion
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == FeedbackType.BUG,
                        onClick = { selectedType = FeedbackType.BUG },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Reportar error",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x2EF59E0B),
                            selectedLabelColor = Color(0xFFF59E0B),
                            selectedLeadingIconColor = Color(0xFFF59E0B)
                        )
                    )
                    FilterChip(
                        selected = selectedType == FeedbackType.SUGGESTION,
                        onClick = { selectedType = FeedbackType.SUGGESTION },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Sugerir función",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x2610B981),
                            selectedLabelColor = Color(0xFF34D399),
                            selectedLeadingIconColor = Color(0xFF34D399)
                        )
                    )
                }

                // Module selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PANTALLA O MÓDULO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 0.6.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FeedbackReporter.MODULES.forEach { mod ->
                            FilterChip(
                                selected = selectedModule == mod,
                                onClick = { selectedModule = mod },
                                shape = RoundedCornerShape(12.dp),
                                label = {
                                    Text(
                                        text = mod,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedModule == mod) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 600) description = it },
                    label = {
                        Text(
                            if (selectedType == FeedbackType.BUG) {
                                "¿Qué error ocurrió y cómo reproducirlo?"
                            } else {
                                "¿Qué más te gustaría ver en la app?"
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            if (selectedType == FeedbackType.BUG) {
                                "Ej. Al escanear una etiqueta con luz baja..."
                            } else {
                                "Ej. Me gustaría exportar el carrito a PDF..."
                            }
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { if (it.length <= 80) contact = it },
                    label = { Text("Contacto opcional (correo o @instagram)") },
                    placeholder = { Text("Solo si deseas recibir respuesta") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Se adjunta versión (${BuildConfig.VERSION_NAME}) y modelo del equipo para validar el reporte.",
                    fontSize = 10.sp,
                    color = textSecondary
                )

                fallbackErrorMessage?.let { msg ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x1FF59E0B))
                            .border(1.dp, Color(0x4DF59E0B), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = {
                                val opened = FeedbackReporter.openEmailFallback(context, buildSubmission())
                                if (opened) {
                                    onDismiss()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "No hay app de correo instalada (soporte@tasave.net)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x66F59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enviar por correo a soporte@tasave.net",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (description.trim().length < 5) {
                            Toast.makeText(
                                context,
                                "Cuéntanos un poco más (mínimo 5 caracteres).",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        isSending = true
                        fallbackErrorMessage = null
                        val submission = buildSubmission()
                        coroutineScope.launch {
                            when (val result = FeedbackReporter.submitFeedback(context, submission)) {
                                is FeedbackSubmitResult.SentToSheet -> {
                                    isSending = false
                                    Toast.makeText(
                                        context,
                                        "¡Recibido! Gracias por ayudarnos a mejorar Tasa VE.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onDismiss()
                                }
                                is FeedbackSubmitResult.WebhookNotConfigured -> {
                                    isSending = false
                                    val opened = FeedbackReporter.openEmailFallback(context, submission)
                                    if (opened) {
                                        onDismiss()
                                    } else {
                                        fallbackErrorMessage = "Envío directo pendiente de activación. Escríbenos a soporte@tasave.net."
                                    }
                                }
                                is FeedbackSubmitResult.Error -> {
                                    isSending = false
                                    fallbackErrorMessage = result.message
                                }
                            }
                        }
                    },
                    enabled = !isSending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.Black
                    )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviando...", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enviar reporte / sugerencia", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalCenterDialog(
    isDark: Boolean,
    initialTab: Int,
    onOpenWeb: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var expandedIndex by remember(selectedTab) { mutableStateOf(0) }

    val dialogBg = if (isDark) Color(0xFF0E0E11) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val borderCol = if (isDark) Color(0x2694A3B8) else Color(0x1F475569)

    val privacySections = remember {
        listOf(
            "1. Origen y actualización de los datos" to
                "Las cotizaciones mostradas (Dólar BCV hoy/mañana, Euro BCV hoy/mañana, USDT paralelo, Peso Colombiano COP de frontera e históricos diarios) provienen de APIs públicas (ve.dolarapi.com, co.dolarapi.com, open.er-api.com y rates.dolarvzla.com). Tasa VE es una herramienta independiente y NO está afiliada ni autorizada por el BCV, SENIAT, SUDEBAN ni entidad bancaria alguna. Las últimas tasas se almacenan en caché local para el Modo Offline y el Widget.",
            "2. Información y estadísticas anónimas" to
                "Tasa VE NO recopila ni almacena en servidores propios cuentas, contraseñas, correos ni ubicación GPS. El historial de conversiones, carrito de compras, preferencias de IVA/IGTF y perfiles opcionales de cobro (Pago Móvil, Cuentas Bancarias, Zelle o Binance Pay) se guardan únicamente en tu dispositivo. De forma adicional, usamos Google Firebase Analytics únicamente para registrar eventos anónimos de navegación (por ejemplo, qué pestaña o herramienta de Útiles se abre), SIN incluir jamás montos, tasas escritas, nombres, cédulas, teléfonos ni datos bancarios. Puedes desactivar estas estadísticas en cualquier momento desde Configuración > Legal y Privacidad.",
            "3. Uso de la información" to
                "Los datos locales se usan exclusivamente para calcular conversiones (incluyendo COP de frontera), totales de carrito, IVA/IGTF, vuelto en pagos mixtos, herramientas de Útiles (¿Con qué pago? y Dividir cuenta), Calendario SUDEBAN, alertas locales, widgets de inicio y tarjetas visuales PNG temporales en caché (shared_cards).",
            "4. Enlaces externos y compartición" to
                "Al pulsar 'Enviar por WhatsApp', 'Tarjeta (IMG)' o abrir enlaces hacia Google Play o políticas web, Android abre la aplicación externa elegida bajo sus propias políticas. Las imágenes PNG temporales generadas para compartir residen solo en caché local compartido por FileProvider sin subirse a servidores externos.",
            "5. Seguridad de los datos" to
                "Todas las consultas de red y la telemetría anónima usan cifrado HTTPS (SSL/TLS). El escáner OCR y las alertas se ejecutan localmente en el teléfono. Además, los perfiles opcionales de Pago Móvil, cuentas bancarias, Zelle y Binance Pay están excluidos del respaldo automático en la nube de Android. Puedes borrar o desactivar estadísticas en cualquier momento desde Configuración.",
            "6. Publicidad, anuncios bonificados y suscripción Ultra" to
                "El Plan Gratuito incluye el conversor multi-tasa (BCV, Euro, USDT y COP), BCV Mañana, Calendario SUDEBAN, las herramientas de Útiles (¿Con qué pago? y Dividir cuenta), Pago Mixto, Tarjeta PNG, Widget Tasas en Vivo, hasta 5 productos en carrito, 1 perfil de cobro y 3 escaneos diarios (con anuncio bonificado opcional para +5 escaneos o 12 h sin banners). Tasa VE Ultra (\$0.99 USD/mes o \$7.99 USD/año en Google Play) elimina todos los anuncios y desbloquea Escáner OCR ilimitado, Carrito ilimitado + IVA/IGTF, perfiles de cobro ilimitados, marca personalizada en Tarjeta PNG, Alertas de tasa y Widget Calculadora y Cobros Ultra.",
            "7. Permisos del sistema" to
                "• Cámara (CAMERA): Opcional y solo en primer plano para leer precios por OCR en el dispositivo.\n• Notificaciones (POST_NOTIFICATIONS): Opcional en Android 13+ para alertas locales de variación de tasa.\n• Internet y Facturación (INTERNET, BILLING): Para cotizaciones públicas, estadísticas anónimas opt-out, anuncios y suscripción Ultra en Google Play.",
            "8. Uso de tecnologías de terceros" to
                "Integramos SDKs certificados: Google Firebase Analytics (métricas anónimas agregadas de uso de funciones, desactivable en Configuración), Google Mobile Ads (AdMob), Google Play Billing, Google ML Kit Text Recognition (OCR local) y APIs públicas de tasas (DolarAPI, DolarVzla y OpenER).",
            "9. Cambios en esta política" to
                "Podemos actualizar esta política para reflejar mejoras o requisitos legales y de Google Play. La versión vigente corresponde al 22 de noviembre de 2026 (v1.2.0). La app no está dirigida a menores de 13 años (o 16 en el EEE).",
            "10. Contacto" to
                "Responsable: Tasa VE\nCorreo de privacidad y soporte: soporte@tasave.net\nSitio web oficial: https://tasave.net"
        )
    }

    val termsSections = remember {
        listOf(
            "1. Naturaleza y No Afiliación Oficial" to
                "Tasa VE es una calculadora matemática e informativa. NO es banco, casa de cambio, procesador de pagos, billetera digital ni autoridad tributaria, y NO está afiliada al BCV, SENIAT, SUDEBAN, Zelle, Binance ni entidad financiera alguna.",
            "2. Catálogo de Bancos, Útiles, Calendario e Impuestos" to
                "Las plantillas de cobro (Pago Móvil, Cuenta Bancaria, Zelle, Binance), el calendario referencial de feriados SUDEBAN y las herramientas de Útiles (¿Con qué pago? y Dividir cuenta) son estrictamente referenciales. Tasa VE no valida cuentas ni procesa transferencias.",
            "3. Fuentes de Datos y Descargo" to
                "Las tasas se consultan de fuentes públicas (ve.dolarapi.com, co.dolarapi.com, open.er-api.com y rates.dolarvzla.com) y pueden presentar desfases frente a operaciones bancarias en vivo. El desarrollador no es responsable por pérdidas comerciales o decisiones tomadas con base en la app.",
            "4. Plan Gratuito y Suscripción Tasa VE Ultra" to
                "El Plan Gratuito ofrece el conversor (BCV, Euro, USDT, COP), Calendario SUDEBAN, las herramientas de Útiles (¿Con qué pago? y Dividir cuenta), Pago Mixto, Tarjeta PNG y Widget Tasas en Vivo con banners e intersticiales ocasionales. Tasa VE Ultra (\$0.99 USD/mes o \$7.99 USD/año) se renueva automáticamente vía Google Play salvo cancelación al menos 24 h antes desde Google Play > Suscripciones.",
            "5. Privacidad, Reembolsos y Contacto" to
                "Las compras se vinculan a tu cuenta Google y pueden restaurarse desde Configuración. Puedes desactivar el registro anónimo de uso (Firebase Analytics) en Configuración > Legal y Privacidad. Contacto: soporte@tasave.net."
        )
    }

    val activeSections = if (selectedTab == 0) privacySections else termsSections

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.84f)
                .border(1.dp, borderCol, RoundedCornerShape(24.dp)),
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
                    Column {
                        Text(
                            text = "Centro Legal — Tasa VE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Actualizado: 21 nov 2026 • Versión 1.2.0",
                            fontSize = 10.5.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                // Segmented Selector: Privacidad (10) / Términos (5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x2610B981),
                            selectedLabelColor = Color(0xFF10B981)
                        ),
                        label = {
                            Text(
                                text = "Privacidad (10)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x2610B981),
                            selectedLabelColor = Color(0xFF10B981)
                        ),
                        label = {
                            Text(
                                text = "Términos y Descargo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeSections.forEachIndexed { index, (title, body) ->
                        SleekDisclosurePanel(
                            title = title,
                            isExpanded = expandedIndex == index,
                            isDark = isDark,
                            onToggle = {
                                expandedIndex = if (expandedIndex == index) -1 else index
                            },
                            content = body
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenWeb,
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver en Web", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
                    ) {
                        Text("Cerrar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
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
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "chevronRotation"
    )

    val containerBg = if (isExpanded) {
        if (isDark) Color(0x1610B981) else Color(0x1210B981)
    } else {
        if (isDark) Color(0x0FFFFFFF) else Color.White
    }
    val borderColor = if (isExpanded) {
        Color(0x6610B981)
    } else {
        if (isDark) Color(0x1A94A3B8) else Color(0x19475569)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .animateContentSize(
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExpanded) Color(0xFF10B981) else if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Cerrar" else "Abrir",
                tint = if (isExpanded) Color(0xFF10B981) else if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                textAlign = TextAlign.Start
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
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
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

@Composable
private fun BcvCalendarDialog(
    isDark: Boolean,
    snapshots: List<RateSnapshot>,
    currentUsd: Double,
    currentEur: Double,
    currentUsdt: Double,
    onApplyQuickAmount: (amountUsd: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dialogBg = if (isDark) Color(0xFF0E0E12) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF16161C) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val borderCol = if (isDark) Color(0x2694A3B8) else Color(0x1F475569)
    val emerald = Color(0xFF10B981)
    val emeraldLight = Color(0xFF34D399)
    val amber = Color(0xFFF59E0B)

    val todayCal = remember { BcvCalendarHelper.todayCaracasCalendar() }
    val todayIso = remember { BcvCalendarHelper.formatIsoDate(todayCal) }
    var visibleYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var visibleMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }

    val gridCells = remember(visibleYear, visibleMonth) {
        BcvCalendarHelper.buildMonthGrid(visibleYear, visibleMonth)
    }

    var selectedDay by remember(visibleYear, visibleMonth) {
        mutableStateOf(
            gridCells.filterNotNull().firstOrNull { it.isoDate == todayIso }
                ?: gridCells.filterNotNull().first()
        )
    }

    var quickUsdText by remember { mutableStateOf("10") }
    var quickUseUsdt by remember { mutableStateOf(false) }

    val dayLookup = remember(selectedDay, snapshots, currentUsd, currentEur, currentUsdt) {
        BcvCalendarHelper.lookupRateForDate(
            isoDate = selectedDay.isoDate,
            snapshots = snapshots,
            fallbackUsd = currentUsd,
            fallbackEur = currentEur,
            fallbackUsdt = currentUsdt
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .border(1.dp, borderCol, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calendario Bancario y Tasas",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Toca cualquier día para ver feriados SUDEBAN y tasa",
                            fontSize = 10.5.sp,
                            color = textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                // Selector de Mes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (visibleMonth == 0) {
                                visibleMonth = 11
                                visibleYear -= 1
                            } else {
                                visibleMonth -= 1
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = textPrimary)
                    }
                    Text(
                        text = BcvCalendarHelper.formatMonthYearEs(visibleYear, visibleMonth),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(
                        onClick = {
                            if (visibleMonth == 11) {
                                visibleMonth = 0
                                visibleYear += 1
                            } else {
                                visibleMonth += 1
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = textPrimary)
                    }
                }

                // Cabecera Lun-Dom
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEach { d ->
                        Text(
                            text = d,
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Cuadrícula del mes
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    gridCells.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 0 until 7) {
                                val cell = week.getOrNull(i)
                                if (cell == null) {
                                    Spacer(modifier = Modifier.weight(1f).height(38.dp))
                                } else {
                                    val isSelected = selectedDay.isoDate == cell.isoDate
                                    val isToday = cell.isoDate == todayIso
                                    val cellBg = when {
                                        isSelected -> emerald.copy(alpha = 0.24f)
                                        cell.isBankHoliday -> amber.copy(alpha = 0.16f)
                                        cell.isWeekend -> if (isDark) Color(0x0D94A3B8) else Color(0x0F475569)
                                        else -> cardBg
                                    }
                                    val cellBorder = when {
                                        isSelected -> emerald
                                        cell.isBankHoliday -> amber.copy(alpha = 0.65f)
                                        isToday -> emeraldLight.copy(alpha = 0.55f)
                                        else -> borderCol
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cellBg)
                                            .border(if (isSelected) 1.5.dp else 1.dp, cellBorder, RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedDay = cell
                                                com.julcer26.tasave.analytics.AnalyticsTracker.logCalendarDaySelected(
                                                    context,
                                                    cell.isBankHoliday
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${cell.dayOfMonth}",
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected || isToday || cell.isBankHoliday) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    cell.isBankHoliday -> amber
                                                    isSelected -> emeraldLight
                                                    else -> textPrimary
                                                }
                                            )
                                            if (cell.isBankHoliday || isToday) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (cell.isBankHoliday) amber else emeraldLight)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Detalle del día seleccionado
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDay.isoDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = if (dayLookup.isExactSnapshot) "Histórico exacto" else "Ref. más cercana",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedDay.isBankHoliday) amber else emeraldLight
                        )
                    }
                    Text(
                        text = selectedDay.statusSummary,
                        fontSize = 11.sp,
                        fontWeight = if (selectedDay.isBankHoliday) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedDay.isBankHoliday) amber else textSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("USD BCV", dayLookup.usdBcv, emeraldLight),
                            Triple("EUR BCV", dayLookup.eurBcv, Color(0xFF60A5FA)),
                            Triple("USDT Bin.", dayLookup.usdt, amber)
                        ).forEach { (label, rateVal, accent) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1F2028) else Color(0xFFF1F5F9))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(label, fontSize = 9.sp, color = textSecondary)
                                Text(
                                    text = "Bs. ${String.format(Locale.US, "%.2f", rateVal)}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accent
                                )
                            }
                        }
                    }

                    // Cálculo rápido en el día elegido
                    val quickAmt = quickUsdText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val chosenRate = if (quickUseUsdt) dayLookup.usdt else dayLookup.usdBcv
                    val quickBs = quickAmt * chosenRate

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickUsdText,
                            onValueChange = { if (it.length <= 8) quickUsdText = it },
                            label = { Text("Cálculo rápido ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (quickUseUsdt) "A tasa USDT:" else "A tasa BCV:",
                                fontSize = 10.sp,
                                color = textSecondary,
                                modifier = Modifier.clickable { quickUseUsdt = !quickUseUsdt }
                            )
                            Text(
                                text = "Bs. ${String.format(Locale.US, "%,.2f", quickBs)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = emeraldLight
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { quickUseUsdt = !quickUseUsdt },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (quickUseUsdt) "Ver en BCV" else "Ver en USDT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { onApplyQuickAmount(quickUsdText) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = emerald, contentColor = Color.Black)
                        ) {
                            Text("Pasar al conversor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}



