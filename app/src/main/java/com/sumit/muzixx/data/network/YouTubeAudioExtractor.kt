package com.sumit.muzixx.data.network

import android.util.Log
import com.sumit.muzixx.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

@Suppress("DEPRECATION")
class YouTubeAudioExtractor {
    companion object {
        private const val TAG = "YTExtractor"
    }

    private val searchBridge = YouTubeMusicScraper()

    suspend fun getSongFromVideoId(videoIdOrQuery: String): Song? = withContext(Dispatchers.IO) {
        try {
            val sanitizedInput = videoIdOrQuery.replace("yt_", "").trim()

            val finalVideoId = if (sanitizedInput.length == 11 && !sanitizedInput.contains(" ")) {
                sanitizedInput
            } else {
                Log.d(TAG, "Resolving text fallback query via Scraper: $videoIdOrQuery")
                val matches = searchBridge.searchSongs(videoIdOrQuery)
                val topMatch = matches.firstOrNull()?.id?.replace("yt_", "") ?: ""

                if (topMatch.isBlank()) {
                    Log.e(TAG, "No match found on YouTube for query: $videoIdOrQuery")
                    return@withContext null
                }
                topMatch
            }

            val url = "https://www.youtube.com/watch?v=$finalVideoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)

            var targetStreamUrl = info.audioStreams
                ?.filter { !it.url.isNullOrBlank() }
                ?.maxByOrNull { it.bitrate }
                ?.url

            if (targetStreamUrl.isNullOrBlank()) {
                Log.d(TAG, "Pure audio streams empty for $finalVideoId. Attempting video stream fallback...")
                targetStreamUrl = info.videoStreams
                    ?.filter { !it.url.isNullOrBlank() }
                    ?.minByOrNull { it.bitrate }
                    ?.url
            }

            if (targetStreamUrl.isNullOrBlank()) {
                Log.e(TAG, "No valid audio or video stream link fetched for $finalVideoId")
                return@withContext null
            }

            val artworkUrl = info.thumbnails?.firstOrNull()?.url
                ?: "https://img.youtube.com/vi/$finalVideoId/hqdefault.jpg"

            return@withContext Song(
                id = "yt_$finalVideoId",
                title = info.name ?: "Unknown",
                artist = info.uploaderName ?: "Unknown",
                uri = targetStreamUrl,
                artUri = artworkUrl,
                duration = info.duration * 1000L,
                isStreaming = true,
                folderName = "YouTube Stream",
                type = "yt"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Extractor process failed unexpectedly", e)
            null
        }
    }
}