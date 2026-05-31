package com.qrscanfast.app.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qrscanfast.core.data.datastore.AppSettings
import kotlinx.coroutines.launch

/**
 * 设置页面。
 *
 * 包含：
 * - 扫描设置（自动跳转网站、扫描震动）
 * - 支持（问题反馈）
 * - 法律（隐私政策、服务条款）
 * - 关于（版本信息）
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

            // 扫描设置
            SectionTitle("扫描设置")

            SettingsSwitchItem(
                icon = Icons.Default.OpenInBrowser,
                title = "自动跳转网站",
                description = "扫描到网址时自动在浏览器中打开",
                checked = autoOpenUrl,
                onCheckedChange = {
                    scope.launch { appSettings.setAutoOpenUrl(it) }
                    com.qrscanfast.core.common.AnalyticsHelper.logSettingChange("auto_open_url", it)
                }
            )

            SettingsSwitchItem(
                icon = Icons.Default.Vibration,
                title = "扫描震动",
                description = "扫描成功时触发震动反馈",
                checked = vibrateOnScan,
                onCheckedChange = {
                    scope.launch { appSettings.setVibrateOnScan(it) }
                    com.qrscanfast.core.common.AnalyticsHelper.logSettingChange("vibrate_on_scan", it)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 支持
            SectionTitle("支持")

            SettingsClickItem(
                icon = Icons.Default.Email,
                title = "问题反馈",
                description = "遇到问题？给我们发邮件",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:ylai18117@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Fast QR Scan 问题反馈")
                        putExtra(Intent.EXTRA_TEXT, "请描述您遇到的问题：\n\n")
                    }
                    context.startActivity(Intent.createChooser(intent, "发送反馈"))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 法律
            SectionTitle("法律")

            SettingsClickItem(
                icon = Icons.Default.PrivacyTip,
                title = "隐私政策",
                description = "查看我们的隐私政策",
                onClick = {
                    openUrl(context, "https://sites.google.com/view/fastqrscan-privacy-policy/%E9%A6%96%E9%A1%B5")
                }
            )

            SettingsClickItem(
                icon = Icons.Default.Description,
                title = "服务条款",
                description = "查看用户服务条款",
                onClick = {
                    openUrl(context, "https://sites.google.com/view/fastqrscan-terms-of-service/%E9%A6%96%E9%A1%B5")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 关于
            SectionTitle("关于")

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Fast QR Scan: Barcode Reader", style = MaterialTheme.typography.bodyLarge)
                Text("v1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
