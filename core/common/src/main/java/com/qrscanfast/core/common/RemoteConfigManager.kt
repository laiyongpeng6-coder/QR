package com.qrscanfast.core.common

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Firebase Remote Config 管理器。
 *
 * 在应用启动时拉取远程配置参数，供广告、订阅等模块读取。
 * 如果拉取失败则使用本地默认值，保证应用正常运行。
 *
 * Remote Config 控制台参数对照：
 * - ads_enabled: 全局广告开关（默认关）
 * - ads_firstpay_show: 首启订阅页是否展示（默认 true）
 * - ads_max_per: 单次会话插屏广告上限（默认 10）
 * - ads_show_sec: 插屏广告最小间隔秒数（默认 60）
 * - mustUpdate_version: 强制更新最低版本号（默认 "1.1.0"）
 */
@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    companion object {
        private const val TAG = "RemoteConfig"

        // 参数 Key（与 Firebase Remote Config 控制台一致）
        const val KEY_ADS_ENABLED = "ads_enabled"
        const val KEY_ADS_FIRSTPAY_SHOW = "ads_firstpay_show"
        const val KEY_ADS_MAX_PER = "ads_max_per"
        const val KEY_ADS_SHOW_SEC = "ads_show_sec"
        const val KEY_MUST_UPDATE_VERSION = "mustUpdate_version"

        // 本地默认值（Remote Config 拉取失败时使用）
        private val DEFAULTS = mapOf(
            KEY_ADS_ENABLED to false,
            KEY_ADS_FIRSTPAY_SHOW to true,
            KEY_ADS_MAX_PER to 10L,
            KEY_ADS_SHOW_SEC to 60L,
            KEY_MUST_UPDATE_VERSION to "1.1.0"
        )
    }

    /**
     * 初始化并拉取远程配置（异步，不阻塞启动）。
     * 在 Application.onCreate 中调用。
     */
    fun initialize() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // 每次冷启动都拉取最新配置
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(DEFAULTS)

        // 拉取并激活
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            _isReady.value = true
            if (task.isSuccessful) {
                Log.d(TAG, "Remote Config 拉取成功，已激活最新配置")
            } else {
                Log.w(TAG, "Remote Config 拉取失败，使用缓存/默认值")
            }
            // 打印当前所有参数值方便调试
            Log.d(TAG, "ads_enabled = $adsEnabled")
            Log.d(TAG, "ads_firstpay_show = $firstPayShow")
            Log.d(TAG, "ads_max_per = $adsMaxPerSession")
            Log.d(TAG, "ads_show_sec = $adsShowIntervalSec")
            Log.d(TAG, "mustUpdate_version = $mustUpdateVersion")
        }
    }

    /**
     * 挂起等待 Remote Config 拉取完成（最多等 5 秒）。
     * 在需要确保参数已就绪后再使用的场景调用（如 StartupOrchestrator）。
     * 超时后直接继续（使用缓存值或默认值）。
     */
    suspend fun awaitReady() {
        if (_isReady.value) return
        withTimeoutOrNull(5000L) {
            _isReady.first { it }
        }
        if (!_isReady.value) {
            Log.w(TAG, "Remote Config 等待超时，使用默认值继续")
        }
    }

    // ─── 公开读取方法 ─────────────────────────────────────────────

    /** 全局广告开关。false = 不展示任何广告 */
    val adsEnabled: Boolean
        get() = remoteConfig.getBoolean(KEY_ADS_ENABLED)

    /** 首启是否展示订阅页 */
    val firstPayShow: Boolean
        get() = remoteConfig.getBoolean(KEY_ADS_FIRSTPAY_SHOW)

    /** 单次会话插屏广告最大次数 */
    val adsMaxPerSession: Int
        get() = remoteConfig.getLong(KEY_ADS_MAX_PER).toInt()

    /** 插屏广告最小间隔（秒） */
    val adsShowIntervalSec: Int
        get() = remoteConfig.getLong(KEY_ADS_SHOW_SEC).toInt()

    /** 强制更新的最低版本号 */
    val mustUpdateVersion: String
        get() = remoteConfig.getString(KEY_MUST_UPDATE_VERSION)
}
