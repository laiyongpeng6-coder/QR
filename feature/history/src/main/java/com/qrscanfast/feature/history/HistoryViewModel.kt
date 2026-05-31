package com.qrscanfast.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanfast.core.domain.model.HistoryRecord
import com.qrscanfast.core.domain.model.RecordSource
import com.qrscanfast.core.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 历史记录界面的 ViewModel。
 *
 * 负责：
 * 1. 从 HistoryRepository 获取历史记录列表（响应式 Flow）
 * 2. 支持搜索过滤
 * 3. 支持按来源 Tab 过滤（扫描记录 / 生成记录）
 * 4. 支持删除和收藏操作
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** 当前选中的 Tab */
    private val _selectedTab = MutableStateFlow(HistoryTab.SCAN)
    val selectedTab: StateFlow<HistoryTab> = _selectedTab.asStateFlow()

    /**
     * 历史记录列表 — 根据搜索词获取数据，再按当前 Tab 的来源过滤。
     */
    val records: StateFlow<List<HistoryRecord>> = combine(
        _searchQuery.debounce(300),
        _selectedTab
    ) { query, tab -> query to tab }
        .flatMapLatest { (query, tab) ->
            val source = flow {
                if (query.isBlank()) {
                    emitAll(historyRepository.getAllRecords())
                } else {
                    emitAll(historyRepository.searchRecords(query))
                }
            }
            source.map { list ->
                list.filter { it.source == tab.toRecordSource() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastDeletedRecord: HistoryRecord? = null

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setSelectedTab(tab: HistoryTab) { _selectedTab.value = tab }

    fun deleteRecord(record: HistoryRecord) {
        lastDeletedRecord = record
        viewModelScope.launch { historyRepository.delete(record.id) }
    }

    fun undoDelete() {
        lastDeletedRecord?.let { record ->
            viewModelScope.launch { historyRepository.insert(record) }
            lastDeletedRecord = null
        }
    }

    fun toggleFavorite(record: HistoryRecord) {
        viewModelScope.launch { historyRepository.updateFavorite(record.id, !record.isFavorite) }
    }
}

/**
 * 历史记录 Tab 枚举。
 */
enum class HistoryTab(val label: String) {
    SCAN("扫描记录"),
    GENERATED("生成记录");

    fun toRecordSource(): RecordSource = when (this) {
        SCAN -> RecordSource.SCAN
        GENERATED -> RecordSource.GENERATED
    }
}
