package com.qrscanfast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qrscanfast.app.navigation.MainNavHost
import com.qrscanfast.core.data.datastore.AppSettings
import com.qrscanfast.core.data.datastore.OnboardingPreferences
import com.qrscanfast.core.ui.theme.FastQrScanTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 应用主 Activity。
 *
 * 使用 Hilt 注入依赖，承载 Compose 根内容。
 * 所有界面逻辑在 Composable 和 ViewModel 中实现。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
