package com.qrscanfast.qr.feature.onboarding.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Represents a single page in the onboarding flow.
 *
 * @property titleRes String resource ID for the page title.
 * @property descriptionRes String resource ID for the page description.
 * @property iconRes Drawable resource ID for the page illustration/icon.
 */
data class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int
)
