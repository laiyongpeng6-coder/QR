package com.qrscanfast.app.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qrscanfast.app.R
import com.qrscanfast.core.common.AnalyticsHelper
import com.qrscanfast.core.common.LocaleManager
import com.qrscanfast.core.data.datastore.AppSettings
import kotlinx.coroutines.launch

/**
 * 设置页面 — 全部文案使用 stringResource 支持多语言。
 *
 * 包含：扫描设置、语言选择、问题反馈、隐私政策、服务条款、关于。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val autoOpenUrl by appSettings.autoOpenUrl.collectAsState(initial = false)
    val vibrateOnScan by appSettings.vibrateOnScan.collectAsState(initial = false)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(LocaleManager.getCurrentLanguage()) }

    val feedbackSubject = stringResource(R.string.settings_feedback_subject)
    val feedbackBody = stringResource(R.string.settings_feedback_body)
    val feedbackChooser = stringResource(R.string.settings_feedback_chooser)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 扫描设置
            SectionTitle(stringResource(R.string.settings_section_scan))

            SettingsSwitchItem(
                icon = Icons.Default.OpenInBrowser,
                title = stringResource(R.string.settings_auto_open_url),
                description = stringResource(R.string.settings_auto_open_url_desc),
                checked = autoOpenUrl,
                onCheckedChange = {
                    scope.launch { appSettings.setAutoOpenUrl(it) }
                    AnalyticsHelper.logSettingChange("auto_open_url", it)
                }
            )

            SettingsSwitchItem(
                icon = Icons.Default.Vibration,
                title = stringResource(R.string.settings_vibrate),
                description = stringResource(R.string.settings_vibrate_desc),
                checked = vibrateOnScan,
                onCheckedChange = {
                    scope.launch { appSettings.setVibrateOnScan(it) }
                    AnalyticsHelper.logSettingChange("vibrate_on_scan", it)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 通用 — 语言
            SectionTitle(stringResource(R.string.settings_section_general))

            SettingsClickItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language),
                description = stringResource(R.string.settings_language_desc),
                trailingText = languageDisplayName(currentLanguage),
                onClick = { showLanguageDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 支持
            SectionTitle(stringResource(R.string.settings_section_support))

            SettingsClickItem(
                icon = Icons.Default.Email,
                title = stringResource(R.string.settings_feedback),
                description = stringResource(R.string.settings_feedback_desc),
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:ylai18117@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                        putExtra(Intent.EXTRA_TEXT, feedbackBody)
                    }
                    context.startActivity(Intent.createChooser(intent, feedbackChooser))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 法律
            SectionTitle(stringResource(R.string.settings_section_legal))

            SettingsClickItem(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.settings_privacy),
                description = stringResource(R.string.settings_privacy_desc),
                onClick = {
                    openUrl(context, "https://sites.google.com/view/fastqrscan-privacy-policy/%E9%A6%96%E9%A1%B5")
                }
            )

            SettingsClickItem(
                icon = Icons.Default.Description,
                title = stringResource(R.string.settings_terms),
                description = stringResource(R.string.settings_terms_desc),
                onClick = {
                    openUrl(context, "https://sites.google.com/view/fastqrscan-terms-of-service/%E9%A6%96%E9%A1%B5")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 关于
            SectionTitle(stringResource(R.string.settings_section_about))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.settings_app_full_name), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // 语言选择对话框
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            current = currentLanguage,
            onSelect = { language ->
                LocaleManager.setLanguage(language)
                currentLanguage = language
                showLanguageDialog = false
                AnalyticsHelper.logSettingChange("language_${language.name}", true)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

/**
 * 语言选择对话框。
 */
@Composable
private fun LanguageSelectionDialog(
    current: LocaleManager.AppLanguage,
    onSelect: (LocaleManager.AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column {
                LocaleManager.AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = current == language,
                                onClick = { onSelect(language) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == language,
                            onClick = { onSelect(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = languageDisplayName(language),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.language_cancel))
            }
        }
    )
}

/**
 * 获取语言显示名称。"跟随系统"使用本地化文案，其余使用各语言母语名。
 */
@Composable
private fun languageDisplayName(language: LocaleManager.AppLanguage): String {
    return if (language == LocaleManager.AppLanguage.SYSTEM) {
        stringResource(R.string.language_system)
    } else {
        language.displayName
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    description: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
