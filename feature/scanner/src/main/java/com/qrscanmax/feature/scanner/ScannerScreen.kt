package com.qrscanmax.feature.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
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
import com.qrscanmax.core.common.PermissionUtils

/**
 * 扫描器主界面 — 全屏相机预览 + 取景框 + 操作按钮。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本 Composable 是扫描功能的入口界面，包含：
 * 1. 全屏 CameraX 预览（通过 AndroidView 包装 PreviewView）
 * 2. 半透明取景框覆盖层（中央透明方框 + 四周暗色遮罩）
 * 3. 右上角相册导入按钮
 * 4. 底部手电筒切换按钮
 * 5. 权限请求和拒绝处理
 *
 * ## 状态管理
 * 通过 ScannerViewModel 的 uiState 驱动 UI：
 * - Scanning → 显示相机预览
 * - ResultDetected → 显示结果（由父级导航处理）
 * - PermissionDenied → 显示权限说明
 *
 * ## 后续开发
 * - 需要在 CameraPreviewContent 中集成 ML Kit BarcodeScanner
 * - 相册导入功能需要集成 ML Kit 对静态图片的解码
 * - 结果检测后应导航到 ScanResultScreen
 */
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
    onResultDetected: (String) -> Unit = {}
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
            CameraPreviewContent(viewModel = viewModel)
        }
        is ScannerUiState.ResultDetected -> {
            val result = (uiState as ScannerUiState.ResultDetected).result
            LaunchedEffect(result) {
                onResultDetected(result.rawValue)
            }
            CameraPreviewContent(viewModel = viewModel)
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
 * 相机预览内容 — 包含预览画面、取景框和操作按钮。
 */
@Composable
private fun CameraPreviewContent(viewModel: ScannerViewModel) {
    var isFlashOn by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览（CameraX PreviewView）
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
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

        // 右上角：相册导入按钮
        IconButton(
            onClick = {
                // TODO [FUTURE]: 实现相册导入功能
                // 1. 启动系统图片选择器 (ActivityResultContracts.GetContent)
                // 2. 获取选中图片的 Uri
                // 3. 用 ML Kit InputImage.fromFilePath 解码图片中的条码
                // 4. 调用 viewModel.onBarcodeDetected() 处理结果
            },
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

        // 底部：手电筒按钮
        IconButton(
            onClick = { isFlashOn = !isFlashOn },
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
 * 取景框覆盖层 — 半透明黑色背景 + 中央透明扫描区域。
 *
 * ## 给其他 AI 开发者的说明
 * 使用 Canvas 绘制，中央方框使用 BlendMode.Clear 实现透明效果。
 * 如果需要添加扫描动画线，可以在此基础上添加动画 Composable。
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
