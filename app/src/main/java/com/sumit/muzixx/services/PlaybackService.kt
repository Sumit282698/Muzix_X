package com.sumit.muzixx.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sumit.muzixx.data.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        private const val CHANNEL_ID = "muzixx_playback_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val callback = @UnstableApi
    object : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: androidx.media3.session.SessionCommand,
            args: android.os.Bundle
        ): ListenableFuture<androidx.media3.session.SessionResult> {

            when (customCommand.customAction) {
                "ACTION_SET_SKIP_SILENCE" -> {
                    val enabled = args.getBoolean("enabled", false)
                    player.skipSilenceEnabled = enabled
                    Log.d("PlaybackService", "Live update: Skip Silence set to $enabled")
                }
                "ACTION_SET_NORMALIZATION" -> {
                    val enabled = args.getBoolean("enabled", false)
                    // Tear down and rebuild the effects envelope pipeline immediately on the current session ID
                    setupAudioEffectsPipeline(enabled)
                    Log.d("PlaybackService", "Live update: Audio Normalization set to $enabled")
                }
            }

            return Futures.immediateFuture(
                androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
            )
        }
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

            val items = (0 until player.mediaItemCount)
                .map { player.getMediaItemAt(it) }

            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    items,
                    player.currentMediaItemIndex,
                    player.currentPosition
                )
            )
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createChannel()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .build()

        val dataSourceFactory = DefaultDataSource.Factory(
            this,
            OkHttpDataSource.Factory(okHttpClient)
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 5000,
                /* maxBufferMs = */ 15000,
                /* bufferForPlaybackMs = */ 1500,
                /* bufferForPlaybackAfterRebufferMs = */ 3000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setLoadControl(loadControl)
            .build()

        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
        player.shuffleModeEnabled = false

        val settingsManager = SettingsManager(this)
        serviceScope.launch {
            try {
                val skipSilenceEnabled = settingsManager.skipSilenceFlow.first()
                val normalizeEnabled = settingsManager.normalizeAudioFlow.first()

                player.skipSilenceEnabled = skipSilenceEnabled
                Log.d(TAG, "ExoPlayer initialized with Skip Silence = $skipSilenceEnabled")

                setupAudioEffectsPipeline(normalizeEnabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed applying initial playback settings variables", e)
            }
        }

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()

        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(callback)
            .build()

        Log.d(TAG, "PlaybackService READY with Instant Pre-Buffering optimizations.")
    }

    @OptIn(UnstableApi::class)
    private fun setupAudioEffectsPipeline(normalizeEnabled: Boolean) {
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                super.onAudioSessionIdChanged(audioSessionId)

                loudnessEnhancer?.release()
                loudnessEnhancer = null

                if (normalizeEnabled && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    try {
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                            setTargetGain(200)
                            enabled = true
                        }
                        Log.d(TAG, "LoudnessEnhancer active on target session ID: $audioSessionId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Hardware driver failed to instantiate LoudnessEnhancer audio filter", e)
                    }
                }
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        mediaSession?.release()
        player.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MuzixX Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }
}