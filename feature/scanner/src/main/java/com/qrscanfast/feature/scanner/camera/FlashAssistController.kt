package com.qrscanfast.feature.scanner.camera

import javax.inject.Inject

/**
 * 闪光灯辅助控制器 — 根据环境光照度自动控制手电筒开关。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 这是一个纯逻辑类，不依赖 Android 框架，方便单元测试。
 * 在 CameraX 的 ImageAnalysis 回调中获取帧的平均亮度后，
 * 调用 [shouldEnableFlash] 判断是否需要开启手电筒。
 *
 * ## 迟滞逻辑（Hysteresis）
 * 为了防止在阈值附近频繁开关手电筒（闪烁），采用双阈值设计：
 * - 开启阈值：亮度 < 40（暗环境，需要补光）
 * - 关闭阈值：亮度 > 60（已经足够亮，可以关闭）
 * - 在 40~60 之间时保持当前状态不变
 *
 * ## 使用示例
 * ```kotlin
 * // 在 ImageAnalysis.Analyzer 的 analyze() 方法中：
 * val luminosity = calculateAverageLuminosity(image)
 * val shouldFlash = flashAssistController.shouldEnableFlash(luminosity)
 * cameraManager.enableTorch(shouldFlash)
 * ```
 */
class FlashAssistController @Inject constructor() {

    companion object {
        /** 开启手电筒的亮度阈值（低于此值时开启） */
        private const val ENABLE_THRESHOLD = 40

        /** 关闭手电筒的亮度阈值（高于此值时关闭） */
        private const val DISABLE_THRESHOLD = 60
    }

    /** 当前手电筒状态，用于迟滞逻辑 */
    private var isFlashEnabled = false

    /**
     * 根据当前环境光照度判断是否应该开启手电筒。
     *
     * 使用迟滞逻辑避免在阈值附近频繁切换：
     * - 亮度 < [ENABLE_THRESHOLD] → 开启
     * - 亮度 > [DISABLE_THRESHOLD] → 关闭
     * - 介于两者之间 → 保持当前状态
     *
     * @param luminosity 环境光照度值，范围 [0, 255]，0 为全黑，255 为最亮
     * @return true 表示应该开启手电筒，false 表示应该关闭
     */
    fun shouldEnableFlash(luminosity: Int): Boolean {
        isFlashEnabled = when {
            luminosity < ENABLE_THRESHOLD -> true
            luminosity > DISABLE_THRESHOLD -> false
            else -> isFlashEnabled // 迟滞区间，保持当前状态
        }
        return isFlashEnabled
    }

    /**
     * 重置控制器状态。
     * 在相机解绑或页面离开时调用，确保下次进入时从初始状态开始。
     */
    fun reset() {
        isFlashEnabled = false
    }
}
