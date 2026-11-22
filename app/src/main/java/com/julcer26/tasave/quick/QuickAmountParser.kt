package com.julcer26.tasave.quick

import java.util.Locale

data class ParsedQuickAmount(
    val amount: Double,
    val inputIsVes: Boolean,
    val suggestedRateKey: String = "USD_BCV"
)

/**
 * Extracts a clean monetary amount, currency direction, and suggested rate key
 * from text selected by the user inside another app (e.g., WhatsApp via PROCESS_TEXT).
 * When multiple numbers exist (e.g., "2 combos por $35"), prioritizes the number
 * attached to a currency symbol or label.
 * Runs 100% locally on-device on the explicitly selected snippet only.
 */
object QuickAmountParser {

    private val numberRegex = Regex("\\d[\\d.,]*\\d|\\d+")
    private val vesHintRegex = Regex("(\\d\\s*bs\\b|\\bbs\\.?\\s*\\d|\\bbs\\b|\\bves\\b|bol[ií]var)", RegexOption.IGNORE_CASE)
    private val foreignHintRegex = Regex("(\\$|\\busd\\b|€|\\beur\\b|\\beuros?\\b|\\busdt\\b|₮|\\bcop\\b|\\bpesos?\\b)", RegexOption.IGNORE_CASE)
    private val prefixCurrencyRegex = Regex("(\\$|\\busd|\\beur|€|\\busdt|₮|\\bcop|\\bpesos?|\\bbs\\.?|\\bves|bol[ií]var[a-z]*)\\s*:?\\s*$", RegexOption.IGNORE_CASE)
    private val suffixCurrencyRegex = Regex("^\\s*(\\$|\\busd\\b|\\beuros?\\b|\\beur\\b|€|\\busdt\\b|₮|\\bcop\\b|\\bpesos?\\b|\\bbs\\.?\\b|\\bves\\b|bol[ií]var)", RegexOption.IGNORE_CASE)

    private val eurHintRegex = Regex("(€|\\beur\\b|\\beuros?\\b)", RegexOption.IGNORE_CASE)
    private val usdtHintRegex = Regex("(\\busdt\\b|₮)", RegexOption.IGNORE_CASE)
    private val copHintRegex = Regex("(\\bcop\\b|\\bpesos?\\b)", RegexOption.IGNORE_CASE)

    fun parse(rawText: String?): ParsedQuickAmount? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()
        val candidates = numberRegex.findAll(trimmed).mapNotNull { match ->
            val normalized = normalizeNumberToken(match.value) ?: return@mapNotNull null
            val value = normalized.toDoubleOrNull() ?: return@mapNotNull null
            if (!value.isFinite() || value <= 0.0) return@mapNotNull null

            val prefixStart = (match.range.first - 14).coerceAtLeast(0)
            val prefix = trimmed.substring(prefixStart, match.range.first)
            val suffixEnd = (match.range.last + 1 + 14).coerceAtMost(trimmed.length)
            val suffix = trimmed.substring(match.range.last + 1, suffixEnd)

            var score = 0
            if (prefixCurrencyRegex.containsMatchIn(prefix)) score += 10
            if (suffixCurrencyRegex.containsMatchIn(suffix)) score += 10
            if (match.value.contains(',') || match.value.contains('.')) score += 2

            Triple(value, score, prefix + " " + match.value + " " + suffix)
        }.toList()

        if (candidates.isEmpty()) return null
        val chosen = candidates.maxByOrNull { it.second } ?: candidates.first()
        val amount = chosen.first
        val contextWindow = chosen.third.lowercase(Locale.ROOT)
        val fullLower = trimmed.lowercase(Locale.ROOT)

        val windowHasVes = vesHintRegex.containsMatchIn(contextWindow)
        val windowHasForeign = foreignHintRegex.containsMatchIn(contextWindow)
        val inputIsVes = when {
            windowHasVes && !windowHasForeign -> true
            windowHasForeign -> false
            else -> vesHintRegex.containsMatchIn(fullLower) && !foreignHintRegex.containsMatchIn(fullLower)
        }

        val suggestedRateKey = when {
            eurHintRegex.containsMatchIn(contextWindow) || eurHintRegex.containsMatchIn(fullLower) -> "EUR_BCV"
            usdtHintRegex.containsMatchIn(contextWindow) || usdtHintRegex.containsMatchIn(fullLower) -> "USDT"
            copHintRegex.containsMatchIn(contextWindow) || copHintRegex.containsMatchIn(fullLower) -> "COP"
            else -> "USD_BCV"
        }

        return ParsedQuickAmount(
            amount = amount,
            inputIsVes = inputIsVes,
            suggestedRateKey = suggestedRateKey
        )
    }

    internal fun normalizeNumberToken(token: String): String? {
        if (token.isBlank()) return null
        val dots = token.count { it == '.' }
        val commas = token.count { it == ',' }

        return when {
            dots > 0 && commas > 0 -> {
                val lastDot = token.lastIndexOf('.')
                val lastComma = token.lastIndexOf(',')
                if (lastComma > lastDot) {
                    // Venezuelan/European format: 1.500,25 -> 1500.25
                    token.replace(".", "").replace(',', '.')
                } else {
                    // US format: 1,500.25 -> 1500.25
                    token.replace(",", "")
                }
            }
            dots > 1 -> token.replace(".", "")
            commas > 1 -> token.replace(",", "")
            commas == 1 -> {
                val before = token.substringBefore(',')
                val after = token.substringAfter(',')
                if (after.length == 3 && before != "0") {
                    token.replace(",", "")
                } else {
                    token.replace(',', '.')
                }
            }
            dots == 1 -> {
                val before = token.substringBefore('.')
                val after = token.substringAfter('.')
                if (after.length == 3 && before != "0") {
                    token.replace(".", "")
                } else {
                    token
                }
            }
            else -> token
        }
    }
}
