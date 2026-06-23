package com.qrscanfast.core.domain.model

/**
 * Available subscription plans for the application.
 *
 * Each plan maps to a Google Play Billing product configuration with
 * its associated product ID, base plan, and optional offer.
 *
 * @property productId The Google Play product identifier.
 * @property basePlanId The base plan identifier for subscriptions (null for one-time purchases).
 * @property offerId The promotional offer identifier (null if no offer applies).
 * @property isOneTime Whether this is a one-time purchase (true for LIFETIME).
 */
enum class SubscriptionPlan(
    val productId: String,
    val basePlanId: String?,
    val offerId: String?,
    val isOneTime: Boolean = false
) {
    TRIAL("fastqr.3dayfree", "qrscan-3dayfree", "3dayfree"),
    WEEKLY("fastqr.3dayfree", "qrscan-3dayfree", null),
    ANNUAL("qrscan.year", "qrscan-year", null),
    LIFETIME("lifetime_vip", null, null, isOneTime = true)
}
