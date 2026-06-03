package com.qrscanfast.feature.scanner.camera

import javax.inject.Inject

/**
 * 自动缩放控制器。
 *
 * ## AI 交接
 * - 职责：根据条码面积占比返回推荐缩放倍率。
 * - 当前状态：纯逻辑类，适合单测。
 * - 依赖：无 Android 框架依赖。
 * - 安全修改范围：阈值、倍率、判定规则。
 * - 风险 / TODO：调整阈值时要同步相机联动与测试预期。
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
