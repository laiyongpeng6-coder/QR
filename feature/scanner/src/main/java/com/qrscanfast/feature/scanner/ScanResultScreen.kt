package com.qrscanfast.feature.scanner

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qrscanfast.core.domain.model.ContentType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 扫描结果详情页面。
 *
 * ## AI 交接
 * - 职责：根据内容类型展示结构化信息和下一步操作。
 * - 当前状态：已支持 URL、WiFi、联系人、电话、邮件、地理位置等分支。
 * - 依赖：`core/domain` 内容类型模型、系统分享/跳转能力。
 * - 安全修改范围：内容展示、主操作按钮、辅助操作、元信息。
 * - 风险 / TODO：新增内容类型时要同步展示逻辑与行动按钮。
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
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = onContinueScan) {
                        Text(stringResource(R.string.result_continue_scan))
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
            ContentTypeHeader(type = type)
            Spacer(modifier = Modifier.height(24.dp))
            ContentDisplayCard(rawContent = rawContent, type = type)
            Spacer(modifier = Modifier.height(24.dp))
            CommonActionButtons(rawContent = rawContent, context = context)
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryActionButton(rawContent = rawContent, type = type, context = context)
            Spacer(modifier = Modifier.height(24.dp))
            MetaInfoSection(format = format, scanTime = scanTime)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ContentTypeHeader(type: ContentType) {
    val icon = getTypeIcon(type)
    val label = stringResource(getTypeLabelRes(type))

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

@Composable
private fun ContentDisplayCard(rawContent: String, type: ContentType) {
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
    Text(stringResource(R.string.result_label_link), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val unknown = stringResource(R.string.result_unknown)
    val ssid = Regex("S:([^;]*)").find(rawContent)?.groupValues?.get(1)?.ifBlank { unknown } ?: unknown
    val security = Regex("T:([^;]*)").find(rawContent)?.groupValues?.get(1)?.ifBlank { unknown } ?: unknown
    val password = Regex("P:([^;]*)").find(rawContent)?.groupValues?.get(1) ?: ""
    var showPassword by remember { mutableStateOf(false) }

    InfoRow(label = stringResource(R.string.result_label_network_name), value = ssid)
    Spacer(modifier = Modifier.height(12.dp))
    InfoRow(label = stringResource(R.string.result_label_security), value = security)
    Spacer(modifier = Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.result_label_password), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                contentDescription = stringResource(
                    if (showPassword) R.string.action_hide_password else R.string.action_show_password
                )
            )
        }
    }
}

