package com.qrscanmax.core.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Utility helpers for camera and storage permission checks.
 *
 * Provides methods to check permission status and create intents
 * for navigating to system settings when permissions are denied.
 */
object PermissionUtils {

    /**
     * The camera permission string constant.
     */
    const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    /**
     * Returns the appropriate storage permission based on the device's API level.
     *
     * - API 33+ (TIRAMISU): Uses [Manifest.permission.READ_MEDIA_IMAGES]
     * - Below API 33: Uses [Manifest.permission.READ_EXTERNAL_STORAGE]
     */
    val storageReadPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /**
     * Checks whether the camera permission has been granted.
     *
     * @param context The Android context for permission checking.
     * @return `true` if camera permission is granted, `false` otherwise.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks whether the storage read permission has been granted.
     *
     * Automatically selects the correct permission based on API level.
     *
     * @param context The Android context for permission checking.
     * @return `true` if storage read permission is granted, `false` otherwise.
     */
    fun hasStorageReadPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            storageReadPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Creates an intent to open the app's system settings page.
     *
     * Used when the user has permanently denied a permission and needs
     * to manually grant it from system settings.
     *
     * @param context The Android context to derive the package name.
     * @return An [Intent] that opens the app's settings detail page.
     */
    fun createAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Opens the app's system settings page.
     *
     * Convenience method that creates and starts the settings activity.
     *
     * @param context The Android context to start the activity from.
     */
    fun openAppSettings(context: Context) {
        context.startActivity(createAppSettingsIntent(context))
    }
}
