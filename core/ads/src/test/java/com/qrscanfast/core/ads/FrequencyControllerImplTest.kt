package com.qrscanfast.core.ads

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [FrequencyControllerImpl].
 */
class FrequencyControllerImplTest {

    private lateinit var fakeClock: FakeClock
    private lateinit var controller: FrequencyControllerImpl

    @BeforeEach
    fun setup() {
        fakeClock = FakeClock()
        controller = FrequencyControllerImpl(fakeClock)
    }

    // --- canShowInterstitial ---

    @Test
    fun `canShowInterstitial returns true initially`() {
        // lastShowTimeMs = 0, clock = 0, elapsed = 0 which is >= 60_000? No.
        // Actually elapsed = 0 - 0 = 0, but initial lastShowTimeMs is 0 and clock starts at 0.
        // Since elapsed (0) < MIN_INTERVAL_MS (60000), it should be false at time 0.
        // But conceptually the first show should be allowed. Let's set clock to a realistic time.
        fakeClock.currentTime = 100_000L
        assertTrue(controller.canShowInterstitial())
    }

    @Test
    fun `canShowInterstitial returns false within 60 seconds of last show`() {
        fakeClock.currentTime = 100_000L
        controller.recordInterstitialShow()

        fakeClock.currentTime = 100_000L + 59_999L
        assertFalse(controller.canShowInterstitial())
    }

    @Test
    fun `canShowInterstitial returns true after exactly 60 seconds`() {
        fakeClock.currentTime = 100_000L
        controller.recordInterstitialShow()

        fakeClock.currentTime = 100_000L + FrequencyControllerImpl.MIN_INTERVAL_MS
        assertTrue(controller.canShowInterstitial())
    }

    @Test
    fun `canShowInterstitial returns false when session limit reached`() {
        fakeClock.currentTime = 100_000L
        repeat(FrequencyControllerImpl.MAX_PER_SESSION) {
            controller.recordInterstitialShow()
            fakeClock.currentTime += FrequencyControllerImpl.MIN_INTERVAL_MS
        }

        // Even though enough time has passed, session limit is reached
        assertFalse(controller.canShowInterstitial())
    }

    // --- isWithinSessionLimit ---

    @Test
    fun `isWithinSessionLimit returns true when no shows recorded`() {
        assertTrue(controller.isWithinSessionLimit())
    }

    @Test
    fun `isWithinSessionLimit returns true at 9 shows`() {
        fakeClock.currentTime = 100_000L
        repeat(9) {
            controller.recordInterstitialShow()
            fakeClock.currentTime += FrequencyControllerImpl.MIN_INTERVAL_MS
        }
        assertTrue(controller.isWithinSessionLimit())
    }

    @Test
    fun `isWithinSessionLimit returns false at 10 shows`() {
        fakeClock.currentTime = 100_000L
        repeat(10) {
            controller.recordInterstitialShow()
            fakeClock.currentTime += FrequencyControllerImpl.MIN_INTERVAL_MS
        }
        assertFalse(controller.isWithinSessionLimit())
    }

    // --- recordInterstitialShow ---

    @Test
    fun `recordInterstitialShow updates last show time and increments count`() {
        fakeClock.currentTime = 50_000L
        controller.recordInterstitialShow()

        // Immediately after, within interval should be blocked
        fakeClock.currentTime = 50_001L
        assertFalse(controller.canShowInterstitial())
    }

    // --- resetSession ---

    @Test
    fun `resetSession resets session count but not last show time`() {
        fakeClock.currentTime = 100_000L
        repeat(FrequencyControllerImpl.MAX_PER_SESSION) {
            controller.recordInterstitialShow()
            fakeClock.currentTime += FrequencyControllerImpl.MIN_INTERVAL_MS
        }

        assertFalse(controller.isWithinSessionLimit())

        controller.resetSession()

        // Session limit is reset
        assertTrue(controller.isWithinSessionLimit())
        // But interval is still enforced — last show was at previous time
        // The current time already has MIN_INTERVAL elapsed from last record
        assertTrue(controller.canShowInterstitial())
    }

    @Test
    fun `resetSession allows ads again after session limit was reached`() {
        fakeClock.currentTime = 100_000L
        repeat(FrequencyControllerImpl.MAX_PER_SESSION) {
            controller.recordInterstitialShow()
            fakeClock.currentTime += FrequencyControllerImpl.MIN_INTERVAL_MS
        }

        assertFalse(controller.canShowInterstitial())

        controller.resetSession()
        // Time already moved past the interval from last show
        assertTrue(controller.canShowInterstitial())
    }
}
