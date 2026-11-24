package com.julcer26.tasave.scanner

import java.util.Locale
import kotlin.math.roundToLong

enum class CurrencyHint {
    USD,
    EUR,
    VES,
    UNKNOWN
}

data class DetectedPrice(
    val amount: Double,
    val rawText: String,
    val currencyHint: CurrencyHint,
    val confidence: Int = 1
)

data class StabilizedScanState(
    val detectedPrices: List<DetectedPrice>,
    val selectedPrice: DetectedPrice?,
    val isUserPinned: Boolean
)

/**
 * Temporal multi-frame stabilizer for live camera OCR price scanning.
 * Eliminates single-frame OCR glitches (e.g. "12.50" momentarily read as "1250"),
 * holds detections briefly across micro-shakes/autofocus frames, remembers currency hints
 * across adjacent frames, and preserves user-tapped price chips until unlocked.
 */
class PriceScannerStabilizer(
    private val windowMs: Long = 1_800L,
    private val emptyHoldMs: Long = 1_450L,
    private val maxRecentFrames: Int = 6
) {
    private data class FrameEntry(
        val timestampMs: Long,
        val prices: List<DetectedPrice>
    )

    private val recentFrames = ArrayDeque<FrameEntry>()
    private var lastStablePrices: List<DetectedPrice> = emptyList()
    private var lastStablePrimary: DetectedPrice? = null
    private var lastNonEmptyMs: Long = -1L
    private var userPinnedPrice: DetectedPrice? = null

    val isPinned: Boolean
        get() = userPinnedPrice != null

    fun pinPrice(price: DetectedPrice?) {
        userPinnedPrice = price
        if (price != null) {
            lastStablePrimary = price
        }
    }

    fun clearPin() {
        userPinnedPrice = null
    }

    fun reset() {
        recentFrames.clear()
        lastStablePrices = emptyList()
        lastStablePrimary = null
        lastNonEmptyMs = -1L
        userPinnedPrice = null
    }

    fun onFrame(extracted: List<DetectedPrice>, nowMs: Long): StabilizedScanState {
        while (recentFrames.isNotEmpty() && nowMs - recentFrames.first().timestampMs > windowMs) {
            recentFrames.removeFirst()
        }
        while (recentFrames.size >= maxRecentFrames) {
            recentFrames.removeFirst()
        }
        recentFrames.addLast(FrameEntry(nowMs, extracted))

        if (extracted.isEmpty()) {
            val pinned = userPinnedPrice
            if (pinned != null) {
                val listWithPinned = if (lastStablePrices.any { sameAmount(it.amount, pinned.amount) }) {
                    lastStablePrices
                } else {
                    listOf(pinned) + lastStablePrices
                }
                return StabilizedScanState(
                    detectedPrices = listWithPinned,
                    selectedPrice = pinned,
                    isUserPinned = true
                )
            }

            if (lastStablePrimary != null && lastNonEmptyMs >= 0L && (nowMs - lastNonEmptyMs) <= emptyHoldMs) {
                return StabilizedScanState(
                    detectedPrices = lastStablePrices,
                    selectedPrice = lastStablePrimary,
                    isUserPinned = false
                )
            }

            lastStablePrices = emptyList()
            lastStablePrimary = null
            return StabilizedScanState(
                detectedPrices = emptyList(),
                selectedPrice = null,
                isUserPinned = false
            )
        }

        lastNonEmptyMs = nowMs

        // Aggregate occurrences & upgrade UNKNOWN currency hints if a recent frame saw $, Bs, or €
        data class BucketStats(
            var count: Int,
            var bestCandidate: DetectedPrice,
            var inCurrentFrame: Boolean
        )

        val buckets = LinkedHashMap<Long, BucketStats>()
        val currentBucketKeys = HashSet<Long>()
        for (p in extracted) {
            currentBucketKeys.add(bucketKey(p.amount))
        }

        for (frame in recentFrames) {
            val seenInThisFrame = HashSet<Long>()
            for (p in frame.prices) {
                val key = bucketKey(p.amount)
                val existing = buckets[key]
                val isFirstInFrame = seenInThisFrame.add(key)
                if (existing == null) {
                    buckets[key] = BucketStats(
                        count = 1,
                        bestCandidate = p,
                        inCurrentFrame = key in currentBucketKeys
                    )
                } else {
                    if (isFirstInFrame) existing.count += 1
                    existing.inCurrentFrame = key in currentBucketKeys
                    // Upgrade candidate if this observation has an explicit currency hint or higher confidence
                    if (shouldPreferCandidate(newCand = p, currentBest = existing.bestCandidate)) {
                        existing.bestCandidate = p
                    }
                }
            }
        }

        // Build enriched candidate list from current frame (plus recent stable primary if flickering 1 frame)
        val mergedList = extracted.map { rawCand ->
            val stats = buckets[bucketKey(rawCand.amount)]
            stats?.bestCandidate ?: rawCand
        }.distinctBy { bucketKey(it.amount) }

        // Choose primary candidate using temporal consensus
        val prevPrimary = lastStablePrimary
        val prevKey = prevPrimary?.let { bucketKey(it.amount) }
        val prevStats = prevKey?.let { buckets[it] }

        val rankedCurrent = mergedList.sortedWith(
            compareByDescending<DetectedPrice> { cand ->
                val occ = buckets[bucketKey(cand.amount)]?.count ?: 1
                // Confirmed multi-frame candidates rank strongly above 1-frame noise
                val confirmedBonus = if (occ >= 2) 10 else 0
                confirmedBonus + cand.confidence
            }
        )

        val topCandidate = rankedCurrent.firstOrNull()
        val topOccurrences = topCandidate?.let { buckets[bucketKey(it.amount)]?.count } ?: 0

        val nextPrimary: DetectedPrice? = when {
            prevPrimary == null -> {
                if (topCandidate != null && (topCandidate.confidence >= 2 || topOccurrences >= 2)) {
                    topCandidate
                } else {
                    null
                }
            }
            prevStats != null && prevStats.inCurrentFrame -> {
                // Current primary is still visible inside reticle: keep it steady unless a much stronger
                // confirmed currency-tagged price has appeared consistently.
                val updatedPrev = prevStats.bestCandidate
                if (topCandidate != null &&
                    bucketKey(topCandidate.amount) != prevKey &&
                    topOccurrences >= 2 &&
                    topCandidate.confidence > updatedPrev.confidence
                ) {
                    topCandidate
                } else {
                    updatedPrev
                }
            }
            else -> {
                // Previous primary left the reticle; switch once new price is confirmed (>= 2 frames),
                // while holding previous primary for 1 transitional frame to prevent 1-frame jumps.
                if (topCandidate != null && topOccurrences >= 2) {
                    topCandidate
                } else {
                    prevPrimary
                }
            }
        }

        val finalPrices = buildList {
            if (nextPrimary != null) add(nextPrimary)
            for (item in rankedCurrent) {
                if (none { sameAmount(it.amount, item.amount) }) {
                    add(item)
                }
            }
        }

        lastStablePrices = finalPrices
        lastStablePrimary = nextPrimary

        val pinned = userPinnedPrice
        if (pinned != null) {
            val listWithPinned = if (finalPrices.any { sameAmount(it.amount, pinned.amount) }) {
                finalPrices
            } else {
                listOf(pinned) + finalPrices
            }
            return StabilizedScanState(
                detectedPrices = listWithPinned,
                selectedPrice = pinned,
                isUserPinned = true
            )
        }

        return StabilizedScanState(
            detectedPrices = finalPrices,
            selectedPrice = nextPrimary,
            isUserPinned = false
        )
    }

    private fun shouldPreferCandidate(newCand: DetectedPrice, currentBest: DetectedPrice): Boolean {
        if (newCand.currencyHint != CurrencyHint.UNKNOWN && currentBest.currencyHint == CurrencyHint.UNKNOWN) {
            return true
        }
        if (newCand.currencyHint == currentBest.currencyHint && newCand.confidence > currentBest.confidence) {
            return true
        }
        return false
    }

    private fun bucketKey(amount: Double): Long = (amount * 100.0).roundToLong()

    private fun sameAmount(a: Double, b: Double): Boolean = bucketKey(a) == bucketKey(b)
}

