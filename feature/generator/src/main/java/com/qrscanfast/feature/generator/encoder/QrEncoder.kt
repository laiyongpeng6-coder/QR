package com.qrscanfast.feature.generator.encoder

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import javax.inject.Inject

/**
 * 二维码 / 条码编码器。
 *
 * ## AI 交接
 * - 职责：把文本内容编码成 QR Code 或条码位图。
 * - 当前状态：已支持常见 QR 与条码格式，适合作为纯逻辑工具复用。
 * - 依赖：ZXing、Bitmap、格式校验逻辑。
 * - 安全修改范围：编码参数、容量检查、格式支持。
 * - 风险 / TODO：扩展新格式时要同步更新 UI 校验与测试。
 */
class QrEncoder @Inject constructor() {

    /**
     * 将文本内容编码为 QR 码位图。
     */
    fun encode(
        content: String,
        size: Int = 512,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        return encodeWithFormat(
            content = content,
            format = BarcodeFormat.QR_CODE,
            width = size,
            height = size,
            errorCorrection = errorCorrection,
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor
        )
    }

    /**
     * 将文本内容编码为指定格式的条码/二维码位图。
     *
     * @param content 要编码的内容
     * @param format ZXing 条码格式
     * @param width 输出宽度（像素）
     * @param height 输出高度（像素）
     * @param errorCorrection 纠错等级（仅 QR Code 有效）
     * @param foregroundColor 前景色
     * @param backgroundColor 背景色
     * @return 生成的 Bitmap
     */
    fun encodeWithFormat(
        content: String,
        format: BarcodeFormat,
        width: Int = 512,
        height: Int = 200,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val hints = mutableMapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )
        // 纠错等级仅对 QR Code 有效
        if (format == BarcodeFormat.QR_CODE) {
            hints[EncodeHintType.ERROR_CORRECTION] = errorCorrection
        }

        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(content, format, width, height, hints)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
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

    /**
     * 验证 EAN-13 格式是否合法（13位纯数字）。
     */
    fun isValidEan13(content: String): Boolean {
        return content.length == 13 && content.all { it.isDigit() }
    }

    /**
     * 验证 EAN-8 格式是否合法（8位纯数字）。
     */
    fun isValidEan8(content: String): Boolean {
        return content.length == 8 && content.all { it.isDigit() }
    }

    /**
     * 验证 UPC-A 格式是否合法（12位纯数字）。
     */
    fun isValidUpcA(content: String): Boolean {
        return content.length == 12 && content.all { it.isDigit() }
    }

    /**
     * 验证 Code 128 格式是否合法（非空 ASCII 字符）。
     */
    fun isValidCode128(content: String): Boolean {
        return content.isNotEmpty() && content.all { it.code in 0..127 }
    }
}
