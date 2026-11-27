package com.julcer26.tasave.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.julcer26.tasave.R
import com.julcer26.tasave.ui.VeNumberFormat
import com.julcer26.tasave.ultra.CartItem
import com.julcer26.tasave.ultra.CartSummary
import com.julcer26.tasave.ultra.PaymentMethodProfile
import com.julcer26.tasave.ultra.PaymentMethodType
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ShareCardGenerator {

    const val WEB_URL = "https://tasave.net"
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.julcer26.tasave"
    const val CUSTOM_BRAND_LOGO_FILENAME = "custom_brand_logo.png"

    fun buildDownloadLinksBlock(): String = buildString {
        appendLine("Descarga Tasa VE:")
        appendLine("Web: $WEB_URL")
        append("Google Play: $PLAY_STORE_URL")
    }

    fun getCustomBrandLogoFile(context: Context): File? {
        val file = File(context.filesDir, CUSTOM_BRAND_LOGO_FILENAME)
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun deleteCustomBrandLogo(context: Context): Boolean {
        val file = File(context.filesDir, CUSTOM_BRAND_LOGO_FILENAME)
        return !file.exists() || file.delete()
    }

    fun importCustomBrandLogo(context: Context, uri: Uri): Boolean {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return false

            var sampleSize = 1
            while ((srcW / sampleSize) > 1024 && (srcH / sampleSize) > 1024) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return false

            val normalized = cropAndScaleSquareBitmap(decoded, targetSize = 512)
            if (normalized !== decoded) {
                decoded.recycle()
            }

            val outFile = File(context.filesDir, CUSTOM_BRAND_LOGO_FILENAME)
            FileOutputStream(outFile).use { fos ->
                normalized.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            normalized.recycle()
            outFile.exists() && outFile.length() > 0L
        }.getOrDefault(false)
    }

    internal fun cropAndScaleSquareBitmap(source: Bitmap, targetSize: Int = 512): Bitmap {
        val size = minOf(source.width, source.height).coerceAtLeast(1)
        val x = ((source.width - size) / 2).coerceAtLeast(0)
        val y = ((source.height - size) / 2).coerceAtLeast(0)
        val cropped = if (source.width == source.height) {
            source
        } else {
            Bitmap.createBitmap(source, x, y, size, size)
        }
        if (cropped.width == targetSize && cropped.height == targetSize) {
            return cropped
        }
        val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        if (cropped !== source && cropped !== scaled) {
            cropped.recycle()
        }
        return scaled
    }

    internal fun extractBrandInitials(brandName: String): String {
        val parts = brandName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "TV"
            parts.size == 1 -> parts[0].take(2).uppercase(Locale.getDefault())
            else -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.getDefault())
        }
    }

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
        com.julcer26.tasave.analytics.AnalyticsTracker.logShareCard(context, "calculation", false)
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

            val maxContentTextW = (width - 296).toFloat()
            fitTextSize(valueBigPaint, inputDisplay, maxContentTextW, minSize = 28f)
            fitTextSize(outputBigPaint, outputDisplay, maxContentTextW, minSize = 30f)

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
                append(buildDownloadLinksBlock())
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
        captionText: String? = null,
        customBrandName: String? = null,
        customBrandLogoFile: File? = null,
        paidSummaryOverride: String? = null
    ) {
        com.julcer26.tasave.analytics.AnalyticsTracker.logShareCard(
            context,
            "checkout",
            !customBrandName.isNullOrBlank() || customBrandLogoFile != null
        )
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

            val customLogoBitmap = runCatching {
                customBrandLogoFile?.takeIf { it.exists() && it.length() > 0L }?.let {
                    BitmapFactory.decodeFile(it.absolutePath)
                }
            }.getOrNull()

            val rateDecimals = if (rateLabel.contains("USDT", ignoreCase = true)) 2 else 4
            val pillLabel = if (hasTotal) "INSTRUCCIONES DE PAGO" else "DATOS DE PAGO"
            try {
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
                    subtitle = "$rateLabel: Bs. ${VeNumberFormat.formatRate(rateValue, rateDecimals)}",
                    customBrandName = customBrandName,
                    customBrandLogo = customLogoBitmap
                )
            } finally {
                customLogoBitmap?.recycle()
            }

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

                val maxSummaryW = (width - 296).toFloat()
                val totalText = "$${VeNumberFormat.formatMoney(totalUsd)}  •  Bs. ${VeNumberFormat.formatMoney(totalVes)}"
                fitTextSize(totalPaint, totalText, maxSummaryW, minSize = 24f)

                canvas.drawText("TOTAL DE LA CUENTA", 148f, currentY + 60f, labelPaint)
                canvas.drawText(
                    totalText,
                    148f,
                    currentY + 118f,
                    totalPaint
                )

                val paidLine = when {
                    !paidSummaryOverride.isNullOrBlank() -> "Abonado: $paidSummaryOverride"
                    paidUsd > 0.0 || paidVes > 0.0 ->
                        "Abonado: $${VeNumberFormat.formatMoney(paidUsd)} + Bs. ${VeNumberFormat.formatMoney(paidVes)}"
                    else -> null
                }
                if (paidLine != null) {
                    fitTextSize(subtitlePaint, paidLine, maxSummaryW, minSize = 19f)
                    canvas.drawText(
                        paidLine,
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

                val statusLine = "$${VeNumberFormat.formatMoney(statusUsd)}  /  Bs. ${VeNumberFormat.formatMoney(statusVes)}"
                fitTextSize(highlightPaint, statusLine, maxSummaryW, minSize = 26f)
                canvas.drawText(statusTitle.uppercase(Locale.getDefault()), 148f, currentY + 266f, labelPaint)
                canvas.drawText(
                    statusLine,
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
                    val headerName = customBrandName?.trim()?.takeIf { it.isNotBlank() } ?: "Tasa VE"
                    appendLine("*$headerName • Instrucciones de Pago*")
                    appendLine("Total: $${VeNumberFormat.formatMoney(totalUsd)} • Bs. ${VeNumberFormat.formatMoney(totalVes)}")
                    appendLine("Tasa ($rateLabel): Bs. ${VeNumberFormat.formatRate(rateValue, rateDecimals)}")
                }
                if (profileLines.isNotEmpty()) {
                    if (isNotEmpty()) appendLine()
                    profileLines.forEach { appendLine(it) }
                }
                if (isNotEmpty()) appendLine()
                append(buildDownloadLinksBlock())
            }.trim()

            shareBitmap(
                context,
                bitmap,
                captionText?.takeIf { it.isNotBlank() } ?: fallbackCaption
            )
        }.onFailure {
            Toast.makeText(context, "No se pudo compartir la tarjeta", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatCartQuoteText(
        items: List<CartItem>,
        summary: CartSummary,
        usdRate: Double,
        applyIva: Boolean,
        applyIgtf: Boolean,
        profile: PaymentMethodProfile? = null,
        customBrandName: String? = null
    ): String {
        val brandHeader = customBrandName?.trim()?.takeIf { it.isNotBlank() } ?: "Tasa VE"
        val profileLines = buildProfileLines(profile)
        return buildString {
            appendLine("*$brandHeader • Presupuesto / Pedido*")
            items.forEachIndexed { idx, item ->
                val lineUsd = item.unitPriceUsd * item.quantity
                val lineVes = lineUsd * usdRate
                appendLine(
                    "${idx + 1}. ${item.quantity}x ${item.name}: $${VeNumberFormat.formatMoney(lineUsd)} (Bs. ${VeNumberFormat.formatMoney(lineVes)})"
                )
            }
            appendLine()
            if (applyIva || applyIgtf) {
                appendLine("Subtotal: $${VeNumberFormat.formatMoney(summary.subtotalUsd)} • Bs. ${VeNumberFormat.formatMoney(summary.subtotalUsd * usdRate)}")
                if (applyIva) {
                    appendLine("IVA (16%): $${VeNumberFormat.formatMoney(summary.ivaUsd)} • Bs. ${VeNumberFormat.formatMoney(summary.ivaUsd * usdRate)}")
                }
                if (applyIgtf) {
                    appendLine("IGTF (3%): $${VeNumberFormat.formatMoney(summary.igtfUsd)} • Bs. ${VeNumberFormat.formatMoney(summary.igtfUsd * usdRate)}")
                }
            }
            appendLine("*Total a pagar: $${VeNumberFormat.formatMoney(summary.totalUsd)} • Bs. ${VeNumberFormat.formatMoney(summary.totalVes)}*")
            if (usdRate > 0.0) {
                appendLine("Tasa (Dólar BCV): Bs. ${VeNumberFormat.formatRate(usdRate, 4)}")
            }
            if (profileLines.isNotEmpty()) {
                appendLine()
                profileLines.forEach { appendLine(it) }
            }
            appendLine()
            append(buildDownloadLinksBlock())
        }.trim()
    }

    fun shareCartQuoteCard(
        context: Context,
        isDark: Boolean,
        items: List<CartItem>,
        summary: CartSummary,
        usdRate: Double,
        applyIva: Boolean,
        applyIgtf: Boolean,
        profile: PaymentMethodProfile? = null,
        captionText: String? = null,
        customBrandName: String? = null,
        customBrandLogoFile: File? = null
    ) {
        if (items.isEmpty()) return
        com.julcer26.tasave.analytics.AnalyticsTracker.logShareCard(
            context,
            "cart_quote",
            !customBrandName.isNullOrBlank() || customBrandLogoFile != null
        )
        runCatching {
            val visibleItems = items.take(7)
            val hiddenCount = (items.size - visibleItems.size).coerceAtLeast(0)
            val itemRowCount = visibleItems.size + if (hiddenCount > 0) 1 else 0
            val taxLinesCount = (if (applyIva || applyIgtf) 1 else 0) +
                (if (applyIva) 1 else 0) +
                (if (applyIgtf) 1 else 0)
            val profileLines = buildProfileLines(profile)

            val width = 1080
            val itemsBoxHeight = 92 + itemRowCount * 62
            val totalsBoxHeight = 224 + taxLinesCount * 48
            val profileBoxHeight = if (profileLines.isNotEmpty()) 96 + profileLines.size * 54 else 0
            val gapProfile = if (profileLines.isNotEmpty()) 32 else 0
            val height = 480 + itemsBoxHeight + 32 + totalsBoxHeight + gapProfile + profileBoxHeight

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

            val customLogoBitmap = runCatching {
                customBrandLogoFile?.takeIf { it.exists() && it.length() > 0L }?.let {
                    BitmapFactory.decodeFile(it.absolutePath)
                }
            }.getOrNull()

            val totalUnits = items.sumOf { it.quantity }
            val subtitleText = "Dólar BCV: Bs. ${VeNumberFormat.formatRate(usdRate, 4)} • $totalUnits art."
            try {
                drawCenteredBrandHeader(
                    context = context,
                    canvas = canvas,
                    width = width,
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    pillText = "PRESUPUESTO / PEDIDO",
                    pillTextColor = emeraldLight,
                    pillBgColor = Color.parseColor("#2410B981"),
                    subtitle = subtitleText,
                    customBrandName = customBrandName,
                    customBrandLogo = customLogoBitmap
                )
            } finally {
                customLogoBitmap?.recycle()
            }

            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = subCardBg }
            var currentY = 326f

            // 1. Items Box
            val itemsBottom = currentY + itemsBoxHeight
            canvas.drawRoundRect(RectF(104f, currentY, width - 104f, itemsBottom), 34f, 34f, innerPaint)

            val sectionLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val itemNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textPrimary
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val itemPricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldLight
                textSize = 27f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            canvas.drawText("DETALLE DE PRODUCTOS", 148f, currentY + 52f, sectionLabelPaint)

            visibleItems.forEachIndexed { idx, item ->
                val rowY = currentY + 108f + idx * 62f
                val lineUsd = item.unitPriceUsd * item.quantity
                val lineVes = lineUsd * usdRate
                var title = "${item.quantity}x ${item.name.trim()}"
                if (title.length > 26) {
                    title = title.take(25).trimEnd() + "…"
                }
                val rightPrice = "$${VeNumberFormat.formatMoney(lineUsd)}  •  Bs. ${VeNumberFormat.formatMoney(lineVes)}"
                canvas.drawText(title, 148f, rowY, itemNamePaint)
                canvas.drawText(rightPrice, width - 148f, rowY, itemPricePaint)
            }

            if (hiddenCount > 0) {
                val hiddenItems = items.drop(visibleItems.size)
                val hiddenUsd = hiddenItems.sumOf { it.unitPriceUsd * it.quantity }
                val hiddenVes = hiddenUsd * usdRate
                val rowY = currentY + 108f + visibleItems.size * 62f
                canvas.drawText("+$hiddenCount producto(s) más", 148f, rowY, sectionLabelPaint)
                canvas.drawText(
                    "$${VeNumberFormat.formatMoney(hiddenUsd)}  •  Bs. ${VeNumberFormat.formatMoney(hiddenVes)}",
                    width - 148f,
                    rowY,
                    itemPricePaint
                )
            }

            currentY = itemsBottom + 32f

            // 2. Totals Box
            val totalsBottom = currentY + totalsBoxHeight
            canvas.drawRoundRect(RectF(104f, currentY, width - 104f, totalsBottom), 34f, 34f, innerPaint)

            var rowCursorY = currentY + 54f
            val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 27f
            }
            val subValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textPrimary
                textSize = 27f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            if (applyIva || applyIgtf) {
                canvas.drawText("Subtotal", 148f, rowCursorY, subTextPaint)
                canvas.drawText(
                    "$${VeNumberFormat.formatMoney(summary.subtotalUsd)}  •  Bs. ${VeNumberFormat.formatMoney(summary.subtotalUsd * usdRate)}",
                    width - 148f,
                    rowCursorY,
                    subValPaint
                )
                rowCursorY += 48f
                if (applyIva) {
                    canvas.drawText("IVA (16%)", 148f, rowCursorY, subTextPaint)
                    canvas.drawText(
                        "$${VeNumberFormat.formatMoney(summary.ivaUsd)}  •  Bs. ${VeNumberFormat.formatMoney(summary.ivaUsd * usdRate)}",
                        width - 148f,
                        rowCursorY,
                        subValPaint
                    )
                    rowCursorY += 48f
                }
                if (applyIgtf) {
                    canvas.drawText("IGTF (3%)", 148f, rowCursorY, subTextPaint)
                    canvas.drawText(
                        "$${VeNumberFormat.formatMoney(summary.igtfUsd)}  •  Bs. ${VeNumberFormat.formatMoney(summary.igtfUsd * usdRate)}",
                        width - 148f,
                        rowCursorY,
                        subValPaint
                    )
                    rowCursorY += 48f
                }

                val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDark) Color.parseColor("#22FFFFFF") else Color.parseColor("#CBD5E1")
                    strokeWidth = 2f
                }
                canvas.drawLine(148f, rowCursorY - 18f, width - 148f, rowCursorY - 18f, divPaint)
                rowCursorY += 20f
            }

            canvas.drawText("TOTAL A PAGAR", 148f, rowCursorY, sectionLabelPaint)
            val bigTotalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldLight
                textSize = 48f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val grandTotalLine = "$${VeNumberFormat.formatMoney(summary.totalUsd)}  •  Bs. ${VeNumberFormat.formatMoney(summary.totalVes)}"
            fitTextSize(bigTotalPaint, grandTotalLine, (width - 296).toFloat(), minSize = 26f)
            canvas.drawText(
                grandTotalLine,
                148f,
                rowCursorY + 62f,
                bigTotalPaint
            )
            canvas.drawText(
                "Equivalente: € ${VeNumberFormat.formatMoney(summary.totalEur)}  •  USDT ${VeNumberFormat.formatMoney(summary.totalUsdt)}",
                148f,
                rowCursorY + 108f,
                subTextPaint
            )

            currentY = totalsBottom + 32f

            // 3. Optional Active Payment Profile Box
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

            val fallbackCaption = formatCartQuoteText(
                items = items,
                summary = summary,
                usdRate = usdRate,
                applyIva = applyIva,
                applyIgtf = applyIgtf,
                profile = profile,
                customBrandName = customBrandName
            )

            shareBitmap(
                context,
                bitmap,
                captionText?.takeIf { it.isNotBlank() } ?: fallbackCaption
            )
        }.onFailure {
            Toast.makeText(context, "No se pudo generar la tarjeta del carrito", Toast.LENGTH_SHORT).show()
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
        subtitle: String,
        customBrandName: String? = null,
        customBrandLogo: Bitmap? = null
    ) {
        val cleanedName = customBrandName?.trim().orEmpty()
        val hasCustomBrand = customBrandLogo != null || cleanedName.isNotEmpty()

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val iconSize = if (hasCustomBrand && cleanedName.isEmpty()) 76f else 68f
        val gap = 18f
        val iconTop = if (hasCustomBrand && cleanedName.isEmpty()) 88f else 92f
        val maxTextWidth = width - 240f - iconSize - gap

        if (!hasCustomBrand) {
            val textWidth = brandPaint.measureText("Tasa VE")
            val lockupWidth = iconSize + gap + textWidth
            val startX = (width - lockupWidth) / 2f

            // Deep Obsidian Rounded App Icon Tile
            val tileRect = RectF(startX, iconTop, startX + iconSize, iconTop + iconSize)
            val tileFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#070C16") }
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
        } else {
            // Fit custom business name smoothly so long names never overflow
            var fittedName = cleanedName
            if (fittedName.isNotEmpty()) {
                var size = 48f
                brandPaint.textSize = size
                while (brandPaint.measureText(fittedName) > maxTextWidth && size > 30f) {
                    size -= 2f
                    brandPaint.textSize = size
                }
                while (brandPaint.measureText(fittedName) > maxTextWidth && fittedName.length > 4) {
                    fittedName = fittedName.dropLast(2).trimEnd() + "…"
                }
            }

            val textWidth = if (fittedName.isNotEmpty()) brandPaint.measureText(fittedName) else 0f
            val lockupWidth = if (fittedName.isNotEmpty()) iconSize + gap + textWidth else iconSize
            val startX = (width - lockupWidth) / 2f
            val tileRect = RectF(startX, iconTop, startX + iconSize, iconTop + iconSize)

            if (customBrandLogo != null) {
                val tileFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDark) Color.parseColor("#0F172A") else Color.parseColor("#F8FAFC")
                }
                canvas.drawRoundRect(tileRect, 18f, 18f, tileFill)

                val saveCount = canvas.save()
                val clipPath = Path().apply {
                    addRoundRect(tileRect, 18f, 18f, Path.Direction.CW)
                }
                canvas.clipPath(clipPath)
                val srcRect = Rect(0, 0, customBrandLogo.width, customBrandLogo.height)
                val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(customBrandLogo, srcRect, tileRect, imgPaint)
                canvas.restoreToCount(saveCount)

                val tileStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDark) Color.parseColor("#4410B981") else Color.parseColor("#CBD5E1")
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRoundRect(tileRect, 18f, 18f, tileStroke)
            } else {
                // Monogram tile when the business sets only a custom name
                val tileFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#070C16") }
                val tileStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#6610B981")
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                }
                canvas.drawRoundRect(tileRect, 18f, 18f, tileFill)
                canvas.drawRoundRect(tileRect, 18f, 18f, tileStroke)

                val initials = extractBrandInitials(cleanedName)
                val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#34D399")
                    textSize = 27f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val baselineY = tileRect.centerY() - ((monoPaint.descent() + monoPaint.ascent()) / 2f)
                canvas.drawText(initials, tileRect.centerX(), baselineY, monoPaint)
            }

            if (fittedName.isNotEmpty()) {
                val textY = tileRect.centerY() - ((brandPaint.descent() + brandPaint.ascent()) / 2f)
                canvas.drawText(fittedName, startX + iconSize + gap, textY, brandPaint)
            }
        }

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

    private fun fitTextSize(paint: Paint, text: String, maxWidth: Float, minSize: Float) {
        var currentSize = paint.textSize
        while (paint.measureText(text) > maxWidth && currentSize > minSize) {
            currentSize -= 2f
            paint.textSize = currentSize
        }
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
