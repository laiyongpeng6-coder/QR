/**
 * ## 给其他 AI 开发者的说明
 *
 * 本文件是应用的顶层导航宿主（Navigation Host），负责：
 * 1. 根据 OnboardingPreferences 的状态决定显示引导页还是主界面
 * 2. 主界面使用 Scaffold + 底部 Tab 导航 + NavHost 实现多 Tab 切换
 * 3. 扫描结果页作为非 Tab 路由，导航时隐藏底部栏
 *
 * ## 三态逻辑说明
 * - null = DataStore 尚未加载完成，显示空白过渡（避免闪烁）
 * - false = 用户未完成引导，显示 OnboardingScreen
 * - true = 用户已完成引导，显示主界面
 */
package com.qrscanmax.app.navigation

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
import com.qrscanmax.core.data.datastore.OnboardingPreferences
import com.qrscanmax.feature.generator.GeneratorScreen
import com.qrscanmax.feature.history.HistoryScreen
import com.qrscanmax.feature.onboarding.OnboardingScreen
import com.qrscanmax.feature.scanner.ScanResultScreen
import com.qrscanmax.feature.scanner.ScannerScreen

/**
 * 应用顶层导航宿主。
 */
@Composable
fun MainNavHost(
    onboardingPreferences: OnboardingPreferences
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
            MainTabScaffold()
        }
    }
}

/**
 * 主界面 Scaffold — 包含底部 Tab 导航和内容区域的 NavHost。
 * 扫描结果页导航时自动隐藏底部 Tab 栏。
 */
@Composable
private fun MainTabScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 判断是否显示底部导航栏（结果页时隐藏）
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
                        // 从历史记录点击也跳转到结果页
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
                    }
                )
            }
            // 创建 Tab
            composable(NavRoutes.Tab.Create.route) {
                GeneratorScreen()
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
