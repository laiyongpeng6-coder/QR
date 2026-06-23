package com.qrscanfast.core.domain.model

/**
 * Represents the types of advertisements supported in the application.
 *
 * Each ad type corresponds to a different ad format provided by AdMob:
 * - [APP_OPEN]: Full-screen ads shown when the app is foregrounded or cold-started.
 * - [INTERSTITIAL]: Full-screen ads shown at natural transition points.
 * - [NATIVE]: Ads rendered inline within UI content using native components.
 */
enum class AdType {
    APP_OPEN,
    INTERSTITIAL,
    NATIVE
}
