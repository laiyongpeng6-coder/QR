package com.qrscanfast.core.ads

import android.app.Activity
import com.qrscanfast.core.common.RemoteConfigManager
import com.qrscanfast.core.domain.ads.AdManager
import com.qrscanfast.core.domain.ads.FrequencyController
import com.qrscanfast.core.domain.model.AdPlacement
import com.qrscanfast.core.domain.repository.SubscriptionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the ad interception flow for gated user actions.
 *
 * The gate flow for free users follows this sequence:
 * 1. Check premium status — if premium, skip everything and return [GateResult.Proceed].
 * 2. Optionally show the subscription screen via the provided [showSubscriptionScreen] callback.
 *    - If the user purchases a subscription, return [GateResult.SubscriptionPurchased].
 *    - If the user dismisses the subscription screen, continue to step 3.
 * 3. Check frequency control — if the interstitial limit is reached, return [GateResult.Proceed].
 * 4. Show an interstitial ad for the given [AdPlacement].
 * 5. Return [GateResult.Proceed] regardless of whether the ad was shown, failed, or was dismissed.
 *
 * This class resides in `:core:ads` and does NOT directly depend on `feature:subscription`.
 * The subscription screen display is delegated to the caller via a suspend lambda callback.
 *
 * @see GateResult
 * @see AdManager
 * @see FrequencyController
 */
@Singleton
class AdGatekeeper @Inject constructor(
    private val adManager: AdManager,
    private val subscriptionRepository: SubscriptionRepository,
    private val frequencyController: FrequencyController,
    private val remoteConfig: RemoteConfigManager
) {

    /**
     * Executes the ad gate flow for a user action.
     *
     * 核心逻辑：
     * - ads_enabled=false（审核期）：直接放行，不展示任何订阅页或广告
     * - Premium 用户：直接放行
     * - Free 用户 + ads_enabled=true：展示订阅页→关闭后展示插屏广告→放行
     *
     * 无论如何，用户的操作最终一定会被执行（VIP 不卡功能，只是去广告）。
     */
    suspend fun gate(
        activity: Activity,
        placement: AdPlacement,
        showSubscriptionScreen: (suspend () -> Boolean)? = null
    ): GateResult {
        // 全局广告开关关闭（审核期）→ 直接放行，不弹任何东西
        if (!remoteConfig.adsEnabled) {
            return GateResult.Proceed
        }

        // Premium users bypass everything
        if (subscriptionRepository.isPremium.value) {
            return GateResult.Proceed
        }

        // Show subscription screen if callback is provided
        if (showSubscriptionScreen != null) {
            val purchased = showSubscriptionScreen()
            if (purchased) {
                return GateResult.SubscriptionPurchased
            }
        }

        // Check frequency control — if limited, skip the ad
        if (!frequencyController.canShowInterstitial()) {
            return GateResult.Proceed
        }

        // Show interstitial ad (result ignored — always proceed after)
        adManager.showFullScreenAd(activity, placement)

        // Always proceed — VIP 不卡功能
        return GateResult.Proceed
    }
}
