package com.qrscanfast.feature.history

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanfast.core.common.DateFormatUtils
import com.qrscanfast.core.domain.model.HistoryRecord

/**
 * 历史记录页面。
 *
 * ## AI 交接
 * - 职责：展示扫描与生成历史，支持搜索、删除和收藏。
 * - 当前状态：功能完整，但列表层级和空态还可以继续优化。
 * - 依赖：`HistoryViewModel`、`DateFormatUtils`、`core/ui` 组件。
 * - 安全修改范围：列表布局、搜索区、分组标题、空态与操作反馈。
 * - 风险 / TODO：删除撤销、分组顺序和性能优化要一起考虑。
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onItemClick: (HistoryRecord) -> Unit = {}
) {
    val records by viewModel.records.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 顶部 Tab：扫描记录 / 生成记录
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                HistoryTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.setSelectedTab(tab) },
                        text = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }

            // 搜索栏
            OutlinedTextField(
                value = searchQuery, onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.history_search_hint)) }, singleLine = true
            )

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            searchQuery.isNotBlank() -> stringResource(R.string.history_no_results)
                            selectedTab == HistoryTab.SCAN -> stringResource(R.string.history_empty_scan)
                            else -> stringResource(R.string.history_empty_generated)
                        },
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
                Text("${record.contentType.name} · ${DateFormatUtils.formatRelativeTime(record.timestamp)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (record.isFavorite) Icons.Filled.Star else Icons.Default.StarBorder,
                    contentDescription = stringResource(R.string.history_favorite),
                    tint = if (record.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.history_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
