package com.julcer26.tasave

import com.julcer26.tasave.data.CachedRatesBundle
import com.julcer26.tasave.data.RateInfo
import com.julcer26.tasave.quick.PersistentRateNotificationManager
import com.julcer26.tasave.quick.QuickAmountParser
import com.julcer26.tasave.share.ShareCardGenerator
import com.julcer26.tasave.ultra.CartItem
import com.julcer26.tasave.ultra.PaymentMethodProfile
import com.julcer26.tasave.ultra.PaymentMethodType
import com.julcer26.tasave.ultra.UltraMath
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
        assertEquals("USD_BCV", dollar.suggestedRateKey)

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
    fun parsePrioritizesCurrencyTaggedNumberInMultiNumberMessages() {
        val multiCombo = QuickAmountParser.parse("2 combos por \$35")
        assertNotNull(multiCombo)
        assertEquals(35.0, multiCombo!!.amount, 0.0001)
        assertFalse(multiCombo.inputIsVes)
        assertEquals("USD_BCV", multiCombo.suggestedRateKey)

        val promoSuffix = QuickAmountParser.parse("Promo 1: 25,50\$")
        assertNotNull(promoSuffix)
        assertEquals(25.5, promoSuffix!!.amount, 0.0001)
        assertFalse(promoSuffix.inputIsVes)

        val multiVes = QuickAmountParser.parse("3 unidades por Bs. 1.500,25")
        assertNotNull(multiVes)
        assertEquals(1500.25, multiVes!!.amount, 0.0001)
        assertTrue(multiVes.inputIsVes)

        val eurMsg = QuickAmountParser.parse("48,50 €")
        assertNotNull(eurMsg)
        assertEquals(48.5, eurMsg!!.amount, 0.0001)
        assertEquals("EUR_BCV", eurMsg.suggestedRateKey)

        val usdtMsg = QuickAmountParser.parse("120 USDT")
        assertNotNull(usdtMsg)
        assertEquals(120.0, usdtMsg!!.amount, 0.0001)
        assertEquals("USDT", usdtMsg.suggestedRateKey)

        val copMsg = QuickAmountParser.parse("85.000 COP")
        assertNotNull(copMsg)
        assertEquals(85000.0, copMsg!!.amount, 0.0001)
        assertEquals("COP", copMsg.suggestedRateKey)
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
        assertNull(QuickAmountParser.parse("0134"))
        assertNull(
            QuickAmountParser.parse(
                "*Pago Móvil*\nBanco: 0134 - Banesco\nCI/RIF: V-18730066\nTel: 0414-1533502"
            )
        )
        assertNull(
            QuickAmountParser.parse("$ 35.00 = Bs. 32,122.13 (USD BCV)")
        )

        val zeroDecimal = QuickAmountParser.parse("0,50")
        assertNotNull(zeroDecimal)
        assertEquals(0.5, zeroDecimal!!.amount, 0.0001)
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

    @Test
    fun formatCartQuoteTextIncludesItemsTaxesAndPaymentProfile() {
        val items = listOf(
            CartItem(id = 1L, name = "Harina PAN", unitPriceUsd = 2.0, quantity = 3),
            CartItem(id = 2L, name = "Queso Blanco", unitPriceUsd = 4.0, quantity = 1)
        )
        val summary = UltraMath.calculateCartSummary(
            items = items,
            applyIva = true,
            applyIgtf = false,
            usdRate = 45.0,
            eurRate = 49.0,
            usdtRate = 50.0
        )
        val profile = PaymentMethodProfile(
            id = 10L,
            alias = "Tienda",
            type = PaymentMethodType.PAGO_MOVIL,
            bankCode = "0134",
            bankName = "Banesco",
            docPrefix = "V",
            docNumber = "12345678",
            phonePrefix = "0414",
            phoneNumber = "1234567"
        )

        val quote = ShareCardGenerator.formatCartQuoteText(
            items = items,
            summary = summary,
            usdRate = 45.0,
            applyIva = true,
            applyIgtf = false,
            profile = profile,
            customBrandName = "Bodegón Central"
        )

        assertTrue(quote.contains("*Bodegón Central • Presupuesto / Pedido*"))
        assertTrue(quote.contains("1. 3x Harina PAN: \$6.00 (Bs. 270.00)"))
        assertTrue(quote.contains("IVA (16%): \$1.60"))
        assertTrue(quote.contains("*Total a pagar: \$11.60 • Bs. 522.00*"))
        assertTrue(quote.contains("DATOS PAGO MÓVIL • Tienda"))
        assertTrue(quote.contains("0134 - Banesco"))
    }
}
