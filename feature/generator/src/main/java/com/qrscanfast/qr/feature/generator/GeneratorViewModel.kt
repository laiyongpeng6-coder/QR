package com.qrscanfast.qr.feature.generator

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanfast.qr.core.domain.model.ContentType
import com.qrscanfast.qr.core.domain.model.HistoryRecord
import com.qrscanfast.qr.core.domain.model.RecordSource
import com.qrscanfast.qr.core.domain.repository.HistoryRepository
import com.qrscanfast.qr.feature.generator.encoder.QrEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * QR 码生成器的 ViewModel，管理输入状态、验证和生成逻辑。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本 ViewModel 负责：
 * 1. 管理用户输入的内容和选择的输入类型
 * 2. 验证输入数据的合法性
 * 3. 调用 QrEncoder 生成 QR 码位图
 * 4. 将生成的 QR 码保存到历史记录
 *
 * ## 输入类型
 * 支持 6 种：纯文本、URL、WiFi、联系人、电话、社交媒体
 *
 * ## 状态流转
 * Input → Generating → Generated（显示预览）
 *       → Error（验证失败或容量超出）
 */
@HiltViewModel
class GeneratorViewModel @Inject constructor(
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
     * 生成 QR 码。验证 → 编码 → 保存历史。
     */
    fun generate() {
        val currentContent = _content.value.trim()

        if (currentContent.isEmpty()) {
            _uiState.value = GeneratorUiState.Error("Please enter content")
            return
        }

        val encodedContent = formatContent(currentContent, _inputType.value)

        if (!qrEncoder.isWithinCapacity(encodedContent)) {
            _uiState.value = GeneratorUiState.Error("Content too large for QR code")
            return
        }

        _uiState.value = GeneratorUiState.Generating
        try {
            val bitmap = qrEncoder.encode(content = encodedContent, size = _selectedResolution.value)
            _uiState.value = GeneratorUiState.Generated(bitmap, encodedContent)

            viewModelScope.launch {
                historyRepository.insert(
                    HistoryRecord(
                        contentType = mapInputTypeToContentType(_inputType.value),
                        rawContent = encodedContent,
                        displayTitle = currentContent.take(50),
                        timestamp = Instant.now(),
                        source = RecordSource.GENERATED
                    )
                )
            }
        } catch (e: Exception) {
            _uiState.value = GeneratorUiState.Error("Generation failed: ${e.message}")
        }
    }

    fun resetToInput() {
        _uiState.value = GeneratorUiState.Input
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
        }
    }
}

/**
 * 生成器支持的输入类型枚举。
 */
enum class GeneratorInputType {
    PLAIN_TEXT, URL, WIFI, CONTACT, PHONE, SOCIAL_MEDIA
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
