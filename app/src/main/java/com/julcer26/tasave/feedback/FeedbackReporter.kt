package com.julcer26.tasave.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.julcer26.tasave.BuildConfig
import com.julcer26.tasave.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

enum class FeedbackType(val label: String) {
    BUG("Error"),
    SUGGESTION("Sugerencia")
}

data class FeedbackSubmission(
    val type: FeedbackType,
    val module: String,
    val description: String,
    val contact: String = "",
    val appVersion: String = BuildConfig.VERSION_NAME,
    val planLabel: String = "Gratis",
    val deviceSummary: String = FeedbackReporter.defaultDeviceSummary()
)

sealed class FeedbackSubmitResult {
    data object SentToSheet : FeedbackSubmitResult()
    data object WebhookNotConfigured : FeedbackSubmitResult()
    data class Error(val message: String) : FeedbackSubmitResult()
}

object FeedbackReporter {

    val MODULES = listOf(
        "Conversor",
        "Escáner OCR",
        "Carrito",
        "Extras / Widgets",
        "Perfiles / Instrucciones de pago",
        "Otro"
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun defaultDeviceSummary(): String {
        val brand = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val release = Build.VERSION.RELEASE.orEmpty().trim()
        val name = listOf(brand, model)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .ifBlank { "Android" }
        return if (release.isNotBlank()) "$name (Android $release)" else name
    }

    internal fun formatCaracasTimestamp(nowMs: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("America/Caracas")
        return sdf.format(Date(nowMs))
    }

    private fun escapeJson(raw: String): String = buildString(raw.length + 8) {
        for (ch in raw) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) {
                    append(String.format(Locale.US, "\\u%04x", ch.code))
                } else {
                    append(ch)
                }
            }
        }
    }

    internal fun buildJsonPayload(
        submission: FeedbackSubmission,
        timestamp: String = formatCaracasTimestamp()
    ): String {
        val fields = listOf(
            "timestamp" to timestamp,
            "type" to submission.type.label,
            "module" to submission.module.trim(),
            "description" to submission.description.trim(),
            "contact" to submission.contact.trim(),
            "appVersion" to submission.appVersion.trim(),
            "plan" to submission.planLabel.trim(),
            "device" to submission.deviceSummary.trim(),
            "status" to "Pendiente"
        )
        return fields.joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}"
        ) { (k, v) -> "\"${escapeJson(k)}\":\"${escapeJson(v)}\"" }
    }

    internal fun buildFallbackEmailBody(submission: FeedbackSubmission): String {
        return buildString {
            appendLine("Tipo: ${submission.type.label}")
            appendLine("Pantalla / Módulo: ${submission.module}")
            appendLine("Versión App: ${submission.appVersion} (${submission.planLabel})")
            appendLine("Equipo: ${submission.deviceSummary}")
            if (submission.contact.isNotBlank()) {
                appendLine("Contacto: ${submission.contact.trim()}")
            }
            appendLine()
            appendLine("Detalle:")
            append(submission.description.trim())
        }
    }

    suspend fun submitFeedback(
        context: Context,
        submission: FeedbackSubmission
    ): FeedbackSubmitResult = withContext(Dispatchers.IO) {
        val webhookUrl = context.getString(R.string.feedback_webhook_url).trim()
        if (webhookUrl.isEmpty() || !webhookUrl.startsWith("https://")) {
            return@withContext FeedbackSubmitResult.WebhookNotConfigured
        }

        val payload = buildJsonPayload(submission)
        val body = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(webhookUrl)
            .post(body)
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext FeedbackSubmitResult.Error(
                        "Error del servidor (${response.code}). Puedes enviarlo por correo."
                    )
                }
                if (rawBody.contains("\"ok\":false") || rawBody.contains("\"error\"")) {
                    return@withContext FeedbackSubmitResult.Error(
                        "No se pudo registrar en la hoja. Intenta enviarlo por correo."
                    )
                }
                FeedbackSubmitResult.SentToSheet
            }
        }.getOrElse {
            FeedbackSubmitResult.Error(
                "Sin conexión o tiempo de espera agotado. Puedes enviarlo por correo."
            )
        }
    }

    fun openEmailFallback(context: Context, submission: FeedbackSubmission): Boolean {
        val subject = "[Tasa VE ${submission.type.label}] ${submission.module}"
        val body = buildFallbackEmailBody(submission)
        val mailtoUri = Uri.parse(
            "mailto:soporte@tasave.net?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        )
        return runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, mailtoUri)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
