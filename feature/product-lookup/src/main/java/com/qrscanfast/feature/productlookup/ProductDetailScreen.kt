package com.qrscanfast.feature.productlookup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanfast.core.ui.components.QrMaxLoadingIndicator

/**
 * 商品详情页面。
 *
 * ## AI 交接
 * - 职责：展示商品查询结果，并承接进一步分析入口。
 * - 当前状态：已能显示基础商品信息，布局仍偏轻量。
 * - 依赖：`ProductLookupViewModel`、`core/ui` 加载态组件。
 * - 安全修改范围：结果布局、空态、错误态、Pro 入口。
 * - 风险 / TODO：比价面板和 AI 深度分析未来需要订阅门控。
 */
@Composable
fun ProductDetailScreen(
    barcode: String,
    viewModel: ProductLookupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(barcode) { viewModel.lookupProduct(barcode) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when (val state = uiState) {
            is ProductLookupUiState.Idle, is ProductLookupUiState.Loading -> {
                Spacer(modifier = Modifier.height(100.dp))
                QrMaxLoadingIndicator(text = "Looking up product...")
            }
            is ProductLookupUiState.Found -> {
                Text(state.product.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                state.product.category?.let { AssistChip(onClick = {}, label = { Text(it) }); Spacer(modifier = Modifier.height(16.dp)) }
                state.product.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge); Spacer(modifier = Modifier.height(16.dp)) }
                Text("Barcode: ${state.product.barcode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // TODO [FUTURE-MONETIZATION]: 比价面板 + AI 深度分析
            }
            is ProductLookupUiState.NotFound -> {
                Spacer(modifier = Modifier.height(80.dp))
                Text("Product Not Found", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Barcode: ${state.barcode}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.reason ?: "No product info available", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}
