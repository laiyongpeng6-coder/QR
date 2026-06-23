package com.qrscanfast.core.ads

import com.qrscanfast.core.domain.model.AdPlacement

/**
 * Provides ad unit IDs for each [AdPlacement].
 *
 * The domain layer cannot access BuildConfig directly (no app module dependency),
 * so this interface allows the app module to inject the real ad unit IDs
 * configured via BuildConfig into the :core:ads module at runtime.
 *
 * Implementation is provided by the app module and bound via Hilt.
 */
interface AdUnitIdProvider {

    /**
     * Returns the ad unit ID string for the given [placement].
     *
     * In debug builds this returns Google's official test ad unit IDs.
     * In release builds this returns real AdMob ad unit IDs.
     */
    fun getAdUnitId(placement: AdPlacement): String
}
