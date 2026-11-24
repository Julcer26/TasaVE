package com.julcer26.tasave.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Venezuelan national banking number formatting & input utilities:
 * - Thousands separator: dot ('.')
 * - Decimal separator: comma (',')
 * Example: 4599113500.0 -> "4.599.113.500,00"
 */
object VeNumberFormat {

    const val MAX_INPUT_INT_DIGITS = 13
    const val MAX_INPUT_DEC_DIGITS = 4

    private val veSymbols: DecimalFormatSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val canonicalSymbols: DecimalFormatSymbols = DecimalFormatSymbols(Locale.US).apply {
        decimalSeparator = '.'
    }

    fun formatMoney(value: Double, decimals: Int = 2): String {
        if (!value.isFinite()) return if (decimals <= 0) "0" else "0," + "0".repeat(decimals)
        val safeValue = if (abs(value) < 0.0000001) 0.0 else value
        val pattern = if (decimals <= 0) "#,##0" else "#,##0." + "0".repeat(decimals)
        val df = DecimalFormat(pattern, veSymbols).apply {
            roundingMode = RoundingMode.HALF_UP
        }
        return df.format(safeValue)
    }

    fun formatInt(value: Double): String = formatMoney(value, decimals = 0)

    fun formatRate(value: Double, decimals: Int = 4): String = formatMoney(value, decimals = decimals)

    fun formatPercent(value: Double, decimals: Int = 2): String = formatMoney(value, decimals = decimals)

    fun formatCop(value: Double): String {
        if (!value.isFinite()) return "0"
        return if (value % 1.0 == 0.0) formatInt(value) else formatMoney(value, decimals = 2)
    }

    /**
     * Converts a finite Double to a plain ungrouped '.' decimal canonical string (never scientific notation),
     * suitable for storing in ViewModel text field state.
     */
    fun toCanonicalFixed(value: Double, decimals: Int = 2): String {
        if (!value.isFinite()) return ""
        val safeValue = if (abs(value) < 0.0000001) 0.0 else value
        val pattern = if (decimals <= 0) "0" else "0." + "0".repeat(decimals)
        val df = DecimalFormat(pattern, canonicalSymbols).apply {
            isGroupingUsed = false
            roundingMode = RoundingMode.HALF_UP
        }
        return df.format(BigDecimal.valueOf(safeValue))
    }

    /**
     * Formats a canonical input string (e.g. "5000000" or "4599113500.00") into Venezuelan visual format
     * ("5.000.000" or "4.599.113.500,00") for display or clipboard copy.
     */
    fun formatCanonicalString(raw: String): String {
        if (raw.isBlank()) return ""
        return buildTransformed(raw).text.text
    }

    /**
     * Parses canonical ("4599113500.00"), Venezuelan ("4.599.113.500,00" / "12,50"),
     * or US ("4,599,113.50") number strings into a Double.
     */
    fun parseFlexibleDouble(raw: String): Double? {
        val cleaned = raw.trim()
            .replace("Bs.", "", ignoreCase = true)
            .replace("Bs", "", ignoreCase = true)
            .replace("USD", "", ignoreCase = true)
            .replace("EUR", "", ignoreCase = true)
            .replace("USDT", "", ignoreCase = true)
            .replace("COP", "", ignoreCase = true)
            .replace("$", "")
            .replace("€", "")
            .replace("₮", "")
            .replace("\\s+".toRegex(), "")

        if (cleaned.isEmpty() || cleaned == "." || cleaned == ",") return null

        val dots = cleaned.count { it == '.' }
        val commas = cleaned.count { it == ',' }

        val normalized = when {
            dots > 0 && commas > 0 -> {
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    // Venezuelan: 4.599.113,50
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    // US: 4,599,113.50
                    cleaned.replace(",", "")
                }
            }
            dots > 1 -> cleaned.replace(".", "")
            commas > 1 -> cleaned.replace(",", "")
            commas == 1 -> cleaned.replace(',', '.')
            else -> cleaned
        }

        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /**
     * Sanitizes user input from a Compose text field into canonical ungrouped form (digits + optional single '.').
     * - Single-keystroke '.' or ',' is always treated as the decimal separator (displayed visually as ',').
     * - Multi-character paste of Venezuelan ("4.599.113,50" or "5.000") or US ("5,000.00") text is normalized cleanly.
     * - Returns null if the edit contains invalid characters and should be ignored.
     */
    fun sanitizeEditableInput(
        newRaw: String,
        previousRaw: String = "",
        maxIntDigits: Int = MAX_INPUT_INT_DIGITS,
        maxDecDigits: Int = MAX_INPUT_DEC_DIGITS
    ): String? {
        if (newRaw.isEmpty()) return ""

        val stripped = newRaw.trim()
            .replace("Bs.", "", ignoreCase = true)
            .replace("Bs", "", ignoreCase = true)
            .replace("$", "")
            .replace("€", "")
            .replace("₮", "")
            .replace(" ", "")

        if (stripped.isEmpty()) return ""
        if (!stripped.all { it.isDigit() || it == '.' || it == ',' }) return null

        val dots = stripped.count { it == '.' }
        val commas = stripped.count { it == ',' }
        val isPasteJump = stripped.length - previousRaw.length >= 4

        val canonicalCandidate = when {
            dots + commas > 1 -> {
                // Pasted formatted number with multiple separators
                when {
                    dots > 0 && commas > 0 -> {
                        if (stripped.lastIndexOf(',') > stripped.lastIndexOf('.')) {
                            stripped.replace(".", "").replace(',', '.')
                        } else {
                            stripped.replace(",", "")
                        }
                    }
                    dots > 1 -> stripped.replace(".", "")
                    else -> stripped.replace(",", "")
                }
            }
            isPasteJump && dots == 1 && Regex("^\\d{1,3}\\.\\d{3}$").matches(stripped) -> {
                // Pasted Venezuelan integer with single thousands dot like "5.000"
                stripped.replace(".", "")
            }
            isPasteJump && commas == 1 && Regex("^\\d{1,3},\\d{3}$").matches(stripped) -> {
                // Pasted US integer with single thousands comma like "5,000"
                stripped.replace(",", "")
            }
            else -> {
                // Normal typing: treat either '.' or ',' from soft keypad as decimal point
                stripped.replace(',', '.')
            }
        }

        val dotIdx = canonicalCandidate.indexOf('.')
        val rawIntPart = if (dotIdx >= 0) canonicalCandidate.substring(0, dotIdx) else canonicalCandidate
        val rawDecPart = if (dotIdx >= 0) canonicalCandidate.substring(dotIdx + 1).replace(".", "") else null

        val normalizedInt = when {
            rawIntPart.isEmpty() -> "0"
            rawIntPart.length > 1 && rawIntPart.startsWith('0') -> rawIntPart.trimStart('0').ifEmpty { "0" }
            else -> rawIntPart
        }

        if (normalizedInt.length > maxIntDigits) return null

        return if (rawDecPart != null) {
            val trimmedDec = rawDecPart.take(maxDecDigits)
            "$normalizedInt.$trimmedDec"
        } else {
            normalizedInt
        }
    }

