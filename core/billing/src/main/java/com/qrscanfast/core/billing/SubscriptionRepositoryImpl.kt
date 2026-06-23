package com.qrscanfast.core.billing

import android.app.Activity
import com.qrscanfast.core.domain.model.PurchaseResult
import com.qrscanfast.core.domain.model.SubscriptionPlan
import com.qrscanfast.core.domain.model.SubscriptionState
import com.qrscanfast.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [SubscriptionRepository] backed by Google Play Billing.
 *
 * Delegates subscription state management to [SubscriptionStateManager] and
 * purchase flow operations to [PlayBillingClientWrapper].
 *
 * The [isPremium] flow is derived from [subscriptionState] and emits `true`
 * only when the state is [SubscriptionState.Premium].
 */
@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val billingClient: PlayBillingClientWrapper,
    private val stateManager: SubscriptionStateManager,
    @BillingScope private val scope: CoroutineScope
) : SubscriptionRepository {

    override val subscriptionState: StateFlow<SubscriptionState>
        get() = stateManager.state

    override val isPremium: StateFlow<Boolean> = stateManager.state
        .map { it is SubscriptionState.Premium }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * Queries Google Play for the user's active purchases and updates state.
     *
     * Delegates to [SubscriptionStateManager.refreshState] which handles
     * both online queries and offline cache fallback.
     *
     * @return The latest [SubscriptionState] after refresh, or an error on unexpected failure.
     */
    override suspend fun queryPurchases(): Result<SubscriptionState> {
        return try {
            stateManager.refreshState()
            Result.success(stateManager.state.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches the Google Play purchase flow for the specified plan.
     *
     * 1. Calls [PlayBillingClientWrapper.launchBillingFlow] to show the purchase dialog.
     * 2. Collects the first emission from [PlayBillingClientWrapper.purchaseResults].
     * 3. On success, calls [SubscriptionStateManager.onPurchaseCompleted] to update state.
     *
     * @param activity The [Activity] required by Google Play to present the purchase UI.
     * @param plan The [SubscriptionPlan] the user wants to purchase.
     * @return A [Result] wrapping the [PurchaseResult] indicating success, cancellation, or error.
     */
    override suspend fun launchPurchaseFlow(
        activity: Activity,
        plan: SubscriptionPlan
    ): Result<PurchaseResult> {
        return try {
            val billingResult = billingClient.launchBillingFlow(activity, plan)
                ?: return Result.success(
                    PurchaseResult.Error(
                        code = -1,
                        message = "Failed to connect to Google Play or query product details"
                    )
                )

            // Wait for the purchase result from the PurchasesUpdatedListener callback
            val purchaseResult = billingClient.purchaseResults.first()

            // If purchase succeeded, update state manager
            if (purchaseResult is PurchaseResult.Success) {
                // Query purchases to get the actual Purchase object for acknowledgement
                val purchases = billingClient.queryPurchases()
                val completedPurchase = purchases.firstOrNull { purchase ->
                    plan.productId in purchase.products
                }
                if (completedPurchase != null) {
                    stateManager.onPurchaseCompleted(completedPurchase)
                }
            }

            Result.success(purchaseResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores previously purchased subscriptions for the current user.
     *
     * Delegates to [SubscriptionStateManager.refreshState] which queries Google Play
     * for any active purchases associated with the user's account.
     *
     * @return The restored [SubscriptionState], or an error on failure.
     */
    override suspend fun restorePurchases(): Result<SubscriptionState> {
        return try {
            stateManager.refreshState()
            Result.success(stateManager.state.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
