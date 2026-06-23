package com.qrscanfast.app.navigation

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qrscanfast.app.ads.NativeCardAd
import com.qrscanfast.app.settings.SettingsScreen
import com.qrscanfast.app.startup.StartupOrchestrator
import com.qrscanfast.app.startup.StartupState
import com.qrscanfast.core.ads.AdGatekeeper
import com.qrscanfast.core.data.datastore.AppSettings
import com.qrscanfast.core.domain.ads.AdManager
import com.qrscanfast.core.domain.model.AdPlacement
import com.qrscanfast.core.data.datastore.OnboardingPreferences
import com.qrscanfast.feature.generator.BarcodeCreateScreen
import com.qrscanfast.feature.generator.GeneratorScreen
import com.qrscanfast.feature.generator.QrBeautifyScreen
import com.qrscanfast.feature.generator.QrCodeCreateScreen
import com.qrscanfast.feature.history.HistoryScreen
import com.qrscanfast.feature.onboarding.OnboardingScreen
import com.qrscanfast.feature.scanner.ScanResultScreen
import com.qrscanfast.feature.scanner.ScannerScreen
import com.qrscanfast.feature.subscription.SubscriptionScreen
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/**
 * 应用顶层导航宿主。
 *
 * 根据引导完成状态决定显示引导页还是启动流程/主界面。
 * Onboarding 完成后，通过 StartupOrchestrator 编排启动流程：
 * - Loading：查询订阅状态
 * - ShowSubscription：展示订阅页
 * - ShowAppOpenAd：展示开屏广告
 * - NavigateToHome：进入主界面
 */
@Composable
fun MainNavHost(
    onboardingPreferences: OnboardingPreferences,
    appSettings: AppSettings,
    startupOrchestrator: StartupOrchestrator,
    activity: Activity,
    adGatekeeper: AdGatekeeper,
    adManager: AdManager,
    subscriptionRepository: com.qrscanfast.core.domain.repository.SubscriptionRepository
) {
    val isOnboardingComplete by onboardingPreferences.isOnboardingComplete
        .collectAsState(initial = null)

    when (isOnboardingComplete) {
        null -> {
            // 状态加载中，显示空白
            Box(modifier = Modifier.fillMaxSize())
        }
        false -> {
            OnboardingScreen(adManager = adManager)
        }
        true -> {
            StartupFlowHost(
                startupOrchestrator = startupOrchestrator,
                activity = activity,
                appSettings = appSettings,
                adGatekeeper = adGatekeeper,
                adManager = adManager,
                subscriptionRepository = subscriptionRepository
            )
        }
    }
}

/**
 * 启动流程宿主。
 *
 * 观察 StartupOrchestrator 的状态，根据当前状态展示：
 * - Loading / ShowAppOpenAd：全屏加载指示器（广告由 Orchestrator 内部展示）
 * - ShowSubscription：订阅页
 * - NavigateToHome：主界面 Tab Scaffold
 */
