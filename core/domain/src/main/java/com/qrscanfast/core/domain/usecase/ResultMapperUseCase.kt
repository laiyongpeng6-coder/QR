package com.qrscanfast.core.domain.usecase

import com.qrscanfast.core.domain.model.ContentType
import javax.inject.Inject

/**
 * Enumerates known social media platforms that can be detected from URLs.
 *
 * Used by [ResultMapperUseCase] to identify which social platform a URL belongs to,
 * enabling platform-specific deep-link actions in the UI layer.
 */
enum class SocialPlatform {
    /** Instagram — matches instagram.com and instagr.am domains. */
    INSTAGRAM,

    /** Twitter/X — matches twitter.com and x.com domains. */
    TWITTER,

    /** Facebook — matches facebook.com and fb.com domains. */
    FACEBOOK,

    /** LinkedIn — matches linkedin.com domain. */
    LINKEDIN,

    /** TikTok — matches tiktok.com/@ profile URLs. */
    TIKTOK,

    /** YouTube — matches youtube.com and youtu.be domains. */
    YOUTUBE,

    /** WhatsApp — matches wa.me domain. */
    WHATSAPP
}

/**
 * Classifies raw decoded barcode/QR content into a [ContentType] using priority-ordered
 * pattern matching rules.
 *
 * The classification follows a strict priority order to resolve ambiguity when content
 * could match multiple patterns. For example, a WiFi configuration string that also
 * contains a URL will be classified as [ContentType.WIFI] because WiFi has higher priority.
 *
 * **Priority Order:**
 * 1. WiFi — starts with `WIFI:` (case-insensitive)
 * 2. vCard — starts with `BEGIN:VCARD` (case-insensitive)
 * 3. Phone — starts with `tel:` or matches phone number regex
 * 4. Email — starts with `mailto:`
 * 5. SMS — starts with `smsto:` or `sms:`
 * 6. Geo — starts with `geo:`
 * 7. Social Media — matches known social media URL patterns
 * 8. URL — matches `http://` or `https://` scheme
 * 9. Plain Text — everything else (fallback)
 *
 * This use case is stateless and safe to call from any thread.
 *
 * @see ContentType
 * @see SocialPlatform
 */
class ResultMapperUseCase @Inject constructor() {

    /**
     * Classifies the given [rawValue] into a [ContentType] based on priority-ordered
     * pattern matching.
     *
     * @param rawValue The raw decoded string content from a barcode or QR code scan.
     * @return The classified [ContentType] for the given content.
     */
    fun classify(rawValue: String): ContentType {
        return when {
            isWifi(rawValue) -> ContentType.WIFI
            isVCard(rawValue) -> ContentType.VCARD
            isPhone(rawValue) -> ContentType.PHONE
            isEmail(rawValue) -> ContentType.EMAIL
            isSms(rawValue) -> ContentType.SMS
            isGeo(rawValue) -> ContentType.GEO
            isSocialMedia(rawValue) -> ContentType.SOCIAL_MEDIA
            isUrl(rawValue) -> ContentType.URL
            else -> ContentType.PLAIN_TEXT
        }
    }

    /**
     * 根据条码格式和内容综合分类。
     *
     * EAN-13、EAN-8、UPC-A、UPC-E 等商品条码格式直接归类为 PRODUCT，
     * 其他格式按内容文本进行模式匹配分类。
     *
     * @param rawValue 条码的原始解码字符串
     * @param format 条码格式
     * @return 分类后的内容类型
     */
    fun classify(rawValue: String, format: com.qrscanfast.core.domain.model.BarcodeFormat): ContentType {
        // 商品条码格式直接归类为 PRODUCT
        if (isProductBarcode(format)) {
            return ContentType.PRODUCT
        }
        // 其他格式按内容文本分类
        return classify(rawValue)
    }

    /**
     * 判断条码格式是否为商品条码（EAN/UPC 系列）。
     */
    private fun isProductBarcode(format: com.qrscanfast.core.domain.model.BarcodeFormat): Boolean {
        return format in listOf(
            com.qrscanfast.core.domain.model.BarcodeFormat.EAN_13,
            com.qrscanfast.core.domain.model.BarcodeFormat.EAN_8,
            com.qrscanfast.core.domain.model.BarcodeFormat.UPC_A,
            com.qrscanfast.core.domain.model.BarcodeFormat.UPC_E
        )
    }

