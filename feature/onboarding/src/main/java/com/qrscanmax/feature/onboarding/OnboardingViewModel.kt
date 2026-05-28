package com.qrscanmax.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanmax.core.data.datastore.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Onboarding screen.
 *
 * Manages the logic for completing or skipping the onboarding flow
 * and persisting that state using [OnboardingPreferences].
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    /**
     * Marks the onboarding as complete in persistent storage.
     * This will trigger the navigation logic in MainNavHost to move to the main app.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingPreferences.setOnboardingComplete()
        }
    }
}
