package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.R
import com.google.common.collect.ImmutableList

/**
 * Manages the display of playback controls in the system notification tray,
 * including play/pause and track navigation (previous/next), seek controls,
 * artwork loading, and interaction intents.
 */
@OptIn(UnstableApi::class)
class MediaNotificationManager(
    private val context: Context
) : MediaNotification.Provider {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "myato_playback_channel"
        const val NOTIFICATION_ID = 2001
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Default MediaNotification provider from Media3 handling media session style actions
    private val defaultNotificationProvider: DefaultMediaNotificationProvider =
        DefaultMediaNotificationProvider.Builder(context)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.playback_notification_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()

    init {
        createNotificationChannel()
    }

    /**
     * Creates and configures the notification channel for playback controls.
     * Uses IMPORTANCE_LOW to prevent intrusive sounds while retaining persistent
     * controls in the system notification tray and lock screen.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.playback_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.playback_notification_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the MediaNotification containing playback controls:
     * play/pause, skip to next, skip to previous, track metadata, and album art.
     */
    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        return defaultNotificationProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            onNotificationChangedCallback
        )
    }

    override fun handleCustomCommand(
        mediaSession: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean {
        return defaultNotificationProvider.handleCustomCommand(mediaSession, action, extras)
    }

    /**
     * Returns a PendingIntent to open the MainActivity when the notification widget is clicked.
     */
    fun createSessionActivityPendingIntent(): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Explicitly cancels the media playback notification.
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