@Composable
private fun PhoneContent(rawContent: String) {
    val phone = rawContent.removePrefix("tel:").removePrefix("TEL:")
    Text(stringResource(R.string.result_label_phone), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = phone, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmailContent(rawContent: String) {
    val email = rawContent.removePrefix("mailto:").removePrefix("MAILTO:").substringBefore("?")
    val subject = Regex("[?&]subject=([^&]*)").find(rawContent)?.groupValues?.get(1) ?: ""

    InfoRow(label = stringResource(R.string.result_label_email), value = email)
    if (subject.isNotBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        InfoRow(label = stringResource(R.string.result_label_subject), value = subject)
    }
}

@Composable
private fun VCardContent(rawContent: String) {
    val name = Regex("FN:(.+)").find(rawContent)?.groupValues?.get(1) ?: stringResource(R.string.result_unknown_contact)
    val phone = Regex("TEL[^:]*:(.+)").find(rawContent)?.groupValues?.get(1)
    val email = Regex("EMAIL[^:]*:(.+)").find(rawContent)?.groupValues?.get(1)
    val org = Regex("ORG:(.+)").find(rawContent)?.groupValues?.get(1)

    Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))

    if (org != null) {
        InfoRow(label = stringResource(R.string.result_label_company), value = org)
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (phone != null) {
        InfoRow(label = stringResource(R.string.result_label_phone), value = phone)
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (email != null) {
        InfoRow(label = stringResource(R.string.result_label_email), value = email)
    }
}

@Composable
private fun GeoContent(rawContent: String) {
    val unknown = stringResource(R.string.result_unknown)
    val coords = rawContent.removePrefix("geo:").split(",")
    val lat = coords.getOrNull(0) ?: unknown
    val lng = coords.getOrNull(1)?.substringBefore("?") ?: unknown

    InfoRow(label = stringResource(R.string.result_label_latitude), value = lat)
    Spacer(modifier = Modifier.height(12.dp))
    InfoRow(label = stringResource(R.string.result_label_longitude), value = lng)
}

@Composable
private fun PlainTextContent(rawContent: String) {
    Text(stringResource(R.string.result_label_content), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun CommonActionButtons(rawContent: String, context: Context) {
    val copiedMsg = stringResource(R.string.toast_copied)
    val favoritedMsg = stringResource(R.string.toast_favorited)
    val shareTitle = stringResource(R.string.action_share)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionChip(
            icon = Icons.Outlined.ContentCopy,
            label = stringResource(R.string.action_copy),
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("QR Content", rawContent))
                Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
            }
        )
        ActionChip(
            icon = Icons.Outlined.Share,
            label = stringResource(R.string.action_share),
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    setType("text/plain")
                    putExtra(Intent.EXTRA_TEXT, rawContent)
                }
                context.startActivity(Intent.createChooser(shareIntent, shareTitle))
            }
        )
        ActionChip(
            icon = Icons.Outlined.FavoriteBorder,
            label = stringResource(R.string.action_favorite),
            onClick = {
                Toast.makeText(context, favoritedMsg, Toast.LENGTH_SHORT).show()
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

@Composable
private fun PrimaryActionButton(rawContent: String, type: ContentType, context: Context) {
    val cannotOpenMsg = stringResource(R.string.toast_cannot_open_link)
    when (type) {
        ContentType.URL, ContentType.SOCIAL_MEDIA -> {
            Button(
                onClick = {
                    try {
                        val url = if (!rawContent.startsWith("http")) "https://$rawContent" else rawContent
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(context, cannotOpenMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_open_in_browser), style = MaterialTheme.typography.labelLarge)
            }
        }
        ContentType.WIFI -> {
            val password = Regex("P:([^;]*)").find(rawContent)?.groupValues?.get(1) ?: ""
            val pwdCopiedMsg = stringResource(R.string.toast_password_copied, password)
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("WiFi Password", password))
                    Toast.makeText(context, pwdCopiedMsg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_copy_password), style = MaterialTheme.typography.labelLarge)
            }
        }
        ContentType.PHONE -> {
            val phone = rawContent.removePrefix("tel:").removePrefix("TEL:")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_call))
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")))
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_send_sms))
                }
            }
        }
        ContentType.EMAIL -> {
            val email = rawContent.removePrefix("mailto:").removePrefix("MAILTO:").substringBefore("?")
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_send_email), style = MaterialTheme.typography.labelLarge)
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
                Text(stringResource(R.string.action_save_contact), style = MaterialTheme.typography.labelLarge)
            }
        }
        ContentType.GEO -> {
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rawContent)))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_open_in_maps), style = MaterialTheme.typography.labelLarge)
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
                Text(stringResource(R.string.action_search_product), style = MaterialTheme.typography.labelLarge)
            }
        }
        else -> {
            val copiedAllMsg = stringResource(R.string.toast_copied)
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("QR Content", rawContent))
                    Toast.makeText(context, copiedAllMsg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_copy_all), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MetaInfoSection(format: String, scanTime: Instant) {
    val formattedTime = remember(scanTime) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        scanTime.atZone(ZoneId.systemDefault()).format(formatter)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(stringResource(R.string.result_label_scan_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formattedTime, style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(stringResource(R.string.result_label_format), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDisplayName(format), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * 获取内容类型对应的图标。
 */
private fun getTypeIcon(type: ContentType): ImageVector {
    return when (type) {
        ContentType.URL -> Icons.Default.Link
        ContentType.WIFI -> Icons.Default.Wifi
        ContentType.VCARD -> Icons.Default.Person
        ContentType.PHONE -> Icons.Default.Phone
        ContentType.EMAIL -> Icons.Default.Email
        ContentType.SMS -> Icons.Default.Sms
        ContentType.SOCIAL_MEDIA -> Icons.Default.Public
        ContentType.GEO -> Icons.Default.LocationOn
        ContentType.PRODUCT -> Icons.Default.ShoppingCart
        ContentType.PLAIN_TEXT -> Icons.Default.TextFields
    }
}

/**
 * 获取内容类型对应的标签资源 ID。
 */
private fun getTypeLabelRes(type: ContentType): Int {
    return when (type) {
        ContentType.URL -> R.string.content_type_url
        ContentType.WIFI -> R.string.content_type_wifi
        ContentType.VCARD -> R.string.content_type_contact
        ContentType.PHONE -> R.string.content_type_phone
        ContentType.EMAIL -> R.string.content_type_email
        ContentType.SMS -> R.string.content_type_sms
        ContentType.SOCIAL_MEDIA -> R.string.content_type_social
        ContentType.GEO -> R.string.content_type_geo
        ContentType.PRODUCT -> R.string.content_type_product
        ContentType.PLAIN_TEXT -> R.string.content_type_text
    }
}

/**
 * 格式化条码格式名称用于显示（品牌名无需翻译）。
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
