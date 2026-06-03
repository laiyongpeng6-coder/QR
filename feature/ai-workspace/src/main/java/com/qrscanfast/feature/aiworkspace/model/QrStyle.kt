package com.qrscanfast.feature.aiworkspace.model

import android.graphics.Color

/**
 * QR 样式配置模型。
 *
 * ## AI 交接
 * - 职责：集中描述二维码美化参数，供 UI 与编码层共享。
 * - 当前状态：覆盖基础前景色、背景色、点形状与圆角。
 * - 依赖：被 `AiWorkspaceViewModel` 持有并复制更新。
 * - 安全修改范围：新增样式字段、默认值、注释说明。
 * - 风险 / TODO：渐变、AI 模板和中心 Logo 未来需考虑订阅门控。
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
