package com.julcer26.tasave

import com.julcer26.tasave.data.CachedRatesBundle
import com.julcer26.tasave.data.RateInfo
import com.julcer26.tasave.quick.PersistentRateNotificationManager
import com.julcer26.tasave.quick.QuickAmountParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickToolsTest {

    @Test
    fun parseDollarAndPlainAmounts() {
        val dollar = QuickAmountParser.parse("Total a pagar \$35")
        assertNotNull(dollar)
        assertEquals(35.0, dollar!!.amount, 0.0001)
        assertFalse(dollar.inputIsVes)

        val commaDecimal = QuickAmountParser.parse("35,50")
        assertNotNull(commaDecimal)
        assertEquals(35.5, commaDecimal!!.amount, 0.0001)
        assertFalse(commaDecimal.inputIsVes)

        val integerOnly = QuickAmountParser.parse("32082")
        assertNotNull(integerOnly)
        assertEquals(32082.0, integerOnly!!.amount, 0.0001)
        assertFalse(integerOnly.inputIsVes)
    }

    @Test
    fun parseBolivarFormattedAmountsAndThousands() {
        val vesVen = QuickAmountParser.parse("Bs 1.500,25")
        assertNotNull(vesVen)
        assertEquals(1500.25, vesVen!!.amount, 0.0001)
        assertTrue(vesVen.inputIsVes)

        val vesTrailing = QuickAmountParser.parse("Transfiere 4500 Bs por favor")
        assertNotNull(vesTrailing)
        assertEquals(4500.0, vesTrailing!!.amount, 0.0001)
        assertTrue(vesTrailing.inputIsVes)

        val usThousands = QuickAmountParser.parse("1,250.75 USD")
        assertNotNull(usThousands)
        assertEquals(1250.75, usThousands!!.amount, 0.0001)
        assertFalse(usThousands.inputIsVes)
    }

    @Test
    fun parseNonNumericAndInvalidInputsReturnsNull() {
        assertNull(QuickAmountParser.parse(null))
        assertNull(QuickAmountParser.parse(""))
        assertNull(QuickAmountParser.parse("   "))
        assertNull(QuickAmountParser.parse("Hola, me pasas el Pago Móvil?"))
        assertNull(QuickAmountParser.parse("0"))
    }

    @Test
    fun persistentNotificationRespectsUltraOptInAndPermissionGating() {
        assertFalse(
            PersistentRateNotificationManager.canShowPersistentNotification(
                isUltraActive = false,
                isOptedIn = true,
                hasPermission = true
            )
        )
        assertFalse(
            PersistentRateNotificationManager.canShowPersistentNotification(
                isUltraActive = true,
                isOptedIn = false,
                hasPermission = true
            )
        )
        assertFalse(
            PersistentRateNotificationManager.canShowPersistentNotification(
                isUltraActive = true,
                isOptedIn = true,
                hasPermission = false
            )
        )
        assertTrue(
            PersistentRateNotificationManager.canShowPersistentNotification(
                isUltraActive = true,
                isOptedIn = true,
                hasPermission = true
            )
        )
    }

    @Test
    fun formatPersistentRatesIncludesLiveAndTomorrowRates() {
        val bundle = CachedRatesBundle(
            usdBcv = RateInfo("USD_BCV", "Dólar BCV", "$", 44.1234, "2026-11-22"),
            eurBcv = RateInfo("EUR_BCV", "Euro BCV", "€", 48.5678, "2026-11-22"),
            usdtParalelo = RateInfo("USDT", "USDT", "₮", 49.85, "2026-11-22"),
            nextUsdBcv = RateInfo("USD_BCV", "BCV Mañana", "$", 44.6500, "2026-11-23"),
            nextBcvDateIso = "2026-11-23"
        )

        val summary = PersistentRateNotificationManager.formatPersistentRates(bundle)
        assertTrue(summary.title.contains("44.12"))
        assertTrue(summary.compactLine.contains("USD 44.12"))
        assertTrue(summary.compactLine.contains("EUR 48.57"))
        assertTrue(summary.compactLine.contains("USDT 49.85"))
        assertTrue(summary.bigText.contains("BCV Mañana (2026-11-23): Bs. 44.6500"))
        assertTrue(summary.copyClipboardText.contains("Tasas del día"))
    }
}
