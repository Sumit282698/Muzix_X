package com.sumit.muzixx.data

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList()
)