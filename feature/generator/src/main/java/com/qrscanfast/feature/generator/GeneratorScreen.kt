package com.qrscanfast.feature.generator

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 创建 Tab 的入口页面。
 *
 * ## AI 交接
 * - 职责：承接“创建二维码 / 创建条码”两个分流入口。
 * - 当前状态：功能完整，但布局偏基础，需要持续优化视觉层次。
 * - 依赖：`GeneratorViewModel`、`core/ui` 通用按钮与卡片。
 * - 安全修改范围：入口布局、卡片样式、引导文案、空状态。
 * - 风险 / TODO：后续如果加 Pro 能力，入口态需要重排。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onCreateQrCode: () -> Unit = {},
    onCreateBarcode: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onVipClick: () -> Unit = {},
    isPremium: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (!isPremium) onVipClick() },
                        enabled = !isPremium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "VIP",
                            tint = if (isPremium) Color(0xFF2DB89A) else Color(0xFF888888)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.create_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 创建二维码卡片
            CreateOptionCard(
                icon = Icons.Default.QrCode2,
                title = stringResource(R.string.create_qrcode_title),
                description = stringResource(R.string.create_qrcode_desc),
                onClick = onCreateQrCode
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 创建条码卡片
            CreateOptionCard(
                icon = Icons.Default.ViewWeek,
                title = stringResource(R.string.create_barcode_title),
                description = stringResource(R.string.create_barcode_desc),
                onClick = onCreateBarcode
            )
        }
    }
}

@Composable
private fun CreateOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
