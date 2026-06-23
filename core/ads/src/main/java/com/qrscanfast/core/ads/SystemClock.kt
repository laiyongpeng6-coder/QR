package com.qrscanfast.core.ads

import javax.inject.Inject

/**
 * Default [Clock] implementation that delegates to [System.currentTimeMillis].
 */
class SystemClock @Inject constructor() : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
