package com.julcer26.tasave

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testCurrencyConversionMath() {
        val usdRate = 899.27
        val foreignAmount = 10.0
        val calculatedVes = foreignAmount * usdRate
        val formattedVes = String.format(Locale.US, "%.2f", calculatedVes)
        assertEquals("8992.70", formattedVes)

        val reversedForeign = calculatedVes / usdRate
        val formattedReversed = String.format(Locale.US, "%.2f", reversedForeign)
        assertEquals("10.00", formattedReversed)
    }

    @Test
    fun testSanitizeInput() {
        val input = "12,50"
        val sanitized = input.replace(",", ".").toDoubleOrNull()
        assertNotNull(sanitized)
        assertEquals(12.50, sanitized!!, 0.001)
    }
}
