package com.qrscanfast.app.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qrscanfast.core.data.datastore.AppSettings
import kotlinx.coroutines.launch

/**
 * 设置页面 — 提供应用行为配置。
 *
 * 当前支持的设置项：
 * 1. 自动跳转网站：扫描到 URL 时自动打开浏览器
 * 2. 扫描震动：扫描成功时触发震动反馈
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val autoOpenUrl by appSettings.autoOpenUrl.collectAsState(initial = false)
    val vibrateOnScan by appSettings.vibrateOnScan.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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

            // 扫描设置分组
            Text(
                text = "扫描设置",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // 自动跳转网站
            SettingsSwitchItem(
                title = "自动跳转网站",
                description = "扫描到网址时自动在浏览器中打开",
                checked = autoOpenUrl,
                onCheckedChange = { enabled ->
                    scope.launch { appSettings.setAutoOpenUrl(enabled) }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 扫描震动
            SettingsSwitchItem(
                title = "扫描震动",
                description = "扫描成功时触发震动反馈",
                checked = vibrateOnScan,
                onCheckedChange = { enabled ->
                    scope.launch { appSettings.setVibrateOnScan(enabled) }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 关于分组
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Text(
                text = "Fast QR Scan: Barcode Reader",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "版本 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 设置项开关组件。
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
