package com.qrscanfast.qr.feature.scanner.camera

import javax.inject.Inject

/**
 * 自动缩放控制器 — 根据条码在画面中的占比自动调整相机焦距。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 这是一个纯逻辑类，不依赖 Android 框架，方便单元测试。
 * 在 ScannerViewModel 或 CameraManager 中调用 [calculateZoomRatio] 方法，
 * 传入 ML Kit 返回的条码边界框面积与帧面积的比值即可。
 *
 * ## 缩放规则（来自 PRD）
 * - 条码占比 < 5%  → 4x 缩放（条码太远太小）
 * - 条码占比 < 15% → 2x 缩放（条码较远）
 * - 条码占比 >= 15% → 1x 不缩放（条码足够大，可正常识别）
 *
 * ## 使用示例
 * ```kotlin
 * val ratio = boundingBox.area() / frameArea.toFloat()
 * val zoom = autoZoomController.calculateZoomRatio(ratio)
 * cameraManager.setZoomRatio(zoom)
 * ```
 */
class AutoZoomController @Inject constructor() {

    companion object {
        /** 极小条码阈值：占比低于此值时使用最大缩放 */
        private const val VERY_SMALL_THRESHOLD = 0.05f

        /** 较小条码阈值：占比低于此值时使用中等缩放 */
        private const val SMALL_THRESHOLD = 0.15f

        /** 极小条码时的缩放倍数 */
        private const val MAX_ZOOM = 4.0f

        /** 较小条码时的缩放倍数 */
        private const val MEDIUM_ZOOM = 2.0f

        /** 正常大小时不缩放 */
        private const val NO_ZOOM = 1.0f
    }

    /**
     * 根据条码边界框占帧面积的比例计算推荐的缩放倍数。
     *
     * @param boundingBoxAreaRatio 条码边界框面积 / 帧总面积，范围 [0, 1]
     * @return 推荐的缩放倍数（1x / 2x / 4x）
     */
    fun calculateZoomRatio(boundingBoxAreaRatio: Float): Float {
        return when {
            boundingBoxAreaRatio < VERY_SMALL_THRESHOLD -> MAX_ZOOM
            boundingBoxAreaRatio < SMALL_THRESHOLD -> MEDIUM_ZOOM
            else -> NO_ZOOM
        }
    }
}
