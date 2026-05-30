package com.qrscanfast.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Manages onboarding completion state using Jetpack DataStore.
 *
 * Persists whether the user has completed (or skipped) the onboarding flow,
 * so it is not shown again on subsequent app launches.
 *
 * @param dataStore The DataStore<Preferences> instance provided by Hilt.
 */
class OnboardingPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * Emits `true` when the user has completed or skipped onboarding,
     * `false` otherwise (first launch default).
     */
    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETE] ?: false
    }

    /**
     * Marks onboarding as complete. Once called, [isOnboardingComplete] will emit `true`
     * and the onboarding flow will not be shown on future launches.
     */
    suspend fun setOnboardingComplete() {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETE] = true
        }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }
}
