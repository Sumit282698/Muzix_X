package com.sumit.muzixx.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        private const val CHANNEL_ID = "muzixx_playback_channel"
    }

    private var mediaSession: MediaSession? = null

    private lateinit var player: ExoPlayer

    private val mediaSessionCallback =
        object : MediaSession.Callback {

            @OptIn(UnstableApi::class)
            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

                val mediaItems = buildList {

                    for (i in 0 until player.mediaItemCount) {
                        add(player.getMediaItemAt(i))
                    }
                }

                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        mediaItems,
                        player.currentMediaItemIndex,
                        player.currentPosition
                    )
                )
            }
        }

    @OptIn(UnstableApi::class)
    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .build()
        )

        val okHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->

                    val request =
                        chain.request()
                            .newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0"
                            )
                            .build()

                    chain.proceed(request)
                }
                .build()

        // this uses okHttp to stream
        val okHttpFactory =
            OkHttpDataSource.Factory(okHttpClient)

        val dataSourceFactory =
            DefaultDataSource.Factory(
                this,
                okHttpFactory
            )

        player =
            ExoPlayer.Builder(this)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(this)
                        .setDataSourceFactory(dataSourceFactory)
                )
                .build()

        mediaSession =
            MediaSession.Builder(this, player)
                .setCallback(mediaSessionCallback)
                .build()

        Log.d(TAG, "PlaybackService initialized")
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {

        return mediaSession
    }

    override fun onDestroy() {

        mediaSession?.run {

            player.release()

            release()
        }

        mediaSession = null

        super.onDestroy()

        Log.d(TAG, "PlaybackService destroyed")
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager?.createNotificationChannel(channel)
        }
    }
}