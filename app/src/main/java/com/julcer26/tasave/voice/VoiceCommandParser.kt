package com.julcer26.tasave.voice

import java.text.Normalizer
import java.util.Locale

data class ParsedVoiceConversion(
    val amount: Double,
    val inputIsVes: Boolean,
    val rateKey: String,
    val useTomorrowBcv: Boolean,
    val rawTranscript: String
)

/**
 * Parses natural Venezuelan Spanish voice phrases into a structured currency conversion command.
 *
 * Examples supported:
 * - "38 con 50 dólares a BCV" -> 38.50 USD_BCV -> Bs.
 * - "47 dólares con 50" -> 47.50 USD_BCV -> Bs.
 * - "cien dólares" -> 100.0 USD_BCV -> Bs.
 * - "25 dólares" -> 25.0 USD_BCV -> Bs.
 * - "20 dólares a euro" / "50 euros" -> 20.0 / 50.0 EUR_BCV -> Bs.
 * - "15 mil bolívares a dólares" -> 15000.0 Bs. -> USD_BCV
 * - "12 mil 500 bolívares a USDT" -> 12500.0 Bs. -> USDT
 * - "50 dólares mañana" -> 50.0 USD_BCV (tomorrow) -> Bs.
 * - "cien mil pesos" / "50 mil COP" -> 100000.0 / 50000.0 COP -> Bs.
 */
object VoiceCommandParser {

    private val tomorrowRegex = Regex(
        "\\b(manana|proximo|proxima|siguiente)\\b",
        RegexOption.IGNORE_CASE
    )

    private val vesRegex = Regex(
        "\\b(bolivares|bolivar|bs|ves|bolos|soberanos|digitales)\\b",
        RegexOption.IGNORE_CASE
    )
    private val usdRegex = Regex(
        "(\\$|\\b(dolares|dolar|usd|bcv|oficial|verdes|verde)\\b)",
        RegexOption.IGNORE_CASE
    )
    private val eurRegex = Regex(
        "(€|\\b(euros|euro|eur)\\b)",
        RegexOption.IGNORE_CASE
    )
    private val usdtRegex = Regex(
        "(₮|\\b(usdt|binance|paralelo|cripto|tether|monitor)\\b)",
        RegexOption.IGNORE_CASE
    )
    private val copRegex = Regex(
        "\\b(pesos|peso|cop|colombianos|colombiano)\\b",
        RegexOption.IGNORE_CASE
    )

    private val currencyUnitWords = setOf(
        "dolar", "dolares", "usd", "euro", "euros", "eur",
        "usdt", "peso", "pesos", "cop", "colombiano", "colombianos",
        "bolivar", "bolivares", "bs", "ves", "bolos", "bcv", "binance", "paralelo"
    )

    private val singleWords: Map<String, Long> = mapOf(
        "cero" to 0L,
        "un" to 1L, "uno" to 1L, "una" to 1L,
        "dos" to 2L, "tres" to 3L, "cuatro" to 4L, "cinco" to 5L,
        "seis" to 6L, "siete" to 7L, "ocho" to 8L, "nueve" to 9L,
        "diez" to 10L, "once" to 11L, "doce" to 12L, "trece" to 13L, "catorce" to 14L,
        "quince" to 15L, "dieciseis" to 16L, "diecisiete" to 17L, "dieciocho" to 18L, "diecinueve" to 19L,
        "veinte" to 20L, "veintiuno" to 21L, "veintiun" to 21L, "veintidos" to 22L, "veintitres" to 23L,
        "veinticuatro" to 24L, "veinticinco" to 25L, "veintiseis" to 26L, "veintisiete" to 27L,
        "veintiocho" to 28L, "veintinueve" to 29L,
        "treinta" to 30L, "cuarenta" to 40L, "cincuenta" to 50L,
        "sesenta" to 60L, "setenta" to 70L, "ochenta" to 80L, "noventa" to 90L,
        "cien" to 100L, "ciento" to 100L,
        "doscientos" to 200L, "doscientas" to 200L,
        "trescientos" to 300L, "trescientas" to 300L,
        "cuatrocientos" to 400L, "cuatrocientas" to 400L,
        "quinientos" to 500L, "quinientas" to 500L,
        "seiscientos" to 600L, "seiscientas" to 600L,
        "setecientos" to 700L, "setecientas" to 700L,
        "ochocientos" to 800L, "ochocientas" to 800L,
        "novecientos" to 900L, "novecientas" to 900L
    )

