package com.julcer26.tasave

import com.julcer26.tasave.voice.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun parsesDecimalsWithConAndBcv() {
        val result = VoiceCommandParser.parse("38 con 50 dólares a BCV")
        assertNotNull(result)
        assertEquals(38.50, result!!.amount, 0.001)
        assertFalse(result.inputIsVes)
        assertEquals("USD_BCV", result.rateKey)
        assertFalse(result.useTomorrowBcv)
    }

    @Test
    fun parsesCurrencyBetweenIntegerAndCents() {
        val result = VoiceCommandParser.parse("47 dólares con 50")
        assertNotNull(result)
        assertEquals(47.50, result!!.amount, 0.001)
        assertFalse(result.inputIsVes)
        assertEquals("USD_BCV", result.rateKey)
    }

    @Test
    fun parsesSpokenWordsCienDolares() {
        val result = VoiceCommandParser.parse("cien dólares")
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.001)
        assertFalse(result.inputIsVes)
        assertEquals("USD_BCV", result.rateKey)
    }

    @Test
    fun parsesSimpleDollars() {
        val result = VoiceCommandParser.parse("25 dólares")
        assertNotNull(result)
        assertEquals(25.0, result!!.amount, 0.001)
        assertFalse(result.inputIsVes)
        assertEquals("USD_BCV", result.rateKey)
    }

    @Test
    fun parsesEurosAndTargetEuro() {
        val euroDirect = VoiceCommandParser.parse("50 euros")
        assertNotNull(euroDirect)
        assertEquals(50.0, euroDirect!!.amount, 0.001)
        assertFalse(euroDirect.inputIsVes)
        assertEquals("EUR_BCV", euroDirect.rateKey)

        val toEuro = VoiceCommandParser.parse("20 dólares a euro")
        assertNotNull(toEuro)
        assertEquals(20.0, toEuro!!.amount, 0.001)
        assertFalse(toEuro.inputIsVes)
        assertEquals("EUR_BCV", toEuro.rateKey)
    }

    @Test
    fun parsesBolivaresToDollarsAndUsdt() {
        val bsToUsd = VoiceCommandParser.parse("15 mil bolívares a dólares")
        assertNotNull(bsToUsd)
        assertEquals(15000.0, bsToUsd!!.amount, 0.001)
        assertTrue(bsToUsd.inputIsVes)
        assertEquals("USD_BCV", bsToUsd.rateKey)

        val bsToUsdt = VoiceCommandParser.parse("12 mil 500 bolívares a USDT")
        assertNotNull(bsToUsdt)
        assertEquals(12500.0, bsToUsdt!!.amount, 0.001)
        assertTrue(bsToUsdt.inputIsVes)
        assertEquals("USDT", bsToUsdt.rateKey)
    }

    @Test
    fun parsesTomorrowBcvFlag() {
        val result = VoiceCommandParser.parse("50 dólares mañana")
        assertNotNull(result)
        assertEquals(50.0, result!!.amount, 0.001)
        assertFalse(result.inputIsVes)
        assertEquals("USD_BCV", result.rateKey)
        assertTrue(result.useTomorrowBcv)
    }

    @Test
    fun parsesColombianPesosAndCop() {
        val cienMilPesos = VoiceCommandParser.parse("cien mil pesos")
        assertNotNull(cienMilPesos)
        assertEquals(100000.0, cienMilPesos!!.amount, 0.001)
        assertFalse(cienMilPesos.inputIsVes)
        assertEquals("COP", cienMilPesos.rateKey)

        val fiftyKCop = VoiceCommandParser.parse("50 mil COP")
        assertNotNull(fiftyKCop)
        assertEquals(50000.0, fiftyKCop!!.amount, 0.001)
        assertFalse(fiftyKCop.inputIsVes)
        assertEquals("COP", fiftyKCop.rateKey)
    }

    @Test
    fun parsesYMedioAndSpokenCents() {
        val half = VoiceCommandParser.parse("10 dólares y medio")
        assertNotNull(half)
        assertEquals(10.5, half!!.amount, 0.001)

        val spokenCents = VoiceCommandParser.parse("treinta y ocho dólares con cincuenta")
        assertNotNull(spokenCents)
        assertEquals(38.50, spokenCents!!.amount, 0.001)
    }

    @Test
    fun returnsNullForNonNumericNoise() {
        assertNull(VoiceCommandParser.parse(""))
        assertNull(VoiceCommandParser.parse("hola tasa ve"))
    }

    @Test
    fun buildsCleanSpokenTtsResult() {
        val parsed = VoiceCommandParser.parse("38 con 50 dólares a BCV")!!
        val spoken = VoiceCommandParser.buildSpokenResult(parsed, 3465.0, effectiveTomorrow = false)
        assertTrue(spoken.contains("38,50 dólares"))
        assertTrue(spoken.contains("3465 bolívares"))
    }
}
