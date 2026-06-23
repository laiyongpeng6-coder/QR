package com.qrscanfast.core.ads

/**
 * A fake [Clock] for testing that returns a controllable timestamp.
 */
class FakeClock(var currentTime: Long = 0L) : Clock {
    override fun currentTimeMillis(): Long = currentTime
}
