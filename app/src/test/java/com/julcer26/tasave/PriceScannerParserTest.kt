package com.julcer26.tasave.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        val prices = PriceScannerParser.extractPrices("Fecha: 17/11/2026\nVence 17.11.26\nDescuento 25%")
        assertTrue(prices.isEmpty())
    }

    @Test
    fun ignoresWeightsVolumesBarcodesPhonesAndProductCodes() {
        val shelfLabel = """
            HARINA DE MAIZ 1 kg
            CONT. NETO 500g / 250 ml / 1.5 L
            PROMO 2x1 VITAMINA B12
            COD: 00452  SKU 89214
            RIF J-123456789  TEL 0414-1234567
            HORA 14:35
            7591234567890
            REF 2,45
        """.trimIndent()

        val prices = PriceScannerParser.extractPrices(shelfLabel)
        assertEquals(1, prices.size)
        assertEquals(2.45, prices[0].amount, 0.001)
        assertEquals(CurrencyHint.USD, prices[0].currencyHint)
    }

    @Test
    fun stabilizerPreventsSingleFrameGlitchesHoldsEmptyFramesAndRespectsUserPin() {
        val stabilizer = PriceScannerStabilizer(windowMs = 1_800L, emptyHoldMs = 1_450L)
        val realUsd = DetectedPrice(12.50, "$12.50", CurrencyHint.USD, confidence = 4)
        val blurryNoSymbol = DetectedPrice(12.50, "12.50", CurrencyHint.UNKNOWN, confidence = 2)
        val singleFrameGlitch = DetectedPrice(1250.0, "1250", CurrencyHint.UNKNOWN, confidence = 1)
        val secondaryBs = DetectedPrice(1150.0, "Bs. 1.150,00", CurrencyHint.VES, confidence = 4)

        // Frame 1: instant lock on high-confidence $12.50
        val s1 = stabilizer.onFrame(listOf(realUsd, secondaryBs), nowMs = 1_000L)
        assertEquals(12.50, s1.selectedPrice!!.amount, 0.001)
        assertEquals(CurrencyHint.USD, s1.selectedPrice!!.currencyHint)

        // Frame 2: single-frame OCR miss of decimal ("1250") must NOT hijack primary selection
        val s2 = stabilizer.onFrame(listOf(singleFrameGlitch), nowMs = 1_300L)
        assertEquals(12.50, s2.selectedPrice!!.amount, 0.001)

        // Frame 3: $ symbol blurs out ("12.50" UNKNOWN), stabilizer remembers CurrencyHint.USD
        val s3 = stabilizer.onFrame(listOf(blurryNoSymbol, secondaryBs), nowMs = 1_600L)
        assertEquals(12.50, s3.selectedPrice!!.amount, 0.001)
        assertEquals(CurrencyHint.USD, s3.selectedPrice!!.currencyHint)

        // Frame 4: brief hand shake (empty OCR) holds last stable price within grace window
        val s4 = stabilizer.onFrame(emptyList(), nowMs = 2_200L)
        assertNotNull(s4.selectedPrice)
        assertEquals(12.50, s4.selectedPrice!!.amount, 0.001)

        // User taps secondary Bs chip -> stays pinned even when subsequent frames arrive
        stabilizer.pinPrice(secondaryBs)
        val s5 = stabilizer.onFrame(listOf(realUsd), nowMs = 2_500L)
        assertTrue(s5.isUserPinned)
        assertEquals(1150.0, s5.selectedPrice!!.amount, 0.001)
        assertEquals(CurrencyHint.VES, s5.selectedPrice!!.currencyHint)

        // Unpinning resumes live tracking; prolonged empty frames eventually clear state
        stabilizer.clearPin()
        val s6 = stabilizer.onFrame(emptyList(), nowMs = 5_000L)
        assertFalse(s6.isUserPinned)
        assertNull(s6.selectedPrice)
    }
}