    fun parse(rawTranscript: String?): ParsedVoiceConversion? {
        if (rawTranscript.isNullOrBlank()) return null
        val cleaned = normalizeForParsing(rawTranscript)
        if (cleaned.isBlank()) return null

        val amount = extractAmount(cleaned) ?: return null
        if (!amount.isFinite() || amount <= 0.0) return null

        val useTomorrow = tomorrowRegex.containsMatchIn(cleaned)

        // Split around target prepositions (" a ", " en ", " para ") to distinguish source vs target currency
        val splitMatch = Regex("\\s+(?:a|en|para)\\s+").find(cleaned)
        val sourcePart = if (splitMatch != null) cleaned.substring(0, splitMatch.range.first) else cleaned
        val targetPart = if (splitMatch != null) cleaned.substring(splitMatch.range.last + 1) else ""

        val sourceHasVes = vesRegex.containsMatchIn(sourcePart)
        val sourceHasForeign = hasForeignCurrency(sourcePart)
        val targetHasVes = vesRegex.containsMatchIn(targetPart)
        val targetHasForeign = hasForeignCurrency(targetPart)

        val inputIsVes = when {
            sourceHasVes && !sourceHasForeign -> true
            sourceHasForeign -> false
            targetHasVes -> false
            targetHasForeign -> true
            else -> vesRegex.containsMatchIn(cleaned) && !hasForeignCurrency(cleaned)
        }

        val rateKey = resolveRateKey(sourcePart, targetPart, cleaned, inputIsVes)

        return ParsedVoiceConversion(
            amount = amount,
            inputIsVes = inputIsVes,
            rateKey = rateKey,
            useTomorrowBcv = useTomorrow && rateKey != "USDT",
            rawTranscript = rawTranscript.trim()
        )
    }

    private fun hasForeignCurrency(text: String): Boolean {
        return usdRegex.containsMatchIn(text) ||
            eurRegex.containsMatchIn(text) ||
            usdtRegex.containsMatchIn(text) ||
            copRegex.containsMatchIn(text)
    }

    private fun resolveRateKey(
        sourcePart: String,
        targetPart: String,
        fullText: String,
        inputIsVes: Boolean
    ): String {
        // When user explicitly specifies a target rate (e.g. "12 mil 500 bolívares a USDT",
        // "20 dólares a euro", "100 dólares a paralelo"), prioritize target rate first.
        if (targetPart.isNotBlank()) {
            when {
                usdtRegex.containsMatchIn(targetPart) -> return "USDT"
                eurRegex.containsMatchIn(targetPart) -> return "EUR_BCV"
                copRegex.containsMatchIn(targetPart) -> return "COP"
                usdRegex.containsMatchIn(targetPart) -> return "USD_BCV"
            }
        }

        // Next check the source part (e.g. "50 euros", "cien mil pesos", "100 USDT")
        if (!inputIsVes) {
            when {
                usdtRegex.containsMatchIn(sourcePart) -> return "USDT"
                eurRegex.containsMatchIn(sourcePart) -> return "EUR_BCV"
                copRegex.containsMatchIn(sourcePart) -> return "COP"
                usdRegex.containsMatchIn(sourcePart) -> return "USD_BCV"
            }
        }

        // Fallback across full text
        return when {
            usdtRegex.containsMatchIn(fullText) -> "USDT"
            eurRegex.containsMatchIn(fullText) -> "EUR_BCV"
            copRegex.containsMatchIn(fullText) -> "COP"
            else -> "USD_BCV"
        }
    }

