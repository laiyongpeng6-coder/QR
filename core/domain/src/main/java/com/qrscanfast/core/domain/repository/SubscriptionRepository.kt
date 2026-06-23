package com.qrscanfast.core.domain.repository

import android.app.Activity
import com.qrscanfast.core.domain.model.PurchaseResult
import com.qrscanfast.core.domain.model.SubscriptionPlan
import com.qrscanfast.core.domain.model.SubscriptionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing subscription state and purchase operations.
 *
 * This interface defines the contract for subscription management, abstracting
 * the underlying Google Play Billing implementation. Implementations are provided
 * in the `:core:billing` module and injected via Hilt.
 *
 * The subscription state is exposed as reactive [StateFlow] properties, enabling
 * UI layers to automatically adapt when the user's subscription status changes.
 *
 * @see SubscriptionState
 * @see SubscriptionPlan
 * @see PurchaseResult
 */
interface SubscriptionRepository {

    /**
     * Observes the current subscription state reactively.
     *
     * Starts with [SubscriptionState.Loading] and transitions to [SubscriptionState.Free]
     * or [SubscriptionState.Premium] once the status is determined from Google Play
     * or the local cache.
     */
    val subscriptionState: StateFlow<SubscriptionState>

    /**
     * Convenience property that emits `true` when the user has an active subscription.
     *
     * Derived from [subscriptionState] — emits `true` only when the state is
     * [SubscriptionState.Premium].
     */
    val isPremium: StateFlow<Boolean>

    /**
     * Queries the current user's active purchases from Google Play.
     *
     * This should be called at app startup to verify the subscription status.
     * On network failure, the implementation should fall back to locally cached state.
     *
     * @return A [Result] containing the resolved [SubscriptionState], or an exception on failure.
     */
    suspend fun queryPurchases(): Result<SubscriptionState>

    /**
     * Launches the Google Play purchase flow for the specified subscription plan.
     *
     * @param activity The [Activity] context required by the Google Play Billing API
     *   to display the purchase dialog.
     * @param plan The [SubscriptionPlan] the user wishes to purchase.
     * @return A [Result] containing the [PurchaseResult] indicating success, cancellation, or error.
     */
    suspend fun launchPurchaseFlow(activity: Activity, plan: SubscriptionPlan): Result<PurchaseResult>

    /**
     * Restores previously purchased subscriptions for the current user.
     *
     * Used when the user reinstalls the app or switches devices. Queries Google Play
     * for any active subscriptions associated with the user's account.
     *
     * @return A [Result] containing the restored [SubscriptionState], or an exception on failure.
     */
    suspend fun restorePurchases(): Result<SubscriptionState>
}