    /**
     * Detects which [SocialPlatform] a URL belongs to, if any.
     *
     * This should be called after [classify] returns [ContentType.SOCIAL_MEDIA] to
     * determine the specific platform for deep-link routing.
     *
     * @param rawValue The raw URL string to check against known social media patterns.
     * @return The detected [SocialPlatform], or `null` if no platform matches.
     */
    fun detectSocialPlatform(rawValue: String): SocialPlatform? {
        val lower = rawValue.lowercase()
        return when {
            INSTAGRAM_PATTERN.containsMatchIn(lower) -> SocialPlatform.INSTAGRAM
            TWITTER_PATTERN.containsMatchIn(lower) -> SocialPlatform.TWITTER
            FACEBOOK_PATTERN.containsMatchIn(lower) -> SocialPlatform.FACEBOOK
            LINKEDIN_PATTERN.containsMatchIn(lower) -> SocialPlatform.LINKEDIN
            TIKTOK_PATTERN.containsMatchIn(lower) -> SocialPlatform.TIKTOK
            YOUTUBE_PATTERN.containsMatchIn(lower) -> SocialPlatform.YOUTUBE
            WHATSAPP_PATTERN.containsMatchIn(lower) -> SocialPlatform.WHATSAPP
            else -> null
        }
    }

    // ─── Private classification helpers ─────────────────────────────────────────

    private fun isWifi(value: String): Boolean =
        value.startsWith("WIFI:", ignoreCase = true)

    private fun isVCard(value: String): Boolean =
        value.startsWith("BEGIN:VCARD", ignoreCase = true)

    private fun isPhone(value: String): Boolean =
        value.startsWith("tel:", ignoreCase = true) || PHONE_REGEX.matches(value)

    private fun isEmail(value: String): Boolean =
        value.startsWith("mailto:", ignoreCase = true)

    private fun isSms(value: String): Boolean =
        value.startsWith("smsto:", ignoreCase = true) ||
            value.startsWith("sms:", ignoreCase = true)

    private fun isGeo(value: String): Boolean =
        value.startsWith("geo:", ignoreCase = true)

    private fun isSocialMedia(value: String): Boolean =
        detectSocialPlatform(value) != null

    private fun isUrl(value: String): Boolean =
        URL_PATTERN.containsMatchIn(value)

    companion object {
        /**
         * Regex matching common phone number formats.
         * Accepts optional leading `+`, followed by digits, spaces, hyphens, dots, and parentheses.
         * Requires at least 3 digits to avoid matching short random strings.
         */
        private val PHONE_REGEX = Regex(
            """^\+?[\d\s\-().]{3,}$"""
        )

        /** Matches http:// or https:// URL scheme at the start of the string. */
        private val URL_PATTERN = Regex(
            """^https?://""", RegexOption.IGNORE_CASE
        )

        // ─── Social media domain patterns ───────────────────────────────────────

        /** Instagram: instagram.com/ or instagr.am/ */
        private val INSTAGRAM_PATTERN = Regex(
            """(instagram\.com|instagr\.am)/"""
        )

        /** Twitter/X: twitter.com/ or x.com/ */
        private val TWITTER_PATTERN = Regex(
            """(twitter\.com|x\.com)/"""
        )

        /** Facebook: facebook.com/ or fb.com/ */
        private val FACEBOOK_PATTERN = Regex(
            """(facebook\.com|fb\.com)/"""
        )

        /** LinkedIn: linkedin.com/ */
        private val LINKEDIN_PATTERN = Regex(
            """linkedin\.com/"""
        )

        /** TikTok: tiktok.com/@ */
        private val TIKTOK_PATTERN = Regex(
            """tiktok\.com/@"""
        )

        /** YouTube: youtube.com/ or youtu.be/ */
        private val YOUTUBE_PATTERN = Regex(
            """(youtube\.com|youtu\.be)/"""
        )

        /** WhatsApp: wa.me/ */
        private val WHATSAPP_PATTERN = Regex(
            """wa\.me/"""
        )
    }
}
