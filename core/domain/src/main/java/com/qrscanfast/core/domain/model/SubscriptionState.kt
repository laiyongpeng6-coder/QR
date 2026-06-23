package com.qrscanfast.core.domain.model

import java.time.Instant

/**
 * Represents the current subscription state of the user.
 *
 * The state machine transitions:
 * - [Loading] → [Free] or [Premium] (on initial query)
 * - [Free] → [Premium] (on successful purchase or restore)
 * - [Premium] → [Free] (on subscription expiry or cancellation)
 */
sealed class SubscriptionState {

    /** Initial state while querying subscription status from Google Play. */
    data object Loading : SubscriptionState()

    /** User has no active subscription. */
    data object Free : SubscriptionState()

    /** User has an active subscription with the given plan and optional expiry time. */
    data class Premium(
        val plan: SubscriptionPlan,
        val expiryTime: Instant?
    ) : SubscriptionState()
}
