package com.qrscanmax.feature.generator.encoder

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import javax.inject.Inject

/**
 * QR 码编码器 — 使用 ZXing 库将文本内容编码为 QR 码位图。
 *
 * ## 给其他 AI 开发者的说明
 *
 * 本类封装了 ZXing 的 QRCodeWriter，提供简洁的 API 生成 QR 码 Bitmap。
 * 支持自定义尺寸和纠错等级。
 *
 * ## 使用示例
 * ```kotlin
 * val bitmap = qrEncoder.encode("https://example.com", size = 512)
 * ```
 *
 * ## 纠错等级说明
 * - L (7%) — 适合内容短、环境干净的场景
 * - M (15%) — 默认推荐，平衡容量和容错
 * - Q (25%) — 适合可能被部分遮挡的场景
 * - H (30%) — 最高容错，适合中心放 Logo 的场景
 */
class QrEncoder @Inject constructor() {

    /**
     * 将文本内容编码为 QR 码位图。
     *
     * @param content 要编码的文本内容
     * @param size 输出位图的宽高（像素）
     * @param errorCorrection 纠错等级，默认为 M
     * @param foregroundColor 前景色，默认黑色
     * @param backgroundColor 背景色，默认白色
     * @return 生成的 QR 码 Bitmap
     * @throws com.google.zxing.WriterException 如果内容超出容量限制
     */
    fun encode(
        content: String,
        size: Int = 512,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to errorCorrection,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) foregroundColor else backgroundColor)
            }
        }
        return bitmap
    }

    /**
     * 检查内容是否超出 QR 码最大容量（约 2953 字节 UTF-8）。
     */
    fun isWithinCapacity(content: String): Boolean {
        return content.toByteArray(Charsets.UTF_8).size <= 2953
    }
}
