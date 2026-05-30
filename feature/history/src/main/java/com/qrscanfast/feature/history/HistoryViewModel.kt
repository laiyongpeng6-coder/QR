package com.qrscanfast.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanfast.core.domain.model.HistoryRecord
import com.qrscanfast.core.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 历史记录界面的 ViewModel。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本 ViewModel 负责：
 * 1. 从 HistoryRepository 获取历史记录列表（响应式 Flow）
 * 2. 支持搜索过滤（按内容文本匹配）
 * 3. 支持删除和收藏操作
 * 4. 提供撤销删除功能（5 秒窗口）
 *
 * ## 数据流
 * HistoryRepository.getAllRecords() → Flow → UI 渲染
 * 搜索时切换为 searchRecords(query) 的 Flow
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** 历史记录列表（根据搜索关键词自动切换数据源，防抖 300ms） */
    val records: StateFlow<List<HistoryRecord>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) historyRepository.getAllRecords()
            else historyRepository.searchRecords(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastDeletedRecord: HistoryRecord? = null

    fun setSearchQuery(query: String) { _searchQuery.value = query }

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
