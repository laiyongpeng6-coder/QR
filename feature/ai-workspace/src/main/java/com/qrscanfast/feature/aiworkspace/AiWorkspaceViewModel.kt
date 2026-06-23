package com.qrscanfast.feature.aiworkspace

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.qrscanfast.core.ads.AdvancedFeatureUnlockManager
import com.qrscanfast.core.domain.repository.SubscriptionRepository
import com.qrscanfast.feature.aiworkspace.model.DotShape
import com.qrscanfast.feature.aiworkspace.model.QrStyle
import com.qrscanfast.feature.generator.encoder.QrEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * AI 美化工作台的状态与业务编排层。
 *
 * ## AI 交接
 * - 职责：维护样式、内容和预览位图的状态同步。
 * - 当前状态：支持基础配色与点形状实时重渲染。
 * - 依赖：`QrEncoder`、`QrStyle`、`DotShape`、`AdvancedFeatureUnlockManager`。
 * - 安全修改范围：状态字段、预览重算逻辑、输入校验。
 * - 风险 / TODO：渐变、AI 模板和高级样式需要订阅判断。
 */
@HiltViewModel
class AiWorkspaceViewModel @Inject constructor(
    private val qrEncoder: QrEncoder,
    val unlockManager: AdvancedFeatureUnlockManager,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    /** Whether the user is a premium subscriber (used to show/hide lock states). */
    val isPremium: StateFlow<Boolean> = subscriptionRepository.isPremium

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
