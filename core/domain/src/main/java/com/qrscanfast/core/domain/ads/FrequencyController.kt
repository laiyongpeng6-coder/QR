package com.qrscanfast.core.domain.ads

/**
 * Interface for controlling interstitial ad display frequency.
 *
 * Enforces two constraints to protect user experience:
 * 1. Minimum interval between consecutive interstitial ads (≥ 60 seconds).
 * 2. Maximum number of interstitial ads per session (≤ 10).
 *
 * The implementation resides in `:core:ads` and uses a clock source for testability.
 * Session limits reset when [resetSession] is called (typically on app cold start).
 *
 * @see AdManager
 */
interface FrequencyController {

    /**
     * Checks whether an interstitial ad can be shown based on both
     * the time interval constraint and the session limit.
     *
     * @return `true` if the minimum interval has elapsed AND the session limit is not reached.
     */
    fun canShowInterstitial(): Boolean

    /**
     * Checks whether the session-level ad count limit has been reached.
     *
     * @return `true` if fewer than the maximum allowed ads have been shown this session.
     */
    fun isWithinSessionLimit(): Boolean

    /**
     * Records that an interstitial ad was shown.
     *
     * Updates the last-show timestamp and increments the session counter.
     * Must be called after every successful interstitial display.
     */
    fun recordInterstitialShow()

    /**
     * Resets the session counter to zero.
     *
     * Typically called on cold start or when a new session begins.
     * Does not reset the last-show timestamp.
     */
    fun resetSession()
}
