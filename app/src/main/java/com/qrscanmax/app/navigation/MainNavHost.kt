/**
 * ## 给其他 AI 开发者的说明
 *
 * 本文件是应用的顶层导航宿主（Navigation Host），负责：
 * 1. 根据 OnboardingPreferences 的状态决定显示引导页还是主界面
 * 2. 主界面使用 Scaffold + 底部 Tab 导航 + NavHost 实现多 Tab 切换
 * 3. 每个 Tab 对应一个真实的 Feature Screen（HistoryScreen、ScannerScreen、GeneratorScreen）
 *
 * ## 三态逻辑说明
 * - null = DataStore 尚未加载完成，显示空白过渡（避免闪烁）
 * - false = 用户未完成引导，显示 OnboardingScreen
 * - true = 用户已完成引导，显示主界面
 *
 * ## 后续开发
 * - 如需添加新 Tab，在 NavRoutes.Tab 中添加路由，在 TabNavigation 中添加 TabItem，
 *   然后在 MainTabScaffold 的 NavHost 中注册对应的 composable。
 * - 如需添加详情页（如 ScanResultScreen），可在 NavHost 中添加非 Tab 路由。
 */
package com.qrscanmax.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qrscanmax.core.data.datastore.OnboardingPreferences
import com.qrscanmax.feature.generator.GeneratorScreen
import com.qrscanmax.feature.history.HistoryScreen
import com.qrscanmax.feature.onboarding.OnboardingScreen
import com.qrscanmax.feature.scanner.ScannerScreen

/**
 * 应用顶层导航宿主。
 *
 * 实现条件导航逻辑：
 * 1. 首次启动（引导未完成）→ 显示引导页
 * 2. 后续启动 → 显示带底部 Tab 的主界面
 *
 * @param onboardingPreferences DataStore 支持的偏好设置，用于检查引导完成状态
 */
@Composable
fun MainNavHost(
    onboardingPreferences: OnboardingPreferences
) {
    // 使用 null 作为初始值，表示"尚未确定"状态，避免闪烁
    val isOnboardingComplete by onboardingPreferences.isOnboardingComplete
        .collectAsState(initial = null)

    when (isOnboardingComplete) {
        null -> {
            // DataStore 尚未加载完成，显示空白过渡（避免闪烁）
            Box(modifier = Modifier.fillMaxSize())
        }
        false -> {
            // 首次启动，显示引导页
            OnboardingScreen()
        }
        true -> {
            // 已完成引导，显示主界面
            MainTabScaffold()
        }
    }
}

/**
 * 主界面 Scaffold — 包含底部 Tab 导航和内容区域的 NavHost。
 *
 * 使用 [rememberNavController] 管理导航状态，通过 saveState/restoreState
 * 独立保存每个 Tab 的返回栈。
 */
@Composable
private fun MainTabScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            TabNavigation(
                currentRoute = currentRoute,
                onTabSelected = { route ->
                    navController.navigate(route) {
                        // 弹出到起始目的地，避免切换 Tab 时堆积大量返回栈
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // 避免同一目的地的多个副本
                        launchSingleTop = true
                        // 重新选择之前选过的 Tab 时恢复状态
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Tab.Scan.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 历史记录 Tab — 显示扫描/生成历史的时间线列表
            composable(NavRoutes.Tab.History.route) {
                HistoryScreen()
            }
            // 扫描 Tab — 全屏相机预览 + 实时条码检测
            composable(NavRoutes.Tab.Scan.route) {
                ScannerScreen()
            }
            // 创建 Tab — QR 码生成器（输入 + 预览）
            composable(NavRoutes.Tab.Create.route) {
                GeneratorScreen()
            }
        }
    }
}
