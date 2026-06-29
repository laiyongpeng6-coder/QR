package com.qrscanfast.feature.scanner

import android.Manifest
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qrscanfast.core.common.AnalyticsHelper
import com.qrscanfast.core.common.PermissionUtils
import com.qrscanfast.core.domain.model.BarcodeFormat
import com.qrscanfast.core.domain.model.ContentType
import java.util.concurrent.Executors

/**
 * 扫描器主页面。
 *
 * ## AI 交接
 * - 职责：承载相机预览、扫描交互、权限分支和结果跳转。
 * - 当前状态：已集成 CameraX 和 ML Kit，交互路径较完整。
 * - 依赖：`ScannerViewModel`、相机控制器、相册导入与权限工具。
 * - 安全修改范围：预览层、遮罩层、按钮区、权限拒绝页。
 * - 风险 / TODO：性能、重复触发与视觉沉浸感需要持续优化。
 */
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
    onResultDetected: (String, BarcodeFormat, ContentType) -> Unit = { _, _, _ -> },
    onSettingsClick: () -> Unit = {},
    onVipClick: () -> Unit = {},
    isPremium: Boolean = false,
    showSubscriptionScreen: (suspend () -> Boolean) = { false }
) {
    val uiState by viewModel.uiState.collectAsState()
    val autoOpenUrl by viewModel.autoOpenUrl.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Observe navigation events from ViewModel (emitted after gate flow completes)
    LaunchedEffect(Unit) {
        viewModel.navigateToResult.collect { result ->
            onResultDetected(result.rawValue, result.format, result.contentType)
            viewModel.resumeScanning()
        }
    }

    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // 检查权限状态
    LaunchedEffect(Unit) {
        if (!PermissionUtils.hasCameraPermission(context)) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when (uiState) {
        is ScannerUiState.Scanning -> {
            CameraPreviewContent(viewModel = viewModel, onResultDetected = onResultDetected, onSettingsClick = onSettingsClick, onVipClick = onVipClick, isPremium = isPremium)
        }
        is ScannerUiState.ResultDetected -> {
            val result = (uiState as ScannerUiState.ResultDetected).result
            LaunchedEffect(result) {
                // 若开启"自动跳转网页"且内容是 URL/社交链接，直接打开浏览器
                val isWebLink = result.contentType == ContentType.URL ||
                    result.contentType == ContentType.SOCIAL_MEDIA
                if (autoOpenUrl && isWebLink) {
                    try {
                        val url = if (!result.rawValue.startsWith("http")) {
                            "https://${result.rawValue}"
                        } else {
                            result.rawValue
                        }
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(url)
                            )
                        )
                    } catch (_: Exception) {
                        // 打开失败则回退到结果页（仍走 gate 流程）
                        if (activity != null) {
                            viewModel.performGateAndNavigate(activity, result, showSubscriptionScreen)
                        } else {
                            onResultDetected(result.rawValue, result.format, result.contentType)
                            viewModel.resumeScanning()
                        }
                    }
                    viewModel.resumeScanning()
                } else {
                    // Normal result: go through ad gate before showing result
                    if (activity != null) {
                        viewModel.performGateAndNavigate(activity, result, showSubscriptionScreen)
                    } else {
                        // Fallback: if Activity is not available, navigate directly
                        onResultDetected(result.rawValue, result.format, result.contentType)
                        viewModel.resumeScanning()
                    }
                }
            }
            CameraPreviewContent(viewModel = viewModel, onResultDetected = onResultDetected, onSettingsClick = onSettingsClick, onVipClick = onVipClick, isPremium = isPremium)
        }
        is ScannerUiState.PermissionDenied -> {
            PermissionDeniedContent(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onOpenSettings = {
                    PermissionUtils.openAppSettings(context)
                }
            )
        }
    }
}

/**
 * 相机预览内容 — 包含 CameraX 预览、ML Kit 条码扫描、取景框和操作按钮。
 */
