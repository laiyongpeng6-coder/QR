package com.qrscanfast.feature.aiworkspace

import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanfast.feature.aiworkspace.model.DotShape

/**
 * AI 美化工作台页面。
 *
 * ## AI 交接
 * - 职责：承载 QR 样式编辑、实时预览和未来 Pro 能力入口。
 * - 当前状态：已接入基础样式编辑和预览，布局偏工具页。
 * - 依赖：`AiWorkspaceViewModel`、`QrEncoder`、`core/ui` 组件。
 * - 安全修改范围：页面布局、交互分组、视觉层次、空态/锁定态。
 * - 风险 / TODO：未来模板、渐变、中心 Logo 需要订阅门控。
 */
@Composable
fun AiWorkspaceScreen(viewModel: AiWorkspaceViewModel = hiltViewModel()) {
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val style by viewModel.style.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Beautify QR Code", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // QR 码预览
        Card(modifier = Modifier.size(240.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            previewBitmap?.let { bitmap ->
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Preview",
                    modifier = Modifier.fillMaxSize().padding(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 前景色
        Text("Foreground Color", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        ColorPalette(selectedColor = style.foregroundColor, onColorSelected = viewModel::setForegroundColor)

        Spacer(modifier = Modifier.height(16.dp))

        // 背景色
        Text("Background Color", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        ColorPalette(selectedColor = style.backgroundColor, onColorSelected = viewModel::setBackgroundColor)

        Spacer(modifier = Modifier.height(16.dp))

        // 点形状
        Text("Dot Shape", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DotShape.entries.forEach { shape ->
                FilterChip(selected = style.dotShape == shape, onClick = { viewModel.setDotShape(shape) },
                    label = { Text(shape.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { /* TODO: 保存 */ }) { Text("Save") }
            OutlinedButton(onClick = { /* TODO: 分享 */ }) { Text("Share") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // TODO [FUTURE-MONETIZATION]: Coming Soon 锁定区域
        Card(modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83D\uDD12 Gradient & AI Templates", fontWeight = FontWeight.Medium)
                Text("Coming Soon with Pro", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ColorPalette(selectedColor: Int, onColorSelected: (Int) -> Unit) {
    val colors = listOf(Color.BLACK, Color.WHITE, Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA,
        Color.CYAN, Color.DKGRAY, 0xFF6200EE.toInt(), 0xFF03DAC5.toInt(), 0xFFFF5722.toInt(), 0xFF795548.toInt())

    LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.fillMaxWidth().height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(colors) { color ->
            Box(modifier = Modifier.size(32.dp).clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color(color))
                .border(if (color == selectedColor) 3.dp else 1.dp,
                    if (color == selectedColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape)
                .clickable { onColorSelected(color) })
        }
    }
}