object PriceScannerParser {

    // Matches optional currency prefix, a formatted number, and optional currency suffix
    private val PRICE_REGEX = Regex(
        pattern = """(?i)(?:(US\$|USD|\$|REF\.?\s*:?|#|EUR|€|BS\.?[SD]?\s*:?|VES)\s*)?(\d+(?:[.,]\d+)*)(?:\s*(USD|\$|EUR|€|BS\.?[SD]?|VES))?"""
    )

    // Measurement units, quantities, percentages, and technical specs that should never be treated as prices
    private val TRAILING_UNIT_REGEX = Regex(
        pattern = """(?i)^\s*(%|g|gr|grs|kg|kgs|mg|ml|cc|l|lt|lts|oz|lb|lbs|cm|mm|m|mt|mts|km|und|unds|unid|unidades|uds|pz|pza|pzas|pack|paq|cja|h|hr|hrs|min|seg|kcal|cal|w|kw|mah|gb|mb|tb)\b"""
    )

    // Non-price identifier prefixes (codes, SKUs, RIF/CI, phones, lot numbers)
    private val NON_PRICE_PREFIX_REGEX = Regex(
        pattern = """(?i)(?:\b(?:cod(?:igo)?|c[oó]d|sku|plu|art(?:iculo)?|item|lote|lot|rif|c\.?i\.?|nit|tel(?:efono)?|telf|cel|fact(?:ura)?|nro|cant(?:idad)?)\s*[:.#-]?\s*)$"""
    )

