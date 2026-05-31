package com.qrscanfast.feature.generator

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

/**
 * 二维码点形状。
 */
enum class DotShape(val label: String) {
    SQUARE("方形"),
    CIRCLE("圆形"),
    ROUNDED("圆角")
}

/**
 * 预设社交媒体 Logo，关联真实的品牌矢量图标资源。
 */
enum class SocialLogo(val label: String, val drawableRes: Int) {
    INSTAGRAM("Instagram", R.drawable.ic_logo_instagram),
    TIKTOK("TikTok", R.drawable.ic_logo_tiktok),
    FACEBOOK("Facebook", R.drawable.ic_logo_facebook),
    WECHAT("微信", R.drawable.ic_logo_wechat),
    WHATSAPP("WhatsApp", R.drawable.ic_logo_whatsapp),
    TWITTER("X", R.drawable.ic_logo_x),
    YOUTUBE("YouTube", R.drawable.ic_logo_youtube),
    LINKEDIN("LinkedIn", R.drawable.ic_logo_linkedin)
}

/**
 * 二维码美化页面。
 *
 * 支持：
 * - 前景色 / 背景色选择
 * - 点形状切换（方形/圆形/圆角，定位角保持方形以保证可扫描性）
 * - 中心 Logo：预设社交 Logo / 自定义上传图片
 * - Logo 大小调节
 * - 实时预览、保存和分享
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrBeautifyScreen(
    content: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 样式状态
    var foregroundColor by remember { mutableStateOf(Color.Black) }
    var backgroundColor by remember { mutableStateOf(Color.White) }
    var dotShape by remember { mutableStateOf(DotShape.SQUARE) }
    var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedSocialLogo by remember { mutableStateOf<SocialLogo?>(null) }
    var logoSizeRatio by remember { mutableFloatStateOf(0.2f) } // Logo 占比，默认 20%

    // 自定义图片选择器
    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                logoBitmap = BitmapFactory.decodeStream(inputStream)
                selectedSocialLogo = null // 清除社交 Logo 选择
                inputStream?.close()
            } catch (_: Exception) {
                Toast.makeText(context, "无法加载图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 计算最终使用的 Logo（自定义图片优先，其次社交 Logo）
    val effectiveLogo: Bitmap? = remember(logoBitmap, selectedSocialLogo) {
        logoBitmap ?: selectedSocialLogo?.let { drawableToBitmap(context, it.drawableRes) }
    }

    // 实时生成美化后的二维码
    val beautifiedBitmap = remember(content, foregroundColor, backgroundColor, dotShape, effectiveLogo, logoSizeRatio) {
        generateBeautifiedQrCode(
            content = content,
            size = 800,
            foregroundColor = foregroundColor.toArgb(),
            backgroundColor = backgroundColor.toArgb(),
            dotShape = dotShape,
            logoBitmap = effectiveLogo,
            logoSizeRatio = logoSizeRatio
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("美化二维码") },
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
            // 预览
            Card(
                modifier = Modifier.size(240.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                beautifiedBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "美化后的二维码",
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 前景色
            StyleSection(title = "前景色") {
                ColorPalette(
                    selectedColor = foregroundColor,
                    onColorSelected = { foregroundColor = it },
                    colors = foregroundColors
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 背景色
            StyleSection(title = "背景色") {
                ColorPalette(
                    selectedColor = backgroundColor,
                    onColorSelected = { backgroundColor = it },
                    colors = backgroundColors
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 点形状
            StyleSection(title = "点形状") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DotShape.entries.forEach { shape ->
                        FilterChip(
                            selected = dotShape == shape,
                            onClick = { dotShape = shape },
                            label = { Text(shape.label) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 中心 Logo — 预设社交 Logo
            StyleSection(title = "中心 Logo") {
                Column {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 无 Logo 选项
                        item {
                            LogoOption(
                                isSelected = effectiveLogo == null,
                                onClick = {
                                    selectedSocialLogo = null
                                    logoBitmap = null
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "无", modifier = Modifier.size(20.dp))
                            }
                        }
                        // 自定义上传选项
                        item {
                            LogoOption(
                                isSelected = logoBitmap != null,
                                onClick = { logoLauncher.launch("image/*") }
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "上传", modifier = Modifier.size(20.dp))
                            }
                        }
                        // 预设社交 Logo
                        items(SocialLogo.entries.toList()) { social ->
                            LogoOption(
                                isSelected = selectedSocialLogo == social && logoBitmap == null,
                                onClick = {
                                    selectedSocialLogo = social
                                    logoBitmap = null
                                },
                                backgroundColor = Color.White
                            ) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = social.drawableRes),
                                    contentDescription = social.label,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Logo 大小滑块（仅在有 Logo 时显示）
            if (effectiveLogo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                StyleSection(title = "Logo 大小：${(logoSizeRatio * 100).toInt()}%") {
                    Slider(
                        value = logoSizeRatio,
                        onValueChange = { logoSizeRatio = it },
                        valueRange = 0.12f..0.28f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 操作按钮
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    beautifiedBitmap?.let {
                        saveToGallery(context, it)
                        com.qrscanfast.core.common.AnalyticsHelper.logBeautifySave()
                    }
                }) { Text("保存") }
                OutlinedButton(onClick = {
                    beautifiedBitmap?.let { shareImage(context, it) }
                }) { Text("分享") }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StyleSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

/**
 * Logo 选项圆形按钮。
 */
