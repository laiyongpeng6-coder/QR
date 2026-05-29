package com.qrscanmax.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanmax.core.domain.model.BarcodeFormat
import com.qrscanmax.core.domain.model.ContentType
import com.qrscanmax.core.domain.model.HistoryRecord
import com.qrscanmax.core.domain.model.RecordSource
import com.qrscanmax.core.domain.model.ScanResult
import com.qrscanmax.core.domain.repository.HistoryRepository
import com.qrscanmax.core.domain.usecase.ResultMapperUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * 扫描器界面的 ViewModel，管理扫描状态和业务逻辑。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本 ViewModel 负责：
 * 1. 管理 UI 状态（扫描中 / 已检测到结果 / 权限被拒绝）
 * 2. 接收 ML Kit 的扫描结果，通过 ResultMapperUseCase 分类内容类型
 * 3. 自动将扫描结果保存到历史记录
 *
 * ## 状态流转
 * - 初始状态：Scanning（相机预览活跃，等待检测）
 * - 检测到条码：ResultDetected（显示结果面板）
 * - 用户关闭结果：回到 Scanning
 * - 权限被拒绝：PermissionDenied（显示权限说明）
 *
 * ## 与 UI 层的交互
 * ScannerScreen 通过 collectAsState 观察 [uiState]，
 * 并在 ML Kit 回调中调用 [onBarcodeDetected]。
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val resultMapper: ResultMapperUseCase,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    /** 当前扫描器 UI 状态 */
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /** 是否暂停扫描（检测到结果后暂停，避免重复触发） */
    private var isPaused = false

    /**
     * ML Kit 检测到条码时调用此方法。
     *
     * 会自动：
     * 1. 分类内容类型（URL/WiFi/vCard 等）
     * 2. 创建 ScanResult 对象
     * 3. 保存到历史记录
     * 4. 更新 UI 状态为 ResultDetected
     *
     * @param rawValue 条码的原始解码字符串
     * @param format 条码格式（QR_CODE、EAN_13 等）
     */
    fun onBarcodeDetected(rawValue: String, format: BarcodeFormat) {
        // 如果已暂停（正在显示结果），忽略新的检测
        if (isPaused) return
        isPaused = true

        val contentType = resultMapper.classify(rawValue)
        val scanResult = ScanResult(
            rawValue = rawValue,
            format = format,
            contentType = contentType,
            timestamp = Instant.now()
        )

        // 更新 UI 状态
        _uiState.value = ScannerUiState.ResultDetected(scanResult)

        // 异步保存到历史记录
        viewModelScope.launch {
            val record = HistoryRecord(
                contentType = contentType,
                rawContent = rawValue,
                displayTitle = generateDisplayTitle(rawValue, contentType),
                timestamp = Instant.now(),
                source = RecordSource.SCAN
            )
            historyRepository.insert(record)
        }
    }

    /**
     * 用户关闭结果面板，恢复扫描状态。
     */
    fun resumeScanning() {
        isPaused = false
        _uiState.value = ScannerUiState.Scanning
    }

    /**
     * 相机权限被拒绝时调用。
     */
    fun onPermissionDenied() {
        _uiState.value = ScannerUiState.PermissionDenied
    }

    /**
     * 权限被授予后恢复扫描。
     */
    fun onPermissionGranted() {
        _uiState.value = ScannerUiState.Scanning
    }

    /**
     * 根据内容类型生成人类可读的显示标题。
     *
     * 例如：
     * - URL → 提取域名 "example.com"
     * - WiFi → 提取 SSID
     * - Phone → 显示号码
     * - 其他 → 截取前 50 个字符
     */
    private fun generateDisplayTitle(rawValue: String, contentType: ContentType): String {
        return when (contentType) {
            ContentType.URL -> {
                rawValue.removePrefix("https://").removePrefix("http://")
                    .substringBefore("/").take(50)
            }
            ContentType.WIFI -> {
                val ssidMatch = Regex("S:([^;]+)").find(rawValue)
                ssidMatch?.groupValues?.get(1) ?: "WiFi Network"
            }
            ContentType.PHONE -> {
                rawValue.removePrefix("tel:").take(20)
            }
            ContentType.VCARD -> {
                val nameMatch = Regex("FN:(.+)").find(rawValue)
                nameMatch?.groupValues?.get(1) ?: "Contact"
            }
            ContentType.EMAIL -> {
                rawValue.removePrefix("mailto:").substringBefore("?").take(50)
            }
            else -> rawValue.take(50)
        }
    }
}

/**
 * 扫描器界面的 UI 状态密封类。
 *
 * ## 给其他 AI 开发者的说明
 * 在 ScannerScreen 中使用 when 表达式匹配这些状态来渲染不同的 UI。
 */
sealed class ScannerUiState {
    /** 正在扫描中，相机预览活跃 */
    data object Scanning : ScannerUiState()

    /** 已检测到扫描结果 */
    data class ResultDetected(val result: ScanResult) : ScannerUiState()

    /** 相机权限被拒绝 */
    data object PermissionDenied : ScannerUiState()
}
