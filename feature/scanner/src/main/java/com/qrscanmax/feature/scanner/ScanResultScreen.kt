package com.qrscanmax.feature.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qrscanmax.core.domain.model.ContentType
import com.qrscanmax.core.domain.model.ScanResult

/**
 * 扫描结果展示界面 — 根据内容类型显示不同的操作按钮。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本界面根据 ScanResult 的 contentType 动态渲染不同的操作面板：
 * - URL → 打开浏览器、复制、分享
 * - WiFi → 显示网络信息、连接按钮
 * - vCard → 显示联系人字段、保存到通讯录
 * - Phone → 拨打电话、发短信
 * - Social Media → 打开对应 App
 * - Plain Text → 复制、分享
 *
 * ## 导航集成
 * 本界面通常作为 BottomSheet 或新页面从 ScannerScreen 导航过来。
 * [onDismiss] 回调用于关闭结果页面并恢复扫描。
 *
 * ## 后续开发
 * - WiFi 连接功能需要使用 WifiNetworkSuggestion API (Android 10+)
 * - vCard 保存需要构建 Intent 并填充联系人字段
 * - 商品条码需要导航到 ProductDetailScreen
 */
@Composable
fun ScanResultScreen(
    scanResult: ScanResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 内容类型标签
        ContentTypeChip(contentType = scanResult.contentType)

        Spacer(modifier = Modifier.height(16.dp))

        // 原始内容显示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = scanResult.rawValue,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                maxLines = 5
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 根据内容类型显示不同的操作按钮
        when (scanResult.contentType) {
            ContentType.URL -> UrlActions(scanResult.rawValue, context)
            ContentType.WIFI -> WifiActions(scanResult.rawValue, context)
            ContentType.PHONE -> PhoneActions(scanResult.rawValue, context)
            ContentType.VCARD -> VCardActions(scanResult.rawValue, context)
            ContentType.EMAIL -> EmailActions(scanResult.rawValue, context)
            ContentType.SOCIAL_MEDIA -> SocialMediaActions(scanResult.rawValue, context)
            else -> DefaultActions(scanResult.rawValue, context)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 继续扫描按钮
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.result_scan_again))
        }
    }
}

/** 内容类型标签芯片 */
@Composable
private fun ContentTypeChip(contentType: ContentType) {
    val label = when (contentType) {
        ContentType.URL -> stringResource(R.string.result_type_url)
        ContentType.WIFI -> stringResource(R.string.result_type_wifi)
        ContentType.VCARD -> stringResource(R.string.result_type_contact)
        ContentType.PHONE -> stringResource(R.string.result_type_phone)
        ContentType.EMAIL -> stringResource(R.string.result_type_email)
        ContentType.SMS -> stringResource(R.string.result_type_sms)
        ContentType.SOCIAL_MEDIA -> stringResource(R.string.result_type_social)
        ContentType.GEO -> stringResource(R.string.result_type_location)
        ContentType.PLAIN_TEXT -> stringResource(R.string.result_type_text)
        ContentType.PRODUCT -> stringResource(R.string.result_type_product)
    }

    AssistChip(
        onClick = {},
        label = { Text(label, fontWeight = FontWeight.Medium) },
        leadingIcon = {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    )
}

/** URL 类型的操作按钮 */
@Composable
private fun UrlActions(rawValue: String, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rawValue))) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.result_open_url))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { copyToClipboard(context, rawValue) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.result_copy))
            }
            OutlinedButton(onClick = { shareText(context, rawValue) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.result_share))
            }
        }
    }
}

/** WiFi 类型的操作按钮 */
@Composable
private fun WifiActions(rawValue: String, context: Context) {
    val ssid = Regex("S:([^;]+)").find(rawValue)?.groupValues?.get(1) ?: "Unknown"
    val security = Regex("T:([^;]+)").find(rawValue)?.groupValues?.get(1) ?: "None"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("${stringResource(R.string.wifi_ssid)}: $ssid", style = MaterialTheme.typography.bodyLarge)
                Text("${stringResource(R.string.wifi_security)}: $security", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(
            onClick = {
                // TODO [FUTURE]: 使用 WifiNetworkSuggestion API 连接 WiFi (Android 10+)
                Toast.makeText(context, "WiFi connect coming soon", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.result_connect_wifi))
        }
    }
}

/** 电话类型的操作按钮 */
@Composable
private fun PhoneActions(rawValue: String, context: Context) {
    val phoneNumber = rawValue.removePrefix("tel:")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(phoneNumber, style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.result_call))
            }
            OutlinedButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Sms, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.result_send_sms))
            }
        }
    }
}

/** vCard 类型的操作按钮 */
@Composable
private fun VCardActions(rawValue: String, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                // TODO [FUTURE]: 解析 vCard 字段并创建 INSERT 联系人 Intent
                Toast.makeText(context, "Save contact coming soon", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.result_save_contact))
        }
        OutlinedButton(onClick = { copyToClipboard(context, rawValue) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.result_copy))
        }
    }
}

/** Email 类型的操作按钮 */
@Composable
private fun EmailActions(rawValue: String, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(rawValue))) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Email, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Email")
        }
        OutlinedButton(onClick = { copyToClipboard(context, rawValue.removePrefix("mailto:")) },
            modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.result_copy))
        }
    }
}

/** 社交媒体类型的操作按钮 */
@Composable
private fun SocialMediaActions(rawValue: String, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rawValue))) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.result_open_app))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { copyToClipboard(context, rawValue) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.result_copy))
            }
            OutlinedButton(onClick = { shareText(context, rawValue) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.result_share))
            }
        }
    }
}

/** 默认操作按钮（纯文本等） */
@Composable
private fun DefaultActions(rawValue: String, context: Context) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { copyToClipboard(context, rawValue) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.result_copy))
        }
        OutlinedButton(onClick = { shareText(context, rawValue) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.result_share))
        }
    }
}

// ─── 工具函数 ───────────────────────────────────────────────────────────────────

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("QR Scan Max", text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
