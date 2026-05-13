package com.sumit.muzixx

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class MusicViewModel : ViewModel() {
    // This is a special list that tells the UI to refresh
    // automatically when a song is added or removed.
    val songs = mutableStateListOf<Song>()

    fun loadSongs(songList: List<Song>) {
        songs.clear()
        songs.addAll(songList)
    }
}