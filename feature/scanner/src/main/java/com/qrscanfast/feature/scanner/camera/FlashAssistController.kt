package com.qrscanfast.feature.scanner.camera

import javax.inject.Inject

/**
 * 闪光灯辅助控制器。
 *
 * ## AI 交接
 * - 职责：根据环境亮度返回是否建议开启闪光灯。
 * - 当前状态：纯逻辑类，带迟滞避免频繁闪烁。
 * - 依赖：无 Android 框架依赖。
 * - 安全修改范围：亮度阈值、迟滞规则、重置逻辑。
 * - 风险 / TODO：阈值变更要同步相机交互和测试。
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
