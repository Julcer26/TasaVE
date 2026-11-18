package com.julcer26.tasave.ultra

import androidx.compose.runtime.Immutable

enum class AppTab(val label: String) {
    CONVERTER("Conversor"),
    CART("Carrito"),
    MARKET("Brecha"),
    CHECKOUT("Cobros")
}

enum class UserTier {
    FREE,
    ULTRA
}

enum class TrendCurrency(val key: String, val label: String) {
    USD_BCV("USD_BCV", "USD BCV"),
    EUR_BCV("EUR_BCV", "EUR BCV"),
    USDT("USDT", "USDT"),
    ALL("ALL", "Todas")
}

enum class TrendRange(val label: String, val days: Int?) {
    D1("1D", 1),
    W1("1S", 7),
    D15("15D", 15),
    M1("1M", 30),
    M3("3M", 90),
    ALL("Todo", null)
}

enum class PaymentMethodType(val label: String) {
    PAGO_MOVIL("Pago Móvil"),
    BANK_ACCOUNT("Cuenta Bancaria"),
    ZELLE("Zelle"),
    BINANCE("Binance Pay")
}

@Immutable
data class VenezuelanBank(
    val code: String,
    val name: String
) {
    fun display(): String = "$code - $name"
}

val VENEZUELAN_BANKS = listOf(
    VenezuelanBank("0102", "Banco de Venezuela (BDV)"),
    VenezuelanBank("0134", "Banesco"),
    VenezuelanBank("0105", "Banco Mercantil"),
    VenezuelanBank("0108", "BBVA Provincial"),
    VenezuelanBank("0191", "BNC - Banco Nacional de Crédito"),
    VenezuelanBank("0172", "Bancamiga"),
    VenezuelanBank("0114", "Bancaribe"),
    VenezuelanBank("0115", "Banco Exterior"),
    VenezuelanBank("0151", "BFC Banco Fondo Común"),
    VenezuelanBank("0174", "Banplus"),
    VenezuelanBank("0163", "Banco del Tesoro"),
    VenezuelanBank("0175", "Banco Digital de los Trabajadores (Bicentenario)"),
    VenezuelanBank("0138", "Banco Plaza"),
    VenezuelanBank("0104", "Venezolano de Crédito"),
    VenezuelanBank("0128", "Banco Caroní"),
    VenezuelanBank("0137", "Banco Sofitasa"),
    VenezuelanBank("0156", "100% Banco"),
    VenezuelanBank("0157", "DelSur Banco Universal"),
    VenezuelanBank("0168", "Bancrecer"),
    VenezuelanBank("0169", "R4 Banco Microfinanciero (Mi Banco)"),
    VenezuelanBank("0171", "Banco Activo"),
    VenezuelanBank("0177", "BANFANB"),
    VenezuelanBank("0146", "Bangente"),
    VenezuelanBank("0166", "Banco Agrícola de Venezuela")
)

val DOC_PREFIXES = listOf("V", "E", "J", "G", "P", "C")
val PHONE_PREFIXES = listOf("0412", "0414", "0424", "0416", "0426", "0422")

@Immutable
data class PaymentMethodProfile(
    val id: Long,
    val alias: String = "",
    val type: PaymentMethodType = PaymentMethodType.PAGO_MOVIL,
    val bankCode: String = "0134",
    val bankName: String = "Banesco",
    val docPrefix: String = "V",
    val docNumber: String = "",
    val phonePrefix: String = "0414",
    val phoneNumber: String = "",
    val accountNumber: String = "",
    val accountHolder: String = "",
    val digitalIdentifier: String = ""
) {
    fun formattedBank(): String = if (bankCode.isNotBlank()) "$bankCode - $bankName" else bankName
    fun formattedDoc(): String = if (docNumber.isNotBlank()) "$docPrefix-$docNumber" else ""
    fun formattedPhone(): String = if (phoneNumber.isNotBlank()) "$phonePrefix-$phoneNumber" else ""
    fun displayTitle(): String {
        if (alias.isNotBlank()) return alias
        return when (type) {
            PaymentMethodType.ZELLE -> "Zelle • ${accountHolder.ifBlank { digitalIdentifier.ifBlank { "Cuenta" } }}"
            PaymentMethodType.BINANCE -> "Binance Pay • ${digitalIdentifier.ifBlank { accountHolder.ifBlank { "Pay" } }}"
            PaymentMethodType.PAGO_MOVIL, PaymentMethodType.BANK_ACCOUNT -> "${type.label} • $bankName"
        }
    }
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
    const val FREE_CART_MAX_ITEMS = 5
    const val FREE_PAYMENT_PROFILES_MAX = 1
    const val REWARDED_SCAN_BONUS = 5
    private const val DAY_MS = 24L * 60L * 60L * 1000L

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

    fun applyPercentModifier(amount: Double, percentModifier: Double): Double {
        if (amount <= 0.0) return 0.0
        return (amount * (1.0 + percentModifier / 100.0)).coerceAtLeast(0.0)
    }

    fun calculateVesPerCop(vesPerUsd: Double, copPerUsd: Double): Double {
        if (vesPerUsd <= 0.0 || copPerUsd <= 0.0) return 0.0
        return vesPerUsd / copPerUsd
    }

    fun calculateCopFromVes(vesAmount: Double, vesPerUsd: Double, copPerUsd: Double): Double {
        val vesPerCop = calculateVesPerCop(vesPerUsd, copPerUsd)
        if (vesPerCop <= 0.0 || vesAmount <= 0.0) return 0.0
        return vesAmount / vesPerCop
    }

    fun canAddCartItem(currentDistinctCount: Int, isUltraUser: Boolean): Boolean {
        return isUltraUser || currentDistinctCount < FREE_CART_MAX_ITEMS
    }

    fun canAddPaymentProfile(currentCount: Int, isUltraUser: Boolean): Boolean {
        return isUltraUser || currentCount < FREE_PAYMENT_PROFILES_MAX
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

    fun filterSnapshotsForRange(
        snapshots: List<RateSnapshot>,
        range: TrendRange,
        nowMs: Long = System.currentTimeMillis()
    ): List<RateSnapshot> {
        val sorted = snapshots.sortedBy { it.timestampMs }
        if (sorted.isEmpty()) return emptyList()
        val days = range.days ?: return sorted
        val cutoff = nowMs - days * DAY_MS
        val filtered = sorted.filter { it.timestampMs >= cutoff }
        return when {
            filtered.size >= 2 -> filtered
            sorted.size >= 2 -> sorted.takeLast(2)
            else -> sorted
        }
    }
}
