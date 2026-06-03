package com.qrscanfast.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanfast.core.data.datastore.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Onboarding 页面状态与完成逻辑。
 *
 * ## AI 交接
 * - 职责：只负责持久化新手流程完成状态。
 * - 当前状态：逻辑极简，主要承接页面完成与跳过。
 * - 依赖：`OnboardingPreferences`。
 * - 安全修改范围：完成状态写入、跳过行为、未来首启逻辑。
 * - 风险 / TODO：如果未来要区分“跳过”和“完成”，建议补独立标记。
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    /**
     * 标记 onboarding 已完成。
     *
     * ## AI 交接
     * - 职责：写入持久化状态并触发外层导航切换。
     * - 当前状态：无额外副作用。
     * - 风险 / TODO：如果未来要区分“跳过”和“完成”，建议补独立标记。
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingPreferences.setOnboardingComplete()
        }
    }
}
