package com.qrscanmax.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Builder utility for creating share intents for text and image content.
 *
 * Simplifies the creation of Android share sheet intents used throughout
 * the app (e.g., sharing scan results, generated QR codes, product info).
 */
object ShareIntentBuilder {

    /**
     * Creates a text share intent for the system share sheet.
     *
     * @param text The text content to share.
     * @param subject Optional subject line (used by email clients).
     * @param chooserTitle Optional title for the share chooser dialog.
     * @return An [Intent] configured for text sharing via the system share sheet.
     */
    fun shareText(
        text: String,
        subject: String? = null,
        chooserTitle: String = "Share via"
    ): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (subject != null) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
        }
        return Intent.createChooser(shareIntent, chooserTitle)
    }

    /**
     * Creates an image share intent for the system share sheet.
     *
     * @param imageUri The content URI of the image to share.
     * @param text Optional text to include alongside the image.
     * @param chooserTitle Optional title for the share chooser dialog.
     * @return An [Intent] configured for image sharing via the system share sheet.
     */
    fun shareImage(
        imageUri: Uri,
        text: String? = null,
        chooserTitle: String = "Share via"
    ): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            if (text != null) {
                putExtra(Intent.EXTRA_TEXT, text)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, chooserTitle)
    }

    /**
     * Launches a text share intent from the given context.
     *
     * Convenience method that creates and starts the share activity.
     *
     * @param context The Android context to start the activity from.
     * @param text The text content to share.
     * @param subject Optional subject line.
     */
    fun launchTextShare(
        context: Context,
        text: String,
        subject: String? = null
    ) {
        val intent = shareText(text, subject)
        context.startActivity(intent)
    }

    /**
     * Launches an image share intent from the given context.
     *
     * Convenience method that creates and starts the share activity.
     *
     * @param context The Android context to start the activity from.
     * @param imageUri The content URI of the image to share.
     * @param text Optional text to include alongside the image.
     */
    fun launchImageShare(
        context: Context,
        imageUri: Uri,
        text: String? = null
    ) {
        val intent = shareImage(imageUri, text)
        context.startActivity(intent)
    }
}
