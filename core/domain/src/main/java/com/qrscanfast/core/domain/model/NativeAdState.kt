package com.qrscanfast.core.domain.model

/**
 * Represents the lifecycle state of a native ad for a given placement.
 *
 * UI components observe this state via [StateFlow] to reactively render
 * or hide native ad cards. The [nativeAd] in [Loaded] is typed as [Any]
 * to avoid leaking AdMob SDK types into the domain layer; the ads module
 * casts it to the appropriate platform type when binding views.
 *
 * @see AdManager.getNativeAdState
 */
sealed interface NativeAdState {
    /** The native ad is currently being loaded. */
    data object Loading : NativeAdState

    /** The native ad loaded successfully and is ready to display. */
    data class Loaded(val nativeAd: Any) : NativeAdState

    /** The native ad failed to load. The UI should hide the ad area. */
    data object Failed : NativeAdState

    /** The native ad area is intentionally hidden (e.g., premium user). */
    data object Hidden : NativeAdState
}
