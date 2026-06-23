package com.qrscanfast.core.ads

import android.app.Activity
import com.qrscanfast.core.domain.ads.AdManager
import com.qrscanfast.core.domain.model.AdPlacement
import com.qrscanfast.core.domain.model.AdShowResult
import com.qrscanfast.core.domain.repository.SubscriptionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the advanced feature unlock flow via ad viewing.
 *
 * This class implements the "高级功能广告解锁" flow:
 * 1. Check premium status — if premium, return [UnlockResult.AlreadyUnlocked].
 * 2. Attempt to show an interstitial ad (INTERSTITIAL_ADVANCED_UNLOCK).
 * 3. If the ad is shown successfully, return [UnlockResult.Unlocked].
 * 4. If the ad fails to load, return [UnlockResult.AdFailed].
 * 5. If frequency limited, return [UnlockResult.Unlocked] (graceful bypass).
 *
 * The caller is responsible for:
 * - Showing the "watch ad to unlock" prompt before calling [requestUnlock].
 * - Handling the [UnlockResult.AdFailed] case by showing a retry prompt.
 * - Handling user cancellation (not calling [requestUnlock] at all).
 *
 * @see UnlockResult
 * @see AdManager
 */
@Singleton
class AdvancedFeatureUnlockManager @Inject constructor(
    private val adManager: AdManager,
    private val subscriptionRepository: SubscriptionRepository
) {

    /**
     * Checks whether the user needs to watch an ad to unlock an advanced feature.
     *
     * @return `true` if the user is a free user and needs to go through the unlock flow.
     */
    fun requiresUnlock(): Boolean {
        return !subscriptionRepository.isPremium.value
    }

    /**
     * Attempts to unlock an advanced feature by showing an interstitial ad.
     *
     * Call this after the user confirms they want to watch an ad.
     *
     * @param activity The activity context required for displaying the ad.
     * @return The result of the unlock attempt.
     */
    suspend fun requestUnlock(activity: Activity): UnlockResult {
        // Premium users bypass the ad entirely
        if (subscriptionRepository.isPremium.value) {
            return UnlockResult.AlreadyUnlocked
        }

        // Show the interstitial ad for advanced feature unlock
        val adResult = adManager.showFullScreenAd(activity, AdPlacement.INTERSTITIAL_ADVANCED_UNLOCK)

        return when (adResult) {
            AdShowResult.Shown -> UnlockResult.Unlocked
            AdShowResult.LoadFailed -> UnlockResult.AdFailed
            AdShowResult.FrequencyLimited -> UnlockResult.Unlocked // Graceful bypass
            AdShowResult.PremiumUser -> UnlockResult.AlreadyUnlocked
        }
    }
}

/**
 * Represents the outcome of an advanced feature unlock attempt.
 */
sealed class UnlockResult {
    /** The ad was shown successfully — the feature is now unlocked for this use. */
    data object Unlocked : UnlockResult()

    /** The user is already premium — no ad needed, feature is always available. */
    data object AlreadyUnlocked : UnlockResult()

    /** The ad failed to load — caller should show a retry prompt. */
    data object AdFailed : UnlockResult()
}
