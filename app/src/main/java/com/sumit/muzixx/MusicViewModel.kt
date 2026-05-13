package com.sumit.muzixx

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class MusicViewModel : ViewModel() {
    val songs = mutableStateListOf<Song>()

    fun loadSongs(songList: List<Song>) {
        songs.clear()
        songs.addAll(songList)
    }
}