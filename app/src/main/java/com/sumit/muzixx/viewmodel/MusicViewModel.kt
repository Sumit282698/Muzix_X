package com.sumit.muzixx.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sumit.muzixx.data.Playlist
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.services.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicViewModel : ViewModel() {
    enum class RepeatMode { NONE, ALL, ONE }

    companion object {
        private const val TAG = "MusicViewModel"
    }

    var currentRepeatMode by mutableStateOf(RepeatMode.NONE)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    private var mediaController: MediaController? = null

    val songs = mutableStateListOf<Song>()
    val searchResults = mutableStateListOf<Song>()

    var selectedSong by mutableStateOf<Song?>(null)

    var currentPosition by mutableLongStateOf(0L)
        private set
    var totalDuration by mutableLongStateOf(0L)
        private set

    val playlists = mutableStateListOf<Playlist>()
    var selectedPlaylist by mutableStateOf<Playlist?>(null)

    private var sharedPreferences: SharedPreferences? = null
    private val gson = Gson()

    private val audioExtractor = com.sumit.muzixx.data.network.YouTubeAudioExtractor()

    init {
        viewModelScope.launch {
            while (true) {
                mediaController?.let {
                    if (it.isPlaying) {
                        currentPosition = it.currentPosition
                        // Dynamically lock the total duration bar for streaming links on the fly
                        if (totalDuration <= 0L || selectedSong?.isStreaming == true) {
                            totalDuration = it.duration.coerceAtLeast(0L)
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    fun initStorage(context: Context) {
        sharedPreferences = context.getSharedPreferences("muzix_prefs", Context.MODE_PRIVATE)
        loadPlaylistsFromStorage()
    }

    private fun loadPlaylistsFromStorage() {
        try {
            val json = sharedPreferences?.getString("custom_playlists", null)
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<Playlist>>() {}.type
                val savedPlaylists: List<Playlist> = gson.fromJson(json, type)

                playlists.clear()
                savedPlaylists.forEach { playlist ->
                    playlists.add(Playlist(id = playlist.id, name = playlist.name, songs = playlist.songs))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            sharedPreferences?.edit { remove("custom_playlists") }
        }
    }

    private fun savePlaylistsToStorage() {
        val customLists = playlists.filter { it.id != "local_songs" }
        val json = gson.toJson(customLists)
        sharedPreferences?.edit { putString("custom_playlists", json) }
    }

    fun createCustomPlaylist(name: String) {
        if (name.isNotBlank()) {
            val newPlaylist = Playlist(id = UUID.randomUUID().toString(), name = name, songs = emptyList())
            playlists.add(newPlaylist)
            savePlaylistsToStorage()
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val targetPlaylist = playlists[index]
            if (!targetPlaylist.songs.contains(song)) {
                val updatedSongs = targetPlaylist.songs + song
                playlists[index] = targetPlaylist.copy(songs = updatedSongs)
                savePlaylistsToStorage()

                if (selectedPlaylist?.id == playlistId) {
                    selectedPlaylist = playlists[index]
                }
            }
        }
    }

    fun loadSongs(songList: List<Song>) {
        songs.clear()
        songs.addAll(songList)
    }

    fun loadSearchResults(results: List<Song>) {
        searchResults.clear()
        searchResults.addAll(results)
    }

    fun initializeLocalSongsPlaylist() {
        if (playlists.none { it.id == "local_songs" }) {
            playlists.add(0, Playlist(id = "local_songs", name = "Local Songs", songs = songs))
        }
    }

    fun playSong(context: Context, songList: List<Song>, startIndex: Int) {
        if (songList.isEmpty() || startIndex !in songList.indices) return

        val song = songList[startIndex]

        viewModelScope.launch {
            val controller = mediaController ?: run {
                Log.e(TAG, "MediaController framework not initialized yet.")
                return@launch
            }

            if (!song.isStreaming) {
                Log.d(TAG, "Playing local offline track: ${song.id}")

                val mediaItems = songList.map { track ->
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setArtworkUri(track.artUri?.toUri())
                        .build()

                    MediaItem.Builder()
                        .setMediaId(track.id)
                        .setUri(track.uri)
                        .setMediaMetadata(mediaMetadata)
                        .build()
                }

                selectedSong = song
                controller.setMediaItems(mediaItems, startIndex, 0L)
                controller.prepare()
                controller.play()
                return@launch
            }

            Log.d(TAG, "Requesting backend video extraction context for streaming ID: ${song.id}")

            val enrichedStreamingSong = audioExtractor.getSongFromVideoId(song.id)

            if (enrichedStreamingSong != null && enrichedStreamingSong.uri.isNotBlank()) {
                Log.d(TAG, "Link extraction success! Building media track element.")

                val mediaItems = songList.map { track ->
                    val isTarget = (track.id == enrichedStreamingSong.id)
                    val mediaMetadataItem = MediaMetadata.Builder()
                        .setTitle(if (isTarget) enrichedStreamingSong.title else track.title)
                        .setArtist(if (isTarget) enrichedStreamingSong.artist else track.artist)
                        .setArtworkUri(if (isTarget) enrichedStreamingSong.artUri?.toUri() else track.artUri?.toUri())
                        .build()

                    MediaItem.Builder()
                        .setMediaId(track.id)
                        .setUri(if (isTarget) enrichedStreamingSong.uri.toUri() else track.id.toUri())
                        .setMediaMetadata(mediaMetadataItem)
                        .build()
                }

                selectedSong = enrichedStreamingSong

                withContext(Dispatchers.Main) {
                    controller.setMediaItems(mediaItems, startIndex, 0L)
                    controller.prepare()
                    controller.play()
                }
            } else {
                Log.e(TAG, "Direct streaming link extraction pipeline returned null/empty payload.")
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)

            mediaController?.let { controller ->
                if (mediaItem == null) return

                val metadata = mediaItem.mediaMetadata
                val mediaId = mediaItem.mediaId

                val checkStreaming = mediaId.startsWith("http") || !mediaId.all { it.isDigit() }

                val currentTrack = Song(
                    id = mediaId,
                    title = metadata.title?.toString() ?: "Unknown Title",
                    artist = metadata.artist?.toString() ?: "Unknown Artist",
                    uri = mediaItem.localConfiguration?.uri?.toString() ?: "",
                    artUri = metadata.artworkUri?.toString(),
                    duration = if (checkStreaming) 0L else controller.duration.coerceAtLeast(0L),
                    isStreaming = checkStreaming
                )

                val currentIndex = controller.currentMediaItemIndex

                if (currentTrack.isStreaming && (mediaItem.localConfiguration?.uri?.toString() == currentTrack.id || mediaItem.localConfiguration?.uri?.toString()?.length == 11)) {
                    viewModelScope.launch {
                        Log.d(TAG, "Running auto-advance engine extraction check for track: ${currentTrack.id}")
                        val freshTrackInfo = audioExtractor.getSongFromVideoId(currentTrack.id)

                        if (freshTrackInfo != null && freshTrackInfo.uri.isNotBlank()) {
                            val updatedItem = mediaItem.buildUpon().setUri(freshTrackInfo.uri.toUri()).build()
                            withContext(Dispatchers.Main) {
                                controller.replaceMediaItem(currentIndex, updatedItem)
                                controller.prepare()
                                controller.play()
                            }
                        }
                    }
                }

                selectedSong = currentTrack
                totalDuration = if (currentTrack.isStreaming) {
                    controller.duration.coerceAtLeast(0L)
                } else {
                    controller.duration.coerceAtLeast(0L)
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            mediaController?.let { controller ->
                if (playbackState == Player.STATE_READY) {
                    totalDuration = controller.duration.coerceAtLeast(0L)
                }
            }
        }

        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            isPlaying = isPlayingNow
            if (isPlayingNow) {
                mediaController?.let { controller ->
                    totalDuration = controller.duration.coerceAtLeast(0L)
                }
            }
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        currentPosition = position
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
            } else {
                it.play()
                isPlaying = true
            }
        }
    }

    fun toggleRepeatMode() {
        currentRepeatMode = when (currentRepeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }

        mediaController?.repeatMode = when (currentRepeatMode) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
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

    fun initMediaController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.removeListener(playerListener)
        mediaController = null
    }
}