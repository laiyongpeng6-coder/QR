package com.qrscanfast.feature.productlookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanfast.core.domain.model.ProductInfo
import com.qrscanfast.core.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 商品查询页面的状态与业务编排层。
 *
 * ## AI 交接
 * - 职责：接收条码并驱动仓库查询状态。
 * - 当前状态：支持 Idle / Loading / Found / NotFound。
 * - 依赖：`ProductRepository`、`ProductInfo`。
 * - 安全修改范围：查询触发、状态流、错误处理。
 * - 风险 / TODO：比价与深度分析未来需要订阅判断。
 */
@HiltViewModel
class ProductLookupViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductLookupUiState>(ProductLookupUiState.Idle)
    val uiState: StateFlow<ProductLookupUiState> = _uiState.asStateFlow()

    fun lookupProduct(barcode: String) {
        _uiState.value = ProductLookupUiState.Loading
        viewModelScope.launch {
            productRepository.lookupProduct(barcode)
                .onSuccess { _uiState.value = ProductLookupUiState.Found(it) }
                .onFailure { _uiState.value = ProductLookupUiState.NotFound(barcode, it.message) }
        }
    }
}

sealed class ProductLookupUiState {
    data object Idle : ProductLookupUiState()
    data object Loading : ProductLookupUiState()
    data class Found(val product: ProductInfo) : ProductLookupUiState()
    data class NotFound(val barcode: String, val reason: String?) : ProductLookupUiState()
}
