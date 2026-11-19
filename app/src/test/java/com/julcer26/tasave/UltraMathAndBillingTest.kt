package com.julcer26.tasave.ultra

import com.julcer26.tasave.billing.BillingManager
import com.julcer26.tasave.data.DolarVzlaBcvResponse
import com.julcer26.tasave.data.DolarVzlaBcvSnapshot
import com.julcer26.tasave.data.DolarVzlaChangePct
import com.julcer26.tasave.data.TasaVeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun billingEntitlements_ultraAndDebugOverridesWork() {
        // Production entitlements
        assertFalse(
            BillingManager.resolveEffectiveUltraEntitlement(
                purchasedUltra = false,
                isDebugBuild = false,
                debugTier = UserTier.ULTRA
            )
        )
        assertTrue(
            BillingManager.resolveEffectiveUltraEntitlement(
                purchasedUltra = true,
                isDebugBuild = false,
                debugTier = UserTier.FREE
            )
        )

        // Debug overrides
        assertFalse(BillingManager.resolveEffectiveUltraEntitlement(true, true, UserTier.FREE))
        assertTrue(BillingManager.resolveEffectiveUltraEntitlement(false, true, UserTier.ULTRA))
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

        val zelleProfile = PaymentMethodProfile(
            id = 2L,
            type = PaymentMethodType.ZELLE,
            accountHolder = "Comercio VE",
            digitalIdentifier = "pagos@ejemplo.com"
        )
        assertEquals("Zelle • Comercio VE", zelleProfile.displayTitle())

        val binanceProfile = PaymentMethodProfile(
            id = 3L,
            type = PaymentMethodType.BINANCE,
            digitalIdentifier = "987654321"
        )
        assertEquals("Binance Pay • 987654321", binanceProfile.displayTitle())
    }

    @Test
    fun percentModifierComputesAccurately() {
        assertEquals(90.0, UltraMath.applyPercentModifier(100.0, -10.0), 0.0001)
        assertEquals(116.0, UltraMath.applyPercentModifier(100.0, 16.0), 0.0001)
        assertEquals(103.0, UltraMath.applyPercentModifier(100.0, 3.0), 0.0001)
        assertEquals(0.0, UltraMath.applyPercentModifier(0.0, 16.0), 0.0001)
    }

    @Test
    fun freemiumCartAndProfileGatesEnforceFreeLimitsAndAllowUltraUnlimited() {
        assertTrue(UltraMath.canAddCartItem(currentDistinctCount = 0, isUltraUser = false))
        assertTrue(UltraMath.canAddCartItem(currentDistinctCount = 4, isUltraUser = false))
        assertFalse(UltraMath.canAddCartItem(currentDistinctCount = 5, isUltraUser = false))
        assertTrue(UltraMath.canAddCartItem(currentDistinctCount = 25, isUltraUser = true))

        assertTrue(UltraMath.canAddPaymentProfile(currentCount = 0, isUltraUser = false))
        assertFalse(UltraMath.canAddPaymentProfile(currentCount = 1, isUltraUser = false))
        assertTrue(UltraMath.canAddPaymentProfile(currentCount = 10, isUltraUser = true))
    }

    @Test
    fun resolveBcvTodayAndNext_splitsFutureRateAndCurrentTodayRate() {
        val futureResponse = DolarVzlaBcvResponse(
            current = DolarVzlaBcvSnapshot(date = "2026-11-19", usd = 49.1234, eur = 53.5678),
            previous = DolarVzlaBcvSnapshot(date = "2026-11-18", usd = 48.5000, eur = 52.8000),
            changePercentage = DolarVzlaChangePct(usd = 1.28, eur = 1.45)
        )
        val split = TasaVeRepository.resolveBcvTodayAndNext(
            bcvResp = futureResponse,
            fallbackUsd = 48.0,
            fallbackEur = 52.0,
            todayIso = "2026-11-18"
        )
        assertEquals(48.5000, split.todayUsd, 0.0001)
        assertEquals(52.8000, split.todayEur, 0.0001)
        assertEquals("2026-11-18", split.todayEffectiveIso)
        assertNotNull(split.nextUsd)
        assertEquals(49.1234, split.nextUsd!!, 0.0001)
        assertEquals(53.5678, split.nextEur!!, 0.0001)
        assertEquals("2026-11-19", split.nextEffectiveIso)
        assertEquals(1.28, split.usdChangePct!!, 0.001)

        val currentResponse = DolarVzlaBcvResponse(
            current = DolarVzlaBcvSnapshot(date = "2026-11-18", usd = 48.5000, eur = 52.8000),
            previous = DolarVzlaBcvSnapshot(date = "2026-11-17", usd = 48.1000, eur = 52.4000),
            changePercentage = DolarVzlaChangePct(usd = 0.83, eur = 0.76)
        )
        val sameDay = TasaVeRepository.resolveBcvTodayAndNext(
            bcvResp = currentResponse,
            fallbackUsd = 48.0,
            fallbackEur = 52.0,
            todayIso = "2026-11-18"
        )
        assertEquals(48.5000, sameDay.todayUsd, 0.0001)
        assertEquals(52.8000, sameDay.todayEur, 0.0001)
        assertNull(sameDay.nextUsd)
        assertNull(sameDay.nextEur)
        assertNull(sameDay.nextEffectiveIso)
    }

    @Test
    fun interstitialPolicy_respectsGracePeriodCooldownSessionCapAndAdFree() {
        val now = 1_000_000L
        val cooldown = com.julcer26.tasave.ads.InterstitialPolicy.COOLDOWN_MS

        // Suppressed during first 2 launches (new user grace period)
        assertFalse(
            com.julcer26.tasave.ads.InterstitialPolicy.shouldShow(
                isAdFree = false,
                appLaunchCount = 1,
                sessionShownCount = 0,
                lastShownElapsedMs = -1L,
                nowElapsedMs = now,
                hasLoadedAd = true
            )
        )
        assertFalse(
            com.julcer26.tasave.ads.InterstitialPolicy.shouldShow(
                isAdFree = false,
                appLaunchCount = 2,
                sessionShownCount = 0,
                lastShownElapsedMs = -1L,
                nowElapsedMs = now,
                hasLoadedAd = true
            )
        )

        // Allowed from launch 3 onwards when ad is loaded
        assertTrue(
            com.julcer26.tasave.ads.InterstitialPolicy.shouldShow(
                isAdFree = false,
                appLaunchCount = 3,
                sessionShownCount = 0,
                lastShownElapsedMs = -1L,
                nowElapsedMs = now,
                hasLoadedAd = true
            )
        )

        // Suppressed when Ultra or 12h Ad-Free Pass is active
        assertFalse(
            com.julcer26.tasave.ads.InterstitialPolicy.shouldShow(
                isAdFree = true,
                appLaunchCount = 10,
                sessionShownCount = 0,
                lastShownElapsedMs = -1L,
                nowElapsedMs = now,
                hasLoadedAd = true
            )
        )

        // Suppressed before 4-minute cooldown, allowed at/after cooldown
        assertFalse(
            com.julcer26.tasave.ads.InterstitialPolicy.shouldShow(
                isAdFree = false,
                appLaunchCount = 5,
                sessionShownCount = 1,
                lastShownElapsedMs = now - (cooldown - 1_000L),
                nowElapsedMs = now,
                hasLoadedAd = true
            )
        )
        assertTrue(
            com.julcer26.tasave.ads.InterstitialPolicy.shouldShow(
                isAdFree = false,
                appLaunchCount = 5,
                sessionShownCount = 1,
                lastShownElapsedMs = now - cooldown,
                nowElapsedMs = now,
                hasLoadedAd = true
            )
        )

        // Suppressed at session cap (3 interstitials/session)
        assertFalse(
            com.julcer26.tasave.ads.InterstitialPolicy.shouldShow(
                isAdFree = false,
                appLaunchCount = 5,
                sessionShownCount = com.julcer26.tasave.ads.InterstitialPolicy.MAX_PER_SESSION,
                lastShownElapsedMs = now - (cooldown * 2),
                nowElapsedMs = now,
                hasLoadedAd = true
            )
        )
    }

    @Test
    fun adUnits_sanitizeProdId_rejectsBlankAndGoogleTestIds() {
        assertNull(com.julcer26.tasave.ads.AdUnits.sanitizeProdId(""))
        assertNull(com.julcer26.tasave.ads.AdUnits.sanitizeProdId("   "))
        assertNull(
            com.julcer26.tasave.ads.AdUnits.sanitizeProdId(
                com.julcer26.tasave.ads.AdUnits.TEST_BANNER_AD_UNIT_ID
            )
        )
        assertEquals(
            "ca-app-pub-1192254236776922/4135771180",
            com.julcer26.tasave.ads.AdUnits.sanitizeProdId("  ca-app-pub-1192254236776922/4135771180  ")
        )
    }
}


