package com.qrscanfast.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanfast.core.ads.ui.NativeCardAd
import com.qrscanfast.core.common.DateFormatUtils
import com.qrscanfast.core.domain.ads.AdManager
import com.qrscanfast.core.domain.ads.ListItem
import com.qrscanfast.core.domain.model.AdPlacement
import com.qrscanfast.core.domain.model.HistoryRecord

/**
 * 历史记录页面。
 *
 * ## AI 交接
 * - 职责：展示扫描与生成历史，支持搜索、删除和收藏，列表中按间隔插入原生广告。
 * - 当前状态：功能完整，已集成 NATIVE_HISTORY_LIST 场景广告。
 * - 依赖：`HistoryViewModel`、`DateFormatUtils`、`core/ui` 组件、`core/ads` NativeCardAd。
 * - 安全修改范围：列表布局、搜索区、分组标题、空态与操作反馈。
 * - 风险 / TODO：删除撤销、分组顺序和性能优化要一起考虑。
 */
@Composable
fun HistoryScreen(
    adManager: AdManager,
    viewModel: HistoryViewModel = hiltViewModel(),
    onItemClick: (HistoryRecord) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onVipClick: () -> Unit = {},
    isPremium: Boolean = false
) {
    val mixedItems by viewModel.mixedItems.collectAsState()
    val records by viewModel.records.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HistoryTopBar(
                onSettingsClick = onSettingsClick,
                onVipClick = onVipClick,
                isPremium = isPremium
            )
        }
    ) { padding ->
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(
                        items = mixedItems,
                        key = { item ->
                            when (item) {
                                is ListItem.Content -> "content_${item.data.id}"
                                is ListItem.AdSlot -> "ad_${mixedItems.indexOf(item)}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is ListItem.Content -> {
                                HistoryRecordItem(
                                    record = item.data,
                                    onClick = { onItemClick(item.data) },
                                    onDelete = { viewModel.deleteRecord(item.data) },
                                    onToggleFavorite = { viewModel.toggleFavorite(item.data) }
                                )
                            }
                            is ListItem.AdSlot -> {
                                // Render native ad; hides automatically on failure
                                NativeCardAd(
                                    placement = AdPlacement.NATIVE_HISTORY_LIST,
                                    adManager = adManager,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
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

/**
 * 历史页顶部栏：左侧设置入口，右侧 VIP 入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(
    onSettingsClick: () -> Unit,
    onVipClick: () -> Unit,
    isPremium: Boolean
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
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
