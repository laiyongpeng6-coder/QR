package com.qrscanfast.core.billing

import com.android.billingclient.api.Purchase
import com.qrscanfast.core.domain.model.SubscriptionPlan
import com.qrscanfast.core.domain.model.SubscriptionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages subscription state by querying Google Play and syncing with local cache.
 *
 * This class is the single source of truth for the user's current subscription state.
 * It coordinates between the Google Play BillingClient (remote source) and
 * [SubscriptionPreferences] (local cache / offline fallback).
 *
 * State transitions:
 * - [SubscriptionState.Loading] → [SubscriptionState.Free] or [SubscriptionState.Premium]
 * - [SubscriptionState.Free] → [SubscriptionState.Premium] (on purchase completed)
 * - [SubscriptionState.Premium] → [SubscriptionState.Free] (on subscription expired)
 *
 * When the BillingClient connection fails, the manager falls back to the locally cached
 * state from [SubscriptionPreferences], ensuring the user is not blocked.
 */
@Singleton
class SubscriptionStateManager @Inject constructor(
    private val billingClient: PlayBillingClientWrapper,
    private val preferences: SubscriptionPreferences
) {

    private val _state = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)

    /** The current subscription state, exposed as an immutable StateFlow. */
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    /**
     * Queries Google Play for active purchases and updates the subscription state.
     *
     * On success:
     * - If an active purchase matching a known [SubscriptionPlan] is found,
     *   the state is set to [SubscriptionState.Premium] and the cache is updated.
     * - If no active purchases are found, the state is set to [SubscriptionState.Free]
     *   and the cache is cleared.
     *
     * On failure (BillingClient connection timeout or error):
     * - Falls back to the locally cached state from [SubscriptionPreferences].
     * - If the cache indicates premium, the state is set to [SubscriptionState.Premium].
     * - Otherwise, defaults to [SubscriptionState.Free].
     */
    suspend fun refreshState() {
        // Attempt to connect first; if connection fails, fall back to cache immediately
        if (!billingClient.ensureConnected()) {
            fallbackToCachedState()
            return
        }

        val purchases = billingClient.queryPurchases()

        // Find an active purchase matching a known subscription plan
        val activePurchase = purchases.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (activePurchase != null) {
            val plan = findPlanForPurchase(activePurchase)
            if (plan != null) {
                val expiryTime = extractExpiryTime(activePurchase)
                val premiumState = SubscriptionState.Premium(plan = plan, expiryTime = expiryTime)
                _state.value = premiumState

                // Sync cache
                preferences.updateStatus(
                    isPremium = true,
                    plan = plan.name,
                    expiryMs = expiryTime?.toEpochMilli()
                )
                return
            }
        }

        // No active purchases found — user is Free
        _state.value = SubscriptionState.Free
        preferences.updateStatus(isPremium = false, plan = null, expiryMs = null)
    }

    /**
     * Handles a successful purchase completion.
     *
     * Acknowledges the purchase (required by Google Play within 3 days),
     * determines the purchased plan, updates the state to [SubscriptionState.Premium],
     * and persists the result to local cache.
     *
     * @param purchase The [Purchase] object from Google Play that has been completed.
     */
    suspend fun onPurchaseCompleted(purchase: Purchase) {
        // Acknowledge the purchase (no-op if already acknowledged)
        billingClient.acknowledgePurchase(purchase)

        // Determine which plan was purchased
        val plan = findPlanForPurchase(purchase) ?: return
        val expiryTime = extractExpiryTime(purchase)

        // Update state to Premium
        val premiumState = SubscriptionState.Premium(plan = plan, expiryTime = expiryTime)
        _state.value = premiumState

        // Persist to local cache
        preferences.updateStatus(
            isPremium = true,
            plan = plan.name,
            expiryMs = expiryTime?.toEpochMilli()
        )
    }

    /**
     * Handles subscription expiration or cancellation.
     *
     * Reverts the state to [SubscriptionState.Free] and clears the local cache.
     */
    suspend fun onSubscriptionExpired() {
        _state.value = SubscriptionState.Free
        preferences.updateStatus(isPremium = false, plan = null, expiryMs = null)
    }

    /**
     * Falls back to the locally cached subscription state when the BillingClient
     * connection fails or times out.
     */
    private suspend fun fallbackToCachedState() {
        val cachedIsPremium = preferences.isPremium.first()

        if (cachedIsPremium) {
            val cachedPlanName = preferences.activePlan.first()
            val cachedExpiryMs = preferences.expiryTimeMs.first()

            val plan = cachedPlanName?.let { name ->
                SubscriptionPlan.entries.firstOrNull { it.name == name }
            }

            if (plan != null) {
                val expiryTime = cachedExpiryMs?.let { Instant.ofEpochMilli(it) }
                _state.value = SubscriptionState.Premium(plan = plan, expiryTime = expiryTime)
            } else {
                // Cache is inconsistent — default to Free
                _state.value = SubscriptionState.Free
            }
        } else {
            _state.value = SubscriptionState.Free
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
     * Extracts the expiry time from a purchase.
     *
     * For one-time purchases (e.g., LIFETIME), returns null since they don't expire.
     * For subscriptions, uses the purchase time as a reference point. Note: the actual
     * expiry time should ideally come from a server-side receipt validation, but for
     * client-side we rely on the purchase being reported as active by queryPurchases().
     */
    private fun extractExpiryTime(purchase: Purchase): Instant? {
        val plan = findPlanForPurchase(purchase) ?: return null
        if (plan.isOneTime) return null

        // The purchase time from Google Play. The actual subscription expiry is managed
        // by Google Play — if queryPurchases() returns it, it's still active.
        // We store null to indicate "managed by Google Play" for recurring subscriptions.
        return null
    }
}
