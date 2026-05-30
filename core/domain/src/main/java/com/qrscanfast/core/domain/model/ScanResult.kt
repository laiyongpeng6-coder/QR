package com.qrscanfast.core.domain.model

import java.time.Instant

/**
 * Represents the result of a successful barcode/QR code scan.
 *
 * A [ScanResult] is produced after ML Kit detects and decodes a barcode from
 * either the camera preview or an imported image. The [contentType] is determined
 * by the [ResultMapperUseCase] based on pattern matching against the [rawValue].
 *
 * This is a pure domain model with no framework dependencies, making it suitable
 * for use across all layers of the application.
 *
 * @property rawValue The raw decoded string content from the barcode.
 * @property format The barcode symbology format (QR, EAN-13, etc.).
 * @property contentType The classified content type determined by pattern matching.
 * @property timestamp The instant when the scan was performed.
 * @property metadata Additional key-value pairs extracted during scanning
 *   (e.g., bounding box coordinates, confidence score). Defaults to empty map.
 *
 * @see ContentType
 * @see BarcodeFormat
 */
data class ScanResult(
    val rawValue: String,
    val format: BarcodeFormat,
    val contentType: ContentType,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)
