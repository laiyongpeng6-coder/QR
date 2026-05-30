package com.qrscanfast.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 应用设置偏好管理类。
 *
 * 使用 Jetpack DataStore 持久化用户设置：
 * - 自动跳转网站：扫描到 URL 时是否自动打开浏览器
 * - 扫描震动：扫描成功时是否触发震动反馈
 */
class AppSettings @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /** 是否启用自动跳转网站（默认关闭） */
    val autoOpenUrl: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_AUTO_OPEN_URL] ?: false
    }

    /** 是否启用扫描震动反馈（默认关闭） */
    val vibrateOnScan: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_VIBRATE_ON_SCAN] ?: false
    }

    /** 设置自动跳转网站开关 */
    suspend fun setAutoOpenUrl(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_OPEN_URL] = enabled
        }
    }

    /** 设置扫描震动开关 */
    suspend fun setVibrateOnScan(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_VIBRATE_ON_SCAN] = enabled
        }
    }

    companion object {
        private val KEY_AUTO_OPEN_URL = booleanPreferencesKey("auto_open_url")
        private val KEY_VIBRATE_ON_SCAN = booleanPreferencesKey("vibrate_on_scan")
    }
}
