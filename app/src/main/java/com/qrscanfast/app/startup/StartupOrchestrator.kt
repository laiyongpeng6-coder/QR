package com.qrscanfast.app.startup

import android.app.Activity
import com.qrscanfast.core.common.RemoteConfigManager
import com.qrscanfast.core.domain.ads.AdManager
import com.qrscanfast.core.domain.model.AdPlacement
import com.qrscanfast.core.domain.model.SubscriptionState
import com.qrscanfast.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Orchestrates the cold-start flow of the application.
 *
 * The flow is state-driven:
 * 1. [StartupState.Loading] — query subscription status
 * 2. If Premium → [StartupState.NavigateToHome]
 * 3. If Free → [StartupState.ShowSubscription]
 *    - User purchases → [StartupState.NavigateToHome]
 *    - User dismisses → [StartupState.ShowAppOpenAd]
 * 4. After ad completes/fails → [StartupState.NavigateToHome]
 *
 * UI layer observes [startupState] and calls the appropriate event methods
 * ([onSubscriptionDismissed], [onSubscriptionPurchased], [onAppOpenAdCompleted])
 * to drive state transitions.
 */
@Singleton
class StartupOrchestrator @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val adManager: AdManager,
    private val remoteConfig: RemoteConfigManager
) {
    private val _startupState = MutableStateFlow<StartupState>(StartupState.Loading)

    /** Observable startup state for UI to react to. */
    val startupState: StateFlow<StartupState> = _startupState.asStateFlow()

    // Continuation for suspending until subscription screen result
    private var subscriptionContinuation: ((SubscriptionScreenResult) -> Unit)? = null

    /** 标记是否已完成启动流程，Activity 重建时跳过重复编排 */
    private var hasCompleted = false

    /**
     * Runs the full startup orchestration flow.
     *
     * This is a suspending function that progresses through startup states,
     * pausing at each state that requires external input (subscription screen result,
     * ad completion) until the UI layer signals via event methods.
     *
     * @param activity The activity context needed for showing ads.
     */
    suspend fun orchestrate(activity: Activity) {
        // 如果启动流程已完成（如 Activity 因语言切换重建），直接进入首页
        if (hasCompleted) {
            _startupState.value = StartupState.NavigateToHome
            return
        }

        // Step 1: Loading — 等待 Remote Config 拉取完成，再查询订阅状态
        _startupState.value = StartupState.Loading

        // 等待远程参数就绪（最多 5 秒，超时则用默认值）
        remoteConfig.awaitReady()

        val queryResult = subscriptionRepository.queryPurchases()
        val currentState = queryResult.getOrElse { SubscriptionState.Free }

        // Step 2: Check if Premium
        if (currentState is SubscriptionState.Premium) {
            hasCompleted = true
            _startupState.value = StartupState.NavigateToHome
            return
        }

        // Step 3: Free user — 根据 Remote Config 决定启动流程
        // ads_enabled=false（审核期）：直接进首页，不展示订阅页和广告
        if (!remoteConfig.adsEnabled) {
            hasCompleted = true
            _startupState.value = StartupState.NavigateToHome
            return
        }

        // ads_enabled=true 且 firstPayShow=true：展示订阅页
        if (remoteConfig.firstPayShow) {
            _startupState.value = StartupState.ShowSubscription

            // Wait for UI layer to report subscription screen result
            val subscriptionResult = awaitSubscriptionResult()

            when (subscriptionResult) {
                SubscriptionScreenResult.Purchased -> {
                    // User purchased — go directly to home
                    hasCompleted = true
                    _startupState.value = StartupState.NavigateToHome
                    return
                }
                SubscriptionScreenResult.Dismissed -> {
                    // User dismissed — show app open ad
                    _startupState.value = StartupState.ShowAppOpenAd
                    adManager.showFullScreenAd(activity, AdPlacement.APP_OPEN_COLD_START)
                    hasCompleted = true
                    _startupState.value = StartupState.NavigateToHome
                }
            }
        } else {
            // Remote Config 关闭了首启订阅页，直接展示开屏广告
            if (remoteConfig.adsEnabled) {
                _startupState.value = StartupState.ShowAppOpenAd
                adManager.showFullScreenAd(activity, AdPlacement.APP_OPEN_COLD_START)
            }
            hasCompleted = true
            _startupState.value = StartupState.NavigateToHome
        }
    }

    // region UI Event Callbacks

    /**
     * Called by the UI layer when the user dismisses the subscription screen
     * without purchasing.
     */
    fun onSubscriptionDismissed() {
        subscriptionContinuation?.invoke(SubscriptionScreenResult.Dismissed)
        subscriptionContinuation = null
    }

    /**
     * Called by the UI layer when the user successfully purchases a subscription
     * from the startup subscription screen.
     */
    fun onSubscriptionPurchased() {
        subscriptionContinuation?.invoke(SubscriptionScreenResult.Purchased)
        subscriptionContinuation = null
    }

    // endregion

    // region Internal Suspension Helpers

    private suspend fun awaitSubscriptionResult(): SubscriptionScreenResult {
        return suspendCancellableCoroutine { cont ->
            subscriptionContinuation = { result ->
                cont.resume(result)
            }
            cont.invokeOnCancellation {
                subscriptionContinuation = null
            }
        }
    }

    // endregion
}

/**
 * Represents the outcome of the subscription screen shown during startup.
 */
private enum class SubscriptionScreenResult {
    /** User successfully purchased a subscription. */
    Purchased,
    /** User dismissed the subscription screen without purchasing. */
    Dismissed
}
