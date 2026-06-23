package com.qrscanfast.app.di

import com.qrscanfast.app.BuildConfig
import com.qrscanfast.core.ads.AdUnitIdProvider
import com.qrscanfast.core.domain.model.AdPlacement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-module implementation of [AdUnitIdProvider] that reads ad unit IDs
 * from BuildConfig fields configured in app/build.gradle.kts.
 *
 * Debug builds use Google's official test ad IDs.
 * Release builds use real AdMob ad unit IDs (to be configured before release).
 */
@Singleton
class AdUnitIdProviderImpl @Inject constructor() : AdUnitIdProvider {

    override fun getAdUnitId(placement: AdPlacement): String = when (placement) {
        AdPlacement.APP_OPEN_COLD_START -> BuildConfig.AD_OPEN_COLD_START
        AdPlacement.INTERSTITIAL_SCAN -> BuildConfig.AD_INTERSTITIAL_SCAN
        AdPlacement.INTERSTITIAL_GENERATE -> BuildConfig.AD_INTERSTITIAL_GENERATE
        AdPlacement.INTERSTITIAL_AI_BEAUTIFY -> BuildConfig.AD_INTERSTITIAL_AI_BEAUTIFY
        AdPlacement.INTERSTITIAL_ADVANCED_UNLOCK -> BuildConfig.AD_INTERSTITIAL_ADVANCED_UNLOCK
        AdPlacement.NATIVE_ONBOARDING -> BuildConfig.AD_NATIVE_ONBOARDING
        AdPlacement.NATIVE_HOME_TAB -> BuildConfig.AD_NATIVE_HOME_TAB
        AdPlacement.NATIVE_HISTORY_LIST -> BuildConfig.AD_NATIVE_HISTORY_LIST
        AdPlacement.NATIVE_SCAN_RESULT_DETAIL -> BuildConfig.AD_NATIVE_SCAN_RESULT_DETAIL
    }
}
