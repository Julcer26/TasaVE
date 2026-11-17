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
 * - "Tasa VE Pro" ($2.99 USD one-time purchase)
 * - "Tasa VE Ultra" ($1.49/month or $9.99/year subscription)
 *
 * Key entitlement rule:
 * - Active Ultra subscribers automatically receive ALL Pro benefits
 *   (no ads + unlimited camera scanner) without needing to purchase Pro separately.
 * - If Ultra expires/cancels, users who previously purchased Pro retain Pro for life.
 */
class BillingManager(
    private val context: Context,
    private val storageHelper: LocalStorageHelper,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_ID_PRO = "tasave_pro_remove_ads"
        const val PRODUCT_ID_ULTRA_MONTHLY = "tasave_ultra_monthly"
        const val PRODUCT_ID_ULTRA_YEARLY = "tasave_ultra_yearly"

        fun resolveEffectiveEntitlements(
            purchasedPro: Boolean,
            purchasedUltra: Boolean,
            isDebugBuild: Boolean,
            debugTier: UserTier
        ): Pair<Boolean, Boolean> {
            return if (isDebugBuild) {
                when (debugTier) {
                    UserTier.FREE -> false to false
                    UserTier.PRO -> true to false
                    UserTier.ULTRA -> true to true
                }
            } else {
                val isUltra = purchasedUltra
                val hasPro = purchasedPro || isUltra
                hasPro to isUltra
            }
        }
    }

    private val _purchasedPro = MutableStateFlow(storageHelper.isProUser())
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
        resolveEffectiveEntitlements(_purchasedPro.value, ultra, BuildConfig.DEBUG, dbg).second
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = resolveEffectiveEntitlements(
            _purchasedPro.value,
            _purchasedUltra.value,
            BuildConfig.DEBUG,
            _debugTier.value
        ).second
    )

    val isProUser: StateFlow<Boolean> = combine(_purchasedPro, _purchasedUltra, _debugTier) { pro, ultra, dbg ->
        resolveEffectiveEntitlements(pro, ultra, BuildConfig.DEBUG, dbg).first
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = resolveEffectiveEntitlements(
            _purchasedPro.value,
            _purchasedUltra.value,
            BuildConfig.DEBUG,
            _debugTier.value
        ).first
    )

    private val _productPrice = MutableStateFlow("$2.99 USD")
    val productPrice: StateFlow<String> = _productPrice.asStateFlow()

    private val _ultraMonthlyPrice = MutableStateFlow("$1.49 / mes")
    val ultraMonthlyPrice: StateFlow<String> = _ultraMonthlyPrice.asStateFlow()

    private val _ultraYearlyPrice = MutableStateFlow("$9.99 / año")
    val ultraYearlyPrice: StateFlow<String> = _ultraYearlyPrice.asStateFlow()

    private val _purchaseStatusMessage = MutableStateFlow<String?>(null)
    val purchaseStatusMessage: StateFlow<String?> = _purchaseStatusMessage.asStateFlow()

    private var proProductDetails: ProductDetails? = null
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
        _purchaseStatusMessage.value = when (tier) {
            UserTier.FREE -> "Modo prueba: Plan Gratis"
            UserTier.PRO -> "Modo prueba: Plan Pro activado"
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
        // 1. One-time Pro INAPP product
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_PRO)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { billingResult, result ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                val details = result.productDetailsList.firstOrNull { it.productId == PRODUCT_ID_PRO }
                if (details != null) {
                    proProductDetails = details
                    details.oneTimePurchaseOfferDetails?.formattedPrice?.let {
                        _productPrice.value = it
                    }
                }
            }
        }

        // 2. Ultra SUBS products
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

    fun launchPurchase(activity: Activity) {
        val details = proProductDetails
        if (details == null) {
            if (!billingClient.isReady) startBillingConnection()
            _purchaseStatusMessage.value = "Conectando con Google Play... Reintenta en un momento."
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, params).responseCode
        if (responseCode != BillingResponseCode.OK) {
            _purchaseStatusMessage.value = "No se pudo iniciar la compra. Código: $responseCode"
        }
    }

    fun launchUltraSubscription(activity: Activity, yearly: Boolean = false) {
        val details = if (yearly) ultraYearlyDetails else ultraMonthlyDetails
        val offerToken = details?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (details == null || offerToken == null) {
            if (!billingClient.isReady) startBillingConnection()
            _purchaseStatusMessage.value = if (BuildConfig.DEBUG) {
                "En prueba puedes alternar los planes Gratis / Pro / Ultra desde Configuración."
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
                _purchaseStatusMessage.value = "¡Ya tienes este plan activo en tu cuenta!"
            }
            else -> {
                _purchaseStatusMessage.value = "Error al procesar compra: ${billingResult.debugMessage}"
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val isPro = purchase.products.contains(PRODUCT_ID_PRO)
            val isUltra = purchase.products.contains(PRODUCT_ID_ULTRA_MONTHLY) ||
                purchase.products.contains(PRODUCT_ID_ULTRA_YEARLY)

            if (isPro) setStoredPro(true)
            if (isUltra) setStoredUltra(true)

            if ((isPro || isUltra) && !purchase.isAcknowledged) {
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
                _purchaseStatusMessage.value = "¡Bienvenido a Tasa VE Ultra! Todas las funciones y beneficios Pro activados."
            } else if (isPro) {
                _purchaseStatusMessage.value = "¡Gracias por adquirir Tasa VE Pro! Anuncios desactivados y escáner ilimitado."
            }
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            startBillingConnection()
            return
        }

        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(inAppParams) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                val hasPro = purchasesList.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_PRO) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                setStoredPro(hasPro)
            }
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
        _purchaseStatusMessage.value = "Verificando compras y suscripciones en Google Play..."
    }

    fun clearStatusMessage() {
        _purchaseStatusMessage.value = null
    }

    private fun setStoredPro(isPro: Boolean) {
        _purchasedPro.value = isPro
        storageHelper.setProUser(isPro)
    }

    private fun setStoredUltra(isUltra: Boolean) {
        _purchasedUltra.value = isUltra
        storageHelper.setUltraUser(isUltra)
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
