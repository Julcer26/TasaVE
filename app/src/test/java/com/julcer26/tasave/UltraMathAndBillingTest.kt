package com.julcer26.tasave.ultra

import com.julcer26.tasave.billing.BillingManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UltraMathAndBillingTest {

    @Test
    fun calculateCartSummary_appliesIvaAndIgtfAndCurrenciesCorrectly() {
        val items = listOf(
            CartItem(id = 1L, name = "Harina", unitPriceUsd = 2.50, quantity = 2), // $5.00
            CartItem(id = 2L, name = "Café", unitPriceUsd = 5.00, quantity = 1)    // $5.00
        )
        // Subtotal = $10.00
        // +16% IVA = $1.60 -> $11.60
        // +3% IGTF over (Subtotal + IVA) = $11.60 * 0.03 = $0.348 -> Total = $11.948
        val summary = UltraMath.calculateCartSummary(
            items = items,
            applyIva = true,
            applyIgtf = true,
            usdRate = 45.0,
            eurRate = 50.0,
            usdtRate = 55.0
        )

        assertEquals(10.0, summary.subtotalUsd, 0.0001)
        assertEquals(1.60, summary.ivaUsd, 0.0001)
        assertEquals(0.348, summary.igtfUsd, 0.0001)
        assertEquals(11.948, summary.totalUsd, 0.0001)
        assertEquals(11.948 * 45.0, summary.totalVes, 0.001)
        assertEquals((11.948 * 45.0) / 50.0, summary.totalEur, 0.001)
        assertEquals((11.948 * 45.0) / 55.0, summary.totalUsdt, 0.001)
    }

    @Test
    fun calculateSpreadPercent_computesSpreadAndHandlesZeroBase() {
        assertEquals(20.0, UltraMath.calculateSpreadPercent(higherRate = 60.0, baseRate = 50.0), 0.0001)
        assertEquals(0.0, UltraMath.calculateSpreadPercent(higherRate = 60.0, baseRate = 0.0), 0.0001)
    }

    @Test
    fun calculateMixedPayment_computesRemainingAndChange() {
        // Total $25.00 at Bs. 50.00 = Bs. 1,250.00
        // Paid $10.00 cash + Bs. 250.00 ($5.00) = $15.00 -> Remaining $10.00 / Bs. 500.00
        val remainingResult = UltraMath.calculateMixedPayment(
            totalUsd = 25.0,
            paidUsdCash = 10.0,
            paidVes = 250.0,
            rate = 50.0
        )
        assertEquals(10.0, remainingResult.remainingUsd, 0.0001)
        assertEquals(500.0, remainingResult.remainingVes, 0.0001)
        assertEquals(0.0, remainingResult.changeUsd, 0.0001)
        assertEquals(0.0, remainingResult.changeVes, 0.0001)

        // Paid $20.00 cash + Bs. 500.00 ($10.00) = $30.00 -> Change $5.00 / Bs. 250.00
        val changeResult = UltraMath.calculateMixedPayment(
            totalUsd = 25.0,
            paidUsdCash = 20.0,
            paidVes = 500.0,
            rate = 50.0
        )
        assertEquals(0.0, changeResult.remainingUsd, 0.0001)
        assertEquals(0.0, changeResult.remainingVes, 0.0001)
        assertEquals(5.0, changeResult.changeUsd, 0.0001)
        assertEquals(250.0, changeResult.changeVes, 0.0001)
    }

    @Test
    fun billingEntitlements_ultraGrantsProBenefitsAndDebugOverridesWork() {
        // Production entitlements
        val free = BillingManager.resolveEffectiveEntitlements(
            purchasedPro = false,
            purchasedUltra = false,
            isDebugBuild = false,
            debugTier = UserTier.ULTRA
        )
        assertFalse(free.first)
        assertFalse(free.second)

        val ultraOnly = BillingManager.resolveEffectiveEntitlements(
            purchasedPro = false,
            purchasedUltra = true,
            isDebugBuild = false,
            debugTier = UserTier.FREE
        )
        assertTrue("Ultra must automatically grant Pro benefits", ultraOnly.first)
        assertTrue(ultraOnly.second)

        // Canceling Ultra falls back to previously purchased Pro
        val proFallback = BillingManager.resolveEffectiveEntitlements(
            purchasedPro = true,
            purchasedUltra = false,
            isDebugBuild = false,
            debugTier = UserTier.FREE
        )
        assertTrue(proFallback.first)
        assertFalse(proFallback.second)

        // Debug overrides
        assertEquals(false to false, BillingManager.resolveEffectiveEntitlements(true, true, true, UserTier.FREE))
        assertEquals(true to false, BillingManager.resolveEffectiveEntitlements(false, false, true, UserTier.PRO))
        assertEquals(true to true, BillingManager.resolveEffectiveEntitlements(false, false, true, UserTier.ULTRA))
    }

    @Test
    fun filterSnapshotsForRange_filtersByDaysAndHandlesSparsePoints() {
        val dayMs = 24L * 60L * 60L * 1000L
        val now = 1_700_000_000_000L
        val snapshots = listOf(
            RateSnapshot(now - 100 * dayMs, "D-100", 36.0, 39.0, 40.0),
            RateSnapshot(now - 20 * dayMs, "D-20", 40.0, 43.0, 46.0),
            RateSnapshot(now - 5 * dayMs, "D-5", 42.0, 45.0, 49.0),
            RateSnapshot(now - 12L * 3600_000L, "12h", 44.0, 47.0, 51.0),
            RateSnapshot(now, "Hoy", 45.0, 48.0, 52.0)
        )

        val d1 = UltraMath.filterSnapshotsForRange(snapshots, TrendRange.D1, now)
        assertEquals(2, d1.size)
        assertEquals("12h", d1.first().label)
        assertEquals("Hoy", d1.last().label)

        val w1 = UltraMath.filterSnapshotsForRange(snapshots, TrendRange.W1, now)
        assertEquals(3, w1.size)
        assertEquals("D-5", w1.first().label)

        val all = UltraMath.filterSnapshotsForRange(snapshots, TrendRange.ALL, now)
        assertEquals(5, all.size)

        // Sparse points: if 1D only has 1 point within 24h, fallback keeps last 2 points so chart draws a segment
        val sparse = listOf(
            RateSnapshot(now - 10 * dayMs, "D-10", 40.0, 43.0, 45.0),
            RateSnapshot(now, "Hoy", 45.0, 48.0, 52.0)
        )
        val d1Sparse = UltraMath.filterSnapshotsForRange(sparse, TrendRange.D1, now)
        assertEquals(2, d1Sparse.size)
    }

    @Test
    fun paymentProfileFormattingAndBanksCatalogAreValid() {
        val codes = VENEZUELAN_BANKS.map { it.code }
        assertEquals(codes.size, codes.distinct().size)
        assertTrue(codes.all { it.length == 4 && it.all(Char::isDigit) })
        assertTrue(VENEZUELAN_BANKS.any { it.code == "0134" && it.name.contains("Banesco") })

        val profile = PaymentMethodProfile(
            id = 1L,
            alias = "Mi Comercio",
            type = PaymentMethodType.PAGO_MOVIL,
            bankCode = "0134",
            bankName = "Banesco",
            docPrefix = "J",
            docNumber = "123456789",
            phonePrefix = "0412",
            phoneNumber = "5551234"
        )
        assertEquals("0134 - Banesco", profile.formattedBank())
        assertEquals("J-123456789", profile.formattedDoc())
        assertEquals("0412-5551234", profile.formattedPhone())
        assertEquals("Mi Comercio", profile.displayTitle())
        assertEquals("Brecha", AppTab.MARKET.label)
    }
}