    /**
     * Visual length of a canonical string once formatted with Venezuelan '.' thousands separators.
     */
    fun visualLength(canonicalRaw: String): Int {
        if (canonicalRaw.isEmpty()) return 4 // "0,00" placeholder
        val sepIdx = canonicalRaw.indexOfFirst { it == '.' || it == ',' }
        val intLen = if (sepIdx >= 0) sepIdx else canonicalRaw.length
        val dots = if (intLen > 3) (intLen - 1) / 3 else 0
        return canonicalRaw.length + dots
    }

    /**
     * Adaptive font size for the main Converter input/output fields so large amounts
     * (e.g. "4.599.113.500,00") never clip or lose digits on narrow phones.
     */
    fun adaptiveConverterFontSize(canonicalRaw: String): TextUnit {
        val len = visualLength(canonicalRaw)
        return when {
            len <= 9 -> 28.sp
            len <= 12 -> 23.5.sp
            len <= 15 -> 20.sp
            len <= 18 -> 17.5.sp
            len <= 21 -> 15.sp
            else -> 13.sp
        }
    }

    /**
     * Adaptive font size for 3-column equivalence cells so values like "$5.000.000,00"
     * stay on a single crisp line instead of wrapping orphan digits.
     */
    fun adaptiveTriColumnFontSize(formattedText: String): TextUnit {
        val len = formattedText.length
        return when {
            len <= 9 -> 14.sp
            len <= 11 -> 12.2.sp
            len <= 13 -> 10.6.sp
            len <= 16 -> 9.2.sp
            else -> 8.sp
        }
    }

    /**
     * Adaptive font size for secondary 3-column Bolivar comparisons ("CON X EN CADA MONEDA").
     */
    fun adaptiveSecondaryTriColumnFontSize(formattedText: String): TextUnit {
        val len = formattedText.length
        return when {
            len <= 11 -> 11.sp
            len <= 14 -> 9.8.sp
            len <= 17 -> 8.8.sp
            else -> 7.8.sp
        }
    }

    /**
     * Transforms canonical raw numeric strings (e.g. "4599113500.00") into Venezuelan format
     * ("4.599.113.500,00") with exact bi-directional cursor offset mapping.
     */
    internal fun buildTransformed(raw: String): TransformedText {
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val n = raw.length
        val sepIndex = raw.indexOfFirst { it == '.' || it == ',' }
        val intEnd = if (sepIndex >= 0) sepIndex else n

        val sb = StringBuilder(n + (intEnd / 3) + 1)
        val origToTrans = IntArray(n + 1)
        val transToOrig = IntArray(n + (if (intEnd > 0) (intEnd - 1) / 3 else 0) + 1)
        var tIdx = 0

        for (i in 0 until n) {
            val ch = raw[i]
            val shouldInsertDot = i in 1 until intEnd &&
                (intEnd - i) % 3 == 0 &&
                ch.isDigit() &&
                raw[i - 1].isDigit()

            if (shouldInsertDot) {
                transToOrig[tIdx] = i
                sb.append('.')
                tIdx++
            }

            origToTrans[i] = tIdx
            transToOrig[tIdx] = i
            sb.append(if (ch == '.' || ch == ',') ',' else ch)
            tIdx++
        }

        origToTrans[n] = tIdx
        transToOrig[tIdx] = n

        val transformedString = sb.toString()
        val finalTransLen = transformedString.length

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, n)
                return origToTrans[safe].coerceIn(0, finalTransLen)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, finalTransLen)
                return transToOrig[safe].coerceIn(0, n)
            }
        }

        return TransformedText(AnnotatedString(transformedString), mapping)
    }
}

object VenezuelanNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return VeNumberFormat.buildTransformed(text.text)
    }
}
