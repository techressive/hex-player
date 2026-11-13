package com.techressive.hex.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class HexPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "hex_playback_channel"
        const val DEFAULT_CHANNEL_ID = "default_channel_id"

        var instance: HexPlaybackService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()

        HexAudioPlayer.activeMediaSession?.let { session ->
            if (!sessions.contains(session)) {
                addSession(session)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        HexAudioPlayer.activeMediaSession?.let { session ->
            if (!sessions.contains(session)) {
                addSession(session)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return HexAudioPlayer.activeMediaSession
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val hexChannel = NotificationChannel(
                CHANNEL_ID,
                "HEX Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audiophile playback controls and active track status"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(hexChannel)

            val defaultChannel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                "HEX Playback Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "System media session notification channel"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(defaultChannel)
        }
    }
}
