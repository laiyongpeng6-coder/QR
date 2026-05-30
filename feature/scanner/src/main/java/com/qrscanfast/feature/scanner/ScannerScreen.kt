package com.qrscanfast.feature.scanner

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qrscanfast.core.common.PermissionUtils
import com.qrscanfast.core.domain.model.BarcodeFormat
import com.qrscanfast.core.domain.model.ContentType
import java.util.concurrent.Executors

/**
 * 扫描器主界面 — 全屏相机预览 + 取景框 + 操作按钮 + 结果展示。
 *
 * 集成 CameraX 预览 + ML Kit 条码扫描，实时检测 QR 码和条形码。
 * 检测到结果后显示底部结果面板，用户可以复制、打开链接、分享或继续扫描。
 */
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
    onResultDetected: (String, BarcodeFormat, ContentType) -> Unit = { _, _, _ -> },
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
            CameraPreviewContent(viewModel = viewModel, onResultDetected = onResultDetected, onSettingsClick = onSettingsClick)
        }
        is ScannerUiState.ResultDetected -> {
            val result = (uiState as ScannerUiState.ResultDetected).result
            LaunchedEffect(result) {
                onResultDetected(result.rawValue, result.format, result.contentType)
                // 导航后恢复扫描状态，以便用户返回时可以继续扫描
                viewModel.resumeScanning()
            }
            CameraPreviewContent(viewModel = viewModel, onResultDetected = onResultDetected, onSettingsClick = onSettingsClick)
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
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFlashOn by remember { mutableStateOf(false) }

    // 保存 Camera 实例用于控制手电筒
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // 相册选择器
    val albumLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
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
                        } else {
                            Toast.makeText(context, "未在图片中检测到二维码或条形码", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "图片解析失败", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "无法读取图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览（CameraX PreviewView）+ ML Kit 条码扫描
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // 配置预览
                    val preview = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    // 配置图像分析（ML Kit 条码扫描）
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val analysisExecutor = Executors.newSingleThreadExecutor()
                    val barcodeScanner = BarcodeScanning.getClient()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
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

                    // 使用后置摄像头
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
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

        // 右上角：相册导入按钮
        IconButton(
            onClick = { albumLauncher.launch("image/*") },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = stringResource(R.string.scanner_import_album),
                tint = Color.White
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

        // 底部：手电筒按钮
        IconButton(
            onClick = {
                isFlashOn = !isFlashOn
                camera?.cameraControl?.enableTorch(isFlashOn)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
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
