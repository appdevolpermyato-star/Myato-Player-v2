package com.example.ui.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper class to manage storage/audio and notification permissions
 * for Android 13+ (Tiramisu, API 33) and legacy Android versions.
 */
object PermissionHelper {

    /**
     * Returns the appropriate audio permission depending on Android version.
     * Android 13+ uses READ_MEDIA_AUDIO, earlier versions use READ_EXTERNAL_STORAGE.
     */
    fun getAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * Returns the POST_NOTIFICATIONS permission required for Android 13+
     * to show the media playback notification/widget in the notification drawer.
     */
    fun getNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }

    /**
     * Returns an array of all permissions required upon app startup.
     */
    fun getStartupPermissions(): Array<String> {
        val permissions = mutableListOf(getAudioPermission())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    /**
     * Checks if the app has permission to read audio files from device storage.
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            getAudioPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if the app has permission to post notifications (status bar media player widget).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Checks if all startup permissions (audio reading + notifications) are granted.
     */
    fun hasAllStartupPermissions(context: Context): Boolean {
        return hasAudioPermission(context) && hasNotificationPermission(context)
    }

    /**
     * Checks whether the user should be shown a rationale before requesting the permission.
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}
