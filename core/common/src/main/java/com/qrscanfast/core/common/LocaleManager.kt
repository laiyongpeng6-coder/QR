package com.qrscanfast.core.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * 应用语言管理工具。
 *
 * 使用 AndroidX 的 per-app language 功能（AppCompatDelegate.setApplicationLocales），
 * 切换后立即生效，无需重启 Activity，且系统会持久化用户的语言选择。
 *
 * 默认跟随系统语言；若系统语言不在支持列表内，会自动 fallback 到英语（因为
 * 默认 res/values/ 资源为英文）。
 */
object LocaleManager {

    /**
     * 应用支持的语言列表。
     *
     * @property tag BCP-47 语言标签（用于 setApplicationLocales）
     * @property displayName 在语言选择列表中显示的名称（用各语言的母语书写）
     */
    enum class AppLanguage(val tag: String, val displayName: String) {
        SYSTEM("", "跟随系统"),
        ENGLISH("en", "English"),
        SIMPLIFIED_CHINESE("zh-CN", "简体中文"),
        TRADITIONAL_CHINESE("zh-TW", "繁體中文"),
        GERMAN("de", "Deutsch"),
        SPANISH("es", "Español"),
        PORTUGUESE("pt", "Português")
    }

    /**
     * 切换应用语言。
     *
     * @param language 目标语言。传入 [AppLanguage.SYSTEM] 表示跟随系统。
     */
    fun setLanguage(language: AppLanguage) {
        val localeList = if (language == AppLanguage.SYSTEM) {
            // 空 LocaleList 表示清除自定义设置，恢复跟随系统
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * 获取当前生效的应用语言。
     *
     * @return 当前选中的 [AppLanguage]，若未设置自定义语言则返回 [AppLanguage.SYSTEM]
     */
    fun getCurrentLanguage(): AppLanguage {
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.isEmpty) return AppLanguage.SYSTEM

        val currentTag = current.toLanguageTags()
        return AppLanguage.entries.firstOrNull { lang ->
            lang.tag.isNotEmpty() && currentTag.startsWith(lang.tag, ignoreCase = true)
        } ?: AppLanguage.SYSTEM
    }
}
