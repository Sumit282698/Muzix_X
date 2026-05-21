package com.sumit.muzixx.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

fun fetchLocalSongs(context: Context): List<Song> {
    val songList = mutableListOf<Song>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID
    )

    val cursor = context.contentResolver.query(uri, projection, null, null, null)

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        while (it.moveToNext()) {
            val idLong = it.getLong(idColumn)
            val title = it.getString(titleColumn) ?: "Unknown Title"
            val artist = it.getString(artistColumn) ?: "Unknown Artist"
            val duration = it.getLong(durationColumn)
            val albumId = it.getLong(albumIdColumn)

            val contentUriString = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, idLong).toString()
            val artUriString = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            ).toString()

            songList.add(
                Song(
                    id = idLong.toString(),
                    title = title,
                    artist = artist,
                    uri = contentUriString,
                    artUri = artUriString,
                    duration = duration,
                    isStreaming = false
                )
            )
        }
    }
    return songList
}