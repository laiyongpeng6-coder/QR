package com.qrscanfast.qr.feature.generator

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanfast.qr.core.ui.components.QrMaxLoadingIndicator
import com.qrscanfast.qr.core.ui.components.QrMaxPrimaryButton
import java.io.File
import java.io.FileOutputStream

/**
 * QR 码生成器主界面 — 输入内容 + 类型选择 + 生成预览。
 *
 * 包含完整的保存到相册和分享功能。
 */
@Composable
fun GeneratorScreen(viewModel: GeneratorViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val inputType by viewModel.inputType.collectAsState()
    val content by viewModel.content.collectAsState()
    val resolution by viewModel.selectedResolution.collectAsState()
    val context = LocalContext.current

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
                    context = context,
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
private fun GeneratorPreviewContent(bitmap: Bitmap, context: Context, onNewCode: () -> Unit) {
    Text("Your QR Code", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))

    Card(modifier = Modifier.size(280.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Generated QR Code",
            modifier = Modifier.fillMaxSize().padding(16.dp))
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { saveQrCodeToGallery(context, bitmap) }) { Text("Save") }
        OutlinedButton(onClick = { shareQrCode(context, bitmap) }) { Text("Share") }
        Button(onClick = {
            Toast.makeText(context, "AI 美化功能即将上线", Toast.LENGTH_SHORT).show()
        }) { Text("Beautify") }
    }

    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onNewCode) { Text("Create New Code") }
}

/**
 * 将 QR 码位图保存到系统相册。
 * 使用 MediaStore API 兼容 Android 10+ 的 Scoped Storage。
 */
private fun saveQrCodeToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "FastQrScan_${System.currentTimeMillis()}.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FastQrScan")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }

            Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 分享 QR 码图片。
 * 先将 Bitmap 写入缓存目录，再通过 FileProvider 分享。
 */
private fun shareQrCode(context: Context, bitmap: Bitmap) {
    try {
        // 写入缓存目录
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val file = File(cachePath, "qr_code_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        // 通过 FileProvider 获取 content:// URI
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        // 创建分享 Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "分享 QR 码"))
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun getInputHint(type: GeneratorInputType): String = when (type) {
    GeneratorInputType.PLAIN_TEXT -> "Enter text content"
    GeneratorInputType.URL -> "Enter URL (e.g. example.com)"
    GeneratorInputType.WIFI -> "WIFI:S:NetworkName;T:WPA;P:Password;;"
    GeneratorInputType.CONTACT -> "BEGIN:VCARD..."
    GeneratorInputType.PHONE -> "Enter phone number"
    GeneratorInputType.SOCIAL_MEDIA -> "Enter profile URL"
}
