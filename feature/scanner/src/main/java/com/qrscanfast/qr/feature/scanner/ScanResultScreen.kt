package com.qrscanfast.qr.feature.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qrscanfast.qr.core.domain.model.BarcodeFormat
import com.qrscanfast.qr.core.domain.model.ContentType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 扫描结果全屏详情页。
 *
 * 根据不同的内容类型展示结构化信息和对应的操作按钮。
 * 支持：复制、分享、收藏、以及类型相关的主操作（打开链接、拨打电话、连接WiFi等）。
 *
 * @param rawContent 扫描到的原始内容
 * @param format 条码格式名称
 * @param contentType 内容类型名称
 * @param onBack 返回上一页回调
 * @param onContinueScan 继续扫描回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    rawContent: String,
    format: String,
    contentType: String,
    onBack: () -> Unit,
    onContinueScan: () -> Unit
) {
    val context = LocalContext.current
    val type = try { ContentType.valueOf(contentType) } catch (_: Exception) { ContentType.PLAIN_TEXT }
    val scanTime = remember { Instant.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫描结果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onContinueScan) {
                        Text("继续扫描")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 内容类型图标和标签
            ContentTypeHeader(type = type)

            Spacer(modifier = Modifier.height(24.dp))

            // 结构化内容展示区
            ContentDisplayCard(rawContent = rawContent, type = type, context = context)

            Spacer(modifier = Modifier.height(24.dp))

            // 通用操作按钮行
            CommonActionButtons(rawContent = rawContent, context = context)

            Spacer(modifier = Modifier.height(16.dp))

            // 类型相关的主操作按钮
            PrimaryActionButton(rawContent = rawContent, type = type, context = context)

            Spacer(modifier = Modifier.height(24.dp))

            // 元信息
            MetaInfoSection(format = format, scanTime = scanTime)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 内容类型图标和标签头部。
 */
@Composable
private fun ContentTypeHeader(type: ContentType) {
    val (icon, label) = getTypeIconAndLabel(type)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

/**
 * 结构化内容展示卡片 — 根据内容类型展示不同布局。
 */
@Composable
private fun ContentDisplayCard(rawContent: String, type: ContentType, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            when (type) {
                ContentType.URL, ContentType.SOCIAL_MEDIA -> UrlContent(rawContent)
                ContentType.WIFI -> WifiContent(rawContent)
                ContentType.PHONE -> PhoneContent(rawContent)
                ContentType.EMAIL -> EmailContent(rawContent)
                ContentType.VCARD -> VCardContent(rawContent)
                ContentType.GEO -> GeoContent(rawContent)
                else -> PlainTextContent(rawContent)
            }
        }
    }
}

@Composable
private fun UrlContent(rawContent: String) {
    Text("链接地址", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = rawContent,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun WifiContent(rawContent: String) {
    val ssid = Regex("S:([^;]*)").find(rawContent)?.groupValues?.get(1) ?: "未知"
    val security = Regex("T:([^;]*)").find(rawContent)?.groupValues?.get(1) ?: "无"
    val password = Regex("P:([^;]*)").find(rawContent)?.groupValues?.get(1) ?: ""
    var showPassword by remember { mutableStateOf(false) }

    InfoRow(label = "网络名称", value = ssid)
    Spacer(modifier = Modifier.height(12.dp))
    InfoRow(label = "加密方式", value = security)
    Spacer(modifier = Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("密码", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (showPassword) password else "••••••••",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = { showPassword = !showPassword }) {
            Icon(
                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showPassword) "隐藏密码" else "显示密码"
            )
        }
    }
}

@Composable
private fun PhoneContent(rawContent: String) {
    val phone = rawContent.removePrefix("tel:").removePrefix("TEL:")
    Text("电话号码", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = phone, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmailContent(rawContent: String) {
    val email = rawContent.removePrefix("mailto:").removePrefix("MAILTO:").substringBefore("?")
    val subject = Regex("[?&]subject=([^&]*)").find(rawContent)?.groupValues?.get(1) ?: ""

    InfoRow(label = "邮箱地址", value = email)
    if (subject.isNotBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        InfoRow(label = "主题", value = subject)
    }
}

@Composable
private fun VCardContent(rawContent: String) {
    val name = Regex("FN:(.+)").find(rawContent)?.groupValues?.get(1) ?: "未知联系人"
    val phone = Regex("TEL[^:]*:(.+)").find(rawContent)?.groupValues?.get(1)
    val email = Regex("EMAIL[^:]*:(.+)").find(rawContent)?.groupValues?.get(1)
    val org = Regex("ORG:(.+)").find(rawContent)?.groupValues?.get(1)

    Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))

    if (org != null) {
        InfoRow(label = "公司", value = org)
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (phone != null) {
        InfoRow(label = "电话", value = phone)
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (email != null) {
        InfoRow(label = "邮箱", value = email)
    }
}

@Composable
private fun GeoContent(rawContent: String) {
    val coords = rawContent.removePrefix("geo:").split(",")
    val lat = coords.getOrNull(0) ?: "未知"
    val lng = coords.getOrNull(1)?.substringBefore("?") ?: "未知"

    InfoRow(label = "纬度", value = lat)
    Spacer(modifier = Modifier.height(12.dp))
    InfoRow(label = "经度", value = lng)
}

@Composable
private fun PlainTextContent(rawContent: String) {
    Text("内容", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(8.dp))
    SelectionContainer {
        Text(
            text = rawContent,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(4.dp))
    Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
}

/**
 * 通用操作按钮行：复制、分享、收藏。
 */
@Composable
private fun CommonActionButtons(rawContent: String, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionChip(
            icon = Icons.Outlined.ContentCopy,
            label = "复制",
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("QR Content", rawContent))
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
        )
        ActionChip(
            icon = Icons.Outlined.Share,
            label = "分享",
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, rawContent)
                }
                context.startActivity(Intent.createChooser(shareIntent, "分享扫描结果"))
            }
        )
        ActionChip(
            icon = Icons.Outlined.FavoriteBorder,
            label = "收藏",
            onClick = {
                Toast.makeText(context, "已收藏", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun ActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * 类型相关的主操作按钮。
 */
@Composable
private fun PrimaryActionButton(rawContent: String, type: ContentType, context: Context) {
    when (type) {
        ContentType.URL, ContentType.SOCIAL_MEDIA -> {
            Button(
                onClick = {
                    try {
                        val url = if (!rawContent.startsWith("http")) "https://$rawContent" else rawContent
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("在浏览器中打开", style = MaterialTheme.typography.labelLarge)
            }
        }
        ContentType.WIFI -> {
            val password = Regex("P:([^;]*)").find(rawContent)?.groupValues?.get(1) ?: ""
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("WiFi Password", password))
                    Toast.makeText(context, "密码已复制: $password", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("复制密码", style = MaterialTheme.typography.labelLarge)
            }
        }
        ContentType.PHONE -> {
            val phone = rawContent.removePrefix("tel:").removePrefix("TEL:")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("拨打电话")
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("发短信")
                }
            }
        }
        ContentType.EMAIL -> {
            val email = rawContent.removePrefix("mailto:").removePrefix("MAILTO:").substringBefore("?")
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("发送邮件", style = MaterialTheme.typography.labelLarge)
            }
        }
        ContentType.VCARD -> {
            Button(
                onClick = {
                    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                        setType(ContactsContract.RawContacts.CONTENT_TYPE)
                        val contactName = Regex("FN:(.+)").find(rawContent)?.groupValues?.get(1) ?: ""
                        val contactPhone = Regex("TEL[^:]*:(.+)").find(rawContent)?.groupValues?.get(1) ?: ""
                        val contactEmail = Regex("EMAIL[^:]*:(.+)").find(rawContent)?.groupValues?.get(1) ?: ""
                        putExtra(ContactsContract.Intents.Insert.NAME, contactName)
                        if (contactPhone.isNotBlank()) putExtra(ContactsContract.Intents.Insert.PHONE, contactPhone)
                        if (contactEmail.isNotBlank()) putExtra(ContactsContract.Intents.Insert.EMAIL, contactEmail)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存到通讯录", style = MaterialTheme.typography.labelLarge)
            }
        }
        ContentType.GEO -> {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawContent))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("在地图中打开", style = MaterialTheme.typography.labelLarge)
            }
        }
        ContentType.PRODUCT -> {
            Button(
                onClick = {
                    val searchUrl = "https://www.google.com/search?q=${Uri.encode(rawContent)}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("搜索商品", style = MaterialTheme.typography.labelLarge)
            }
        }
        else -> {
            // 纯文本 — 复制全文作为主操作
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("QR Content", rawContent))
                    Toast.makeText(context, "已复制全文", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("复制全文", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * 元信息区域 — 显示扫描时间和条码格式。
 */
@Composable
private fun MetaInfoSection(format: String, scanTime: Instant) {
    val formattedTime = remember(scanTime) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        scanTime.atZone(ZoneId.systemDefault()).format(formatter)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("扫描时间", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formattedTime, style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("条码格式", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDisplayName(format), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * 获取内容类型对应的图标和中文标签。
 */
private fun getTypeIconAndLabel(type: ContentType): Pair<ImageVector, String> {
    return when (type) {
        ContentType.URL -> Icons.Default.Link to "网址"
        ContentType.WIFI -> Icons.Default.Wifi to "WiFi 网络"
        ContentType.VCARD -> Icons.Default.Person to "联系人"
        ContentType.PHONE -> Icons.Default.Phone to "电话号码"
        ContentType.EMAIL -> Icons.Default.Email to "邮箱"
        ContentType.SMS -> Icons.Default.Sms to "短信"
        ContentType.SOCIAL_MEDIA -> Icons.Default.Public to "社交媒体"
        ContentType.GEO -> Icons.Default.LocationOn to "地理位置"
        ContentType.PRODUCT -> Icons.Default.ShoppingCart to "商品条码"
        ContentType.PLAIN_TEXT -> Icons.Default.TextFields to "文本"
    }
}

/**
 * 格式化条码格式名称用于显示。
 */
private fun formatDisplayName(format: String): String {
    return when (format) {
        "QR_CODE" -> "QR Code"
        "EAN_13" -> "EAN-13"
        "EAN_8" -> "EAN-8"
        "UPC_A" -> "UPC-A"
        "UPC_E" -> "UPC-E"
        "CODE_128" -> "Code 128"
        "CODE_39" -> "Code 39"
        "ITF" -> "ITF"
        "PDF_417" -> "PDF 417"
        "DATA_MATRIX" -> "Data Matrix"
        "AZTEC" -> "Aztec"
        else -> format
    }
}
