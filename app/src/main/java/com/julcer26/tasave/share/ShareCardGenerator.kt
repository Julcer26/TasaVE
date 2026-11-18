package com.julcer26.tasave.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.Toast
import androidx.core.content.FileProvider
import com.julcer26.tasave.ultra.PaymentMethodProfile
import com.julcer26.tasave.ultra.PaymentMethodType
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ShareCardGenerator {

    fun shareCalculationCard(
        context: Context,
        isDark: Boolean,
        inputLabel: String,
        inputDisplay: String,
        outputLabel: String,
        outputDisplay: String,
        rateLabel: String,
        rateDisplay: String,
        updatedAt: String?,
        modifierText: String?,
        adjustedTotalText: String?,
        equivalences: List<Pair<String, String>>,
        isTomorrowBcv: Boolean
    ) {
        runCatching {
            val width = 1080
            val height = if (equivalences.isNotEmpty()) 1280 else 1020
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgTop = if (isDark) Color.parseColor("#0A0A0D") else Color.parseColor("#F8FAFC")
            val bgBottom = if (isDark) Color.parseColor("#13151B") else Color.parseColor("#E2E8F0")
            val cardBg = if (isDark) Color.parseColor("#181920") else Color.WHITE
            val subCardBg = if (isDark) Color.parseColor("#21232C") else Color.parseColor("#F1F5F9")
            val textPrimary = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
            val textSecondary = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569")
            val emerald = Color.parseColor("#10B981")
            val emeraldLight = if (isDark) Color.parseColor("#34D399") else Color.parseColor("#047857")
            val amber = Color.parseColor("#F59E0B")

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, height.toFloat(), bgTop, bgBottom, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Outer Container Card
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) Color.parseColor("#2634D399") else Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val outerRect = RectF(56f, 56f, width - 56f, height - 56f)
            canvas.drawRoundRect(outerRect, 48f, 48f, cardPaint)
            canvas.drawRoundRect(outerRect, 48f, 48f, borderPaint)

            // Brand Header
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textPrimary
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isTomorrowBcv) Color.parseColor("#33F59E0B") else Color.parseColor("#2610B981")
            }
            val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isTomorrowBcv) amber else emeraldLight
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            canvas.drawText("Tasa VE", 104f, 148f, brandPaint)
            val badgeLabel = if (isTomorrowBcv) "PRÓXIMA BCV" else "CONVERSIÓN EN VIVO"
            val badgeWidth = pillTextPaint.measureText(badgeLabel) + 48f
            val badgeRect = RectF(width - 104f - badgeWidth, 108f, width - 104f, 160f)
            canvas.drawRoundRect(badgeRect, 20f, 20f, pillPaint)
            canvas.drawText(badgeLabel, badgeRect.left + 24f, 143f, pillTextPaint)

            // Rate + Date subtitle
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 28f
            }
            val rateLine = "$rateLabel: $rateDisplay" + (updatedAt?.let { " • $it" } ?: "")
            canvas.drawText(rateLine, 104f, 212f, subPaint)

            // Input / Output Box
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = subCardBg }
            val boxRect = RectF(104f, 256f, width - 104f, 570f)
            canvas.drawRoundRect(boxRect, 34f, 34f, innerPaint)

            val smallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val valueBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textPrimary
                textSize = 54f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val outputBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldLight
                textSize = 62f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            canvas.drawText(inputLabel.uppercase(Locale.getDefault()), 148f, 316f, smallLabelPaint)
            canvas.drawText(inputDisplay, 148f, 382f, valueBigPaint)

            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) Color.parseColor("#22FFFFFF") else Color.parseColor("#CBD5E1")
                strokeWidth = 2f
            }
            canvas.drawLine(148f, 418f, width - 148f, 418f, dividerPaint)

            canvas.drawText(outputLabel.uppercase(Locale.getDefault()), 148f, 466f, smallLabelPaint)
            canvas.drawText(outputDisplay, 148f, 538f, outputBigPaint)

            var nextY = 620f
            if (!modifierText.isNullOrBlank() && !adjustedTotalText.isNullOrBlank()) {
                val modRect = RectF(104f, nextY, width - 104f, nextY + 130f)
                val modBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#22F59E0B")
                }
                canvas.drawRoundRect(modRect, 28f, 28f, modBg)
                val modTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = amber
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val modValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textPrimary
                    textSize = 36f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("AJUSTE APLICADO ($modifierText)", 148f, nextY + 46f, modTitlePaint)
                canvas.drawText("Total Final: $adjustedTotalText", 148f, nextY + 100f, modValPaint)
                nextY += 164f
            }

            if (equivalences.isNotEmpty()) {
                val eqHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = emerald
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("EQUIVALENCIAS EN OTRAS TASAS", 104f, nextY + 24f, eqHeaderPaint)
                nextY += 54f

                val eqRect = RectF(104f, nextY, width - 104f, nextY + (equivalences.size * 68f) + 32f)
                canvas.drawRoundRect(eqRect, 28f, 28f, innerPaint)

                val eqNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textSecondary
                    textSize = 30f
                }
                val eqValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textPrimary
                    textSize = 32f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                equivalences.forEachIndexed { idx, (k, v) ->
                    val rowY = nextY + 62f + idx * 66f
                    canvas.drawText(k, 148f, rowY, eqNamePaint)
                    canvas.drawText(v, width - 148f, rowY, eqValPaint)
                }
            }

            // Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 26f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Calculado con Tasa VE • tasave.net", width / 2f, height - 96f, footerPaint)

            shareBitmap(context, bitmap, "Tasa VE • $inputDisplay = $outputDisplay")
        }.onFailure {
            Toast.makeText(context, "No se pudo generar la tarjeta visual", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareCheckoutCard(
        context: Context,
        isDark: Boolean,
        totalUsd: Double,
        totalVes: Double,
        rateLabel: String,
        rateValue: Double,
        paidUsd: Double,
        paidVes: Double,
        statusTitle: String,
        statusUsd: Double,
        statusVes: Double,
        profile: PaymentMethodProfile?
    ) {
        runCatching {
            val profileLines = buildProfileLines(profile)
            val width = 1080
            val height = if (profileLines.isEmpty()) 960 else 1020 + profileLines.size * 52
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgTop = if (isDark) Color.parseColor("#0A0A0D") else Color.parseColor("#F8FAFC")
            val bgBottom = if (isDark) Color.parseColor("#13151B") else Color.parseColor("#E2E8F0")
            val cardBg = if (isDark) Color.parseColor("#181920") else Color.WHITE
            val subCardBg = if (isDark) Color.parseColor("#21232C") else Color.parseColor("#F1F5F9")
            val textPrimary = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
            val textSecondary = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569")
            val emeraldLight = if (isDark) Color.parseColor("#34D399") else Color.parseColor("#047857")

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, height.toFloat(), bgTop, bgBottom, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) Color.parseColor("#2634D399") else Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val outerRect = RectF(56f, 56f, width - 56f, height - 56f)
            canvas.drawRoundRect(outerRect, 48f, 48f, cardPaint)
            canvas.drawRoundRect(outerRect, 48f, 48f, borderPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textPrimary
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 28f
            }
            canvas.drawText("Resumen de Cobro — Tasa VE", 104f, 148f, titlePaint)
            canvas.drawText(
                "$rateLabel: Bs. ${String.format(Locale.US, "%.4f", rateValue)}",
                104f,
                200f,
                subtitlePaint
            )

            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = subCardBg }
            val summaryRect = RectF(104f, 240f, width - 104f, 640f)
            canvas.drawRoundRect(summaryRect, 34f, 34f, innerPaint)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textPrimary
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldLight
                textSize = 52f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            canvas.drawText("TOTAL DE LA CUENTA", 148f, 300f, labelPaint)
            canvas.drawText(
                "$${String.format(Locale.US, "%,.2f", totalUsd)}  •  Bs. ${String.format(Locale.US, "%,.2f", totalVes)}",
                148f,
                358f,
                totalPaint
            )

            if (paidUsd > 0.0 || paidVes > 0.0) {
                canvas.drawText(
                    "Abonado: $${String.format(Locale.US, "%,.2f", paidUsd)} + Bs. ${String.format(Locale.US, "%,.2f", paidVes)}",
                    148f,
                    414f,
                    subtitlePaint
                )
            }

            val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) Color.parseColor("#22FFFFFF") else Color.parseColor("#CBD5E1")
                strokeWidth = 2f
            }
            canvas.drawLine(148f, 452f, width - 148f, 452f, divPaint)

            canvas.drawText(statusTitle.uppercase(Locale.getDefault()), 148f, 504f, labelPaint)
            canvas.drawText(
                "$${String.format(Locale.US, "%,.2f", statusUsd)}  /  Bs. ${String.format(Locale.US, "%,.2f", statusVes)}",
                148f,
                576f,
                highlightPaint
            )

            if (profileLines.isNotEmpty()) {
                val pTop = 684f
                val pBottom = pTop + 60f + profileLines.size * 52f
                canvas.drawRoundRect(RectF(104f, pTop, width - 104f, pBottom), 34f, 34f, innerPaint)
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textPrimary
                    textSize = 30f
                }
                val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = emeraldLight
                    textSize = 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                profileLines.forEachIndexed { idx, line ->
                    val y = pTop + 56f + idx * 50f
                    canvas.drawText(line, 148f, y, if (idx == 0) headPaint else linePaint)
                }
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 26f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Generado con Tasa VE • tasave.net", width / 2f, height - 92f, footerPaint)

            shareBitmap(
                context,
                bitmap,
                "Cobro Tasa VE • Total $${String.format(Locale.US, "%.2f", totalUsd)}"
            )
        }.onFailure {
            Toast.makeText(context, "No se pudo compartir la tarjeta de cobro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildProfileLines(profile: PaymentMethodProfile?): List<String> {
        if (profile == null) return emptyList()
        return buildList {
            when (profile.type) {
                PaymentMethodType.PAGO_MOVIL -> {
                    add("DATOS PAGO MÓVIL${if (profile.alias.isNotBlank()) " (${profile.alias})" else ""}")
                    add("Banco: ${profile.formattedBank()}")
                    if (profile.docNumber.isNotBlank()) add("CI/RIF: ${profile.formattedDoc()}")
                    if (profile.phoneNumber.isNotBlank()) add("Teléfono: ${profile.formattedPhone()}")
                }
                PaymentMethodType.BANK_ACCOUNT -> {
                    add("CUENTA BANCARIA${if (profile.alias.isNotBlank()) " (${profile.alias})" else ""}")
                    add("Banco: ${profile.formattedBank()}")
                    if (profile.accountNumber.isNotBlank()) add("Cuenta: ${profile.accountNumber}")
                    if (profile.docNumber.isNotBlank()) add("CI/RIF: ${profile.formattedDoc()}")
                    if (profile.accountHolder.isNotBlank()) add("Titular: ${profile.accountHolder}")
                }
                PaymentMethodType.ZELLE -> {
                    add("DATOS ZELLE${if (profile.alias.isNotBlank()) " (${profile.alias})" else ""}")
                    if (profile.digitalIdentifier.isNotBlank()) add("Correo / Tel: ${profile.digitalIdentifier}")
                    if (profile.accountHolder.isNotBlank()) add("Titular: ${profile.accountHolder}")
                }
                PaymentMethodType.BINANCE -> {
                    add("DATOS BINANCE PAY${if (profile.alias.isNotBlank()) " (${profile.alias})" else ""}")
                    if (profile.digitalIdentifier.isNotBlank()) add("Pay ID / Correo: ${profile.digitalIdentifier}")
                    if (profile.accountHolder.isNotBlank()) add("Alias / Titular: ${profile.accountHolder}")
                }
            }
        }
    }

    private fun shareBitmap(context: Context, bitmap: Bitmap, caption: String) {
        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        // Clean previous temporary share cards to keep cache tiny
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(3)?.forEach { it.delete() }

        val outFile = File(dir, "tasave_card_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir tarjeta Tasa VE"))
    }
}
