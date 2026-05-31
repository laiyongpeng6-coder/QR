package com.qrscanfast.feature.generator

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
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
import com.qrscanfast.core.ui.components.QrMaxLoadingIndicator
import com.qrscanfast.core.ui.components.QrMaxPrimaryButton
import java.io.File
import java.io.FileOutputStream

/**
 * 二维码创建页 — 独立全屏页面。
 * 支持文本、网址、WiFi、联系人、电话、社交媒体类型。
 * 结果页提供保存、分享、美化功能。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeCreateScreen(
    viewModel: GeneratorViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onBeautify: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputType by viewModel.inputType.collectAsState()
    val content by viewModel.content.collectAsState()
    val resolution by viewModel.selectedResolution.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建二维码") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState) {
                is GeneratorUiState.Input, is GeneratorUiState.Error -> {
                    QrCodeInputContent(
                        inputType = inputType, content = content, resolution = resolution,
                        errorMessage = (uiState as? GeneratorUiState.Error)?.message,
                        onTypeChanged = viewModel::setInputType,
                        onContentChanged = viewModel::setContent,
                        onResolutionChanged = viewModel::setResolution,
                        onGenerate = viewModel::generate
                    )
                }
                is GeneratorUiState.Generating -> {
                    Spacer(modifier = Modifier.height(100.dp))
                    QrMaxLoadingIndicator(text = "生成中...")
                }
                is GeneratorUiState.Generated -> {
                    QrCodeResultContent(
                        bitmap = (uiState as GeneratorUiState.Generated).bitmap,
                        content = (uiState as GeneratorUiState.Generated).content,
                        context = context,
                        onNewCode = viewModel::resetToInput,
                        onBeautify = onBeautify
                    )
                }
            }
        }
    }
}

@Composable
private fun QrCodeInputContent(
    inputType: GeneratorInputType, content: String, resolution: Int,
    errorMessage: String?, onTypeChanged: (GeneratorInputType) -> Unit,
    onContentChanged: (String) -> Unit, onResolutionChanged: (Int) -> Unit, onGenerate: () -> Unit
) {
    // 仅显示 QR Code 类型
    val qrTypes = listOf(
        GeneratorInputType.PLAIN_TEXT to "文本",
        GeneratorInputType.URL to "网址",
        GeneratorInputType.WIFI to "WiFi",
        GeneratorInputType.CONTACT to "联系人",
        GeneratorInputType.PHONE to "电话",
        GeneratorInputType.SOCIAL_MEDIA to "社交"
    )
    val selectedIndex = qrTypes.indexOfFirst { it.first == inputType }.coerceAtLeast(0)

    ScrollableTabRow(selectedTabIndex = selectedIndex, modifier = Modifier.fillMaxWidth()) {
        qrTypes.forEachIndexed { _, (type, label) ->
            Tab(selected = inputType == type, onClick = { onTypeChanged(type) }, text = { Text(label) })
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = content, onValueChange = onContentChanged, modifier = Modifier.fillMaxWidth(),
        label = { Text(getQrInputHint(inputType)) }, isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        minLines = 3, maxLines = 5
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("分辨率", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(256, 512, 1024).forEach { res ->
            FilterChip(selected = resolution == res, onClick = { onResolutionChanged(res) }, label = { Text("${res}px") })
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    QrMaxPrimaryButton(text = "生成二维码", onClick = onGenerate, enabled = content.isNotBlank())
}

@Composable
private fun QrCodeResultContent(bitmap: Bitmap, content: String, context: Context, onNewCode: () -> Unit, onBeautify: (String) -> Unit) {
    Text("生成的二维码", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))

    Card(modifier = Modifier.size(280.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Generated QR Code",
            modifier = Modifier.fillMaxSize().padding(16.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 操作按钮
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { saveToGallery(context, bitmap) }) { Text("保存") }
        OutlinedButton(onClick = { shareImage(context, bitmap) }) { Text("分享") }
        Button(onClick = { onBeautify(content) }) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("美化")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onNewCode) { Text("创建新二维码") }
}

private fun getQrInputHint(type: GeneratorInputType): String = when (type) {
    GeneratorInputType.PLAIN_TEXT -> "输入文本内容"
    GeneratorInputType.URL -> "输入网址 (如 example.com)"
    GeneratorInputType.WIFI -> "WIFI:S:网络名;T:WPA;P:密码;;"
    GeneratorInputType.CONTACT -> "BEGIN:VCARD..."
    GeneratorInputType.PHONE -> "输入电话号码"
    GeneratorInputType.SOCIAL_MEDIA -> "输入社交媒体链接"
    else -> "输入内容"
}

private fun saveToGallery(context: Context, bitmap: Bitmap) {
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
            resolver.openOutputStream(it)?.use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
            Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareImage(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val file = File(cachePath, "qr_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            setType("image/png")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享二维码"))
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
