package com.qrscanfast.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.qrscanfast.app.navigation.MainNavHost
import com.qrscanfast.core.data.datastore.AppSettings
import com.qrscanfast.core.data.datastore.OnboardingPreferences
import com.qrscanfast.core.ui.theme.FastQrScanTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 应用主 Activity。
 *
 * 继承 AppCompatActivity 以支持 per-app 语言切换（AppCompatDelegate.setApplicationLocales）。
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var onboardingPreferences: OnboardingPreferences

    @Inject
    lateinit var appSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FastQrScanTheme {
                MainNavHost(
                    onboardingPreferences = onboardingPreferences,
                    appSettings = appSettings
                )
            }
        }
    }
}
