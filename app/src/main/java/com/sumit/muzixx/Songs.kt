package com.sumit.muzixx

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String,
    val duration: Long,
    val uri: android.net.Uri,
    val artUri: Uri?
)