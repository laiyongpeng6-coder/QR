package com.qrscanfast.core.common

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics 埋点工具类。
 *
 * 统一管理所有用户行为事件的上报。
 * 在 Firebase Console → Analytics → Events 中查看数据。
 */
object AnalyticsHelper {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    // ==================== 扫描相关 ====================

    /** 用户完成一次扫描（相机实时扫描） */
    fun logScanComplete(contentType: String, barcodeFormat: String) {
        analytics.logEvent("scan_complete") {
            param("content_type", contentType)
            param("barcode_format", barcodeFormat)
        }
    }

    /** 用户从相册导入图片扫描 */
    fun logAlbumImport(success: Boolean) {
        analytics.logEvent("album_import") {
            param("success", if (success) "true" else "false")
        }
    }

    /** 用户使用手电筒 */
    fun logFlashToggle(enabled: Boolean) {
        analytics.logEvent("flash_toggle") {
            param("enabled", if (enabled) "true" else "false")
        }
    }

    // ==================== 二维码生成相关 ====================

    /** 用户生成二维码 */
    fun logQrCodeGenerate(inputType: String) {
        analytics.logEvent("qr_generate") {
            param("input_type", inputType)
        }
    }

    /** 用户生成条码 */
    fun logBarcodeGenerate(barcodeType: String) {
        analytics.logEvent("barcode_generate") {
            param("barcode_type", barcodeType)
        }
    }

    // ==================== 结果页操作 ====================

    /** 用户在结果页复制内容 */
    fun logResultCopy(contentType: String) {
        analytics.logEvent("result_copy") {
            param("content_type", contentType)
        }
    }

    /** 用户在结果页分享内容 */
    fun logResultShare(contentType: String) {
        analytics.logEvent("result_share") {
            param("content_type", contentType)
        }
    }

    /** 用户在结果页执行主操作（打开链接/拨打电话等） */
    fun logResultAction(action: String, contentType: String) {
        analytics.logEvent("result_action") {
            param("action", action)
            param("content_type", contentType)
        }
    }

    // ==================== 保存和分享 ====================

    /** 用户保存生成的码到相册 */
    fun logSaveToGallery(codeType: String) {
        analytics.logEvent("save_to_gallery") {
            param("code_type", codeType)
        }
    }

    /** 用户分享生成的码 */
    fun logShareCode(codeType: String) {
        analytics.logEvent("share_code") {
            param("code_type", codeType)
        }
    }

    // ==================== 美化相关 ====================

    /** 用户进入美化页面 */
    fun logBeautifyEnter() {
        analytics.logEvent("beautify_enter") {}
    }

    /** 用户保存美化后的二维码 */
    fun logBeautifySave() {
        analytics.logEvent("beautify_save") {}
    }

    // ==================== 设置相关 ====================

    /** 用户修改设置 */
    fun logSettingChange(settingName: String, enabled: Boolean) {
        analytics.logEvent("setting_change") {
            param("setting_name", settingName)
            param("enabled", if (enabled) "true" else "false")
        }
    }

    // ==================== 引导页 ====================

    /** 用户完成引导 */
    fun logOnboardingComplete(skipped: Boolean) {
        analytics.logEvent("onboarding_complete") {
            param("skipped", if (skipped) "true" else "false")
        }
    }
}
