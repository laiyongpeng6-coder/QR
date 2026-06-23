package com.qrscanfast.core.domain.model

/**
 * Result of a subscription purchase attempt.
 *
 * Encapsulates all possible outcomes of a Google Play Billing purchase flow.
 */
sealed class PurchaseResult {

    /** Purchase completed successfully for the given plan. */
    data class Success(val plan: SubscriptionPlan) : PurchaseResult()

    /** User cancelled the purchase flow. */
    data object Cancelled : PurchaseResult()

    /** Purchase failed due to an error. */
    data class Error(val code: Int, val message: String) : PurchaseResult()
}
