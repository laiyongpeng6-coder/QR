package com.qrscanfast.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.qrscanfast.core.domain.model.PurchaseResult
import com.qrscanfast.core.domain.model.SubscriptionPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Wraps Google Play BillingClient to provide a coroutine-friendly API.
 *
 * Responsibilities:
 * - Manage BillingClient connection lifecycle with 5-second timeout and auto-reconnect
 * - Query product details for subscriptions and one-time purchases
 * - Launch the Google Play purchase flow
 * - Query existing purchases (for restore and status check)
 * - Acknowledge purchases
 * - Parse purchase update callbacks into domain [PurchaseResult]
 */
@Singleton
class PlayBillingClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 5_000L
        private const val MAX_RECONNECT_ATTEMPTS = 3
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        val result = parsePurchaseResult(billingResult, purchases)
        _purchaseResults.tryEmit(result)
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val _purchaseResults = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 1)

    /**
     * Emits parsed [PurchaseResult] whenever the Play Store purchase dialog completes.
     * Collectors should subscribe before launching the billing flow.
     */
    val purchaseResults: SharedFlow<PurchaseResult> = _purchaseResults.asSharedFlow()

    private var reconnectAttempts = 0

    /**
     * Ensures the BillingClient is connected, with a 5-second timeout.
     *
     * If already connected, returns immediately. On failure or timeout,
     * attempts automatic reconnection up to [MAX_RECONNECT_ATTEMPTS] times.
     *
     * @return `true` if connected successfully, `false` otherwise.
     */
    suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) {
            reconnectAttempts = 0
            return true
        }

        val connected = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            reconnectAttempts = 0
                            if (continuation.isActive) continuation.resume(true)
                        } else {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        // Will be handled by reconnect logic on next operation
                        if (continuation.isActive) continuation.resume(false)
                    }
                })

                continuation.invokeOnCancellation {
                    // Timeout or coroutine cancellation — no cleanup needed for startConnection
                }
            }
        }

        if (connected == true) return true

        // Auto-reconnect attempt
        reconnectAttempts++
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            return ensureConnected()
        }

        reconnectAttempts = 0
        return false
    }

    /**
     * Queries product details for the given [SubscriptionPlan].
     *
     * Automatically determines whether to query for subscription or one-time (inapp) products
     * based on [SubscriptionPlan.isOneTime].
     *
     * @param plan The subscription plan to query details for.
     * @return The [ProductDetailsResult], or `null` if the connection fails.
     */
    suspend fun queryProductDetails(plan: SubscriptionPlan): ProductDetailsResult? {
        if (!ensureConnected()) return null

        val productType = if (plan.isOneTime) {
            BillingClient.ProductType.INAPP
        } else {
            BillingClient.ProductType.SUBS
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(plan.productId)
                .setProductType(productType)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        return billingClient.queryProductDetails(params)
    }

    /**
     * Queries product details for all available subscription plans.
     *
     * Groups products by type (subscriptions vs one-time) and queries them separately,
     * then merges the results.
     *
     * @return A map of productId to ProductDetails, or an empty map on failure.
     */
    suspend fun queryAllProductDetails(): Map<String, com.android.billingclient.api.ProductDetails> {
        if (!ensureConnected()) return emptyMap()

        val result = mutableMapOf<String, com.android.billingclient.api.ProductDetails>()

        // Query subscriptions
        val subsProducts = SubscriptionPlan.entries
            .filter { !it.isOneTime }
            .map { it.productId }
            .distinct()
            .map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

        if (subsProducts.isNotEmpty()) {
            val subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subsProducts)
                .build()
            val subsResult = billingClient.queryProductDetails(subsParams)
            subsResult.productDetailsList?.forEach { details ->
                result[details.productId] = details
            }
        }

        // Query one-time purchases
        val inappProducts = SubscriptionPlan.entries
            .filter { it.isOneTime }
            .map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId.productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }

        if (inappProducts.isNotEmpty()) {
            val inappParams = QueryProductDetailsParams.newBuilder()
                .setProductList(inappProducts)
                .build()
            val inappResult = billingClient.queryProductDetails(inappParams)
            inappResult.productDetailsList?.forEach { details ->
                result[details.productId] = details
            }
        }

        return result
    }

    /**
     * Launches the Google Play purchase flow for the specified plan.
     *
     * Builds appropriate [BillingFlowParams] based on the plan's type:
     * - Subscriptions: includes basePlanId and optional offerId
     * - One-time purchases: uses simple product details
     *
     * @param activity The activity context required by Google Play to show the purchase UI.
     * @param plan The [SubscriptionPlan] to purchase.
     * @return The [BillingResult] from launching the flow, or `null` if connection/product query fails.
     */
    suspend fun launchBillingFlow(
        activity: Activity,
        plan: SubscriptionPlan
    ): BillingResult? {
        if (!ensureConnected()) return null

        val productDetailsResult = queryProductDetails(plan) ?: return null

        val productDetails = productDetailsResult.productDetailsList?.firstOrNull() ?: return null

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        // For subscriptions, set the offer token (basePlan + optional offer)
        if (!plan.isOneTime) {
            val offerToken = findOfferToken(productDetails, plan)
            if (offerToken != null) {
                productDetailsParamsBuilder.setOfferToken(offerToken)
            }
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Queries existing purchases for both subscriptions and one-time products.
     *
     * @return A list of active [Purchase] objects, or an empty list on failure.
     */
    suspend fun queryPurchases(): List<Purchase> {
        if (!ensureConnected()) return emptyList()

        val allPurchases = mutableListOf<Purchase>()

        // Query active subscriptions
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val subsResult = billingClient.queryPurchasesAsync(subsParams)
        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            allPurchases.addAll(subsResult.purchasesList)
        }

        // Query one-time purchases
        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val inappResult = billingClient.queryPurchasesAsync(inappParams)
        if (inappResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            allPurchases.addAll(inappResult.purchasesList)
        }

        return allPurchases
    }

    /**
     * Acknowledges a purchase that has not yet been acknowledged.
     *
     * Purchases must be acknowledged within 3 days or they will be refunded automatically
     * by Google Play.
     *
     * @param purchase The [Purchase] to acknowledge.
     * @return `true` if acknowledgement succeeded or purchase was already acknowledged, `false` otherwise.
     */
    suspend fun acknowledgePurchase(purchase: Purchase): Boolean {
        if (purchase.isAcknowledged) return true
        if (!ensureConnected()) return false

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        val result = suspendCancellableCoroutine { continuation ->
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (continuation.isActive) {
                    continuation.resume(
                        billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    )
                }
            }
        }

        return result
    }

    /**
     * Ends the BillingClient connection and releases resources.
     *
     * Should be called when the billing functionality is no longer needed
     * (e.g., in Application.onTerminate or when the billing scope is destroyed).
     */
    fun endConnection() {
        billingClient.endConnection()
    }

    /**
     * Parses the [PurchasesUpdatedListener] callback into a domain [PurchaseResult].
     */
    private fun parsePurchaseResult(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ): PurchaseResult {
        return when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase != null && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val plan = findPlanForPurchase(purchase)
                    if (plan != null) {
                        PurchaseResult.Success(plan)
                    } else {
                        PurchaseResult.Error(
                            code = billingResult.responseCode,
                            message = "Unknown product purchased"
                        )
                    }
                } else {
                    PurchaseResult.Error(
                        code = billingResult.responseCode,
                        message = "Purchase not completed: ${billingResult.debugMessage}"
                    )
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                PurchaseResult.Cancelled
            }

            else -> {
                PurchaseResult.Error(
                    code = billingResult.responseCode,
                    message = billingResult.debugMessage
                )
            }
        }
    }

    /**
     * Finds the matching [SubscriptionPlan] for a given [Purchase] based on product IDs.
     */
    private fun findPlanForPurchase(purchase: Purchase): SubscriptionPlan? {
        val productIds = purchase.products
        return SubscriptionPlan.entries.firstOrNull { plan ->
            plan.productId in productIds
        }
    }

    /**
     * Finds the appropriate offer token for a subscription plan.
     *
     * For plans with an offerId, looks for the matching offer.
     * For plans without an offerId, uses the base plan's offer token.
     */
    private fun findOfferToken(
        productDetails: com.android.billingclient.api.ProductDetails,
        plan: SubscriptionPlan
    ): String? {
        val offerDetailsList = productDetails.subscriptionOfferDetails ?: return null

        // If plan has a specific offerId, find it
        if (plan.offerId != null) {
            val matchingOffer = offerDetailsList.firstOrNull { offer ->
                offer.basePlanId == plan.basePlanId && offer.offerId == plan.offerId
            }
            if (matchingOffer != null) return matchingOffer.offerToken
        }

        // Fall back to the base plan offer (no specific offerId)
        val basePlanOffer = offerDetailsList.firstOrNull { offer ->
            offer.basePlanId == plan.basePlanId && offer.offerId == null
        }
        if (basePlanOffer != null) return basePlanOffer.offerToken

        // Last resort: first matching base plan
        return offerDetailsList.firstOrNull { offer ->
            offer.basePlanId == plan.basePlanId
        }?.offerToken
    }
}
