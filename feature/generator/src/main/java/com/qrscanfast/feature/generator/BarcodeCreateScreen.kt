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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.qrscanfast.core.ui.components.QrMaxPrimaryButton
import com.qrscanfast.feature.generator.encoder.QrEncoder
import java.io.File
import java.io.FileOutputStream

/**
 * 条码类型枚举。
 */
enum class BarcodeType(val label: String, val format: BarcodeFormat, val hint: String, val digitCount: Int?) {
    EAN_13("EAN-13", BarcodeFormat.EAN_13, "输入 12 位数字（校验位自动计算）", 12),
    EAN_8("EAN-8", BarcodeFormat.EAN_8, "输入 7 位数字（校验位自动计算）", 7),
    UPC_A("UPC-A", BarcodeFormat.UPC_A, "输入 11 位数字（校验位自动计算）", 11),
    CODE_128("Code 128", BarcodeFormat.CODE_128, "输入 ASCII 文本", null)
}

/**
 * 条码创建页 — 独立全屏页面。
 * 支持 EAN-13、EAN-8、UPC-A、Code 128 格式。
 * 自动计算 EAN/UPC 校验位。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeCreateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val qrEncoder = remember { QrEncoder() }

    var selectedType by remember { mutableStateOf(BarcodeType.EAN_13) }
    var content by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建条码") },
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
            if (generatedBitmap == null) {
                // 输入界面
                BarcodeInputContent(
                    selectedType = selectedType,
                    content = content,
                    errorMessage = errorMessage,
                    onTypeChanged = {
                        selectedType = it
                        content = ""
                        errorMessage = null
                    },
                    onContentChanged = {
                        content = it
                        errorMessage = null
                    },
                    onGenerate = {
                        val result = generateBarcode(qrEncoder, content.trim(), selectedType)
                        if (result.isSuccess) {
                            generatedBitmap = result.getOrNull()
                            errorMessage = null
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "生成失败"
                        }
                    }
                )
            } else {
                // 结果界面
                BarcodeResultContent(
                    bitmap = generatedBitmap!!,
                    context = context,
                    onNewCode = {
                        generatedBitmap = null
                        content = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun BarcodeInputContent(
    selectedType: BarcodeType,
    content: String,
    errorMessage: String?,
    onTypeChanged: (BarcodeType) -> Unit,
    onContentChanged: (String) -> Unit,
    onGenerate: () -> Unit
) {
    // 条码类型选择
    val selectedIndex = BarcodeType.entries.indexOf(selectedType)
    TabRow(selectedTabIndex = selectedIndex, modifier = Modifier.fillMaxWidth()) {
        BarcodeType.entries.forEachIndexed { _, type ->
            Tab(
                selected = selectedType == type,
                onClick = { onTypeChanged(type) },
                text = { Text(type.label, maxLines = 1) }
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 输入框
    OutlinedTextField(
        value = content,
        onValueChange = { newValue ->
            // EAN/UPC 类型只允许输入数字
            if (selectedType.digitCount != null) {
                val filtered = newValue.filter { it.isDigit() }
                if (filtered.length <= selectedType.digitCount) {
                    onContentChanged(filtered)
                }
            } else {
                onContentChanged(newValue)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(selectedType.hint) },
        isError = errorMessage != null,
        supportingText = if (errorMessage != null) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else if (selectedType.digitCount != null) {
            { Text("${content.length} / ${selectedType.digitCount} 位") }
        } else null,
        singleLine = true,
        keyboardOptions = if (selectedType.digitCount != null) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        }
    )

    Spacer(modifier = Modifier.height(24.dp))

    QrMaxPrimaryButton(
        text = "生成条码",
        onClick = onGenerate,
        enabled = content.isNotBlank()
    )
}

@Composable
private fun BarcodeResultContent(bitmap: Bitmap, context: Context, onNewCode: () -> Unit) {
    Text("生成的条码", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))

    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Generated Barcode",
            modifier = Modifier.fillMaxSize().padding(16.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { saveToGallery(context, bitmap) }) { Text("保存") }
        OutlinedButton(onClick = { shareImage(context, bitmap) }) { Text("分享") }
    }

    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onNewCode) { Text("创建新条码") }
}

/**
 * 生成条码，自动计算 EAN/UPC 校验位。
 */
private fun generateBarcode(encoder: QrEncoder, content: String, type: BarcodeType): Result<Bitmap> {
    return try {
        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("请输入内容"))
        }

        val finalContent = when (type) {
            BarcodeType.EAN_13 -> {
                if (content.length != 12 || !content.all { it.isDigit() }) {
                    return Result.failure(IllegalArgumentException("EAN-13 需要输入 12 位数字"))
                }
                content + calculateEanCheckDigit(content)
            }
            BarcodeType.EAN_8 -> {
                if (content.length != 7 || !content.all { it.isDigit() }) {
                    return Result.failure(IllegalArgumentException("EAN-8 需要输入 7 位数字"))
                }
                content + calculateEanCheckDigit(content)
            }
            BarcodeType.UPC_A -> {
                if (content.length != 11 || !content.all { it.isDigit() }) {
                    return Result.failure(IllegalArgumentException("UPC-A 需要输入 11 位数字"))
                }
                content + calculateEanCheckDigit(content)
            }
            BarcodeType.CODE_128 -> {
                if (content.isEmpty()) {
                    return Result.failure(IllegalArgumentException("请输入内容"))
                }
                content
            }
        }

        val bitmap = encoder.encodeWithFormat(
            content = finalContent,
            format = type.format,
            width = 600,
            height = 200
        )
        Result.success(bitmap)
    } catch (e: Exception) {
        Result.failure(IllegalArgumentException("生成失败: ${e.message}"))
    }
}

/**
 * 计算 EAN/UPC 校验位。
 * 算法：奇数位权重1，偶数位权重3（从右往左计数），总和对10取模后用10减。
 */
private fun calculateEanCheckDigit(digits: String): Char {
    var sum = 0
    val reversed = digits.reversed()
    for (i in reversed.indices) {
        val digit = reversed[i] - '0'
        sum += if (i % 2 == 0) digit * 3 else digit
    }
    val checkDigit = (10 - (sum % 10)) % 10
    return ('0' + checkDigit)
}

private fun saveToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "FastQrScan_barcode_${System.currentTimeMillis()}.png"
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
        val file = File(cachePath, "barcode_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            setType("image/png")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享条码"))
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