@Composable
private fun LogoOption(
    isSelected: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    colors: List<Color>
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(colors) { color ->
            val isSelected = color == selectedColor
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isLightColor(color)) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 判断颜色是否为浅色（用于决定勾选图标的对比色）。
 */
private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5
}

/**
 * 将 drawable 资源（含矢量图）渲染为 Bitmap，用于嵌入二维码中心。
 */
private fun drawableToBitmap(context: Context, drawableRes: Int): Bitmap? {
    return try {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, drawableRes)
            ?: return null
        val size = 240
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        bitmap
    } catch (_: Exception) {
        null
    }
}

/**
 * 生成美化后的二维码 Bitmap。
 *
 * 关键改进：
 * - 三个定位角（左上、右上、左下）始终用方形渲染，保证可扫描性
 * - 数据模块按所选形状渲染，形状差异明显
 * - Logo 大小可调
 */
private fun generateBeautifiedQrCode(
    content: String,
    size: Int,
    foregroundColor: Int,
    backgroundColor: Int,
    dotShape: DotShape,
    logoBitmap: Bitmap?,
    logoSizeRatio: Float
): Bitmap? {
    if (content.isBlank()) return null

    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val matrixSize = bitMatrix.width

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(backgroundColor)

        val moduleSize = size.toFloat() / matrixSize
        paint.color = foregroundColor

        // 定位角探测区域大小为 7x7 模块
        val finderSize = 7

        for (x in 0 until matrixSize) {
            for (y in 0 until matrixSize) {
                if (!bitMatrix[x, y]) continue

                val left = x * moduleSize
                val top = y * moduleSize

                // 判断是否在三个定位角范围内
                val inFinder = isInFinderPattern(x, y, matrixSize, finderSize)

                if (inFinder) {
                    // 定位角始终用方形，保证可扫描
                    canvas.drawRect(left, top, left + moduleSize, top + moduleSize, paint)
                } else {
                    // 数据模块按所选形状渲染
                    when (dotShape) {
                        DotShape.SQUARE -> {
                            canvas.drawRect(left, top, left + moduleSize, top + moduleSize, paint)
                        }
                        DotShape.CIRCLE -> {
                            val radius = moduleSize / 2f
                            canvas.drawCircle(left + radius, top + radius, radius * 0.92f, paint)
                        }
                        DotShape.ROUNDED -> {
                            val rect = RectF(left, top, left + moduleSize, top + moduleSize)
                            canvas.drawRoundRect(rect, moduleSize * 0.4f, moduleSize * 0.4f, paint)
                        }
                    }
                }
            }
        }

        // 绘制中心 Logo
        logoBitmap?.let { logo ->
            val logoSize = (size * logoSizeRatio).toInt()
            val logoLeft = (size - logoSize) / 2f
            val logoTop = (size - logoSize) / 2f

            // 白色圆角背景衬底
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            bgPaint.color = backgroundColor
            val pad = logoSize * 0.08f
            val bgRect = RectF(
                logoLeft - pad, logoTop - pad,
                logoLeft + logoSize + pad, logoTop + logoSize + pad
            )
            canvas.drawRoundRect(bgRect, logoSize * 0.15f, logoSize * 0.15f, bgPaint)

            val scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
            canvas.drawBitmap(scaledLogo, logoLeft, logoTop, null)
        }

        bitmap
    } catch (_: Exception) {
        null
    }
}

/**
 * 判断模块坐标是否位于三个定位角（finder pattern）区域内。
 * 左上角、右上角、左下角各占 7x7 模块。
 */
private fun isInFinderPattern(x: Int, y: Int, matrixSize: Int, finderSize: Int): Boolean {
    // 左上角
    if (x < finderSize && y < finderSize) return true
    // 右上角
    if (x >= matrixSize - finderSize && y < finderSize) return true
    // 左下角
    if (x < finderSize && y >= matrixSize - finderSize) return true
    return false
}

// 预设前景色
private val foregroundColors = listOf(
    Color.Black,
    Color(0xFF1A237E),
    Color(0xFF004D40),
    Color(0xFF880E4F),
    Color(0xFF4A148C),
    Color(0xFFBF360C),
    Color(0xFF1B5E20),
    Color(0xFF0D47A1),
    Color(0xFF311B92),
    Color(0xFF3E2723),
)

// 预设背景色
private val backgroundColors = listOf(
    Color.White,
    Color(0xFFFFF8E1),
    Color(0xFFE8F5E9),
    Color(0xFFE3F2FD),
    Color(0xFFFCE4EC),
    Color(0xFFF3E5F5),
    Color(0xFFFFF3E0),
    Color(0xFFE0F7FA),
    Color(0xFFF1F8E9),
    Color(0xFFEFEBE9),
)

private fun saveToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "FastQrScan_beautified_${System.currentTimeMillis()}.png"
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
        val file = File(cachePath, "qr_beautified_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            setType("image/png")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享美化二维码"))
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
