package com.qrscanfast.app.navigation

import android.net.Uri

/**
 * Defines all navigation destinations for the QR Scan Max application.
 *
 * Routes are organized as a sealed hierarchy:
 * - [NavRoutes.Onboarding] 鈥?First-launch onboarding flow.
 * - [NavRoutes.Tab] 鈥?Main bottom-navigation tabs (History, Scan, Create).
 *
 * Each route exposes a [route] string used by Compose Navigation's NavHost.
 *
 * AI Continuity Note:
 * When adding new screens, create a new object inside [NavRoutes] and register
 * it in the appropriate NavHost graph (MainNavHost or a nested graph).
 */
sealed class NavRoutes(val route: String) {

    /** Onboarding graph entry point (shown on first launch only). */
    data object Onboarding : NavRoutes("onboarding")

    /** Main content graph entry point (tab-based navigation). */
    data object Main : NavRoutes("main")

    /** 鎵弿缁撴灉璇︽儏椤碉紝鍙傛暟涓哄巻鍙茶褰?ID 鎴栫洿鎺ヤ紶閫掑師濮嬪唴瀹?*/
    /** 扫描结果详情页 */
    data object ScanResult : NavRoutes("scan_result/{rawContent}/{format}/{contentType}") {
        fun createRoute(rawContent: String, format: String, contentType: String): String {
            return "scan_result/${Uri.encode(rawContent)}/${format}/${contentType}"
        }
    }

    /** 设置页 */
    data object Settings : NavRoutes("settings")

    /**
     * Bottom-navigation tab destinations.
     *
     * Each tab has its own back-stack managed by the NavHost inside [MainNavHost].
     */
    sealed class Tab(route: String) : NavRoutes(route) {
        /** History tab 鈥?displays scan/generation history in reverse-chronological order. */
        data object History : Tab("tab_history")

        /** Scan tab 鈥?camera-based QR/barcode scanner (default landing tab). */
        data object Scan : Tab("tab_scan")

        /** Create tab 鈥?QR code generator with multiple input types. */
        data object Create : Tab("tab_create")
    }
}
