package com.example.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.ui.music.AudioEffectManager

/**
 * Foreground MediaSessionService that provides continuous background playback
 * and controls the system notification bar media player widget via MediaNotificationManager.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var mediaNotificationManager: MediaNotificationManager? = null

    companion object {
        var currentSession: MediaSession? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize MediaNotificationManager to handle notification tray playback controls
        val notificationManager = MediaNotificationManager(this)
        mediaNotificationManager = notificationManager

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sessionId = player.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                        AudioEffectManager.instance.initEffects(sessionId)
                    }
                }
            }
        })

        // Configure session with pending intent to open app on notification click
        val session = MediaSession.Builder(this, player)
            .setSessionActivity(notificationManager.createSessionActivityPendingIntent())
            .build()
        mediaSession = session
        currentSession = session

        // Set the custom MediaNotificationManager as the media notification provider
        setMediaNotificationProvider(notificationManager)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        AudioEffectManager.instance.release()
        mediaNotificationManager?.cancelNotification()
        mediaNotificationManager = null
        currentSession = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
