package com.julcer26.tasave.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceScannerParserTest {

    @Test
    fun parsesUsdFormats() {
        val prices = PriceScannerParser.extractPrices("Precio: $12.50\nREF 4.99\n# 8.50\n19,99 USD")
        assertEquals(4, prices.size)
        assertEquals(12.50, prices[0].amount, 0.001)
        assertEquals(CurrencyHint.USD, prices[0].currencyHint)
        assertEquals(4.99, prices[1].amount, 0.001)
        assertEquals(CurrencyHint.USD, prices[1].currencyHint)
        assertEquals(8.50, prices[2].amount, 0.001)
        assertEquals(CurrencyHint.USD, prices[2].currencyHint)
        assertEquals(19.99, prices[3].amount, 0.001)
        assertEquals(CurrencyHint.USD, prices[3].currencyHint)
    }

    @Test
    fun parsesVenezuelanBolivaresFormats() {
        val prices = PriceScannerParser.extractPrices("Total Bs. 1.250,50\nIVA 16%\n450,00 Bs")
        assertEquals(2, prices.size)
        assertEquals(1250.50, prices[0].amount, 0.001)
        assertEquals(CurrencyHint.VES, prices[0].currencyHint)
        assertEquals(450.00, prices[1].amount, 0.001)
        assertEquals(CurrencyHint.VES, prices[1].currencyHint)
    }

    @Test
    fun parsesEuroAndPlainNumbers() {
        val prices = PriceScannerParser.extractPrices("€ 15,25\n24.90")
        assertEquals(2, prices.size)
        assertEquals(15.25, prices[0].amount, 0.001)
        assertEquals(CurrencyHint.EUR, prices[0].currencyHint)
        assertEquals(24.90, prices[1].amount, 0.001)
        assertEquals(CurrencyHint.UNKNOWN, prices[1].currencyHint)
    }

    @Test
    fun ignoresDatesAndPercentages() {
        val prices = PriceScannerParser.extractPrices("Fecha: 17/11/2026\nDescuento 25%")
        assertTrue(prices.isEmpty())
    }
}
