package com.qrscanfast.app.startup

/**
 * Represents the state of the app startup flow.
 *
 * The startup orchestrator drives the app through these states sequentially:
 * [Loading] → (query subscription) → [NavigateToHome] (premium) or [ShowSubscription] (free)
 * → [ShowAppOpenAd] (if user dismisses subscription) → [NavigateToHome]
 */
sealed class StartupState {

    /** Initial state while querying subscription status. */
    data object Loading : StartupState()

    /** Free user should see the subscription screen. */
    data object ShowSubscription : StartupState()

    /** Show app open ad after subscription screen is dismissed. */
    data object ShowAppOpenAd : StartupState()

    /** Final state — navigate to the home screen. */
    data object NavigateToHome : StartupState()
}
