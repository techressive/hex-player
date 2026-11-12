package com.techressive.hex.audio

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class HexPlaybackService : MediaSessionService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return HexAudioPlayer.activeMediaSession
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
