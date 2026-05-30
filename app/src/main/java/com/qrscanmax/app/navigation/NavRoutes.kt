package com.qrscanmax.app.navigation

import android.net.Uri

/**
 * Defines all navigation destinations for the QR Scan Max application.
 *
 * Routes are organized as a sealed hierarchy:
 * - [NavRoutes.Onboarding] — First-launch onboarding flow.
 * - [NavRoutes.Tab] — Main bottom-navigation tabs (History, Scan, Create).
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

    /** 扫描结果详情页，参数为历史记录 ID 或直接传递原始内容 */
    data object ScanResult : NavRoutes("scan_result/{rawContent}/{format}/{contentType}") {
        fun createRoute(rawContent: String, format: String, contentType: String): String {
            return "scan_result/${Uri.encode(rawContent)}/${format}/${contentType}"
        }
    }

    /**
     * Bottom-navigation tab destinations.
     *
     * Each tab has its own back-stack managed by the NavHost inside [MainNavHost].
     */
    sealed class Tab(route: String) : NavRoutes(route) {
        /** History tab — displays scan/generation history in reverse-chronological order. */
        data object History : Tab("tab_history")

        /** Scan tab — camera-based QR/barcode scanner (default landing tab). */
        data object Scan : Tab("tab_scan")

        /** Create tab — QR code generator with multiple input types. */
        data object Create : Tab("tab_create")
    }
}
