package com.qrscanfast.feature.onboarding.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Onboarding 页面配置。
 *
 * ## AI 交接
 * - 职责：承载单页引导的标题、说明、主图标和视觉色彩。
 * - 当前状态：只服务 onboarding 页面，方便用同一套路由渲染三页。
 * - 依赖：`OnboardingScreen` 会直接消费这个模型。
 * - 安全修改范围：文案、图标、颜色、页序。
 * - 风险 / TODO：如果未来增加更多引导页，建议同步扩展页面内容和分页逻辑。
 */
data class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
    val accentColorSecondary: Color
)
