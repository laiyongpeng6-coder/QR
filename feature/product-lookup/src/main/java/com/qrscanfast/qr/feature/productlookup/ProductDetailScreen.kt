package com.qrscanfast.qr.feature.productlookup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanfast.qr.core.ui.components.QrMaxLoadingIndicator

/**
 * 商品详情界面 — 显示产品名称、描述、类别。
 *
 * ## 给其他 AI 开发者的说明
 * 从 ScanResultScreen 导航过来（当条码为 EAN/UPC 产品码时）。
 * TODO [FUTURE-MONETIZATION]: 添加比价面板和 AI 深度分析（Pro 功能）。
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
