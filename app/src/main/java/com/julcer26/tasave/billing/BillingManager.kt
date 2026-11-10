package com.julcer26.tasave.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.julcer26.tasave.data.LocalStorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages Google Play In-App Billing for "Tasa VE Pro" ($2.99 USD one-time purchase).
 *
 * Implements Google Play Developer Program Policies:
 * - Uses official Google Play Billing Library.
 * - Handles purchase acknowledgment.
 * - Supports restoration of previous purchases.
 */
class BillingManager(
    private val context: Context,
    private val storageHelper: LocalStorageHelper,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_ID_PRO = "tasave_pro_remove_ads"
    }

    private val _isProUser = MutableStateFlow(storageHelper.isProUser())
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _productPrice = MutableStateFlow("$2.99 USD")
    val productPrice: StateFlow<String> = _productPrice.asStateFlow()

    private val _purchaseStatusMessage = MutableStateFlow<String?>(null)
    val purchaseStatusMessage: StateFlow<String?> = _purchaseStatusMessage.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        startBillingConnection()
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
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PRO)
                .setProductType(ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                val details = queryProductDetailsList.firstOrNull { it.productId == PRODUCT_ID_PRO }
                if (details != null) {
                    productDetails = details
                    val formattedPrice = details.oneTimePurchaseOfferDetails?.formattedPrice
                    if (formattedPrice != null) {
                        _productPrice.value = formattedPrice
                    }
                }
            } else {
                Log.w(TAG, "Error querying product details: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails
        if (details == null) {
            if (!billingClient.isReady) {
                startBillingConnection()
            }
            _purchaseStatusMessage.value = "Conectando con Google Play... Por favor, reintenta en un momento."
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        if (responseCode != BillingResponseCode.OK) {
            _purchaseStatusMessage.value = "No se pudo iniciar la compra. Código: $responseCode"
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
                setProStatus(true)
                _purchaseStatusMessage.value = "¡Ya tienes activada la versión Pro!"
            }
            else -> {
                _purchaseStatusMessage.value = "Error al procesar compra: ${billingResult.debugMessage}"
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(PRODUCT_ID_PRO)) {
                setProStatus(true)
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { result ->
                        if (result.responseCode == BillingResponseCode.OK) {
                            Log.d(TAG, "Purchase acknowledged successfully.")
                        } else {
                            Log.e(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
                        }
                    }
                }
                _purchaseStatusMessage.value = "¡Gracias por adquirir Tasa VE Pro! Anuncios desactivados."
            }
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            startBillingConnection()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                val hasPro = purchasesList.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_PRO) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                setProStatus(hasPro)
            }
        }
    }

    fun restorePurchases() {
        queryPurchases()
        _purchaseStatusMessage.value = "Verificando compras anteriores en Google Play..."
    }

    fun clearStatusMessage() {
        _purchaseStatusMessage.value = null
    }

    private fun setProStatus(isPro: Boolean) {
        _isProUser.value = isPro
        storageHelper.setProUser(isPro)
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
