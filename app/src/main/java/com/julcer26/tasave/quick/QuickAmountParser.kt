package com.julcer26.tasave.quick

import java.util.Locale

data class ParsedQuickAmount(
    val amount: Double,
    val inputIsVes: Boolean
)

/**
 * Extracts a clean monetary amount and likely currency direction from text
 * selected by the user inside another app (e.g., WhatsApp via PROCESS_TEXT).
 * Runs 100% locally on-device on the explicitly selected snippet only.
 */
object QuickAmountParser {

    private val numberRegex = Regex("\\d[\\d.,]*\\d|\\d+")
    private val vesHintRegex = Regex("(\\d\\s*bs\\b|\\bbs\\.?\\s*\\d|\\bbs\\b|\\bves\\b|bol[ií]var)", RegexOption.IGNORE_CASE)
    private val foreignHintRegex = Regex("(\\$|\\busd\\b|€|\\beur\\b|\\busdt\\b|\\bcop\\b)", RegexOption.IGNORE_CASE)

    fun parse(rawText: String?): ParsedQuickAmount? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()
        val match = numberRegex.find(trimmed) ?: return null
        val normalized = normalizeNumberToken(match.value) ?: return null
        val amount = normalized.toDoubleOrNull() ?: return null
        if (!amount.isFinite() || amount <= 0.0) return null

        val lower = trimmed.lowercase(Locale.ROOT)
        val hasVesHint = vesHintRegex.containsMatchIn(lower)
        val hasForeignHint = foreignHintRegex.containsMatchIn(lower)
        val inputIsVes = hasVesHint && !hasForeignHint

        return ParsedQuickAmount(
            amount = amount,
            inputIsVes = inputIsVes
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
