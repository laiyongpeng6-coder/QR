package com.qrscanmax.feature.aiworkspace

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.qrscanmax.feature.aiworkspace.model.DotShape
import com.qrscanmax.feature.aiworkspace.model.QrStyle
import com.qrscanmax.feature.generator.encoder.QrEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * AI 美化工作台的 ViewModel。
 *
 * ## 给其他 AI 开发者的说明
 * 负责持有样式状态，每次变化时重新渲染 QR 码预览。
 * TODO [FUTURE-MONETIZATION]: 渐变色和 AI 模板需要检查订阅状态。
 */
@HiltViewModel
class AiWorkspaceViewModel @Inject constructor(
    private val qrEncoder: QrEncoder
) : ViewModel() {

    private val _style = MutableStateFlow(QrStyle())
    val style: StateFlow<QrStyle> = _style.asStateFlow()

    private val _content = MutableStateFlow("https://example.com")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    init { regeneratePreview() }

    fun setContent(content: String) { _content.value = content; regeneratePreview() }
    fun setForegroundColor(color: Int) { _style.value = _style.value.copy(foregroundColor = color); regeneratePreview() }
    fun setBackgroundColor(color: Int) { _style.value = _style.value.copy(backgroundColor = color); regeneratePreview() }
    fun setDotShape(shape: DotShape) { _style.value = _style.value.copy(dotShape = shape); regeneratePreview() }

    private fun regeneratePreview() {
        try {
            _previewBitmap.value = qrEncoder.encode(
                content = _content.value, size = 512,
                foregroundColor = _style.value.foregroundColor,
                backgroundColor = _style.value.backgroundColor
            )
        } catch (_: Exception) { }
    }
}
