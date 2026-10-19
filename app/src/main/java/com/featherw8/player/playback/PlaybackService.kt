package com.featherw8.player.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
    private var activeDurationMs = 0L
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
            // Playing local files: hold a partial wake lock while playing. Without it a phone with the screen
            // off may let the CPU sleep between audio buffers and delay everything on the main thread,
            // including the play counter's polling. (The WAKE_LOCK permission was declared but never used.)
            .setWakeMode(C.WAKE_MODE_LOCAL)
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
                activeDurationMs = knownDurationMs(mediaItem)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // A pause closes a stretch of listening and a resume opens one: take the player's position
                // at that moment instead of waiting for the next poll, which may be seconds away.
                updateActualPlayTime()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val now = SystemClock.elapsedRealtime()
                val oldId = oldPosition.mediaItem?.mediaId
                // By track, not by index: moving the playing track within the queue changes its index but
                // it is still the same listening. (A track repeating itself is an automatic transition.)
                val leftTrack = reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION ||
                    oldId != newPosition.mediaItem?.mediaId
                when {
                    // The track ended or was skipped: what was listened to up to that point still counts.
                    leftTrack -> finishTrack(oldId, oldPosition.positionMs, now)
                    reason == Player.DISCONTINUITY_REASON_SEEK -> {
                        val counted = actualPlayCounter.onSeek(
                            fromMs = oldPosition.positionMs,
                            toMs = newPosition.positionMs,
                            isPlaying = player.isPlaying,
                            durationMs = activeDurationMs,
                            nowMs = now
                        )
                        if (counted) countPlay(oldId)
                    }
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
            activeDurationMs = knownDurationMs(item)
        }
        if (id.isNullOrBlank()) return

        // The player's own duration is the better one once it knows it.
        player.duration.takeIf { it > 0L }?.let { activeDurationMs = it }
        val counted = actualPlayCounter.sample(
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition,
            durationMs = activeDurationMs,
            nowMs = SystemClock.elapsedRealtime()
        )
        if (counted) countPlay(id)
    }

    /** The track is over (ended, or skipped): count it if it had been listened to enough, then start over. */
    private fun finishTrack(id: String?, endPositionMs: Long, nowMs: Long) {
        if (!id.isNullOrBlank() &&
            actualPlayCounter.sample(isPlaying = true, positionMs = endPositionMs, durationMs = activeDurationMs, nowMs = nowMs)
        ) {
            countPlay(id)
        }
        actualPlayCounter = ActualPlayCounter()
    }

    private fun countPlay(id: String?) {
        if (!id.isNullOrBlank()) PlayCountStore.increment(this, id)
    }

    /**
     * The duration the 90 % rule works with. Some files (VBR MP3 without a length header, for one) have
     * no duration in the player; the one the library found is carried on the item for that case.
     */
    private fun knownDurationMs(item: MediaItem?): Long =
        player.duration.takeIf { it > 0L } ?: item?.mediaMetadata?.durationMs ?: 0L

    override fun onDestroy() {
        handler.removeCallbacks(countMonitor)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
