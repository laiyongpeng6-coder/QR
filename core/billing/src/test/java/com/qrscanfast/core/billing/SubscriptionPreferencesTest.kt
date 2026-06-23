package com.qrscanfast.core.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionPreferencesTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @TempDir
    lateinit var tempDir: File

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: SubscriptionPreferences

    @BeforeEach
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempDir, "test_preferences.preferences_pb") }
        )
        preferences = SubscriptionPreferences(dataStore)
    }

    @Test
    fun `isPremium defaults to false`() = testScope.runTest {
        preferences.isPremium.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `activePlan defaults to null`() = testScope.runTest {
        preferences.activePlan.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expiryTimeMs defaults to null`() = testScope.runTest {
        preferences.expiryTimeMs.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateStatus sets premium state correctly`() = testScope.runTest {
        val expiryMs = 1700000000000L

        preferences.updateStatus(isPremium = true, plan = "WEEKLY", expiryMs = expiryMs)

        preferences.isPremium.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        preferences.activePlan.test {
            assertEquals("WEEKLY", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        preferences.expiryTimeMs.test {
            assertEquals(expiryMs, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateStatus clears plan and expiry when null`() = testScope.runTest {
        // First set premium state
        preferences.updateStatus(isPremium = true, plan = "ANNUAL", expiryMs = 1700000000000L)

        // Then downgrade to free
        preferences.updateStatus(isPremium = false, plan = null, expiryMs = null)

        preferences.isPremium.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        preferences.activePlan.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        preferences.expiryTimeMs.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateStatus with LIFETIME plan and no expiry`() = testScope.runTest {
        preferences.updateStatus(isPremium = true, plan = "LIFETIME", expiryMs = null)

        preferences.isPremium.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        preferences.activePlan.test {
            assertEquals("LIFETIME", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        preferences.expiryTimeMs.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateStatus overwrites previous values`() = testScope.runTest {
        preferences.updateStatus(isPremium = true, plan = "WEEKLY", expiryMs = 1000L)
        preferences.updateStatus(isPremium = true, plan = "ANNUAL", expiryMs = 2000L)

        preferences.activePlan.test {
            assertEquals("ANNUAL", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        preferences.expiryTimeMs.test {
            assertEquals(2000L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
