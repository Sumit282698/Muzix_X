package com.sumit.muzixx.data.manager

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.services.PlaybackService
import com.sumit.muzixx.data.network.JioSaavnApiService
import kotlinx.coroutines.*

class PlayerController(
    private val scope: CoroutineScope,
    private val onPlaybackStarted: (Boolean, MediaController?) -> Unit,
    private val onPlaybackStopped: () -> Unit,
    private val onTrackSwitched: (Song) -> Unit,
    private val onQueueUpdated: (List<Song>) -> Unit,
    private val resolveYouTubeStream: suspend (Song) -> String?,
    private val jioSaavnApiService: JioSaavnApiService,
    private val getAudioQualityPreference: () -> String
) {

    companion object {
        private const val TAG = "PlayerController"
    }

    var mediaController: MediaController? = null
    var isPlaying by mutableStateOf(false)
    var selectedSong by mutableStateOf<Song?>(null)
    var currentPosition by mutableLongStateOf(0L)
    var totalDuration by mutableLongStateOf(0L)

    var activePlaybackQueue = mutableStateListOf<Song>()
        private set

    var activePlaylistIndex by mutableIntStateOf(0)
        private set

    fun initMediaController(context: Context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            controller.addListener(playerListener)
            Log.d(TAG, "MediaController successfully attached to PlaybackService.")
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            val controller = mediaController ?: return

            val exoIndex = controller.currentMediaItemIndex

            if (exoIndex in activePlaybackQueue.indices) {
                activePlaylistIndex = exoIndex

                val currentMediaId = mediaItem?.mediaId
                val matchedSong = activePlaybackQueue.find { it.id == currentMediaId }
                    ?: activePlaybackQueue[exoIndex]

                selectedSong = matchedSong

                // 💡 UPDATED: Pass matchedSong safely out to the callback trigger pipeline
                onTrackSwitched(matchedSong)

                if (matchedSong.uri.isBlank()) {
                    resolveAndPlayTrackOnDemand(matchedSong, exoIndex)
                }
            }

            totalDuration = controller.duration.coerceAtLeast(0L)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_READY) {
                mediaController?.let { totalDuration = it.duration.coerceAtLeast(0L) }
            }
            if (playbackState == Player.STATE_ENDED) {
                onPlaybackStopped()
            }
        }

        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            isPlaying = isPlayingNow
            if (isPlayingNow) {
                mediaController?.let { totalDuration = it.duration.coerceAtLeast(0L) }
                onPlaybackStarted(isPlayingNow, mediaController)
            } else {
                onPlaybackStopped()
            }
        }
    }

    private suspend fun resolveSongUri(song: Song): String {
        if (song.uri.isNotBlank() && (song.uri.startsWith("http") || song.uri.startsWith("content://"))) {
            return song.uri
        }

        val explicitType = song.type.lowercase().trim()
        val rawId = song.id.trim()

        val cleanId = if (rawId.contains("_") && explicitType != "yt") {
            rawId.substringBefore("_")
        } else {
            rawId
        }

        val isSaavn = explicitType == "saavn" || (explicitType.isBlank() && cleanId.all { it.isDigit() })
        val isYoutube = explicitType == "yt" || (explicitType.isBlank() && cleanId.startsWith("yt_"))

        return when {
            isYoutube -> {
                Log.d(TAG, "Resolving stream link from YouTube ID: $cleanId")
                resolveYouTubeStream(song) ?: ""
            }
            isSaavn -> {
                var saavnUrl = ""
                try {
                    Log.d(TAG, "Resolving stream link via JioSaavn for ID: $cleanId")
                    val response = jioSaavnApiService.getSongDetailsById(cleanId)
                    val trackData = response.data?.firstOrNull()
                    val downloadUrls = trackData?.downloadUrl ?: emptyList()

                    // Fetch the user's active quality preference string
                    val preferredQuality = getAudioQualityPreference()
                    Log.d(TAG, "User preferred audio quality setting is: $preferredQuality")

                    saavnUrl = when {
                        downloadUrls.isEmpty() -> ""

                        preferredQuality.contains("96") -> {
                            downloadUrls.find { it.quality?.contains("96kbps") == true }?.url
                                ?: downloadUrls.firstOrNull()?.url
                                ?: ""
                        }
                        preferredQuality.contains("160") -> {
                            downloadUrls.find { it.quality?.contains("160kbps") == true }?.url
                                ?: downloadUrls.getOrNull(downloadUrls.size / 2)?.url
                                ?: downloadUrls.firstOrNull()?.url
                                ?: ""
                        }
                        preferredQuality.contains("320") -> {
                            downloadUrls.find { it.quality?.contains("320kbps") == true }?.url
                                ?: downloadUrls.lastOrNull()?.url
                                ?: ""
                        }
                        else -> {
                            downloadUrls.lastOrNull()?.url ?: ""
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed retrieving JioSaavn media endpoint", e)
                    saavnUrl = ""
                }
                saavnUrl
            }
            else -> {
                Log.d(TAG, "Playing local asset path: ${song.uri}")
                song.uri
            }
        }
    }

    private fun resolveAndPlayTrackOnDemand(song: Song, engineIndex: Int) {
        val controller = mediaController ?: return
        scope.launch {
            val resolvedUri = resolveSongUri(song)
            if (!resolvedUri.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    if (engineIndex in activePlaybackQueue.indices && activePlaybackQueue[engineIndex].id == song.id) {
                        activePlaybackQueue[engineIndex] = song.copy(uri = resolvedUri)

                        val updatedMediaItem = buildMediaItem(activePlaybackQueue[engineIndex], resolvedUri)
                        controller.replaceMediaItem(engineIndex, updatedMediaItem)
                    }
                }
            }
        }
    }

    private fun buildMediaItem(song: Song, streamUrl: String): MediaItem {
        val safeUri = if (streamUrl.isBlank() || streamUrl == song.id) {
            "https://localhost/placeholder.mp3"
        } else {
            streamUrl
        }

        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(safeUri.toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setSubtitle(song.folderName)
                    .setArtworkUri(song.artUri?.toUri())
                    .build()
            )
            .build()
    }

    fun submitQueueToPlayer(songList: List<Song>, startIndex: Int, playWhenReady: Boolean = true) {
        if (songList.isEmpty() || startIndex !in songList.indices) return

        val controller = mediaController ?: run {
            Log.e(TAG, "MediaController framework not initialized yet.")
            return
        }

        activePlaybackQueue.clear()
        activePlaybackQueue.addAll(songList)
        activePlaylistIndex = startIndex
        onQueueUpdated(activePlaybackQueue.toList())

        val targetedSong = activePlaybackQueue[activePlaylistIndex]

        scope.launch {
            Log.d(TAG, "Resolving URL path for initial track: ${targetedSong.title}")
            val resolvedUri = withContext(Dispatchers.IO) { resolveSongUri(targetedSong) }

            withContext(Dispatchers.Main) {
                val finalUri = if (resolvedUri.isBlank()) targetedSong.uri else resolvedUri

                if (activePlaylistIndex in activePlaybackQueue.indices) {
                    activePlaybackQueue[activePlaylistIndex] = targetedSong.copy(uri = finalUri)
                }

                val mediaItems = activePlaybackQueue.mapIndexed { index, song ->
                    val streamUrl = if (index == activePlaylistIndex) finalUri else song.uri
                    buildMediaItem(song, streamUrl)
                }

                controller.stop()
                controller.setMediaItems(mediaItems, activePlaylistIndex, 0L)
                controller.prepare()

                if (playWhenReady) {
                    controller.play()
                    Log.d(TAG, "ExoPlayer playback initialized successfully.")
                } else {
                    controller.pause()
                    Log.d(TAG, "ExoPlayer track hydrated silently for persistence setup.")
                }

                // Prefetch next track sequence...
                val nextIndex = activePlaylistIndex + 1
                if (nextIndex in activePlaybackQueue.indices) {
                    val nextSong = activePlaybackQueue[nextIndex]
                    if (nextSong.uri.isBlank()) {
                        val nextUri = withContext(Dispatchers.IO) { resolveSongUri(nextSong) }
                        if (nextUri.isNotBlank()) {
                            activePlaybackQueue[nextIndex] = nextSong.copy(uri = nextUri)
                            controller.replaceMediaItem(nextIndex, buildMediaItem(activePlaybackQueue[nextIndex], nextUri))
                        }
                    }
                }
            }
        }
    }

    fun injectTracksToQueue(suggestedSongs: List<Song>) {
        val controller = mediaController ?: return
        if (suggestedSongs.isEmpty()) return

        val oldSize = activePlaybackQueue.size
        activePlaybackQueue.addAll(suggestedSongs)
        onQueueUpdated(activePlaybackQueue.toList())

        val newMediaItems = suggestedSongs.map { buildMediaItem(it, it.uri) }
        controller.addMediaItems(oldSize, newMediaItems)
    }

    fun setSkipSilenceOnPlayer(enabled: Boolean) {
        scope.launch(Dispatchers.Main) {
            try {
                mediaController?.let { controller ->
                    if (controller.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) {

                    }

                    val args = android.os.Bundle().apply { putBoolean("enabled", enabled) }
                    controller.sendCustomCommand(
                        androidx.media3.session.SessionCommand("ACTION_SET_SKIP_SILENCE", android.os.Bundle.EMPTY),
                        args
                    )
                    Log.d("PlayerController", "Dispatched live Skip Silence event down to service channel: $enabled")
                }
            } catch (e: Exception) {
                Log.e("PlayerController", "Failed setting live hardware Skip Silence filter modification", e)
            }
        }
    }

    fun setAudioNormalizationOnPlayer(enabled: Boolean) {
        scope.launch(Dispatchers.Main) {
            try {
                mediaController?.let { controller ->
                    val args = android.os.Bundle().apply { putBoolean("enabled", enabled) }
                    controller.sendCustomCommand(
                        androidx.media3.session.SessionCommand("ACTION_SET_NORMALIZATION", android.os.Bundle.EMPTY),
                        args
                    )
                    Log.d("PlayerController", "Dispatched live Normalization event down to service channel: $enabled")
                }
            } catch (e: Exception) {
                Log.e("PlayerController", "Failed setting live audio Normalization equalizer filter state", e)
            }
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        currentPosition = position
    }

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun playNext() {
        mediaController?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNextMediaItem()
            }
        }
    }

    fun playPrevious() {
        mediaController?.let {
            if (it.hasPreviousMediaItem()) {
                it.seekToPreviousMediaItem()
            }
        }
    }

    fun release() {
        mediaController?.removeListener(playerListener)
        mediaController = null
    }
}