    private val DOTTED_THOUSANDS_REGEX = Regex("""^\d{1,3}(\.\d{3})+$""")
    private val COMMA_THOUSANDS_REGEX = Regex("""^\d{1,3}(,\d{3})+$""")

    fun extractPrices(ocrText: String): List<DetectedPrice> {
        if (ocrText.isBlank()) return emptyList()

        val rawCandidates = mutableListOf<DetectedPrice>()
        val seenKeys = mutableSetOf<String>()

        for (line in ocrText.lines()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            for (match in PRICE_REGEX.findAll(trimmedLine)) {
                val fullMatch = match.value.trim()
                val prefix = match.groupValues[1].trim()
                val numberPart = match.groupValues[2].trim()
                val suffix = match.groupValues[3].trim()

                val startIdx = match.range.first
                val endIdx = match.range.last + 1
                val charBefore = if (startIdx > 0) trimmedLine[startIdx - 1] else ' '
                val charAfter = if (endIdx < trimmedLine.length) trimmedLine[endIdx] else ' '

                // 1. Reject numbers glued to letters/digits (e.g. "500g", "250ml", "B12", "2x1", "5G")
                val prefixStartsAlpha = prefix.firstOrNull()?.isLetter() == true
                if ((prefix.isEmpty() || prefixStartsAlpha) && charBefore.isLetterOrDigit()) {
                    continue
                }
                val suffixEndsAlpha = suffix.lastOrNull()?.isLetter() == true
                if ((suffix.isEmpty() || suffixEndsAlpha) && charAfter.isLetterOrDigit()) {
                    continue
                }

                // 2. Skip percentages and measurement units (e.g. "16%", "500 g", "1.5 L", "12 und")
                val tailAfterMatch = if (endIdx < trimmedLine.length) trimmedLine.substring(endIdx) else ""
                if (tailAfterMatch.trimStart().startsWith("%")) {
                    continue
                }
                if (suffix.isEmpty() && TRAILING_UNIT_REGEX.containsMatchIn(tailAfterMatch)) {
                    continue
                }

                // 3. Skip dates (17/11/2026), hyphenated codes/phones/RIFs (J-12345678, 0414-1234567), and times (14:30)
                if (charBefore == '/' || charAfter == '/' || charBefore == '-' || charAfter == '-') {
                    continue
                }
                val isTimeBefore = charBefore == ':' && startIdx >= 2 && trimmedLine[startIdx - 2].isDigit()
                val isTimeAfter = charAfter == ':' && endIdx < trimmedLine.length &&
                    trimmedLine.getOrNull(endIdx + 1)?.isDigit() == true
                if (isTimeBefore || isTimeAfter) {
                    continue
                }

                // 4. Reject leading-zero codes (e.g. "0414", "0134", "00125") while keeping "0.50" / "0,75"
                if (numberPart.length > 1 && numberPart[0] == '0' && numberPart[1].isDigit()) {
                    continue
                }

                val hint = resolveCurrencyHint(prefix, suffix)

                // 5. Reject non-price identifier labels when no currency tag is present
                if (hint == CurrencyHint.UNKNOWN && startIdx > 0) {
                    val textBefore = trimmedLine.substring(0, startIdx)
                    if (NON_PRICE_PREFIX_REGEX.containsMatchIn(textBefore)) {
                        continue
                    }
                }

                val parsedValue = parseFlexibleNumber(numberPart) ?: continue
                if (parsedValue <= 0.0 || parsedValue > 10_000_000.0) continue

                val hasDecimals = hasExplicitDecimalPart(numberPart)

                // 6. Reject plain un-tagged 6+ digit integers (barcodes, SKUs, cedulas without Bs/$)
                if (hint == CurrencyHint.UNKNOWN && !hasDecimals && parsedValue >= 100_000.0) {
                    continue
                }

                val confidence = when {
                    hint != CurrencyHint.UNKNOWN && hasDecimals -> 4
                    hint != CurrencyHint.UNKNOWN -> 3
                    hasDecimals -> 2
                    parsedValue >= 10.0 -> 1
                    else -> 0
                }

                val dedupKey = "${String.format(Locale.US, "%.2f", parsedValue)}_${hint.name}"
                if (seenKeys.add(dedupKey)) {
                    rawCandidates.add(
                        DetectedPrice(
                            amount = parsedValue,
                            rawText = fullMatch,
                            currencyHint = hint,
                            confidence = confidence
                        )
                    )
                }
            }
        }

        val hasStrongCandidate = rawCandidates.any { it.confidence >= 2 }
        val filtered = if (hasStrongCandidate) {
            rawCandidates.filter { it.confidence > 0 }
        } else {
            rawCandidates
        }

        // Stable sort: prioritizes currency-tagged & decimal-formatted prices while preserving line order within ties
        return filtered.sortedByDescending { it.confidence }
    }

