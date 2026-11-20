package com.julcer26.tasave.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.julcer26.tasave.BuildConfig
import com.julcer26.tasave.data.LocalStorageHelper
import com.julcer26.tasave.ultra.UserTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Manages Google Play In-App Billing for:
 * - "Tasa VE Ultra" ($0.99/month or $7.99/year subscription)
 *
 * Ultra removes all ads and unlocks every feature:
 * unlimited camera OCR, unlimited smart cart + IVA/IGTF, unlimited payment profiles,
 * rate alerts, and home screen widget.
 */
class BillingManager(
    private val context: Context,
    private val storageHelper: LocalStorageHelper,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_ID_ULTRA_MONTHLY = "tasave_ultra_monthly"
        const val PRODUCT_ID_ULTRA_YEARLY = "tasave_ultra_yearly"

        fun resolveEffectiveUltraEntitlement(
            purchasedUltra: Boolean,
            isDebugBuild: Boolean,
            debugTier: UserTier
        ): Boolean {
            return if (isDebugBuild) {
                debugTier == UserTier.ULTRA
            } else {
                purchasedUltra
            }
        }
    }

    private val _purchasedUltra = MutableStateFlow(storageHelper.isUltraUser())

    private val _debugTier = MutableStateFlow(
        if (BuildConfig.DEBUG) {
            runCatching { UserTier.valueOf(storageHelper.getDebugTierOverride()) }
                .getOrDefault(UserTier.ULTRA)
        } else {
            UserTier.FREE
        }
    )
    val debugTier: StateFlow<UserTier> = _debugTier.asStateFlow()

    val isUltraUser: StateFlow<Boolean> = combine(_purchasedUltra, _debugTier) { ultra, dbg ->
        resolveEffectiveUltraEntitlement(ultra, BuildConfig.DEBUG, dbg)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = resolveEffectiveUltraEntitlement(
            _purchasedUltra.value,
            BuildConfig.DEBUG,
            _debugTier.value
        )
    )

    private val _ultraMonthlyPrice = MutableStateFlow("$0.99 / mes")
    val ultraMonthlyPrice: StateFlow<String> = _ultraMonthlyPrice.asStateFlow()

    private val _ultraYearlyPrice = MutableStateFlow("$7.99 / año")
    val ultraYearlyPrice: StateFlow<String> = _ultraYearlyPrice.asStateFlow()

    private val _purchaseStatusMessage = MutableStateFlow<String?>(null)
    val purchaseStatusMessage: StateFlow<String?> = _purchaseStatusMessage.asStateFlow()

    private var ultraMonthlyDetails: ProductDetails? = null
    private var ultraYearlyDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    init {
        startBillingConnection()
    }

    fun setDebugTier(tier: UserTier) {
        if (!BuildConfig.DEBUG) return
        _debugTier.value = tier
        storageHelper.setDebugTierOverride(tier.name)
        com.julcer26.tasave.widget.TasaVeRatesWidgetProvider.updateAllWidgets(context)
        _purchaseStatusMessage.value = when (tier) {
            UserTier.FREE -> "Modo prueba: Plan Gratis"
            UserTier.ULTRA -> "Modo prueba: Plan Ultra activado"
        }
    }

    private fun startBillingConnection() {
        if (!billingClient.isReady) {
            billingClient.startConnection(this)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingResponseCode.OK) {
            Log.d(TAG, "Google Play Billing setup successful.")
            queryProductDetails()
            queryPurchases()
        } else {
            Log.w(TAG, "Google Play Billing setup failed with code: ${billingResult.responseCode}")
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Billing service disconnected. Will attempt reconnection on next action.")
    }

    private fun queryProductDetails() {
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_ULTRA_MONTHLY)
                        .setProductType(ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_ULTRA_YEARLY)
                        .setProductType(ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { billingResult, result ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                result.productDetailsList.forEach { details ->
                    val offerPrice = details.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases
                        ?.pricingPhaseList
                        ?.lastOrNull()
                        ?.formattedPrice
                    when (details.productId) {
                        PRODUCT_ID_ULTRA_MONTHLY -> {
                            ultraMonthlyDetails = details
                            if (offerPrice != null) _ultraMonthlyPrice.value = "$offerPrice / mes"
                        }
                        PRODUCT_ID_ULTRA_YEARLY -> {
                            ultraYearlyDetails = details
                            if (offerPrice != null) _ultraYearlyPrice.value = "$offerPrice / año"
                        }
                    }
                }
            }
        }
    }

    fun launchUltraSubscription(activity: Activity, yearly: Boolean = false) {
        val details = if (yearly) ultraYearlyDetails else ultraMonthlyDetails
        val offerToken = details?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (details == null || offerToken == null) {
            if (!billingClient.isReady) startBillingConnection()
            _purchaseStatusMessage.value = if (BuildConfig.DEBUG) {
                "En prueba puedes alternar Gratis / Ultra desde Configuración."
            } else {
                "Conectando suscripción Ultra con Google Play... Reintenta en un momento."
            }
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, params).responseCode
        if (responseCode != BillingResponseCode.OK) {
            _purchaseStatusMessage.value = "No se pudo iniciar la suscripción. Código: $responseCode"
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingResponseCode.USER_CANCELED -> {
                _purchaseStatusMessage.value = "Compra cancelada."
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryPurchases()
                _purchaseStatusMessage.value = "¡Ya tienes Tasa VE Ultra activo en tu cuenta!"
            }
            else -> {
                _purchaseStatusMessage.value = "Error al procesar compra: ${billingResult.debugMessage}"
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val isUltra = purchase.products.contains(PRODUCT_ID_ULTRA_MONTHLY) ||
                purchase.products.contains(PRODUCT_ID_ULTRA_YEARLY)

            if (isUltra) setStoredUltra(true)

            if (isUltra && !purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { result ->
                    if (result.responseCode == BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully.")
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
                    }
                }
            }

            if (isUltra) {
                _purchaseStatusMessage.value = "¡Bienvenido a Tasa VE Ultra! Cero anuncios y todas las funciones desbloqueadas."
            }
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            startBillingConnection()
            return
        }

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                val hasUltra = purchasesList.any { purchase ->
                    (purchase.products.contains(PRODUCT_ID_ULTRA_MONTHLY) ||
                        purchase.products.contains(PRODUCT_ID_ULTRA_YEARLY)) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                setStoredUltra(hasUltra)
            }
        }
    }

    fun restorePurchases() {
        queryPurchases()
        _purchaseStatusMessage.value = "Verificando suscripciones en Google Play..."
    }

    fun clearStatusMessage() {
        _purchaseStatusMessage.value = null
    }

    private fun setStoredUltra(isUltra: Boolean) {
        _purchasedUltra.value = isUltra
        storageHelper.setUltraUser(isUltra)
        com.julcer26.tasave.widget.TasaVeRatesWidgetProvider.updateAllWidgets(context)
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
