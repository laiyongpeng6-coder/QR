package com.qrscanfast.app.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qrscanfast.app.settings.SettingsScreen
import com.qrscanfast.core.data.datastore.AppSettings
import com.qrscanfast.core.data.datastore.OnboardingPreferences
import com.qrscanfast.feature.generator.BarcodeCreateScreen
import com.qrscanfast.feature.generator.GeneratorScreen
import com.qrscanfast.feature.generator.QrBeautifyScreen
import com.qrscanfast.feature.generator.QrCodeCreateScreen
import com.qrscanfast.feature.history.HistoryScreen
import com.qrscanfast.feature.onboarding.OnboardingScreen
import com.qrscanfast.feature.scanner.ScanResultScreen
import com.qrscanfast.feature.scanner.ScannerScreen

/**
 * 应用顶层导航宿主。
 *
 * 根据引导完成状态决定显示引导页还是主界面。
 */
@Composable
fun MainNavHost(
    onboardingPreferences: OnboardingPreferences,
    appSettings: AppSettings
) {
    val isOnboardingComplete by onboardingPreferences.isOnboardingComplete
        .collectAsState(initial = null)

    when (isOnboardingComplete) {
        null -> {
            Box(modifier = Modifier.fillMaxSize())
        }
        false -> {
            OnboardingScreen()
        }
        true -> {
            MainTabScaffold(appSettings = appSettings)
        }
    }
}

/**
 * 主界面 Scaffold — 包含底部 Tab 导航和内容区域的 NavHost。
 * 扫描结果页和设置页导航时自动隐藏底部 Tab 栏。
 */
@Composable
private fun MainTabScaffold(appSettings: AppSettings) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 仅在 Tab 页面显示底部导航栏
    val showBottomBar = currentRoute in listOf(
        NavRoutes.Tab.History.route,
        NavRoutes.Tab.Scan.route,
        NavRoutes.Tab.Create.route
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                TabNavigation(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Tab.Scan.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 历史记录 Tab
            composable(NavRoutes.Tab.History.route) {
                HistoryScreen(
                    onItemClick = { record ->
                        val route = NavRoutes.ScanResult.createRoute(
                            rawContent = record.rawContent,
                            format = "QR_CODE",
                            contentType = record.contentType.name
                        )
                        navController.navigate(route)
                    }
                )
            }
            // 扫描 Tab
            composable(NavRoutes.Tab.Scan.route) {
                ScannerScreen(
                    onResultDetected = { rawValue, format, contentType ->
                        val route = NavRoutes.ScanResult.createRoute(
                            rawContent = rawValue,
                            format = format.name,
                            contentType = contentType.name
                        )
                        navController.navigate(route)
                    },
                    onSettingsClick = {
                        navController.navigate(NavRoutes.Settings.route)
                    }
                )
            }
            // 创建 Tab（入口页）
            composable(NavRoutes.Tab.Create.route) {
                GeneratorScreen(
                    onCreateQrCode = {
                        navController.navigate(NavRoutes.CreateQrCode.route)
                    },
                    onCreateBarcode = {
                        navController.navigate(NavRoutes.CreateBarcode.route)
                    }
                )
            }
            // 二维码创建页
            composable(NavRoutes.CreateQrCode.route) {
                QrCodeCreateScreen(
                    onBack = { navController.popBackStack() },
                    onBeautify = { content ->
                        navController.navigate(NavRoutes.QrBeautify.createRoute(content))
                    }
                )
            }
            // 条码创建页
            composable(NavRoutes.CreateBarcode.route) {
                BarcodeCreateScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            // 二维码美化页
            composable(
                route = NavRoutes.QrBeautify.route,
                arguments = listOf(
                    navArgument("content") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val qrContent = Uri.decode(backStackEntry.arguments?.getString("content") ?: "")
                QrBeautifyScreen(
                    content = qrContent,
                    onBack = { navController.popBackStack() }
                )
            }
            // 设置页
            composable(NavRoutes.Settings.route) {
                SettingsScreen(
                    appSettings = appSettings,
                    onBack = { navController.popBackStack() }
                )
            }
            // 扫描结果详情页
            composable(
                route = NavRoutes.ScanResult.route,
                arguments = listOf(
                    navArgument("rawContent") { type = NavType.StringType },
                    navArgument("format") { type = NavType.StringType },
                    navArgument("contentType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val rawContent = Uri.decode(backStackEntry.arguments?.getString("rawContent") ?: "")
                val format = backStackEntry.arguments?.getString("format") ?: "QR_CODE"
                val contentType = backStackEntry.arguments?.getString("contentType") ?: "PLAIN_TEXT"

                ScanResultScreen(
                    rawContent = rawContent,
                    format = format,
                    contentType = contentType,
                    onBack = { navController.popBackStack() },
                    onContinueScan = {
                        navController.popBackStack(NavRoutes.Tab.Scan.route, inclusive = false)
                    }
                )
            }
        }
    }
}
