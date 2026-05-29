package com.qrscanmax.feature.scanner.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * CameraX 相机管理器 — 封装相机的绑定、预览、手电筒和缩放控制。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本类是 CameraX API 的封装层，提供简洁的接口给 ScannerScreen 使用。
 * 主要职责：
 * 1. 绑定相机预览到 PreviewView
 * 2. 配置 ImageAnalysis 用于 ML Kit 条码扫描
 * 3. 提供手电筒开关控制
 * 4. 提供缩放倍数控制
 *
 * ## 生命周期管理
 * 相机通过 [bindToLifecycle] 绑定到 LifecycleOwner，
 * 会自动在 Activity/Fragment 销毁时释放资源。
 *
 * ## 使用流程
 * 1. 在 Composable 中创建 PreviewView
 * 2. 调用 [bindToLifecycle] 绑定相机
 * 3. 设置 [imageAnalyzer] 用于条码检测
 * 4. 根据需要调用 [enableTorch] 和 [setZoomRatio]
 * 5. 页面离开时调用 [unbind] 释放资源
 *
 * @param context 应用上下文
 */
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** 相机实例，绑定后可用 */
    private var camera: Camera? = null

    /** 相机提供者 */
    private var cameraProvider: ProcessCameraProvider? = null

    /** 图像分析执行器（单线程，避免帧堆积） */
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** 图像分析器，由外部设置（ML Kit BarcodeScanner） */
    var imageAnalyzer: ImageAnalysis.Analyzer? = null

    /**
     * 将相机绑定到生命周期并开始预览。
     *
     * @param lifecycleOwner 生命周期拥有者（通常是 Activity 或 Fragment）
     * @param previewView 用于显示相机预览的 View
     * @param onBound 相机绑定成功后的回调（可选）
     */
    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onBound: (() -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            // 配置预览
            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            // 配置图像分析（用于 ML Kit 条码扫描）
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    imageAnalyzer?.let { analyzer ->
                        analysis.setAnalyzer(analysisExecutor, analyzer)
                    }
                }

            // 使用后置摄像头
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 解绑之前的用例
                provider.unbindAll()

                // 绑定新的用例
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                onBound?.invoke()
            } catch (e: Exception) {
                // 相机绑定失败（设备不支持或权限问题）
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 开启或关闭手电筒。
     *
     * @param enabled true 开启，false 关闭
     */
    fun enableTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    /**
     * 设置相机缩放倍数。
     *
     * @param ratio 缩放倍数，1.0f 为无缩放
     */
    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    /**
     * 解绑相机并释放资源。
     */
    fun unbind() {
        cameraProvider?.unbindAll()
        camera = null
    }

    /**
     * 释放分析执行器。
     */
    fun shutdown() {
        analysisExecutor.shutdown()
        unbind()
    }
}
