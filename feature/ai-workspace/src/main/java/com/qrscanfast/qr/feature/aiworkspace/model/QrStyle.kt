package com.qrscanfast.qr.feature.aiworkspace.model

import android.graphics.Color

/**
 * QR 码视觉样式配置数据类。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本数据类定义了 QR 码美化的所有可调参数。
 * AiWorkspaceViewModel 持有一个 QrStyle 实例，每次用户修改样式时
 * 创建新的 copy 并触发 QR 码重新渲染。
 *
 * ## 当前支持的免费样式
 * - 前景色、背景色、点形状、圆角半径
 *
 * ## 后续开发（需要订阅）
 * - 渐变色填充、AI 艺术模板、中心 Logo 嵌入
 */
data class QrStyle(
    val foregroundColor: Int = Color.BLACK,
    val backgroundColor: Int = Color.WHITE,
    val dotShape: DotShape = DotShape.SQUARE,
    val cornerRadius: Float = 0f
    // TODO [FUTURE-MONETIZATION]: 添加 gradientColors, aiTemplateId, centerLogoUri
)

/**
 * QR 码点形状枚举。
 */
enum class DotShape {
    SQUARE,
    CIRCLE,
    ROUNDED
}
