package com.qrscanmax.feature.generator

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanmax.core.ui.components.QrMaxLoadingIndicator
import com.qrscanmax.core.ui.components.QrMaxPrimaryButton

/**
 * QR 码生成器主界面 — 输入内容 + 类型选择 + 生成预览。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本界面包含两个主要状态：
 * 1. 输入状态：显示类型选择 Tab + 输入框 + 生成按钮
 * 2. 预览状态：显示生成的 QR 码 + 保存/分享/美化按钮
 *
 * ## 后续开发
 * - WiFi 类型需要多字段表单（SSID、密码、加密方式）
 * - 联系人类型需要多字段表单（姓名、电话、邮箱等）
 * - 美化按钮应导航到 AI Workspace 模块
 * - 保存功能需要使用 MediaStore API
 */
@Composable
fun GeneratorScreen(viewModel: GeneratorViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val inputType by viewModel.inputType.collectAsState()
    val content by viewModel.content.collectAsState()
    val resolution by viewModel.selectedResolution.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState) {
            is GeneratorUiState.Input, is GeneratorUiState.Error -> {
                GeneratorInputContent(
                    inputType = inputType, content = content, resolution = resolution,
                    errorMessage = (uiState as? GeneratorUiState.Error)?.message,
                    onTypeChanged = viewModel::setInputType, onContentChanged = viewModel::setContent,
                    onResolutionChanged = viewModel::setResolution, onGenerate = viewModel::generate
                )
            }
            is GeneratorUiState.Generating -> {
                Spacer(modifier = Modifier.height(100.dp))
                QrMaxLoadingIndicator(text = "Generating QR Code...")
            }
            is GeneratorUiState.Generated -> {
                GeneratorPreviewContent(
                    bitmap = (uiState as GeneratorUiState.Generated).bitmap,
                    onNewCode = viewModel::resetToInput
                )
            }
        }
    }
}

@Composable
private fun GeneratorInputContent(
    inputType: GeneratorInputType, content: String, resolution: Int,
    errorMessage: String?, onTypeChanged: (GeneratorInputType) -> Unit,
    onContentChanged: (String) -> Unit, onResolutionChanged: (Int) -> Unit, onGenerate: () -> Unit
) {
    Text("Create QR Code", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))

    // 输入类型选择
    val types = GeneratorInputType.entries
    val typeLabels = listOf("Text", "URL", "WiFi", "Contact", "Phone", "Social")
    ScrollableTabRow(selectedTabIndex = types.indexOf(inputType), modifier = Modifier.fillMaxWidth()) {
        types.forEachIndexed { index, type ->
            Tab(selected = inputType == type, onClick = { onTypeChanged(type) }, text = { Text(typeLabels[index]) })
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 输入框
    OutlinedTextField(
        value = content, onValueChange = onContentChanged, modifier = Modifier.fillMaxWidth(),
        label = { Text(getInputHint(inputType)) }, isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        minLines = 3, maxLines = 5
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 分辨率选择
    Text("Resolution", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(256, 512, 1024).forEach { res ->
            FilterChip(selected = resolution == res, onClick = { onResolutionChanged(res) }, label = { Text("${res}px") })
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    QrMaxPrimaryButton(text = "Generate", onClick = onGenerate, enabled = content.isNotBlank())
}

@Composable
private fun GeneratorPreviewContent(bitmap: Bitmap, onNewCode: () -> Unit) {
    Text("Your QR Code", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))

    Card(modifier = Modifier.size(280.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Generated QR Code",
            modifier = Modifier.fillMaxSize().padding(16.dp))
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { /* TODO [FUTURE]: MediaStore 保存 */ }) { Text("Save") }
        OutlinedButton(onClick = { /* TODO [FUTURE]: ShareIntentBuilder 分享 */ }) { Text("Share") }
        Button(onClick = { /* TODO [FUTURE]: 导航到 AI Workspace; TODO [FUTURE-MONETIZATION]: 订阅检查 */ }) { Text("Beautify") }
    }

    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onNewCode) { Text("Create New Code") }
}

private fun getInputHint(type: GeneratorInputType): String = when (type) {
    GeneratorInputType.PLAIN_TEXT -> "Enter text content"
    GeneratorInputType.URL -> "Enter URL (e.g. example.com)"
    GeneratorInputType.WIFI -> "WIFI:S:NetworkName;T:WPA;P:Password;;"
    GeneratorInputType.CONTACT -> "BEGIN:VCARD..."
    GeneratorInputType.PHONE -> "Enter phone number"
    GeneratorInputType.SOCIAL_MEDIA -> "Enter profile URL"
}
