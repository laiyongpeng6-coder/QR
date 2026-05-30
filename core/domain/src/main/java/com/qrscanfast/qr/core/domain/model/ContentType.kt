package com.qrscanfast.qr.core.domain.model

/**
 * Represents the classified content type of a scanned or generated barcode/QR code.
 *
 * The [ResultMapperUseCase] uses priority-ordered pattern matching to determine
 * which content type a decoded string belongs to. Each type enables type-specific
 * actions in the UI (e.g., opening a URL, connecting to WiFi, saving a contact).
 *
 * @see com.qrscanfast.qr.core.domain.usecase.ResultMapperUseCase
 */
enum class ContentType {
    /** A web URL (http/https scheme). Actions: open in browser, copy, share. */
    URL,

    /** WiFi network configuration (WIFI: protocol). Actions: connect to network. */
    WIFI,

    /** vCard contact data (BEGIN:VCARD format). Actions: save to contacts. */
    VCARD,

    /** Phone number (tel: scheme or phone regex). Actions: call, send SMS. */
    PHONE,

    /** Email address (mailto: scheme). Actions: compose email. */
    EMAIL,

    /** SMS message (sms:/smsto: scheme). Actions: open messaging app. */
    SMS,

    /** Social media profile URL (Instagram, Twitter, Facebook, etc.). Actions: open in app or browser. */
    SOCIAL_MEDIA,

    /** Geographic coordinates (geo: scheme). Actions: open in maps app. */
    GEO,

    /** Plain text content that doesn't match any specific pattern. Actions: copy, share. */
    PLAIN_TEXT,

    /** Product barcode (EAN/UPC). Actions: lookup product info, price comparison. */
    PRODUCT
}
