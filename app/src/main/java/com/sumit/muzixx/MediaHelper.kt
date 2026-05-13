package com.sumit.muzixx

fun fetchLocalSongs(context: Context): List<Song> {
    val songList = mutableListOf<Song>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION
    )

    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext()) {
            songList.add(Song(
                id = cursor.getLong(idColumn),
                title = cursor.getString(titleColumn),
                artist = cursor.getString(artistColumn),
                data = cursor.getString(dataColumn),
                duration = cursor.getInt(durationColumn)
            ))
        }
    }
    return songList
}