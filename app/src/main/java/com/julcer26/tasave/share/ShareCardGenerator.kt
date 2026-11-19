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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.julcer26.tasave.R
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
            val hasModifier = !modifierText.isNullOrBlank() && !adjustedTotalText.isNullOrBlank()
            val modHeight = if (hasModifier) 168 else 0
            val eqHeight = if (equivalences.isNotEmpty()) 96 + equivalences.size * 66 else 0
            val height = 804 + modHeight + eqHeight

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

            // Prominent Centered Brand Header + Status Pill + Rate Subtitle
            val badgeLabel = if (isTomorrowBcv) "PRÓXIMA BCV" else "CONVERSIÓN EN VIVO"
            val rateLine = "$rateLabel: $rateDisplay" + (updatedAt?.let { " • $it" } ?: "")
            drawCenteredBrandHeader(
                context = context,
                canvas = canvas,
                width = width,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                pillText = badgeLabel,
                pillTextColor = if (isTomorrowBcv) amber else emeraldLight,
                pillBgColor = if (isTomorrowBcv) Color.parseColor("#2EF59E0B") else Color.parseColor("#2410B981"),
                subtitle = rateLine
            )

            // Input / Output Box
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = subCardBg }
            val boxRect = RectF(104f, 326f, width - 104f, 640f)
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

            canvas.drawText(inputLabel.uppercase(Locale.getDefault()), 148f, 386f, smallLabelPaint)
            canvas.drawText(inputDisplay, 148f, 452f, valueBigPaint)

            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) Color.parseColor("#22FFFFFF") else Color.parseColor("#CBD5E1")
                strokeWidth = 2f
            }
            canvas.drawLine(148f, 488f, width - 148f, 488f, dividerPaint)

            canvas.drawText(outputLabel.uppercase(Locale.getDefault()), 148f, 536f, smallLabelPaint)
            canvas.drawText(outputDisplay, 148f, 608f, outputBigPaint)

            var nextY = 678f
            if (hasModifier) {
                val modRect = RectF(104f, nextY, width - 104f, nextY + 132f)
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
                canvas.drawText("AJUSTE APLICADO ($modifierText)", 148f, nextY + 48f, modTitlePaint)
                canvas.drawText("Total Final: $adjustedTotalText", 148f, nextY + 102f, modValPaint)
                nextY += 168f
            }

            if (equivalences.isNotEmpty()) {
                val eqHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = emerald
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("EQUIVALENCIAS EN OTRAS TASAS", 104f, nextY + 24f, eqHeaderPaint)
                nextY += 52f

                val eqRect = RectF(104f, nextY, width - 104f, nextY + (equivalences.size * 66f) + 32f)
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
                    val rowY = nextY + 60f + idx * 66f
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
            canvas.drawText("Calculado con Tasa VE • https://tasave.net", width / 2f, height - 92f, footerPaint)

            val caption = buildString {
                appendLine("*Tasa VE • Conversión*")
                appendLine("$inputDisplay = $outputDisplay")
                appendLine("Tasa ($rateLabel): $rateDisplay")
                if (hasModifier) {
                    appendLine("Ajuste ($modifierText): $adjustedTotalText")
                }
                appendLine()
                append("Descarga Tasa VE: https://tasave.net")
            }.trim()

            shareBitmap(context, bitmap, caption)
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
        profile: PaymentMethodProfile?,
        captionText: String? = null
    ) {
        runCatching {
            val hasTotal = totalUsd > 0.005
            val profileLines = buildProfileLines(profile)
            val width = 1080
            val summaryBoxHeight = if (hasTotal) 428 else 0
            val profileBoxHeight = if (profileLines.isNotEmpty()) 96 + profileLines.size * 54 else 0
            val gapAfterSummary = if (hasTotal && profileLines.isNotEmpty()) 36 else 0
            val height = maxOf(760, 480 + summaryBoxHeight + gapAfterSummary + profileBoxHeight)

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

            val rateFmt = if (rateLabel.contains("USDT", ignoreCase = true)) "%.2f" else "%.4f"
            val pillLabel = if (hasTotal) "RESUMEN DE COBRO" else "DATOS DE COBRO"
            drawCenteredBrandHeader(
                context = context,
                canvas = canvas,
                width = width,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                pillText = pillLabel,
                pillTextColor = emeraldLight,
                pillBgColor = Color.parseColor("#2410B981"),
                subtitle = "$rateLabel: Bs. ${String.format(Locale.US, rateFmt, rateValue)}"
            )

            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = subCardBg }
            var currentY = 326f

            if (hasTotal) {
                val summaryRect = RectF(104f, currentY, width - 104f, currentY + 404f)
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
                val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textSecondary
                    textSize = 28f
                }
                val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = emeraldLight
                    textSize = 52f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                canvas.drawText("TOTAL DE LA CUENTA", 148f, currentY + 60f, labelPaint)
                canvas.drawText(
                    "$${String.format(Locale.US, "%,.2f", totalUsd)}  •  Bs. ${String.format(Locale.US, "%,.2f", totalVes)}",
                    148f,
                    currentY + 118f,
                    totalPaint
                )

                if (paidUsd > 0.0 || paidVes > 0.0) {
                    canvas.drawText(
                        "Abonado: $${String.format(Locale.US, "%,.2f", paidUsd)} + Bs. ${String.format(Locale.US, "%,.2f", paidVes)}",
                        148f,
                        currentY + 174f,
                        subtitlePaint
                    )
                }

                val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDark) Color.parseColor("#22FFFFFF") else Color.parseColor("#CBD5E1")
                    strokeWidth = 2f
                }
                canvas.drawLine(148f, currentY + 212f, width - 148f, currentY + 212f, divPaint)

                canvas.drawText(statusTitle.uppercase(Locale.getDefault()), 148f, currentY + 266f, labelPaint)
                canvas.drawText(
                    "$${String.format(Locale.US, "%,.2f", statusUsd)}  /  Bs. ${String.format(Locale.US, "%,.2f", statusVes)}",
                    148f,
                    currentY + 338f,
                    highlightPaint
                )

                currentY += 440f
            }

            if (profileLines.isNotEmpty()) {
                val pBottom = currentY + 66f + profileLines.size * 54f
                canvas.drawRoundRect(RectF(104f, currentY, width - 104f, pBottom), 34f, 34f, innerPaint)
                val lineLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textSecondary
                    textSize = 27f
                }
                val lineValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = emeraldLight
                    textSize = 31f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = emeraldLight
                    textSize = 30f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                profileLines.forEachIndexed { idx, line ->
                    val y = currentY + 60f + idx * 54f
                    if (idx == 0) {
                        canvas.drawText(line, 148f, y, headPaint)
                    } else {
                        val colonIdx = line.indexOf(':')
                        if (colonIdx in 1 until line.length - 1) {
                            val labelPart = line.substring(0, colonIdx + 1) + " "
                            val valPart = line.substring(colonIdx + 1).trim()
                            canvas.drawText(labelPart, 148f, y, lineLabelPaint)
                            val offset = lineLabelPaint.measureText(labelPart)
                            canvas.drawText(valPart, 148f + offset, y, lineValuePaint)
                        } else {
                            canvas.drawText(line, 148f, y, lineValuePaint)
                        }
                    }
                }
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 26f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Generado con Tasa VE • https://tasave.net", width / 2f, height - 92f, footerPaint)

            val fallbackCaption = buildString {
                if (hasTotal) {
                    appendLine("*Tasa VE • Resumen de Cobro*")
                    appendLine("Total: $${String.format(Locale.US, "%.2f", totalUsd)} • Bs. ${String.format(Locale.US, "%,.2f", totalVes)}")
                    appendLine("Tasa ($rateLabel): Bs. ${String.format(Locale.US, rateFmt, rateValue)}")
                }
                if (profileLines.isNotEmpty()) {
                    if (isNotEmpty()) appendLine()
                    profileLines.forEach { appendLine(it) }
                }
                if (isNotEmpty()) appendLine()
                append("Descarga Tasa VE: https://tasave.net")
            }.trim()

            shareBitmap(
                context,
                bitmap,
                captionText?.takeIf { it.isNotBlank() } ?: fallbackCaption
            )
        }.onFailure {
            Toast.makeText(context, "No se pudo compartir la tarjeta de cobro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawCenteredBrandHeader(
        context: Context,
        canvas: Canvas,
        width: Int,
        isDark: Boolean,
        textPrimary: Int,
        textSecondary: Int,
        pillText: String,
        pillTextColor: Int,
        pillBgColor: Int,
        subtitle: String
    ) {
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val iconSize = 68f
        val gap = 18f
        val textWidth = brandPaint.measureText("Tasa VE")
        val lockupWidth = iconSize + gap + textWidth
        val startX = (width - lockupWidth) / 2f
        val iconTop = 92f

        // Dark Obsidian Rounded App Icon Tile
        val tileRect = RectF(startX, iconTop, startX + iconSize, iconTop + iconSize)
        val tileFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A") }
        val tileStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5510B981")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(tileRect, 18f, 18f, tileFill)
        canvas.drawRoundRect(tileRect, 18f, 18f, tileStroke)

        // Canonical Tasa VE Folded-V + Dollar Vector Mark (ic_brand_mark.xml)
        ContextCompat.getDrawable(context, R.drawable.ic_brand_mark)?.let { mark ->
            val inset = 6
            mark.setBounds(
                startX.toInt() + inset,
                iconTop.toInt() + inset,
                (startX + iconSize).toInt() - inset,
                (iconTop + iconSize).toInt() - inset
            )
            mark.draw(canvas)
        }

        // Bold "Tasa VE" Wordmark next to emblem
        canvas.drawText("Tasa VE", startX + iconSize + gap, iconTop + 50f, brandPaint)

        // Subtle Header Divider
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#1AFFFFFF") else Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }
        canvas.drawLine(104f, 184f, width - 104f, 184f, divPaint)

        // Centered Status Pill
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillBgColor }
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pillTextColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        }
        val pillW = pillTextPaint.measureText(pillText) + 46f
        val pillLeft = (width - pillW) / 2f
        val pillRect = RectF(pillLeft, 204f, pillLeft + pillW, 248f)
        canvas.drawRoundRect(pillRect, 18f, 18f, pillPaint)
        canvas.drawText(pillText, pillLeft + 23f, 235f, pillTextPaint)

        // Centered Rate / Date Subtitle
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 27f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subtitle, width / 2f, 292f, subPaint)
    }

    private fun buildProfileLines(profile: PaymentMethodProfile?): List<String> {
        if (profile == null) return emptyList()
        return buildList {
            val aliasTag = if (profile.alias.isNotBlank()) " • ${profile.alias}" else ""
            when (profile.type) {
                PaymentMethodType.PAGO_MOVIL -> {
                    add("DATOS PAGO MÓVIL$aliasTag")
                    add("Banco: ${profile.formattedBank()}")
                    if (profile.docNumber.isNotBlank()) add("CI/RIF: ${profile.formattedDoc()}")
                    if (profile.phoneNumber.isNotBlank()) add("Teléfono: ${profile.formattedPhone()}")
                }
                PaymentMethodType.BANK_ACCOUNT -> {
                    add("CUENTA BANCARIA$aliasTag")
                    add("Banco: ${profile.formattedBank()}")
                    if (profile.accountNumber.isNotBlank()) add("Cuenta: ${profile.accountNumber}")
                    if (profile.docNumber.isNotBlank()) add("CI/RIF: ${profile.formattedDoc()}")
                    if (profile.accountHolder.isNotBlank()) add("Titular: ${profile.accountHolder}")
                }
                PaymentMethodType.ZELLE -> {
                    add("DATOS ZELLE$aliasTag")
                    if (profile.digitalIdentifier.isNotBlank()) add("Correo / Tel: ${profile.digitalIdentifier}")
                    if (profile.accountHolder.isNotBlank()) add("Titular: ${profile.accountHolder}")
                }
                PaymentMethodType.BINANCE -> {
                    add("DATOS BINANCE PAY$aliasTag")
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
