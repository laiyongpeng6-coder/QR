package com.qrscanfast.feature.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanfast.core.domain.model.PurchaseResult
import com.qrscanfast.core.domain.model.SubscriptionPlan
import com.qrscanfast.core.domain.model.SubscriptionState
import com.qrscanfast.core.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the subscription screen.
 *
 * Manages the list of available subscription plans, the currently selected plan,
 * and the purchase flow state. Communicates with [SubscriptionRepository] to
 * execute purchases and restore previous subscriptions.
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _plans = MutableStateFlow(createDefaultPlans())
    val plans: StateFlow<List<SubscriptionPlanUiModel>> = _plans.asStateFlow()

    private val _selectedPlan = MutableStateFlow<SubscriptionPlan?>(null)
    val selectedPlan: StateFlow<SubscriptionPlan?> = _selectedPlan.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    val purchaseState: StateFlow<PurchaseUiState> = _purchaseState.asStateFlow()

    /**
     * Selects a subscription plan for purchase.
     */
    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    /**
     * Confirms and launches the purchase flow for the currently selected plan.
     *
     * @param activity The activity context required by Google Play Billing.
     */
    fun confirmPurchase(activity: Activity) {
        val plan = _selectedPlan.value ?: return
        _purchaseState.value = PurchaseUiState.Loading

        viewModelScope.launch {
            val result = subscriptionRepository.launchPurchaseFlow(activity, plan)
            result.fold(
                onSuccess = { purchaseResult ->
                    _purchaseState.value = when (purchaseResult) {
                        is PurchaseResult.Success -> PurchaseUiState.Success
                        is PurchaseResult.Cancelled -> PurchaseUiState.Cancelled
                        is PurchaseResult.Error -> PurchaseUiState.Error(purchaseResult.message)
                    }
                },
                onFailure = { throwable ->
                    _purchaseState.value = PurchaseUiState.Error(
                        throwable.message ?: "Purchase failed"
                    )
                }
            )
        }
    }

    /**
     * Restores previously purchased subscriptions.
     */
    fun restorePurchases() {
        _purchaseState.value = PurchaseUiState.Loading

        viewModelScope.launch {
            val result = subscriptionRepository.restorePurchases()
            result.fold(
                onSuccess = { state ->
                    _purchaseState.value = when (state) {
                        is SubscriptionState.Premium -> PurchaseUiState.RestoreSuccess
                        else -> PurchaseUiState.RestoreEmpty
                    }
                },
                onFailure = { throwable ->
                    _purchaseState.value = PurchaseUiState.Error(
                        throwable.message ?: "Restore failed"
                    )
                }
            )
        }
    }

    /**
     * Resets the purchase state back to idle.
     * Called after the UI has shown the feedback to the user.
     */
    fun resetPurchaseState() {
        _purchaseState.value = PurchaseUiState.Idle
    }

    private fun createDefaultPlans(): List<SubscriptionPlanUiModel> = listOf(
        SubscriptionPlanUiModel(
            plan = SubscriptionPlan.TRIAL,
            title = "免费试用",
            price = "3天免费，之后 $6.99/周",
            description = "体验全部 Premium 功能",
            isRecommended = false,
            badgeText = "3天免费"
        ),
        SubscriptionPlanUiModel(
            plan = SubscriptionPlan.WEEKLY,
            title = "周订阅",
            price = "$6.99/周",
            description = "按周付费，随时取消",
            isRecommended = false,
            badgeText = null
        ),
        SubscriptionPlanUiModel(
            plan = SubscriptionPlan.ANNUAL,
            title = "年订阅",
            price = "$16.99/年",
            description = "最划算的方案，年付仅需 $16.99",
            isRecommended = true,
            badgeText = "最受欢迎"
        ),
        SubscriptionPlanUiModel(
            plan = SubscriptionPlan.LIFETIME,
            title = "终身会员",
            price = "$19.99 一次性",
            description = "一次购买，永久使用",
            isRecommended = false,
            badgeText = "买断"
        )
    )
}
