package com.qrscanfast.core.ads

/**
 * Represents the outcome of an [AdGatekeeper.gate] call.
 *
 * The caller uses this to determine whether the user purchased a subscription
 * during the gate flow, or simply proceeded (either by watching/skipping an ad,
 * or being a premium user).
 */
sealed class GateResult {
    /**
     * The gate flow completed without a subscription purchase.
     * The caller should proceed with the original operation.
     *
     * This result is returned when:
     * - The user is already premium
     * - The user dismissed the subscription screen and the ad was shown/skipped
     * - Frequency control prevented the ad from being shown
     * - The ad failed to load (graceful degradation)
     */
    data object Proceed : GateResult()

    /**
     * The user purchased a subscription during the gate flow.
     * The caller should proceed with the original operation and update
     * any UI that reflects the user's subscription status.
     */
    data object SubscriptionPurchased : GateResult()
}
