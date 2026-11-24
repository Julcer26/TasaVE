package com.julcer26.tasave

import androidx.compose.ui.text.AnnotatedString
import com.julcer26.tasave.ui.VeNumberFormat
import com.julcer26.tasave.ui.VenezuelanNumberVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VeNumberFormatTest {

    @Test
    fun formatsLargeAmountsAndRatesInVenezuelanBankingStyle() {
        assertEquals("4.599.113.500,00", VeNumberFormat.formatMoney(4_599_113_500.0))
        assertEquals("5.000.000,00", VeNumberFormat.formatMoney(5_000_000.0))
        assertEquals("0,50", VeNumberFormat.formatMoney(0.5))
        assertEquals("17.529.600.000", VeNumberFormat.formatInt(17_529_600_000.0))
        assertEquals("919,8227", VeNumberFormat.formatRate(919.8227, 4))
        assertEquals("1.011,1795", VeNumberFormat.formatRate(1011.1795, 4))
        assertEquals("1.107,05", VeNumberFormat.formatRate(1107.05, 2))
    }

    @Test
    fun sanitizesTypingAndPastedVenezuelanAndUsNumbers() {
        // Typing comma or dot acts as decimal point
        assertEquals("1250.", VeNumberFormat.sanitizeEditableInput("1250,", "1250"))
        assertEquals("1250.5", VeNumberFormat.sanitizeEditableInput("1250.5", "1250."))

        // Pasting Venezuelan formatted strings
        assertEquals("4599113.50", VeNumberFormat.sanitizeEditableInput("4.599.113,50", ""))
        assertEquals("5000000", VeNumberFormat.sanitizeEditableInput("5.000.000", ""))
        assertEquals("5000", VeNumberFormat.sanitizeEditableInput("5.000", ""))

        // Pasting US formatted strings
        assertEquals("1250.75", VeNumberFormat.sanitizeEditableInput("1,250.75", ""))
        assertEquals("5000000", VeNumberFormat.sanitizeEditableInput("5,000,000", ""))

        // Rejects invalid letters or exceeding max integer digits
        assertNull(VeNumberFormat.sanitizeEditableInput("12abc", "12"))
        assertNull(VeNumberFormat.sanitizeEditableInput("12345678901234", "1234567890123"))
    }

    @Test
    fun visualTransformationFormatsAndMaintainsValidMonotonicCursorOffsets() {
        val cases = listOf(
            "" to "",
            "0" to "0",
            "45" to "45",
            "1234" to "1.234",
            "5000000" to "5.000.000",
            "4599113500.00" to "4.599.113.500,00",
            "1250." to "1.250,",
            "0.50" to "0,50",
            "9999999999999.99" to "9.999.999.999.999,99"
        )

        for ((raw, expectedVisual) in cases) {
            val result = VenezuelanNumberVisualTransformation.filter(AnnotatedString(raw))
            assertEquals(expectedVisual, result.text.text)

            val mapping = result.offsetMapping
            var prevTrans = 0
            for (origOffset in 0..raw.length) {
                val trans = mapping.originalToTransformed(origOffset)
                assertTrue(
                    "orig=$origOffset -> trans=$trans out of bounds for '$raw'",
                    trans in 0..expectedVisual.length
                )
                assertTrue(
                    "origToTrans not monotonic at $origOffset for '$raw'",
                    trans >= prevTrans
                )
                prevTrans = trans
            }

            var prevOrig = 0
            for (transOffset in 0..expectedVisual.length) {
                val orig = mapping.transformedToOriginal(transOffset)
                assertTrue(
                    "trans=$transOffset -> orig=$orig out of bounds for '$raw'",
                    orig in 0..raw.length
                )
                assertTrue(
                    "transToOrig not monotonic at $transOffset for '$raw'",
                    orig >= prevOrig
                )
                prevOrig = orig
            }
        }
    }
}
