package com.qrscanfast.app.navigation

import android.net.Uri

/**
 * 应用所有导航目的地定义。
 */
sealed class NavRoutes(val route: String) {

    data object Onboarding : NavRoutes("onboarding")
    data object Main : NavRoutes("main")

    /** 扫描结果详情页 */
    data object ScanResult : NavRoutes("scan_result/{rawContent}/{format}/{contentType}") {
        fun createRoute(rawContent: String, format: String, contentType: String): String {
            return "scan_result/${Uri.encode(rawContent)}/${format}/${contentType}"
        }
    }

    /** 订阅页（启动流程中展示） */
    data object Subscription : NavRoutes("subscription")

    /** 设置页 */
    data object Settings : NavRoutes("settings")

    /** 二维码创建页 */
    data object CreateQrCode : NavRoutes("create_qr_code")

    /** 条码创建页 */
    data object CreateBarcode : NavRoutes("create_barcode")

    /** 二维码美化页 */
    data object QrBeautify : NavRoutes("qr_beautify/{content}") {
        fun createRoute(content: String): String {
            return "qr_beautify/${Uri.encode(content)}"
        }
    }

    /** 底部 Tab 导航 */
    sealed class Tab(route: String) : NavRoutes(route) {
        data object History : Tab("tab_history")
        data object Scan : Tab("tab_scan")
        data object Create : Tab("tab_create")
    }
}
