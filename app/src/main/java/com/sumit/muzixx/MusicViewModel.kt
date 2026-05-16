package com.sumit.muzixx

import android.content.Context
import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    enum class RepeatMode { NONE, ALL, ONE }

    var currentRepeatMode by mutableStateOf(RepeatMode.NONE)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    private var mediaController: MediaController? = null
    val songs = mutableStateListOf<Song>()
    var selectedSong by mutableStateOf<Song?>(null)

    var currentPosition by mutableLongStateOf(0L)
        private set
    var totalDuration by mutableLongStateOf(0L)
        private set

    private var exoPlayer: Player? = null

    init {
        viewModelScope.launch {
            while (true) {
                exoPlayer?.let {
                    if (it.isPlaying) {
                        currentPosition = it.currentPosition
                        totalDuration = it.duration.coerceAtLeast(0L)
                    }
                }
                delay(1000)
            }
        }
    }

    fun loadSongs(songList: List<Song>) {
        songs.clear()
        songs.addAll(songList)
    }

    fun playSong(context: Context, song: Song) {
        selectedSong = song

        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }

        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(song.uri))
            prepare()
            play()

            this@MusicViewModel.totalDuration = song.duration
        }
        this.isPlaying = true
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        currentPosition = position
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
            } else {
                it.play()
                isPlaying = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }

    fun toggleRepeatMode() {
        currentRepeatMode = when (currentRepeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }

        exoPlayer?.repeatMode = when (currentRepeatMode) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun playNext(context: Context) {
        val currentIndex = songs.indexOf(selectedSong)
        if (currentIndex < songs.size - 1) {
            playSong(context, songs[currentIndex + 1])
        }
    }

    fun playPrevious(context: Context) {
        val currentIndex = songs.indexOf(selectedSong)
        if (currentIndex > 0) {
            playSong(context, songs[currentIndex - 1])
        }
    }

    fun initMediaController(context: Context) {
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            exoPlayer = mediaController
        }, MoreExecutors.directExecutor())
    }
}