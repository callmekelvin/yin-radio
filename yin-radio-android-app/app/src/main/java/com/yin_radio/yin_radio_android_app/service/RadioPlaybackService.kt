package com.yin_radio.yin_radio_android_app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.yin_radio.yin_radio_android_app.R

@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    companion object {
        private const val CHANNEL_ID = "yin_radio_playback"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        Log.v("RadioPlaybackService", "onCreate()")
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        mediaSession = MediaSession.Builder(this, player!!).build()

        createNotificationChannel()

        player!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    if (player!!.isPlaying) {
                        startForeground()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startForeground()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (player!!.isPlaying) {
                    startForeground()
                }
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        Log.v("RadioPlaybackService", "onDestroy()")
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        Log.v("RadioPlaybackService", "onTaskRemoved()")
        val p = player ?: return
        if (!p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        Log.v("RadioPlaybackService", "createNotificationChannel()")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for Yin Radio playback"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForeground() {
        Log.v("RadioPlaybackService", "startForeground()")
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val mediaItem = player?.currentMediaItem
        val title = mediaItem?.mediaMetadata?.title ?: getString(R.string.app_name)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(getString(R.string.playback_notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
            )
            .build()
    }
}
