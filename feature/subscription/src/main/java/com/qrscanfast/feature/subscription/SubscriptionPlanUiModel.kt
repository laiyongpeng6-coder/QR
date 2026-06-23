package com.qrscanfast.feature.subscription

import com.qrscanfast.core.domain.model.SubscriptionPlan

/**
 * UI representation of a subscription plan displayed in the SubscriptionScreen.
 *
 * @property plan The underlying domain subscription plan.
 * @property title Human-readable plan name (e.g., "3天免费试用", "周订阅").
 * @property price Formatted price string (e.g., "$6.99/周").
 * @property description Benefits description for the plan.
 * @property isRecommended Whether this plan should be highlighted as recommended.
 * @property badgeText Optional badge text (e.g., "最受欢迎", "3天免费").
 */
data class SubscriptionPlanUiModel(
    val plan: SubscriptionPlan,
    val title: String,
    val price: String,
    val description: String,
    val isRecommended: Boolean = false,
    val badgeText: String? = null
)
