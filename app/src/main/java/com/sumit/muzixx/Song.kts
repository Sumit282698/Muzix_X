package com.sumit.muzixx

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String, // This is the file path
    val duration: Int
)