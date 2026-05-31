package com.qrscanfast.feature.generator

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.qrscanfast.core.common.AnalyticsHelper
import com.qrscanfast.core.domain.model.ContentType
import com.qrscanfast.core.domain.model.HistoryRecord
import com.qrscanfast.core.domain.model.RecordSource
import com.qrscanfast.core.domain.repository.HistoryRepository
import com.qrscanfast.feature.generator.encoder.QrEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * 码生成器的 ViewModel，管理输入状态、验证和生成逻辑。
 * 支持 QR Code 和常见条码格式（EAN-13、Code 128、EAN-8、UPC-A）。
 */
@HiltViewModel
class GeneratorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val qrEncoder: QrEncoder,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GeneratorUiState>(GeneratorUiState.Input)
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    private val _inputType = MutableStateFlow(GeneratorInputType.PLAIN_TEXT)
    val inputType: StateFlow<GeneratorInputType> = _inputType.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _selectedResolution = MutableStateFlow(512)
    val selectedResolution: StateFlow<Int> = _selectedResolution.asStateFlow()

    fun setInputType(type: GeneratorInputType) {
        _inputType.value = type
        _content.value = ""
        _uiState.value = GeneratorUiState.Input
    }

    fun setContent(text: String) {
        _content.value = text
        if (_uiState.value is GeneratorUiState.Error) {
            _uiState.value = GeneratorUiState.Input
        }
    }

    fun setResolution(resolution: Int) {
        _selectedResolution.value = resolution
    }

    /**
     * 生成码。验证 → 编码 → 保存历史。
     */
    fun generate() {
        val currentContent = _content.value.trim()
        val type = _inputType.value

        if (currentContent.isEmpty()) {
            _uiState.value = GeneratorUiState.Error(context.getString(R.string.qr_error_empty))
            return
        }

        // 根据类型验证内容格式
        val validationError = validateContent(currentContent, type)
        if (validationError != null) {
            _uiState.value = GeneratorUiState.Error(validationError)
            return
        }

        val encodedContent = formatContent(currentContent, type)

        // QR Code 容量检查
        if (type.isQrCode && !qrEncoder.isWithinCapacity(encodedContent)) {
            _uiState.value = GeneratorUiState.Error(context.getString(R.string.qr_error_too_large))
            return
        }

        _uiState.value = GeneratorUiState.Generating
        try {
            val bitmap = if (type.isQrCode) {
                qrEncoder.encode(content = encodedContent, size = _selectedResolution.value)
            } else {
                // 条码：宽度用分辨率，高度为宽度的 40%
                val width = _selectedResolution.value
                val height = (width * 0.4f).toInt()
                qrEncoder.encodeWithFormat(
                    content = encodedContent,
                    format = type.toBarcodeFormat(),
                    width = width,
                    height = height
                )
            }
            _uiState.value = GeneratorUiState.Generated(bitmap, encodedContent)

            // 埋点：生成码
            if (type.isQrCode) {
                AnalyticsHelper.logQrCodeGenerate(type.name)
            } else {
                AnalyticsHelper.logBarcodeGenerate(type.name)
            }

            viewModelScope.launch {
                historyRepository.insert(
                    HistoryRecord(
                        contentType = mapInputTypeToContentType(type),
                        rawContent = encodedContent,
                        displayTitle = currentContent.take(50),
                        timestamp = Instant.now(),
                        source = RecordSource.GENERATED
                    )
                )
            }
        } catch (e: Exception) {
            _uiState.value = GeneratorUiState.Error(context.getString(R.string.barcode_error_generate, e.message ?: ""))
        }
    }

    fun resetToInput() {
        _uiState.value = GeneratorUiState.Input
    }

    /**
     * 验证内容格式是否符合所选类型要求。
     * @return 错误信息，null 表示验证通过
     */
    private fun validateContent(content: String, type: GeneratorInputType): String? {
        return when (type) {
            GeneratorInputType.BARCODE_EAN13 -> {
                if (!qrEncoder.isValidEan13(content)) context.getString(R.string.barcode_error_ean13) else null
            }
            GeneratorInputType.BARCODE_EAN8 -> {
                if (!qrEncoder.isValidEan8(content)) context.getString(R.string.barcode_error_ean8) else null
            }
            GeneratorInputType.BARCODE_UPC_A -> {
                if (!qrEncoder.isValidUpcA(content)) context.getString(R.string.barcode_error_upca) else null
            }
            GeneratorInputType.BARCODE_CODE128 -> {
                if (!qrEncoder.isValidCode128(content)) context.getString(R.string.barcode_error_empty) else null
            }
            else -> null
        }
    }

    private fun formatContent(content: String, type: GeneratorInputType): String {
        return when (type) {
            GeneratorInputType.URL -> {
                if (!content.startsWith("http://") && !content.startsWith("https://")) "https://$content" else content
            }
            GeneratorInputType.PHONE -> "tel:$content"
            else -> content
        }
    }

    private fun mapInputTypeToContentType(type: GeneratorInputType): ContentType {
        return when (type) {
            GeneratorInputType.PLAIN_TEXT -> ContentType.PLAIN_TEXT
            GeneratorInputType.URL -> ContentType.URL
            GeneratorInputType.WIFI -> ContentType.WIFI
            GeneratorInputType.CONTACT -> ContentType.VCARD
            GeneratorInputType.PHONE -> ContentType.PHONE
            GeneratorInputType.SOCIAL_MEDIA -> ContentType.SOCIAL_MEDIA
            GeneratorInputType.BARCODE_EAN13,
            GeneratorInputType.BARCODE_EAN8,
            GeneratorInputType.BARCODE_UPC_A,
            GeneratorInputType.BARCODE_CODE128 -> ContentType.PRODUCT
        }
    }
}

/**
 * 生成器支持的输入类型枚举。
 */
enum class GeneratorInputType {
    // QR Code 类型
    PLAIN_TEXT, URL, WIFI, CONTACT, PHONE, SOCIAL_MEDIA,
    // 条码类型
    BARCODE_EAN13, BARCODE_EAN8, BARCODE_UPC_A, BARCODE_CODE128;

    /** 是否为 QR Code 类型（非条码） */
    val isQrCode: Boolean get() = this in listOf(PLAIN_TEXT, URL, WIFI, CONTACT, PHONE, SOCIAL_MEDIA)

    /** 转换为 ZXing BarcodeFormat */
    fun toBarcodeFormat(): BarcodeFormat = when (this) {
        BARCODE_EAN13 -> BarcodeFormat.EAN_13
        BARCODE_EAN8 -> BarcodeFormat.EAN_8
        BARCODE_UPC_A -> BarcodeFormat.UPC_A
        BARCODE_CODE128 -> BarcodeFormat.CODE_128
        else -> BarcodeFormat.QR_CODE
    }
}

/**
 * 生成器 UI 状态密封类。
 */
sealed class GeneratorUiState {
    data object Input : GeneratorUiState()
    data object Generating : GeneratorUiState()
    data class Generated(val bitmap: Bitmap, val content: String) : GeneratorUiState()
    data class Error(val message: String) : GeneratorUiState()
}
