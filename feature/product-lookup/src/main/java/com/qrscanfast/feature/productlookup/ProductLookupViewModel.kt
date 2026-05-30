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
 * 商品查询界面的 ViewModel。
 *
 * ## 给其他 AI 开发者的说明
 * 接收条码 → 调用 ProductRepository → 管理查询状态。
 * TODO [FUTURE-MONETIZATION]: 比价功能需要 Pro 订阅。
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
