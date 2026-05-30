package com.qrscanfast.qr.core.domain.model

/**
 * Represents the symbology format of a scanned barcode or QR code.
 *
 * This enum covers all barcode formats supported by ML Kit Barcode Scanning API
 * that are relevant to the Fast QR Scan application. The format is detected
 * automatically during scanning and stored alongside the decoded content.
 *
 * @see ScanResult
 */
enum class BarcodeFormat {
    /** 2D matrix barcode. Most common format for encoding URLs, text, and structured data. */
    QR_CODE,

    /** European Article Number (13 digits). Used for retail products worldwide. */
    EAN_13,

    /** European Article Number (8 digits). Compact version for small packages. */
    EAN_8,

    /** Universal Product Code (12 digits). Standard retail barcode in North America. */
    UPC_A,

    /** Universal Product Code (8 digits). Compact version for small items. */
    UPC_E,

    /** High-density linear barcode supporting full ASCII character set. Used in shipping and packaging. */
    CODE_128,

    /** Variable-length alphanumeric barcode. Used in military, healthcare, and automotive industries. */
    CODE_39,

    /** Interleaved 2 of 5. Numeric-only barcode used in warehouse and distribution. */
    ITF,

    /** Portable Data File 417. Stacked linear barcode used in transport, ID cards, and government. */
    PDF_417,

    /** 2D matrix barcode. High density with built-in error correction. Used in industrial and mail. */
    DATA_MATRIX,

    /** 2D matrix barcode with concentric square bullseye pattern. Used in transport and ticketing. */
    AZTEC
}
