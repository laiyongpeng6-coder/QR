package com.qrscanfast.core.domain.model

/**
 * Represents the outcome of a full-screen ad show attempt.
 *
 * This sealed interface models all possible results when the [AdManager]
 * attempts to display a full-screen ad (app open or interstitial).
 * Callers use this to determine whether to proceed with the next action
 * or handle a specific failure scenario.
 *
 * @see AdManager
 */
sealed interface AdShowResult {
    /** The ad was successfully displayed and dismissed by the user. */
    data object Shown : AdShowResult

    /** The ad failed to load (network error, no fill, timeout, etc.). */
    data object LoadFailed : AdShowResult

    /** The ad was skipped due to frequency control limits. */
    data object FrequencyLimited : AdShowResult

    /** The ad was skipped because the user is a premium subscriber. */
    data object PremiumUser : AdShowResult
}
