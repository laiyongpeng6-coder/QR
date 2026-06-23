package com.qrscanfast.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanfast.core.domain.ads.ListAdInserter
import com.qrscanfast.core.domain.ads.ListItem
import com.qrscanfast.core.domain.model.HistoryRecord
import com.qrscanfast.core.domain.model.RecordSource
import com.qrscanfast.core.domain.repository.HistoryRepository
import com.qrscanfast.core.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 历史记录页面的状态与业务编排层。
 *
 * ## AI 交接
 * - 职责：聚合历史列表、搜索、分组过滤、删除和收藏逻辑。
 * - 当前状态：已支持响应式查询和按来源切换，已集成广告插入。
 * - 依赖：`HistoryRepository`、`SubscriptionRepository`、`ListAdInserter`、`HistoryRecord`、`HistoryTab`。
 * - 安全修改范围：搜索策略、过滤条件、撤销删除、状态流。
 * - 风险 / TODO：搜索和列表量大时要注意性能与分页。
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    companion object {
        /** Number of content items between each ad slot in the history list. */
        private const val AD_INTERVAL = 5
    }

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

    /**
     * 混合列表 — 在非 Premium 用户的历史记录中按间隔插入广告位。
     * Premium 用户直接获取纯内容列表（无 AdSlot）。
     */
    val mixedItems: StateFlow<List<ListItem<HistoryRecord>>> = combine(
        records,
        subscriptionRepository.isPremium
    ) { recordList, isPremium ->
        if (isPremium) {
            // Premium users: no ads, just wrap in Content
            recordList.map { ListItem.Content(it) }
        } else {
            // Free users: insert ad slots at regular intervals
            ListAdInserter.insertAds(recordList, AD_INTERVAL)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
 * 历史记录筛选 Tab。
 *
 * ## AI 交接
 * - 职责：区分扫描历史与生成历史。
 * - 当前状态：与 `HistoryScreen` 和 `HistoryViewModel` 共同使用。
 * - 安全修改范围：Tab 文案、映射规则、枚举顺序。
 * - 风险 / TODO：新增来源类型时要同步 UI 和查询逻辑。
 */
enum class HistoryTab(val labelRes: Int) {
    SCAN(com.qrscanfast.feature.history.R.string.history_tab_scan),
    GENERATED(com.qrscanfast.feature.history.R.string.history_tab_generated);

    fun toRecordSource(): RecordSource = when (this) {
        SCAN -> RecordSource.SCAN
        GENERATED -> RecordSource.GENERATED
    }
}
