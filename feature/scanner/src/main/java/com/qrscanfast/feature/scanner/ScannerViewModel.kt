package com.qrscanfast.feature.scanner

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanfast.core.common.AnalyticsHelper
import com.qrscanfast.core.data.datastore.AppSettings
import com.qrscanfast.core.domain.model.BarcodeFormat
import com.qrscanfast.core.domain.model.ContentType
import com.qrscanfast.core.domain.model.HistoryRecord
import com.qrscanfast.core.domain.model.RecordSource
import com.qrscanfast.core.domain.model.ScanResult
import com.qrscanfast.core.domain.repository.HistoryRepository
import com.qrscanfast.core.domain.usecase.ResultMapperUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * 扫描器界面的 ViewModel，管理扫描状态和业务逻辑。
 *
 * 负责：
 * 1. 管理 UI 状态（扫描中 / 已检测到结果 / 权限被拒绝）
 * 2. 接收 ML Kit 的扫描结果，分类内容类型
 * 3. 根据设置触发震动反馈
 * 4. 自动保存扫描结果到历史记录
 * 5. 暴露"自动跳转网页"设置状态供 UI 判断
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resultMapper: ResultMapperUseCase,
    private val historyRepository: HistoryRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /** 是否启用自动跳转网页（来自设置，供 UI 判断是否直接打开浏览器） */
    val autoOpenUrl: StateFlow<Boolean> = appSettings.autoOpenUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 是否暂停扫描（检测到结果后暂停，避免重复触发） */
    private var isPaused = false

    /**
     * ML Kit 检测到条码时调用。
     */
    fun onBarcodeDetected(rawValue: String, format: BarcodeFormat) {
        if (isPaused) return
        isPaused = true

        val contentType = resultMapper.classify(rawValue, format)
        val scanResult = ScanResult(
            rawValue = rawValue,
            format = format,
            contentType = contentType,
            timestamp = Instant.now()
        )

        _uiState.value = ScannerUiState.ResultDetected(scanResult)

        // 埋点：扫描完成
        AnalyticsHelper.logScanComplete(contentType.name, format.name)

        // 根据设置触发震动反馈
        viewModelScope.launch {
            try {
                val shouldVibrate = appSettings.vibrateOnScan.first()
                if (shouldVibrate) {
                    triggerVibration()
                }
            } catch (_: Exception) {
                // 忽略震动失败
            }
        }

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
     * 触发一次短促震动（100ms）。
     */
    private fun triggerVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(100)
            }
        }
    }

    fun resumeScanning() {
        isPaused = false
        _uiState.value = ScannerUiState.Scanning
    }

    fun onPermissionDenied() {
        _uiState.value = ScannerUiState.PermissionDenied
    }

    fun onPermissionGranted() {
        _uiState.value = ScannerUiState.Scanning
    }

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
 */
sealed class ScannerUiState {
    data object Scanning : ScannerUiState()
    data class ResultDetected(val result: ScanResult) : ScannerUiState()
    data object PermissionDenied : ScannerUiState()
}