@Composable
private fun CameraPreviewContent(
    viewModel: ScannerViewModel,
    onResultDetected: (String, BarcodeFormat, ContentType) -> Unit,
    onSettingsClick: () -> Unit,
    onVipClick: () -> Unit,
    isPremium: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFlashOn by remember { mutableStateOf(false) }

    // 提前取出本地化文案（回调中无法直接调用 stringResource）
    val msgNoBarcode = stringResource(R.string.scanner_no_barcode_in_image)
    val msgParseFailed = stringResource(R.string.scanner_image_parse_failed)
    val msgReadFailed = stringResource(R.string.scanner_image_read_failed)

    // 使用 LifecycleCameraController（封装了 ProcessCameraProvider，无需手动处理 ListenableFuture）
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // 配置 ML Kit 条码分析器
    DisposableEffect(Unit) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val barcodeScanner = BarcodeScanning.getClient()

        cameraController.setImageAnalysisAnalyzer(analysisExecutor) { imageProxy ->
            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.let { barcode ->
                            barcode.rawValue?.let { rawValue ->
                                val format = mapMlKitFormat(barcode.format)
                                viewModel.onBarcodeDetected(rawValue, format)
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        // 绑定到生命周期
        cameraController.bindToLifecycle(lifecycleOwner)

        onDispose {
            cameraController.unbind()
            analysisExecutor.shutdown()
        }
    }

    // 相册选择器（使用系统照片选择器，无需 READ_MEDIA_IMAGES 权限）
    val albumLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // 先恢复扫描状态，确保 onBarcodeDetected 不会被 isPaused 拦截
            viewModel.resumeScanning()
            try {
                val image = InputImage.fromFilePath(context, it)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val barcode = barcodes.firstOrNull()
                        if (barcode != null && barcode.rawValue != null) {
                            val format = mapMlKitFormat(barcode.format)
                            viewModel.onBarcodeDetected(barcode.rawValue!!, format)
                            AnalyticsHelper.logAlbumImport(true)
                        } else {
                            Toast.makeText(context, msgNoBarcode, Toast.LENGTH_SHORT).show()
                            AnalyticsHelper.logAlbumImport(false)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, msgParseFailed, Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, msgReadFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览（CameraX PreviewView + LifecycleCameraController）
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    controller = cameraController
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 取景框覆盖层
        ViewfinderOverlay(modifier = Modifier.fillMaxSize())

        // 提示文字
        Text(
            text = stringResource(R.string.scanner_scanning),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )

        // 右上角：VIP 入口
        IconButton(
            onClick = onVipClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "VIP",
                tint = if (isPremium) Color(0xFF2DB89A) else Color(0xFF888888)
            )
        }

        // 左上角：设置按钮
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "设置",
                tint = Color.White
            )
        }

        // 底部按钮行：左侧相册，中间闪光灯
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧相册导入
            IconButton(
                onClick = { albumLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(R.string.scanner_import_album),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(48.dp))

            // 中间闪光灯
            IconButton(
                onClick = {
                    isFlashOn = !isFlashOn
                    cameraController.enableTorch(isFlashOn)
                    AnalyticsHelper.logFlashToggle(isFlashOn)
                }
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (isFlashOn) {
                        stringResource(R.string.scanner_flash_on)
                    } else {
                        stringResource(R.string.scanner_flash_off)
                    },
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * 将 ML Kit 的条码格式映射为应用的 BarcodeFormat 枚举。
 */
private fun mapMlKitFormat(mlKitFormat: Int): BarcodeFormat {
    return when (mlKitFormat) {
        Barcode.FORMAT_QR_CODE -> BarcodeFormat.QR_CODE
        Barcode.FORMAT_EAN_13 -> BarcodeFormat.EAN_13
        Barcode.FORMAT_EAN_8 -> BarcodeFormat.EAN_8
        Barcode.FORMAT_UPC_A -> BarcodeFormat.UPC_A
        Barcode.FORMAT_UPC_E -> BarcodeFormat.UPC_E
        Barcode.FORMAT_CODE_128 -> BarcodeFormat.CODE_128
        Barcode.FORMAT_CODE_39 -> BarcodeFormat.CODE_39
        Barcode.FORMAT_ITF -> BarcodeFormat.ITF
        Barcode.FORMAT_PDF417 -> BarcodeFormat.PDF_417
        Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        Barcode.FORMAT_AZTEC -> BarcodeFormat.AZTEC
        else -> BarcodeFormat.QR_CODE
    }
}

/**
 * 取景框覆盖层 — 半透明黑色背景 + 中央透明扫描区域。
 */
@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 扫描框大小：屏幕宽度的 70%
        val scanBoxSize = canvasWidth * 0.7f
        val left = (canvasWidth - scanBoxSize) / 2
        val top = (canvasHeight - scanBoxSize) / 2

        // 绘制半透明黑色背景
        drawRect(color = Color.Black.copy(alpha = 0.5f), size = size)

        // 在中央绘制透明方框（清除该区域的遮罩）
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(scanBoxSize, scanBoxSize),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear
        )

        // 绘制扫描框白色边框
        drawRoundRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(left, top),
            size = Size(scanBoxSize, scanBoxSize),
            cornerRadius = CornerRadius(16f, 16f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }
}

/**
 * 权限被拒绝时的说明界面。
 */
@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.scanner_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.scanner_permission_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.scanner_permission_button))
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onOpenSettings) {
            Text(stringResource(R.string.scanner_permission_settings))
        }
    }
}
