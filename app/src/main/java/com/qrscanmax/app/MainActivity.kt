package com.qrscanmax.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qrscanmax.app.navigation.MainNavHost
import com.qrscanmax.core.data.datastore.OnboardingPreferences
import com.qrscanmax.core.ui.theme.QRScanMaxTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single Activity host for the QR Scan Max application.
 *
 * Uses Hilt for dependency injection and hosts the root Compose content
 * wrapped in [QRScanMaxTheme]. Navigation is delegated to [MainNavHost]
 * which handles onboarding checks and tab-based navigation.
 *
 * AI Continuity Note:
 * This activity should remain minimal. All screen logic lives in composables
 * and ViewModels. Edge-to-edge display is enabled for modern system bar handling.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var onboardingPreferences: OnboardingPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRScanMaxTheme {
                MainNavHost(onboardingPreferences = onboardingPreferences)
            }
        }
    }
}
