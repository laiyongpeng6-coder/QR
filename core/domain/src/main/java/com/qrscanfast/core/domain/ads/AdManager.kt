package com.qrscanfast.core.domain.ads

import android.app.Activity
import com.qrscanfast.core.domain.model.AdPlacement
import com.qrscanfast.core.domain.model.AdShowResult
import com.qrscanfast.core.domain.model.NativeAdState
import kotlinx.coroutines.flow.StateFlow

/**
 * Core interface for managing advertisement lifecycle in the application.
 *
 * This interface abstracts all ad operations so that feature modules can request
 * ad displays without depending on AdMob SDK types directly. The implementation
 * resides in the `:core:ads` module and is injected via Hilt.
 *
 * Key responsibilities:
 * - Determine whether an ad should be shown for a given placement (checks premium status + frequency limits)
 * - Preload ads for upcoming placements to minimize latency
 * - Show full-screen ads (app open / interstitial) with result feedback
 * - Provide reactive state for native ad rendering
 *
 * @see AdPlacement
 * @see AdShowResult
 * @see NativeAdState
 */
interface AdManager {

    /**
     * Checks whether an ad should be displayed for the given [placement].
     *
     * Returns `false` if the user is premium, frequency limits are reached,
     * or other conditions prevent showing the ad.
     *
     * @param placement The ad placement scenario to check.
     * @return `true` if the ad can be shown; `false` otherwise.
     */
    fun shouldShowAd(placement: AdPlacement): Boolean

    /**
     * Preloads an ad for the given [placement] so it is ready for immediate display.
     *
     * This should be called ahead of time (e.g., when the user is likely to trigger
     * an action that shows an ad) to minimize perceived latency.
     *
     * @param placement The ad placement to preload.
     */
    suspend fun preload(placement: AdPlacement)

    /**
     * Attempts to show a full-screen ad (app open or interstitial) for the given [placement].
     *
     * This is a suspending function that completes when the ad is dismissed or fails.
     *
     * @param activity The activity context required by AdMob to display the ad.
     * @param placement The ad placement scenario.
     * @return The result of the show attempt.
     */
    suspend fun showFullScreenAd(activity: Activity, placement: AdPlacement): AdShowResult

    /**
     * Observes the state of a native ad for the given [placement].
     *
     * UI components collect this [StateFlow] to reactively render native ad cards
     * or hide the ad area when loading fails.
     *
     * @param placement The native ad placement to observe.
     * @return A [StateFlow] emitting the current [NativeAdState].
     */
    fun getNativeAdState(placement: AdPlacement): StateFlow<NativeAdState>
}
