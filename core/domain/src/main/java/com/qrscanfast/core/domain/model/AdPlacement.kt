package com.qrscanfast.core.domain.model

/**
 * Enumerates all ad placement scenarios in the application.
 *
 * Each placement identifies a specific location/trigger where an ad may be shown.
 * The [type] indicates the ad format, and [scene] provides a human-readable
 * identifier for analytics and AdMob backend attribution.
 *
 * Note: Actual ad unit IDs are not stored here (domain layer has no BuildConfig access).
 * The mapping from [AdPlacement] to ad unit ID is provided by the `:core:ads` module
 * via a configuration class at runtime.
 *
 * @property type The ad format type for this placement.
 * @property scene A unique scene identifier used for analytics and ad unit ID mapping.
 *
 * @see AdType
 */
enum class AdPlacement(val type: AdType, val scene: String) {
    /** App open ad shown after user dismisses subscription screen on cold start. */
    APP_OPEN_COLD_START(AdType.APP_OPEN, "cold_start"),

    /** Interstitial ad shown after scan result is obtained. */
    INTERSTITIAL_SCAN(AdType.INTERSTITIAL, "scan"),

    /** Interstitial ad shown after QR/barcode generation. */
    INTERSTITIAL_GENERATE(AdType.INTERSTITIAL, "generate"),

    /** Interstitial ad shown before entering AI beautify page. */
    INTERSTITIAL_AI_BEAUTIFY(AdType.INTERSTITIAL, "ai_beautify"),

    /** Interstitial ad shown when user confirms watching ad to unlock advanced feature. */
    INTERSTITIAL_ADVANCED_UNLOCK(AdType.INTERSTITIAL, "advanced_unlock"),

    /** Native card ad embedded in onboarding flow. */
    NATIVE_ONBOARDING(AdType.NATIVE, "onboarding"),

    /** Native card ad displayed above bottom tab navigation on home screen. */
    NATIVE_HOME_TAB(AdType.NATIVE, "home_tab"),

    /** Native card ad inserted periodically in history list. */
    NATIVE_HISTORY_LIST(AdType.NATIVE, "history_list"),

    /** Native card ad displayed in scan result detail page. */
    NATIVE_SCAN_RESULT_DETAIL(AdType.NATIVE, "scan_result_detail")
}