    internal fun extractAmount(normalized: String): Double? {
        // Tokenize preserving numeric tokens (including decimals/thousands) and words
        val tokens = normalized
            .replace("$", " $ ")
            .replace("€", " € ")
            .replace("₮", " ₮ ")
            .replace(Regex("[^a-z0-9.,$€₮\\s]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return null

        // Special colloquial pattern: "medio millon" -> 500000
        for (i in 0 until tokens.size - 1) {
            if (tokens[i] == "medio" && (tokens[i + 1] == "millon" || tokens[i + 1] == "millones")) {
                return 500_000.0
            }
        }

        var idx = 0
        while (idx < tokens.size) {
            if (isNumberStartToken(tokens[idx])) {
                val (intPart, nextIdx) = consumeIntegerValue(tokens, idx)
                if (intPart != null && intPart > 0.0) {
                    val fraction = consumeFractionalSuffix(tokens, nextIdx)
                    return intPart + fraction
                }
            }
            idx++
        }
        return null
    }

    private fun isNumberStartToken(token: String): Boolean {
        if (token.firstOrNull()?.isDigit() == true) return true
        if (token in singleWords) return true
        if (token == "mil" || token == "millon" || token == "millones") return true
        return false
    }

    /**
     * Consumes an integer or already-decimal numeric phrase starting at [startIdx].
     * Supports mixed forms like "12 mil 500", "15 mil", "cien mil", "treinta y ocho", "38.50", "38,50".
     */
    private fun consumeIntegerValue(tokens: List<String>, startIdx: Int): Pair<Double?, Int> {
        var i = startIdx
        var total = 0.0
        var current = 0.0
        var consumedAny = false
        var hasDirectDecimal = false

        while (i < tokens.size) {
            val tok = tokens[i]
            when {
                tok == "y" -> {
                    // Only bridge "treinta y ocho", "cincuenta y cinco", etc. (not "y medio")
                    val next = tokens.getOrNull(i + 1)
                    if (next != null && (next in singleWords || next.firstOrNull()?.isDigit() == true)) {
                        i++
                    } else {
                        break
                    }
                }
                tok == "mil" -> {
                    val base = if (current == 0.0) 1.0 else current
                    total += base * 1_000.0
                    current = 0.0
                    consumedAny = true
                    i++
                }
                tok == "millon" || tok == "millones" -> {
                    val base = if (current == 0.0) 1.0 else current
                    total += base * 1_000_000.0
                    current = 0.0
                    consumedAny = true
                    i++
                }
                tok in singleWords -> {
                    val v = singleWords.getValue(tok).toDouble()
                    current += v
                    consumedAny = true
                    i++
                }
                tok.firstOrNull()?.isDigit() == true -> {
                    val parsedNum = parseNumericToken(tok) ?: break
                    // If we already have a decimal value, don't merge another standalone number
                    if (hasDirectDecimal) break
                    if (tok.contains('.') || tok.contains(',')) {
                        val isDecimalToken = !isThousandsOnlyToken(tok)
                        if (isDecimalToken) {
                            hasDirectDecimal = true
                        }
                    }
                    current += parsedNum
                    consumedAny = true
                    i++
                }
                else -> break
            }
        }

        if (!consumedAny) return null to startIdx
        return (total + current) to i
    }

    /**
     * Consumes optional fractional suffix after the main integer, allowing up to 2 currency words
     * in between (e.g. "47 dólares con 50", "38 con 50", "38 punto 5", "10 dólares y medio").
     */
    private fun consumeFractionalSuffix(tokens: List<String>, startIdx: Int): Double {
        var i = startIdx
        var skippedCurrencyWords = 0
        while (i < tokens.size && skippedCurrencyWords < 2 && tokens[i] in currencyUnitWords) {
            skippedCurrencyWords++
            i++
        }
        if (i >= tokens.size) return 0.0

        val connector = tokens[i]
        // "y medio" / "con medio" / "y media"
        if ((connector == "y" || connector == "con") &&
            (tokens.getOrNull(i + 1) == "medio" || tokens.getOrNull(i + 1) == "media")
        ) {
            return 0.5
        }

        if (connector == "con" || connector == "punto" || connector == "coma") {
            val fracStart = i + 1
            val firstFracToken = tokens.getOrNull(fracStart) ?: return 0.0

            // Handle explicit leading-zero digits like "con 05" -> 0.05
            if (firstFracToken.all { it.isDigit() }) {
                val digits = firstFracToken
                return when (digits.length) {
                    1 -> digits.toDouble() / 10.0
                    2 -> digits.toDouble() / 100.0
                    else -> ("0.$digits").toDoubleOrNull() ?: 0.0
                }
            }

            // Handle spoken cents like "con cincuenta", "con veinticinco", "con cinco"
            val (wordVal, _) = consumeIntegerValue(tokens, fracStart)
            if (wordVal != null && wordVal >= 0.0 && wordVal < 100.0) {
                return if (wordVal < 10.0 && connector != "con") {
                    wordVal / 10.0
                } else if (wordVal < 10.0) {
                    wordVal / 100.0
                } else {
                    wordVal / 100.0
                }
            }
        }
        return 0.0
    }

    private fun isThousandsOnlyToken(token: String): Boolean {
        val dots = token.count { it == '.' }
        val commas = token.count { it == ',' }
        if (dots > 1 && commas == 0) return true
        if (commas > 1 && dots == 0) return true
        if (dots == 1 && commas == 0) {
            val before = token.substringBefore('.')
            val after = token.substringAfter('.')
            return after.length == 3 && before != "0"
        }
        if (commas == 1 && dots == 0) {
            val before = token.substringBefore(',')
            val after = token.substringAfter(',')
            return after.length == 3 && before != "0"
        }
        return false
    }

    private fun parseNumericToken(token: String): Double? {
        val dots = token.count { it == '.' }
        val commas = token.count { it == ',' }
        val normalized = when {
            dots > 0 && commas > 0 -> {
                if (token.lastIndexOf(',') > token.lastIndexOf('.')) {
                    token.replace(".", "").replace(',', '.')
                } else {
                    token.replace(",", "")
                }
            }
            dots > 1 -> token.replace(".", "")
            commas > 1 -> token.replace(",", "")
            commas == 1 -> {
                val before = token.substringBefore(',')
                val after = token.substringAfter(',')
                if (after.length == 3 && before != "0") token.replace(",", "") else token.replace(',', '.')
            }
            dots == 1 -> {
                val before = token.substringBefore('.')
                val after = token.substringAfter('.')
                if (after.length == 3 && before != "0") token.replace(".", "") else token
            }
            else -> token
        }
        return normalized.toDoubleOrNull()
    }

    private fun normalizeForParsing(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").trim()
    }

    /**
     * Builds a clean, natural Spanish sentence for Android TextToSpeech.
     */
    fun buildSpokenResult(
        parsed: ParsedVoiceConversion,
        convertedValue: Double,
        effectiveTomorrow: Boolean
    ): String {
        val rateSpoken = when (parsed.rateKey) {
            "EUR_BCV" -> if (effectiveTomorrow) "euro B C V de mañana" else "euro B C V"
            "USDT" -> "U S D T Binance"
            "COP" -> if (effectiveTomorrow) "peso colombiano de mañana" else "peso colombiano"
            else -> if (effectiveTomorrow) "dólar B C V de mañana" else "dólar B C V"
        }

        val foreignUnit = when (parsed.rateKey) {
            "EUR_BCV" -> "euros"
            "USDT" -> "U S D T"
            "COP" -> "pesos colombianos"
            else -> "dólares"
        }

        fun formatSpokenNumber(value: Double, decimals: Int = 2): String {
            return if (decimals == 0 || (value % 1.0 == 0.0 && value < 1_000_000_000.0)) {
                String.format(Locale.US, "%.0f", value)
            } else {
                String.format(Locale.US, "%.2f", value).replace('.', ',')
            }
        }

        return if (parsed.inputIsVes) {
            val fromStr = formatSpokenNumber(parsed.amount)
            val toStr = formatSpokenNumber(convertedValue, if (parsed.rateKey == "COP") 0 else 2)
            "$fromStr bolívares a $rateSpoken son $toStr $foreignUnit."
        } else {
            val fromStr = formatSpokenNumber(parsed.amount, if (parsed.rateKey == "COP") 0 else 2)
            val toStr = formatSpokenNumber(convertedValue)
            "$fromStr $foreignUnit a $rateSpoken son $toStr bolívares."
        }
    }
}
