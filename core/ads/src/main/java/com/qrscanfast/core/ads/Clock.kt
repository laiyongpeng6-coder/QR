package com.qrscanfast.core.ads

/**
 * Abstraction over system time to enable deterministic testing.
 *
 * Production code uses [SystemClock]; tests inject a fake implementation
 * that returns controlled timestamps.
 */
interface Clock {
    /**
     * Returns the current time in milliseconds since epoch.
     */
    fun currentTimeMillis(): Long
}
