package com.julcer26.tasave.scanner

import java.util.Locale

enum class CurrencyHint {
    USD,
    EUR,
    VES,
    UNKNOWN
}

data class DetectedPrice(
    val amount: Double,
    val rawText: String,
    val currencyHint: CurrencyHint
)

object PriceScannerParser {

    // Matches optional currency prefix, a formatted number, and optional currency suffix
    private val PRICE_REGEX = Regex(
        pattern = """(?i)(?:(US\$|USD|\$|REF\.?|EUR|€|BS\.?[SD]?|VES)\s*)?(\d+(?:[.,]\d+)*)(?:\s*(USD|\$|EUR|€|BS\.?[SD]?|VES))?"""
    )

    fun extractPrices(ocrText: String): List<DetectedPrice> {
        if (ocrText.isBlank()) return emptyList()

        val results = mutableListOf<DetectedPrice>()
        val seenKeys = mutableSetOf<String>()

        for (line in ocrText.lines()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            for (match in PRICE_REGEX.findAll(trimmedLine)) {
                val fullMatch = match.value.trim()
                val prefix = match.groupValues[1].trim()
                val numberPart = match.groupValues[2].trim()
                val suffix = match.groupValues[3].trim()

                // Skip percentages (e.g. "16%", "10.5 %")
                val endIdx = match.range.last + 1
                if (endIdx < trimmedLine.length && trimmedLine.substring(endIdx).trimStart().startsWith("%")) {
                    continue
                }

                // Skip numbers that are part of a date like 17/11/2026 or 17-11-2026
                val startIdx = match.range.first
                val charBefore = if (startIdx > 0) trimmedLine[startIdx - 1] else ' '
                val charAfter = if (endIdx < trimmedLine.length) trimmedLine[endIdx] else ' '
                if (charBefore == '/' || charBefore == '-' || charAfter == '/' || charAfter == '-') {
                    continue
                }

                val parsedValue = parseFlexibleNumber(numberPart) ?: continue
                if (parsedValue <= 0.0 || parsedValue > 10_000_000.0) continue

                val hint = resolveCurrencyHint(prefix, suffix)
                val dedupKey = "${String.format(Locale.US, "%.2f", parsedValue)}_${hint.name}"
                if (seenKeys.add(dedupKey)) {
                    results.add(
                        DetectedPrice(
                            amount = parsedValue,
                            rawText = fullMatch,
                            currencyHint = hint
                        )
                    )
                }
            }
        }

        // Prioritize prices that have an explicit currency symbol/tag
        return results.sortedByDescending { it.currencyHint != CurrencyHint.UNKNOWN }
    }

    internal fun parseFlexibleNumber(raw: String): Double? {
        if (raw.isBlank()) return null

        val hasDot = raw.contains('.')
        val hasComma = raw.contains(',')

        val normalized = when {
            hasDot && hasComma -> {
                val lastDot = raw.lastIndexOf('.')
                val lastComma = raw.lastIndexOf(',')
                if (lastComma > lastDot) {
                    // Format like 1.250,50 -> dots are thousands, comma is decimal
                    raw.replace(".", "").replace(',', '.')
                } else {
                    // Format like 1,250.50 -> commas are thousands, dot is decimal
                    raw.replace(",", "")
                }
            }
            hasComma -> {
                val commaCount = raw.count { it == ',' }
                if (commaCount > 1) {
                    // Format like 1,250,000
                    raw.replace(",", "")
                } else {
                    val parts = raw.split(',')
                    val decimals = parts.getOrNull(1) ?: ""
                    if (decimals.length == 3 && parts[0] != "0") {
                        // Format like 1,250
                        raw.replace(",", "")
                    } else {
                        // Format like 12,50 or 5,5
                        raw.replace(',', '.')
                    }
                }
            }
            hasDot -> {
                val dotCount = raw.count { it == '.' }
                if (dotCount > 1) {
                    // Format like 1.250.000
                    raw.replace(".", "")
                } else {
                    val parts = raw.split('.')
                    val decimals = parts.getOrNull(1) ?: ""
                    if (decimals.length == 3 && parts[0] != "0") {
                        // Format like 1.250
                        raw.replace(".", "")
                    } else {
                        // Format like 12.50
                        raw
                    }
                }
            }
            else -> raw
        }

        return normalized.toDoubleOrNull()
    }

    private fun resolveCurrencyHint(prefix: String, suffix: String): CurrencyHint {
        val tag = (prefix.ifEmpty { suffix }).uppercase(Locale.US)
        return when {
            tag.contains("$") || tag.contains("USD") || tag.contains("REF") -> CurrencyHint.USD
            tag.contains("€") || tag.contains("EUR") -> CurrencyHint.EUR
            tag.contains("BS") || tag.contains("VES") -> CurrencyHint.VES
            else -> CurrencyHint.UNKNOWN
        }
    }
}
