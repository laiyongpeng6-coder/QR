package com.qrscanfast.core.ads

import com.qrscanfast.core.common.RemoteConfigManager
import com.qrscanfast.core.domain.ads.FrequencyController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [FrequencyController].
 *
 * 频率限制参数从 Firebase Remote Config 动态读取：
 * - ads_show_sec: 插屏广告最小间隔（秒）
 * - ads_max_per: 单次会话插屏广告上限
 *
 * Uses an injectable [Clock] for testability.
 */
@Singleton
class FrequencyControllerImpl @Inject constructor(
    private val clock: Clock,
    private val remoteConfig: RemoteConfigManager
) : FrequencyController {

    private var lastShowTimeMs: Long = 0L
    private var sessionCount: Int = 0

    companion object {
        // 仅用于测试的默认值回退
        const val DEFAULT_MIN_INTERVAL_MS = 60_000L
        const val DEFAULT_MAX_PER_SESSION = 10

        // 保留旧常量名兼容测试
        const val MIN_INTERVAL_MS = DEFAULT_MIN_INTERVAL_MS
        const val MAX_PER_SESSION = DEFAULT_MAX_PER_SESSION
    }

    /** 从 Remote Config 读取的最小间隔（毫秒） */
    private val minIntervalMs: Long
        get() = remoteConfig.adsShowIntervalSec.toLong() * 1000L

    /** 从 Remote Config 读取的会话上限 */
    private val maxPerSession: Int
        get() = remoteConfig.adsMaxPerSession

    override fun canShowInterstitial(): Boolean {
        val elapsed = clock.currentTimeMillis() - lastShowTimeMs
        return elapsed >= minIntervalMs && sessionCount < maxPerSession
    }

    override fun isWithinSessionLimit(): Boolean = sessionCount < maxPerSession

    override fun recordInterstitialShow() {
        lastShowTimeMs = clock.currentTimeMillis()
        sessionCount++
    }

    override fun resetSession() {
        sessionCount = 0
    }
}
