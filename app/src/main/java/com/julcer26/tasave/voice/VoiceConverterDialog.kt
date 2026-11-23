package com.julcer26.tasave.voice

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.julcer26.tasave.data.LocalStorageHelper
import com.julcer26.tasave.data.RateInfo
import java.util.Locale

private enum class VoiceUiStatus {
    IDLE,
    LISTENING,
    RESULT,
    ERROR
}

@Composable
fun VoiceConverterDialog(
    isDark: Boolean,
    isUltraUser: Boolean,
    remainingFreeVoiceUses: Int,
    ttsEnabled: Boolean,
    usdBcv: RateInfo?,
    eurBcv: RateInfo?,
    usdtParalelo: RateInfo?,
    nextUsdBcv: RateInfo?,
    nextEurBcv: RateInfo?,
    copPerUsd: Double,
    onToggleTts: (Boolean) -> Unit,
    onApplyVoiceConversion: (ParsedVoiceConversion) -> Unit,
    onUpgradeUltra: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val canUseVoice = isUltraUser || remainingFreeVoiceUses > 0

    var status by remember { mutableStateOf(VoiceUiStatus.IDLE) }
    var partialText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastParsed by remember { mutableStateOf<ParsedVoiceConversion?>(null) }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val latestTtsEnabled by rememberUpdatedState(ttsEnabled)
    val latestApply by rememberUpdatedState(onApplyVoiceConversion)
    val latestCanUse by rememberUpdatedState(canUseVoice)

    val speechAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    // TextToSpeech lifecycle
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { initStatus ->
            if (initStatus == TextToSpeech.SUCCESS) {
                val res = engine?.setLanguage(Locale("es", "VE"))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine?.setLanguage(Locale("es", "ES"))
                }
                ttsReady = true
            }
        }
        engine
    }

    val speechRecognizer = remember {
        if (speechAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    fun resolveRateFor(parsed: ParsedVoiceConversion): Triple<Double, String, Boolean> {
        val hasTomorrow = nextUsdBcv != null && nextUsdBcv.rate > 0.0
        val effectiveTomorrow = parsed.useTomorrowBcv && hasTomorrow && parsed.rateKey != "USDT"
        val usdVal = if (effectiveTomorrow) (nextUsdBcv?.rate ?: usdBcv?.rate ?: 0.0) else (usdBcv?.rate ?: 0.0)
        val eurVal = if (effectiveTomorrow) (nextEurBcv?.rate ?: eurBcv?.rate ?: 0.0) else (eurBcv?.rate ?: 0.0)
        val usdtVal = usdtParalelo?.rate ?: 0.0
        val copRateVes = if (usdVal > 0.0 && copPerUsd > 0.0) usdVal / copPerUsd else 0.0
        val tomorrowTag = if (effectiveTomorrow) " Mañana" else ""

        return when (parsed.rateKey) {
            "EUR_BCV" -> Triple(eurVal, "EUR BCV$tomorrowTag", effectiveTomorrow)
            "USDT" -> Triple(usdtVal, "USDT Binance", false)
            "COP" -> Triple(copRateVes, "COP BCV$tomorrowTag", effectiveTomorrow)
            else -> Triple(usdVal, "USD BCV$tomorrowTag", effectiveTomorrow)
        }
    }

    fun speakIfEnabled(parsed: ParsedVoiceConversion) {
        if (!latestTtsEnabled || !ttsReady) return
        val (rate, _, effectiveTomorrow) = resolveRateFor(parsed)
        if (rate <= 0.0) return
        val converted = if (parsed.inputIsVes) parsed.amount / rate else parsed.amount * rate
        val spoken = VoiceCommandParser.buildSpokenResult(parsed, converted, effectiveTomorrow)
        runCatching {
            tts?.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "tasave_voice_result")
        }
    }

    fun startListening() {
        if (!latestCanUse) return
        if (!speechAvailable || speechRecognizer == null) {
            status = VoiceUiStatus.ERROR
            errorMessage = "Tu dispositivo no tiene reconocimiento de voz de Google activo."
            return
        }
        runCatching { tts?.stop() }
        partialText = ""
        errorMessage = null
        status = VoiceUiStatus.LISTENING

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-VE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-419")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching {
            speechRecognizer.startListening(recognizerIntent)
        }.onFailure {
            status = VoiceUiStatus.ERROR
            errorMessage = "No se pudo iniciar el micrófono. Intenta de nuevo."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            startListening()
        } else {
            status = VoiceUiStatus.ERROR
            errorMessage = "Permiso de micrófono denegado. Actívalo para dictar montos."
        }
    }

    fun requestOrStartListening() {
        if (!latestCanUse) return
        if (hasAudioPermission) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                status = VoiceUiStatus.LISTENING
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                status = VoiceUiStatus.ERROR
                errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "No escuché un monto claro. Toca el micrófono e intenta de nuevo."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        "Falta el permiso de micrófono para dictar."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Sin conexión con el servicio de voz."
                    else -> "No se pudo completar el dictado. Intenta nuevamente."
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val parsed = matches.firstNotNullOfOrNull { VoiceCommandParser.parse(it) }
                if (parsed != null) {
                    lastParsed = parsed
                    partialText = parsed.rawTranscript
                    status = VoiceUiStatus.RESULT
                    latestApply(parsed)
                    speakIfEnabled(parsed)
                } else {
                    partialText = matches.firstOrNull().orEmpty()
                    status = VoiceUiStatus.ERROR
                    errorMessage = "No detecté un monto en \"${partialText.ifBlank { "..." }}\". Prueba con \"38 con 50 dólares a BCV\"."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val live = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!live.isNullOrBlank()) {
                    partialText = live
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        onDispose {
            runCatching { speechRecognizer?.cancel() }
            runCatching { speechRecognizer?.destroy() }
            runCatching { tts?.stop() }
            runCatching { tts?.shutdown() }
        }
    }

    // Auto-start listening when dialog opens if quota & permission are already granted
    LaunchedEffect(Unit) {
        if (canUseVoice && speechAvailable) {
            if (hasAudioPermission) {
                startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val dialogBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val surfaceBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val emerald = Color(0xFF10B981)
    val emeraldText = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
    val amber = Color(0xFFF59E0B)
    val borderCol = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)

    val pulseTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 390.dp),
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            border = BorderStroke(1.dp, emerald.copy(alpha = 0.35f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Conversor por Voz",
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isUltraUser) {
                                "Tasa VE Ultra • Ilimitado"
                            } else {
                                "Pruebas gratis hoy: $remainingFreeVoiceUses de ${LocalStorageHelper.MAX_FREE_VOICE_PER_DAY}"
                            },
                            color = if (isUltraUser) amber else emeraldText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(surfaceBg)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (!canUseVoice && lastParsed == null) {
                    // Daily Free Limit Reached
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(amber.copy(alpha = 0.12f))
                            .border(1.dp, amber.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = amber,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "Límite diario alcanzado",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ya usaste tus ${LocalStorageHelper.MAX_FREE_VOICE_PER_DAY} pruebas gratis de hoy. Activa Tasa VE Ultra para dictar sin límites.",
                            color = textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onUpgradeUltra,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = amber,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Activar Tasa VE Ultra", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Mic Button & Listening State
                    val isListening = status == VoiceUiStatus.LISTENING
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(if (isListening) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Color(0xFFEF4444) else emerald
                            )
                            .clickable {
                                if (isListening) {
                                    runCatching { speechRecognizer?.stopListening() }
                                    status = VoiceUiStatus.IDLE
                                } else {
                                    requestOrStartListening()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isListening) "Detener" else "Dictar monto",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = when (status) {
                            VoiceUiStatus.LISTENING -> "Escuchando..."
                            VoiceUiStatus.RESULT -> "Dictado aplicado a la calculadora"
                            VoiceUiStatus.ERROR -> errorMessage ?: "Intenta nuevamente"
                            VoiceUiStatus.IDLE -> "Toca el micrófono para dictar"
                        },
                        color = if (status == VoiceUiStatus.ERROR) Color(0xFFF87171) else textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    if (partialText.isNotBlank()) {
                        Text(
                            text = "\"$partialText\"",
                            color = textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    } else if (status == VoiceUiStatus.IDLE || status == VoiceUiStatus.LISTENING) {
                        Text(
                            text = "Ej: \"38 con 50 dólares a BCV\" o \"15 mil bolívares a USDT\"",
                            color = textSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Parsed Result Card
                    val parsed = lastParsed
                    if (parsed != null) {
                        val (rate, rateLabel, _) = resolveRateFor(parsed)
                        val converted = when {
                            rate <= 0.0 -> 0.0
                            parsed.inputIsVes -> parsed.amount / rate
                            else -> parsed.amount * rate
                        }
                        val unitSymbol = when (parsed.rateKey) {
                            "EUR_BCV" -> "€"
                            "USDT" -> "USDT"
                            "COP" -> "COP"
                            else -> "$"
                        }
                        val fromText = if (parsed.inputIsVes) {
                            "Bs. ${String.format(Locale.US, "%,.2f", parsed.amount)}"
                        } else if (parsed.rateKey == "COP") {
                            "${String.format(Locale.US, "%,.0f", parsed.amount)} COP"
                        } else {
                            "$unitSymbol ${String.format(Locale.US, "%,.2f", parsed.amount)}"
                        }
                        val toText = if (parsed.inputIsVes) {
                            if (parsed.rateKey == "COP") {
                                "${String.format(Locale.US, "%,.0f", converted)} COP"
                            } else {
                                "$unitSymbol ${String.format(Locale.US, "%,.2f", converted)}"
                            }
                        } else {
                            "Bs. ${String.format(Locale.US, "%,.2f", converted)}"
                        }
                        val copyLine = "$fromText = $toText ($rateLabel)"

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(surfaceBg)
                                .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = rateLabel,
                                color = emeraldText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = fromText,
                                color = textSecondary,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = toText,
                                color = textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Conversión Tasa VE", copyLine))
                                        Toast.makeText(context, "Resultado copiado", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = emerald,
                                        contentColor = Color(0xFF042F2E)
                                    )
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copiar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                if (canUseVoice) {
                                    OutlinedButton(
                                        onClick = { requestOrStartListening() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, borderCol)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = textPrimary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Otro", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = onUpgradeUltra,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, amber)
                                    ) {
                                        Text("Ultra ∞", color = amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // TTS Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(surfaceBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = emeraldText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Leer resultado en voz alta",
                                color = textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = onToggleTts,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = emerald
                            )
                        )
                    }
                }
            }
        }
    }
}
