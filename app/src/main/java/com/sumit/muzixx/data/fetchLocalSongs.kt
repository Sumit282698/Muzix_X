package com.sumit.muzixx.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

fun fetchLocalSongs(context: Context): List<Song> {
    val songList = mutableListOf<Song>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    // 💡 Add MediaStore.Audio.Media.DATA to the projection to extract file paths for folders
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATA
    )

    // 💡 THE VIBE GUARD: Ignore tracks under 1 min (60,000ms) and ensure it's classified as music
    val selection = "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val selectionArgs = arrayOf("60000")
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        while (it.moveToNext()) {
            val idLong = it.getLong(idColumn)
            val title = it.getString(titleColumn) ?: "Unknown Title"
            val artist = it.getString(artistColumn) ?: "Unknown Artist"
            val duration = it.getLong(durationColumn)
            val albumId = it.getLong(albumIdColumn)
            val filePath = it.getString(dataColumn) ?: ""

            // 💡 FOLDER EXTRACTION: Identify the immediate parent directory name
            val folderName = try {
                val file = File(filePath)
                file.parentFile?.name ?: "Internal Storage"
            } catch (e: Exception) {
                "Internal Storage"
            }

            val contentUriString = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, idLong).toString()

            // ✅ Preserved your original working Album Art Uri builder perfectly!
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
                    isStreaming = false,
                    folderName = folderName // 💡 Attaches the folder tag to the data model securely
                )
            )
        }
    }
    return songList
}