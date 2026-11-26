package com.julcer26.tasave.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.util.Log
import android.util.Size as AndroidSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.julcer26.tasave.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.julcer26.tasave.data.RateInfo
import com.julcer26.tasave.ui.VeNumberFormat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

@OptIn(ExperimentalGetImage::class)
@Composable
fun PriceScannerDialog(
    isUltraUser: Boolean,
    remainingFreeScans: Int,
    ultraPrice: String,
    selectedRateKey: String,
    usdBcv: RateInfo?,
    eurBcv: RateInfo?,
    usdtParalelo: RateInfo?,
    isRewardedAdLoading: Boolean = false,
    onWatchRewardedAd: () -> Unit = {},
    onSelectRate: (String) -> Unit,
    onApplyPrice: (amount: Double, isVes: Boolean, rateKey: String) -> Unit,
    onUpgradeUltra: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val canScan = isUltraUser || remainingFreeScans > 0

    LaunchedEffect(canScan) {
        if (canScan && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var activeRateKey by remember(selectedRateKey) {
        mutableStateOf(
            if (selectedRateKey in listOf("USD_BCV", "EUR_BCV", "USDT")) selectedRateKey else "USD_BCV"
        )
    }
    var detectedPrices by remember { mutableStateOf<List<DetectedPrice>>(emptyList()) }
    var selectedPrice by remember { mutableStateOf<DetectedPrice?>(null) }
    var isUserPinned by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var manualIsVesOverride by remember { mutableStateOf<Boolean?>(null) }
    val stabilizer = remember { PriceScannerStabilizer() }

    // Bounds for mapping the targeting reticle strictly to ML Kit image coordinates
    var previewBounds by remember { mutableStateOf<Rect?>(null) }
    var reticleBounds by remember { mutableStateOf<Rect?>(null) }

    val activeRateInfo = when (activeRateKey) {
        "USD_BCV" -> usdBcv
        "EUR_BCV" -> eurBcv
        "USDT" -> usdtParalelo
        else -> usdBcv
    }

    val effectiveIsVes = manualIsVesOverride ?: when (selectedPrice?.currencyHint) {
        CurrencyHint.VES -> true
        CurrencyHint.USD, CurrencyHint.EUR -> false
        else -> false
    }

    val emerald = Color(0xFF10B981)
    val emeraldLight = Color(0xFF34D399)

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0B))
    ) {
            if (!canScan) {
                // Freemium Paywall + Opt-In Rewarded Ad View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0x2610B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_price_scanner_123),
                            contentDescription = null,
                            tint = emeraldLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Límite diario alcanzado",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ya usaste tus escaneos gratuitos de hoy. Mira un anuncio corto para desbloquear +5 escaneos hoy, o pásate a Tasa VE Ultra ($ultraPrice) para escáner ilimitado y cero anuncios.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = onWatchRewardedAd,
                        enabled = !isRewardedAdLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = emerald,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        if (isRewardedAdLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cargando anuncio...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(19.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ver anuncio corto (+5 escaneos hoy)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onUpgradeUltra,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFBBF24)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Activar Tasa VE Ultra ∞",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Volver a la calculadora", color = Color(0xFF94A3B8))
                    }
                }
            } else if (!hasCameraPermission) {
                // Camera Permission Explanation View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0x2610B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_price_scanner_123),
                            contentDescription = null,
                            tint = emeraldLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permiso de Cámara Necesario",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Para leer precios en etiquetas y pantallas en tiempo real, Tasa VE necesita acceso a la cámara. El análisis ocurre 100% en tu dispositivo.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = emerald,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text("Permitir cámara", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color(0xFF94A3B8))
                    }
                }
            } else {
                // Active CameraX Preview + ML Kit OCR (strictly inside reticle, off-main-thread pipeline)
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
                val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
                var cameraRef by remember { mutableStateOf<Camera?>(null) }
                var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
                var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
                val lastAnalyzedAt = remember { AtomicLong(0L) }
                val isProcessingFrame = remember { AtomicBoolean(false) }
                val isDisposed = remember { AtomicBoolean(false) }
                val lastEmittedStateRef = remember { java.util.concurrent.atomic.AtomicReference<StabilizedScanState?>(null) }
                val isPausedState by rememberUpdatedState(isPaused)
                val previewBoundsState by rememberUpdatedState(previewBounds)
                val reticleBoundsState by rememberUpdatedState(reticleBounds)

                val triggerReticleFocus: () -> Unit = {
                    val cam = cameraRef
                    val pv = previewViewRef
                    val pB = previewBounds
                    val rB = reticleBounds
                    if (cam != null && pv != null && pB != null && rB != null) {
                        runCatching {
                            val cx = (rB.center.x - pB.left).coerceIn(0f, pB.width)
                            val cy = (rB.center.y - pB.top).coerceIn(0f, pB.height)
                            val point = pv.meteringPointFactory.createPoint(cx, cy)
                            val action = FocusMeteringAction.Builder(
                                point,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                            cam.cameraControl.startFocusAndMetering(action)
                        }
                    }
                }

                LaunchedEffect(cameraRef, reticleBounds != null, previewBounds != null) {
                    if (cameraRef != null && reticleBounds != null && previewBounds != null) {
                        triggerReticleFocus()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        isDisposed.set(true)
                        try {
                            cameraProviderRef?.unbindAll()
                        } catch (_: Exception) {
                        }
                        runCatching { textRecognizer.close() }
                        cameraExecutor.shutdown()
                    }
                }

                LaunchedEffect(isTorchOn, cameraRef) {
                    if (hasFlash) {
                        cameraRef?.cameraControl?.enableTorch(isTorchOn)
                    }
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            val b = coords.boundsInRoot()
                            if (previewBounds != b) previewBounds = b
                        },
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        previewViewRef = previewView
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            if (isDisposed.get()) return@addListener
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProviderRef = cameraProvider

                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                // Cap analysis stream to crisp 720p so ML Kit OCR stays fast (~60-90ms) without lag
                                val analysisResolution = ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                        ResolutionStrategy(
                                            AndroidSize(1280, 720),
                                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                        )
                                    )
                                    .build()

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setResolutionSelector(analysisResolution)
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val now = System.currentTimeMillis()
                                    if (isDisposed.get() ||
                                        isPausedState ||
                                        now - lastAnalyzedAt.get() < 200L ||
                                        !isProcessingFrame.compareAndSet(false, true)
                                    ) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }

                                    val mediaImage = imageProxy.image
                                    if (mediaImage == null) {
                                        isProcessingFrame.set(false)
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }

                                    lastAnalyzedAt.set(now)
                                    val rotation = imageProxy.imageInfo.rotationDegrees
                                    val rotatedImgW = if (rotation == 90 || rotation == 270) {
                                        imageProxy.height.toFloat()
                                    } else {
                                        imageProxy.width.toFloat()
                                    }
                                    val rotatedImgH = if (rotation == 90 || rotation == 270) {
                                        imageProxy.width.toFloat()
                                    } else {
                                        imageProxy.height.toFloat()
                                    }

                                    val roiInImage = computeReticleRectInImage(
                                        previewBounds = previewBoundsState,
                                        reticleBounds = reticleBoundsState,
                                        imgWidth = rotatedImgW,
                                        imgHeight = rotatedImgH
                                    )

                                    val image = InputImage.fromMediaImage(mediaImage, rotation)

                                    textRecognizer.process(image)
                                        .addOnSuccessListener(cameraExecutor) { visionText ->
                                            if (isDisposed.get() || isPausedState) return@addOnSuccessListener
                                            val textInsideBox = if (roiInImage != null) {
                                                val padX = roiInImage.width() * 0.06f
                                                val padY = roiInImage.height() * 0.08f
                                                val expandedRoi = RectF(
                                                    (roiInImage.left - padX).coerceAtLeast(0f),
                                                    (roiInImage.top - padY).coerceAtLeast(0f),
                                                    (roiInImage.right + padX).coerceAtMost(rotatedImgW),
                                                    (roiInImage.bottom + padY).coerceAtMost(rotatedImgH)
                                                )
                                                val matchingLines = visionText.textBlocks
                                                    .flatMap { it.lines }
                                                    .filter { line ->
                                                        val box = line.boundingBox ?: return@filter false
                                                        expandedRoi.contains(box.exactCenterX(), box.exactCenterY())
                                                    }
                                                val maxLineH = matchingLines.maxOfOrNull { it.boundingBox?.height() ?: 0 } ?: 0
                                                // Prioritize visually larger price text over fine print (weights, SKUs)
                                                val orderedLines = if (maxLineH > 0) {
                                                    matchingLines.sortedByDescending { line ->
                                                        val h = line.boundingBox?.height() ?: 0
                                                        when {
                                                            h >= maxLineH * 0.72f -> 2
                                                            h >= maxLineH * 0.48f -> 1
                                                            else -> 0
                                                        }
                                                    }
                                                } else {
                                                    matchingLines
                                                }
                                                orderedLines.joinToString("\n") { it.text }
                                            } else {
                                                visionText.text
                                            }

                                            val extracted = PriceScannerParser.extractPrices(textInsideBox)
                                            val state = stabilizer.onFrame(extracted, System.currentTimeMillis())
                                            // Only push to Compose main thread when the stabilized result actually changes
                                            if (lastEmittedStateRef.getAndSet(state) != state) {
                                                mainExecutor.execute {
                                                    if (!isDisposed.get() && !isPausedState) {
                                                        detectedPrices = state.detectedPrices
                                                        selectedPrice = state.selectedPrice
                                                        isUserPinned = state.isUserPinned
                                                    }
                                                }
                                            }
                                        }
                                        .addOnFailureListener(cameraExecutor) { e ->
                                            Log.w("PriceScanner", "ML Kit error: ${e.message}")
                                        }
                                        .addOnCompleteListener(cameraExecutor) {
                                            isProcessingFrame.set(false)
                                            imageProxy.close()
                                        }
                                }

                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                cameraRef = camera
                                hasFlash = camera.cameraInfo.hasFlashUnit()
                            } catch (e: Exception) {
                                Log.e("PriceScanner", "Camera bind failed", e)
                            }
                        }, mainExecutor)

                        previewView
                    }
                )

                // Lightweight 4-band dark scrim around the Reticle (zero offscreen GPU FBO / BlendMode.Clear stutter)
                val pBounds = previewBounds
                val rBounds = reticleBounds
                if (pBounds != null && rBounds != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val scrim = Color(0x77000000)
                        val left = (rBounds.left - pBounds.left).coerceIn(0f, size.width)
                        val top = (rBounds.top - pBounds.top).coerceIn(0f, size.height)
                        val right = (left + rBounds.width).coerceIn(left, size.width)
                        val bottom = (top + rBounds.height).coerceIn(top, size.height)
                        if (top > 0f) drawRect(color = scrim, topLeft = Offset.Zero, size = Size(size.width, top))
                        if (bottom < size.height) drawRect(color = scrim, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
                        if (left > 0f) drawRect(color = scrim, topLeft = Offset(0f, top), size = Size(left, bottom - top))
                        if (right < size.width) drawRect(color = scrim, topLeft = Offset(right, top), size = Size(size.width - right, bottom - top))
                    }
                }

                // Top Overlay Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xCC0A0A0B), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar escáner",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Escáner de Precios",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(
                            modifier = Modifier.padding(top = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isUltraUser) Color(0x33F59E0B) else Color(0x3310B981)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isUltraUser) "ULTRA • Ilimitado" else "$remainingFreeScans gratis hoy",
                                    color = if (isUltraUser) Color(0xFFFBBF24) else emeraldLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!isUltraUser && remainingFreeScans <= 1) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x33F59E0B))
                                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isRewardedAdLoading) { onWatchRewardedAd() }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isRewardedAdLoading) "Cargando..." else "+5 Ad",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    if (hasFlash) {
                        IconButton(
                            onClick = { isTorchOn = !isTorchOn },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isTorchOn) emerald else Color(0x88000000))
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Linterna",
                                tint = if (isTorchOn) Color.Black else Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                // Center Targeting Reticle (positioned in the upper-middle camera area; tap to autofocus)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 120.dp)
                        .width(270.dp)
                        .height(110.dp)
                        .onGloballyPositioned { coords ->
                            val b = coords.boundsInRoot()
                            if (reticleBounds != b) reticleBounds = b
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            triggerReticleFocus()
                        }
                        .border(
                            width = 2.dp,
                            color = when {
                                isUserPinned -> Color(0xFFF59E0B)
                                selectedPrice != null -> emerald
                                else -> Color(0xCCFFFFFF)
                            },
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = when {
                            isPaused -> "Lectura pausada"
                            isUserPinned -> "Precio fijado • Toca otro o reanuda"
                            selectedPrice != null -> "Precio detectado • Toca para enfocar"
                            else -> "Enfoca el precio aquí"
                        },
                        color = if (isUserPinned) Color(0xFFFBBF24) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x99000000))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                // Bottom Conversion & Controls Card (adapts dynamically to 3-button nav bar, gesture bar, or no nav bar)
                val view = LocalView.current
                val density = LocalDensity.current
                val composeNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val rootNavBottom = with(density) {
                    val insets = ViewCompat.getRootWindowInsets(view)
                    val px = insets?.getInsets(
                        WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
                    )?.bottom ?: 0
                    px.toDp()
                }
                val dynamicBottomPadding = maxOf(composeNavBottom, rootNavBottom) + 16.dp

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color(0xF5121216))
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = dynamicBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rate Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val rates = listOf(
                            Triple("USD_BCV", "USD BCV", usdBcv?.rate),
                            Triple("EUR_BCV", "EUR BCV", eurBcv?.rate),
                            Triple("USDT", "USDT", usdtParalelo?.rate)
                        )
                        rates.forEach { (key, label, rateVal) ->
                            val selected = activeRateKey == key
                            val decimals = if (key == "USD_BCV" || key == "EUR_BCV") 4 else 2
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) Color(0x2610B981) else Color(0xFF1E1E24))
                                    .border(
                                        1.dp,
                                        if (selected) emerald else Color(0x22FFFFFF),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        activeRateKey = key
                                        onSelectRate(key)
                                    }
                                    .padding(vertical = 7.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) emeraldLight else Color.White
                                    )
                                    Text(
                                        text = if (rateVal != null) "Bs. ${VeNumberFormat.formatRate(rateVal, decimals)}" else "--",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    // Detected Prices Strip (only prices inside the reticle; tapping pins the choice)
                    val currencySymbol = activeRateInfo?.symbol ?: "$"
                    if (detectedPrices.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            detectedPrices.take(6).forEach { candidate ->
                                val isSelected = selectedPrice?.amount == candidate.amount &&
                                    selectedPrice?.currencyHint == candidate.currencyHint
                                val chipLabel = when (candidate.currencyHint) {
                                    CurrencyHint.VES -> "Bs. ${VeNumberFormat.formatMoney(candidate.amount)}"
                                    CurrencyHint.EUR -> "€ ${VeNumberFormat.formatMoney(candidate.amount)}"
                                    CurrencyHint.USD -> "$ ${VeNumberFormat.formatMoney(candidate.amount)}"
                                    CurrencyHint.UNKNOWN -> {
                                        val sym = if (effectiveIsVes) "Bs." else currencySymbol
                                        "$sym ${VeNumberFormat.formatMoney(candidate.amount)}"
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected && isUserPinned -> Color(0xFFF59E0B)
                                                isSelected -> emerald
                                                else -> Color(0xFF1E1E24)
                                            }
                                        )
                                        .clickable {
                                            if (isSelected && isUserPinned) {
                                                stabilizer.clearPin()
                                                isUserPinned = false
                                            } else {
                                                stabilizer.pinPrice(candidate)
                                                selectedPrice = candidate
                                                isUserPinned = true
                                                manualIsVesOverride = null
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = chipLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Live Conversion Result Card (Stacked cleanly so amounts never overlap)
                    val current = selectedPrice
                    val rateValue = activeRateInfo?.rate ?: 0.0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1A20))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (current == null) {
                            Text(
                                text = "Enfoca un precio dentro del recuadro...",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        } else {
                            val converted = if (rateValue > 0) {
                                if (effectiveIsVes) current.amount / rateValue else current.amount * rateValue
                            } else 0.0

                            val detectedStr = if (effectiveIsVes) {
                                "Bs. ${VeNumberFormat.formatMoney(current.amount)}"
                            } else {
                                "$currencySymbol ${VeNumberFormat.formatMoney(current.amount)}"
                            }
                            val convertedStr = if (effectiveIsVes) {
                                "$currencySymbol ${VeNumberFormat.formatMoney(converted)}"
                            } else {
                                "Bs. ${VeNumberFormat.formatMoney(converted)}"
                            }

                            // Row 1: Detected Input + Swap Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = if (effectiveIsVes) "Detectado (Bolívares)" else "Detectado (Divisa)",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = detectedStr,
                                        fontSize = if (detectedStr.length > 15) 14.5.sp else 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x1FFFFFFF))
                                        .clickable { manualIsVesOverride = !effectiveIsVes }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Cambiar moneda",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (effectiveIsVes) "Bs → $currencySymbol" else "$currencySymbol → Bs",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0x18FFFFFF))

                            // Row 2: Converted Output (full width, clean spacing)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (effectiveIsVes) "Equivalente" else "Total en Bs.",
                                    fontSize = 11.sp,
                                    color = emeraldLight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                val convertedFontSize = when {
                                    convertedStr.length <= 14 -> 20.sp
                                    convertedStr.length <= 18 -> 17.sp
                                    else -> 14.5.sp
                                }
                                Text(
                                    text = convertedStr,
                                    fontSize = convertedFontSize,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = emeraldLight,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isUserPinned && !isPaused) {
                                    stabilizer.clearPin()
                                    isUserPinned = false
                                } else {
                                    val nextPaused = !isPaused
                                    isPaused = nextPaused
                                    if (!nextPaused) {
                                        stabilizer.clearPin()
                                        isUserPinned = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused || isUserPinned) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isPaused -> "Reanudar"
                                    isUserPinned -> "Reanudar"
                                    else -> "Pausar"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                val price = selectedPrice ?: return@Button
                                onApplyPrice(price.amount, effectiveIsVes, activeRateKey)
                                onDismiss()
                            },
                            enabled = selectedPrice != null,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emerald,
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0xFF1E293B),
                                disabledContentColor = Color(0xFF64748B)
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Usar precio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

private fun computeReticleRectInImage(
    previewBounds: Rect?,
    reticleBounds: Rect?,
    imgWidth: Float,
    imgHeight: Float
): RectF? {
    if (previewBounds == null || reticleBounds == null) return null
    if (previewBounds.width <= 0f || previewBounds.height <= 0f || imgWidth <= 0f || imgHeight <= 0f) {
        return null
    }

    val viewW = previewBounds.width
    val viewH = previewBounds.height

    // PreviewView uses FILL_CENTER (center-crop)
    val scale = max(viewW / imgWidth, viewH / imgHeight)
    val scaledW = imgWidth * scale
    val scaledH = imgHeight * scale
    val offsetX = (scaledW - viewW) / 2f
    val offsetY = (scaledH - viewH) / 2f

    val relLeft = reticleBounds.left - previewBounds.left
    val relTop = reticleBounds.top - previewBounds.top
    val relRight = reticleBounds.right - previewBounds.left
    val relBottom = reticleBounds.bottom - previewBounds.top

    return RectF(
        ((relLeft + offsetX) / scale).coerceIn(0f, imgWidth),
        ((relTop + offsetY) / scale).coerceIn(0f, imgHeight),
        ((relRight + offsetX) / scale).coerceIn(0f, imgWidth),
        ((relBottom + offsetY) / scale).coerceIn(0f, imgHeight)
    )
}