    internal fun hasExplicitDecimalPart(raw: String): Boolean {
        val lastDot = raw.lastIndexOf('.')
        val lastComma = raw.lastIndexOf(',')
        val lastSep = maxOf(lastDot, lastComma)
        if (lastSep < 0) return false
        val after = raw.substring(lastSep + 1)
        val before = raw.substring(0, lastSep)
        val totalSeps = raw.count { it == '.' || it == ',' }
        return when {
            after.length in 1..2 -> true
            after.length == 3 && before == "0" && totalSeps == 1 -> true
            else -> false
        }
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
                    // Format like 1.250,50 -> validate dotted thousands before lastComma
                    val intPart = raw.substring(0, lastComma)
                    val decPart = raw.substring(lastComma + 1)
                    if (decPart.isEmpty() || decPart.length > 4 || decPart.contains('.')) return null
                    if (intPart.contains(',') || !DOTTED_THOUSANDS_REGEX.matches(intPart)) return null
                    intPart.replace(".", "") + "." + decPart
                } else {
                    // Format like 1,250.50 -> validate comma thousands before lastDot
                    val intPart = raw.substring(0, lastDot)
                    val decPart = raw.substring(lastDot + 1)
                    if (decPart.isEmpty() || decPart.length > 4 || decPart.contains(',')) return null
                    if (intPart.contains('.') || !COMMA_THOUSANDS_REGEX.matches(intPart)) return null
                    intPart.replace(",", "") + "." + decPart
                }
            }
            hasComma -> {
                val commaCount = raw.count { it == ',' }
                if (commaCount > 1) {
                    // Format like 1,250,000 (reject dates/serials like 17,11,26)
                    if (!COMMA_THOUSANDS_REGEX.matches(raw)) return null
                    raw.replace(",", "")
                } else {
                    val parts = raw.split(',')
                    val before = parts.getOrNull(0).orEmpty()
                    val decimals = parts.getOrNull(1).orEmpty()
                    if (before.isEmpty() || decimals.isEmpty() || decimals.length > 4) return null
                    if (decimals.length == 3 && before != "0") {
                        // Format like 1,250
                        if (before.length > 3) return null
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
                    // Format like 1.250.000 (reject dotted dates/versions like 17.11.26 or 1.2.0)
                    if (!DOTTED_THOUSANDS_REGEX.matches(raw)) return null
                    raw.replace(".", "")
                } else {
                    val parts = raw.split('.')
                    val before = parts.getOrNull(0).orEmpty()
                    val decimals = parts.getOrNull(1).orEmpty()
                    if (before.isEmpty() || decimals.isEmpty() || decimals.length > 4) return null
                    if (decimals.length == 3 && before != "0") {
                        // Format like 1.250
                        if (before.length > 3) return null
                        raw.replace(".", "")
                    } else {
                        // Format like 12.50
                        raw
                    }
                }
            }
            else -> raw
        }

        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    private fun resolveCurrencyHint(prefix: String, suffix: String): CurrencyHint {
        val tag = (prefix.ifEmpty { suffix }).uppercase(Locale.US)
        return when {
            tag.contains("$") || tag.contains("USD") || tag.contains("REF") || tag == "#" -> CurrencyHint.USD
            tag.contains("€") || tag.contains("EUR") -> CurrencyHint.EUR
            tag.contains("BS") || tag.contains("VES") -> CurrencyHint.VES
            else -> CurrencyHint.UNKNOWN
        }
    }
}
