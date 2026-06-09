package com.sumit.muzixx.data.model

import com.google.gson.annotations.SerializedName

//Wrapper for search endpoints and playlist tracks
data class SaavnPlaylistResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SaavnPlaylistData?
)

data class SaavnDirectSongResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<SaavnTrackData>?
)

data class SaavnPlaylistData(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("songs", alternate = ["results"]) val songs: List<SaavnTrackData>?
)
//Saavn Playlist Search Response
data class SaavnPlaylistSearchResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SaavnPlaylistSearchData?
)

data class SaavnPlaylistSearchData(
    @SerializedName("results") val results: List<SaavnCloudPlaylistObject>?
)

data class SaavnCloudPlaylistObject(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("songCount") val songCount: Int?,
    @SerializedName("username") val username: String?,
    @SerializedName("image") val image: List<SaavnImageObject>?
)

// Main Track Data Class used across Search, Playlists, and Recommendations
data class SaavnTrackData(
    @SerializedName("id") val id: String?,
    @SerializedName("name", alternate = ["title"]) val name: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("image") val image: List<SaavnImageObject>?,
    @SerializedName("artists") val artists: SaavnArtistGroup?,
    @SerializedName("downloadUrl") val downloadUrl: List<SaavnDownloadUrlObject>?
)

data class SaavnImageObject(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url") val url: String?
)

data class SaavnArtistGroup(
    @SerializedName("primary") val primary: List<SaavnArtistObject>?
)

data class SaavnArtistObject(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

data class SaavnDownloadUrlObject(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url") val url: String?
)