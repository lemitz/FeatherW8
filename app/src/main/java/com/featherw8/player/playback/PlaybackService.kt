package com.featherw8.player.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.featherw8.player.MainActivity
import com.featherw8.player.audio.DsdExtractor
import com.featherw8.player.data.ActualPlayCounter
import com.featherw8.player.data.PlayCountStore

class PlaybackService : MediaSessionService() {
    private companion object {
        const val PREFS_NAME = "featherw8"
        const val PREF_REPEAT_MODE = "playback_repeat_mode"
        const val PREF_SHUFFLE_ENABLED = "playback_shuffle_enabled"
    }

    private lateinit var player: androidx.media3.exoplayer.ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val handler = Handler(Looper.getMainLooper())
    private var activeMediaId: String? = null
    private var actualPlayCounter = ActualPlayCounter()

    private val countMonitor = object : Runnable {
        override fun run() {
            updateActualPlayTime()
            handler.postDelayed(this, 250L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val defaultExtractors = DefaultExtractorsFactory()
        val extractors = ExtractorsFactory {
            arrayOf<androidx.media3.extractor.Extractor>(DsdExtractor()) + defaultExtractors.createExtractors()
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(this, extractors)
        player = androidx.media3.exoplayer.ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        player.repeatMode = when (preferences.getInt(PREF_REPEAT_MODE, Player.REPEAT_MODE_OFF)) {
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        player.shuffleModeEnabled = preferences.getBoolean(PREF_SHUFFLE_ENABLED, false)
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                activeMediaId = mediaItem?.mediaId
                actualPlayCounter = ActualPlayCounter()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    actualPlayCounter.onSeek()
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                preferences.edit().putInt(PREF_REPEAT_MODE, repeatMode).apply()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                preferences.edit().putBoolean(PREF_SHUFFLE_ENABLED, shuffleModeEnabled).apply()
            }
        })
        // What a tap on the player card in the notification shade (or on the lock screen) opens.
        // Without it some systems, Nothing OS among them, do nothing when the card is tapped.
        val openPlayer = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_PLAYER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player).setSessionActivity(openPlayer).build()
        handler.post(countMonitor)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    private fun updateActualPlayTime() {
        val item = player.currentMediaItem
        val id = item?.mediaId
        if (id != activeMediaId) {
            activeMediaId = id
            actualPlayCounter = ActualPlayCounter()
        }
        if (id.isNullOrBlank()) return

        if (actualPlayCounter.sample(player.isPlaying, player.currentPosition, player.duration)) {
            PlayCountStore.increment(this, id)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(countMonitor)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
