package com.qrscanfast.qr.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanfast.qr.core.common.DateFormatUtils
import com.qrscanfast.qr.core.domain.model.HistoryRecord

/**
 * 历史记录主界面 — 按日期分组的时间线列表 + 搜索 + 操作按钮。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本界面包含：
 * 1. 顶部搜索栏
 * 2. 按日期分组的 LazyColumn 列表
 * 3. 每条记录支持点击查看详情、删除、收藏
 *
 * ## 后续开发
 * - 滑动手势需要使用 SwipeToDismiss API（左滑删除、右滑收藏）
 * - 点击记录应导航到 HistoryDetailScreen
 * - TODO [FUTURE-MONETIZATION]: 每 4-5 条记录后插入原生信息流广告
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onItemClick: (HistoryRecord) -> Unit = {}
) {
    val records by viewModel.records.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery, onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search history...") }, singleLine = true
            )

            // TODO [FUTURE-MONETIZATION]: Pro 会员 Banner 广告位

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) "No scan history yet" else "No results found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val groupedRecords = records.groupBy { DateFormatUtils.toLocalDate(it.timestamp) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    groupedRecords.forEach { (_, recordsInGroup) ->
                        item {
                            Text(
                                text = DateFormatUtils.formatDateHeader(recordsInGroup.first().timestamp),
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(recordsInGroup, key = { it.id }) { record ->
                            HistoryRecordItem(
                                record = record, onClick = { onItemClick(record) },
                                onDelete = { viewModel.deleteRecord(record) },
                                onToggleFavorite = { viewModel.toggleFavorite(record) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordItem(
    record: HistoryRecord, onClick: () -> Unit,
    onDelete: () -> Unit, onToggleFavorite: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.displayTitle, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${record.contentType.name} • ${DateFormatUtils.formatRelativeTime(record.timestamp)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (record.isFavorite) Icons.Filled.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (record.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
