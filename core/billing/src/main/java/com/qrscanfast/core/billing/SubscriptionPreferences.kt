package com.qrscanfast.core.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 订阅状态本地缓存，使用 Jetpack DataStore Preferences 存储。
 *
 * 当 Google Play BillingClient 连接失败或查询超时时，
 * 系统可从本地缓存读取最近一次的订阅状态，实现离线降级。
 *
 * 缓存字段：
 * - isPremium：用户是否为付费会员
 * - activePlan：当前生效的订阅方案名称（如 "WEEKLY"、"ANNUAL"、"LIFETIME"）
 * - expiryTimeMs：订阅到期时间戳（毫秒），LIFETIME 方案为 null
 *
 * @param dataStore 由 Hilt 注入的全局 DataStore<Preferences> 实例（core:data 模块提供）
 */
class SubscriptionPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /** 用户是否为 Premium 会员（默认 false） */
    val isPremium: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_PREMIUM] ?: false
    }

    /** 当前生效的订阅方案名称，Free 用户为 null */
    val activePlan: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_PLAN]
    }

    /** 订阅到期时间戳（毫秒），LIFETIME 或 Free 用户为 null */
    val expiryTimeMs: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[KEY_EXPIRY_TIME_MS]
    }

    /**
     * 更新本地缓存的订阅状态。
     *
     * 在以下时机调用：
     * - 购买成功确认后
     * - 订阅状态刷新成功后
     * - 订阅过期/取消后（isPremium=false, plan=null, expiryMs=null）
     *
     * @param isPremium 是否为付费会员
     * @param plan 订阅方案名称，Free 用户传 null
     * @param expiryMs 到期时间戳（毫秒），LIFETIME 或 Free 用户传 null
     */
    suspend fun updateStatus(isPremium: Boolean, plan: String?, expiryMs: Long?) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_PREMIUM] = isPremium
            if (plan != null) {
                preferences[KEY_ACTIVE_PLAN] = plan
            } else {
                preferences.remove(KEY_ACTIVE_PLAN)
            }
            if (expiryMs != null) {
                preferences[KEY_EXPIRY_TIME_MS] = expiryMs
            } else {
                preferences.remove(KEY_EXPIRY_TIME_MS)
            }
        }
    }

    companion object {
        private val KEY_IS_PREMIUM = booleanPreferencesKey("subscription_is_premium")
        private val KEY_ACTIVE_PLAN = stringPreferencesKey("subscription_active_plan")
        private val KEY_EXPIRY_TIME_MS = longPreferencesKey("subscription_expiry_time_ms")
    }
}
