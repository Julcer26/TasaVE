package com.julcer26.tasave.ultra

import androidx.compose.runtime.Immutable

enum class AppTab(val label: String) {
    CONVERTER("Conversor"),
    CART("Carrito"),
    MARKET("Mercado"),
    CHECKOUT("Cobros")
}

enum class UserTier {
    FREE,
    PRO,
    ULTRA
}

@Immutable
data class CartItem(
    val id: Long,
    val name: String,
    val unitPriceUsd: Double,
    val quantity: Int = 1
)

@Immutable
data class CartSummary(
    val subtotalUsd: Double,
    val ivaUsd: Double,
    val igtfUsd: Double,
    val totalUsd: Double,
    val totalVes: Double,
    val totalEur: Double,
    val totalUsdt: Double
)

@Immutable
data class RateSnapshot(
    val timestampMs: Long,
    val label: String,
    val usdBcv: Double,
    val eurBcv: Double,
    val usdt: Double
)

@Immutable
data class AlertSettings(
    val enabled: Boolean = false,
    val watchUsdBcv: Boolean = true,
    val watchEurBcv: Boolean = true,
    val watchUsdt: Boolean = true,
    val thresholdPercent: Double = 1.0
)

@Immutable
data class PagoMovilInfo(
    val bank: String = "",
    val documentId: String = "",
    val phone: String = ""
)

@Immutable
data class MixedPaymentResult(
    val totalUsd: Double,
    val totalVes: Double,
    val paidForeignUsd: Double,
    val paidVes: Double,
    val remainingUsd: Double,
    val remainingVes: Double,
    val changeUsd: Double,
    val changeVes: Double
)

object UltraMath {

    const val IVA_RATE = 0.16
    const val IGTF_RATE = 0.03

    fun calculateCartSummary(
        items: List<CartItem>,
        applyIva: Boolean,
        applyIgtf: Boolean,
        usdRate: Double,
        eurRate: Double,
        usdtRate: Double
    ): CartSummary {
        val subtotalUsd = items.sumOf { it.unitPriceUsd * it.quantity }
        val ivaUsd = if (applyIva) subtotalUsd * IVA_RATE else 0.0
        val basePlusIva = subtotalUsd + ivaUsd
        val igtfUsd = if (applyIgtf) basePlusIva * IGTF_RATE else 0.0
        val totalUsd = basePlusIva + igtfUsd

        val totalVes = totalUsd * usdRate
        val totalEur = if (eurRate > 0.0) totalVes / eurRate else 0.0
        val totalUsdt = if (usdtRate > 0.0) totalVes / usdtRate else 0.0

        return CartSummary(
            subtotalUsd = subtotalUsd,
            ivaUsd = ivaUsd,
            igtfUsd = igtfUsd,
            totalUsd = totalUsd,
            totalVes = totalVes,
            totalEur = totalEur,
            totalUsdt = totalUsdt
        )
    }

    fun calculateSpreadPercent(higherRate: Double, baseRate: Double): Double {
        if (baseRate <= 0.0) return 0.0
        return ((higherRate - baseRate) / baseRate) * 100.0
    }

    fun calculateMixedPayment(
        totalUsd: Double,
        paidUsdCash: Double,
        paidVes: Double,
        rate: Double
    ): MixedPaymentResult {
        val safeRate = if (rate > 0.0) rate else 1.0
        val totalVes = totalUsd * safeRate
        val paidVesInUsd = paidVes / safeRate
        val totalPaidUsd = paidUsdCash + paidVesInUsd
        val diffUsd = totalUsd - totalPaidUsd

        return if (diffUsd >= 0.0) {
            MixedPaymentResult(
                totalUsd = totalUsd,
                totalVes = totalVes,
                paidForeignUsd = paidUsdCash,
                paidVes = paidVes,
                remainingUsd = diffUsd,
                remainingVes = diffUsd * safeRate,
                changeUsd = 0.0,
                changeVes = 0.0
            )
        } else {
            val changeUsd = -diffUsd
            MixedPaymentResult(
                totalUsd = totalUsd,
                totalVes = totalVes,
                paidForeignUsd = paidUsdCash,
                paidVes = paidVes,
                remainingUsd = 0.0,
                remainingVes = 0.0,
                changeUsd = changeUsd,
                changeVes = changeUsd * safeRate
            )
        }
    }
}