@Composable
private fun StartupFlowHost(
    startupOrchestrator: StartupOrchestrator,
    activity: Activity,
    appSettings: AppSettings,
    adGatekeeper: AdGatekeeper,
    adManager: AdManager,
    subscriptionRepository: com.qrscanfast.core.domain.repository.SubscriptionRepository
) {
    val startupState by startupOrchestrator.startupState.collectAsState()

    // 记录是否已触发 orchestrate，避免重复调用
    var hasStarted by remember { mutableStateOf(false) }

    // 启动编排流程
    LaunchedEffect(Unit) {
        if (!hasStarted) {
            hasStarted = true
            startupOrchestrator.orchestrate(activity)
        }
    }

    when (startupState) {
        StartupState.Loading -> {
            // 查询订阅状态中，展示居中加载指示器
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        StartupState.ShowSubscription -> {
            // 展示订阅页面
            SubscriptionScreen(
                onDismiss = {
                    startupOrchestrator.onSubscriptionDismissed()
                },
                onPurchaseSuccess = {
                    startupOrchestrator.onSubscriptionPurchased()
                }
            )
        }

        StartupState.ShowAppOpenAd -> {
            // 开屏广告由 StartupOrchestrator.orchestrate() 内部通过
            // adManager.showFullScreenAd() 展示（挂起等待广告完成）。
            // 广告完成后 orchestrator 自动推进到 NavigateToHome。
            // 此处仅展示加载指示器作为过渡。
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        StartupState.NavigateToHome -> {
            // 启动流程完成，展示主界面
            MainTabScaffold(activity = activity, appSettings = appSettings, adGatekeeper = adGatekeeper, adManager = adManager, subscriptionRepository = subscriptionRepository)
        }
    }
}

/**
 * 主界面 Scaffold — 包含底部 Tab 导航和内容区域的 NavHost。
 * 扫描结果页和设置页导航时自动隐藏底部 Tab 栏。
 */
@Composable
private fun MainTabScaffold(activity: Activity, appSettings: AppSettings, adGatekeeper: AdGatekeeper, adManager: AdManager, subscriptionRepository: com.qrscanfast.core.domain.repository.SubscriptionRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 仅在 Tab 页面显示底部导航栏
    val showBottomBar = currentRoute in listOf(
        NavRoutes.Tab.History.route,
        NavRoutes.Tab.Scan.route,
        NavRoutes.Tab.Create.route
    )

    // State for showing subscription screen overlay during ad gate flow.
    // The CompletableDeferred allows the suspend lambda to wait for the user's action.
    var subscriptionDeferred by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val showSubscriptionOverlay = subscriptionDeferred != null

    // Native ad state for home tab bar area
    val homeTabAdState = remember { adManager.getNativeAdState(AdPlacement.NATIVE_HOME_TAB) }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Column {
                    // Native ad card above bottom tab bar (≤ 80dp)
                    NativeCardAd(
                        adState = homeTabAdState,
                        maxHeight = 80
                    )

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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Tab.Scan.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 历史记录 Tab
            composable(NavRoutes.Tab.History.route) {
                val isPremiumState by subscriptionRepository.isPremium.collectAsState()
                HistoryScreen(
                    adManager = adManager,
                    onItemClick = { record ->
                        val route = NavRoutes.ScanResult.createRoute(
                            rawContent = record.rawContent,
                            format = "QR_CODE",
                            contentType = record.contentType.name
                        )
                        navController.navigate(route)
                    },
                    onSettingsClick = { navController.navigate(NavRoutes.Settings.route) },
                    onVipClick = { navController.navigate(NavRoutes.Subscription.route) },
                    isPremium = isPremiumState
                )
            }
            // 扫描 Tab
            composable(NavRoutes.Tab.Scan.route) {
                val isPremiumState by subscriptionRepository.isPremium.collectAsState()
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
                    },
                    onVipClick = {
                        navController.navigate(NavRoutes.Subscription.route)
                    },
                    isPremium = isPremiumState,
                    showSubscriptionScreen = {
                        // Show subscription overlay and suspend until user action completes
                        val deferred = CompletableDeferred<Boolean>()
                        subscriptionDeferred = deferred
                        deferred.await()
                    }
                )
            }
            // 创建 Tab（入口页）
            composable(NavRoutes.Tab.Create.route) {
                val isPremiumState by subscriptionRepository.isPremium.collectAsState()
                GeneratorScreen(
                    onCreateQrCode = {
                        navController.navigate(NavRoutes.CreateQrCode.route)
                    },
                    onCreateBarcode = {
                        navController.navigate(NavRoutes.CreateBarcode.route)
                    },
                    onSettingsClick = { navController.navigate(NavRoutes.Settings.route) },
                    onVipClick = { navController.navigate(NavRoutes.Subscription.route) },
                    isPremium = isPremiumState
                )
            }
            // 二维码创建页
            composable(NavRoutes.CreateQrCode.route) {
                val coroutineScope = rememberCoroutineScope()
                QrCodeCreateScreen(
                    onBack = { navController.popBackStack() },
                    onBeautify = { content ->
                        coroutineScope.launch {
                            // 在导航至 AI 美化页面前，展示插屏广告（Premium 用户直接跳过）
                            adGatekeeper.gate(
                                activity = activity,
                                placement = AdPlacement.INTERSTITIAL_AI_BEAUTIFY,
                                showSubscriptionScreen = {
                                    val deferred = CompletableDeferred<Boolean>()
                                    subscriptionDeferred = deferred
                                    deferred.await()
                                }
                            )
                            // 广告完成/失败后继续导航至美化页面
                            navController.navigate(NavRoutes.QrBeautify.createRoute(content))
                        }
                    },
                    onShowSubscriptionScreen = {
                        val deferred = CompletableDeferred<Boolean>()
                        subscriptionDeferred = deferred
                        deferred.await()
                    }
                )
            }
            // 条码创建页
            composable(NavRoutes.CreateBarcode.route) {
                BarcodeCreateScreen(
                    onBack = { navController.popBackStack() },
                    adGatekeeper = adGatekeeper,
                    onShowSubscriptionScreen = {
                        val deferred = CompletableDeferred<Boolean>()
                        subscriptionDeferred = deferred
                        deferred.await()
                    }
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
            // 订阅页（从主界面内部导航时使用，如设置页中的"升级会员"）
            composable(NavRoutes.Subscription.route) {
                SubscriptionScreen(
                    onDismiss = { navController.popBackStack() },
                    onPurchaseSuccess = { navController.popBackStack() }
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
                    adManager = adManager,
                    onBack = { navController.popBackStack() },
                    onContinueScan = {
                        navController.popBackStack(NavRoutes.Tab.Scan.route, inclusive = false)
                    }
                )
            }
        }
    }

    // Subscription screen overlay — shown during ad gate flow
    AnimatedVisibility(
        visible = showSubscriptionOverlay,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        SubscriptionScreen(
            onDismiss = {
                // User dismissed subscription screen without purchasing
                subscriptionDeferred?.complete(false)
                subscriptionDeferred = null
            },
            onPurchaseSuccess = {
                // User purchased a subscription
                subscriptionDeferred?.complete(true)
                subscriptionDeferred = null
            }
        )
    }
}
