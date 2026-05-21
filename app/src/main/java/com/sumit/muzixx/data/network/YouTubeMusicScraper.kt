package com.sumit.muzixx.data.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sumit.muzixx.data.Song
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class YouTubeMusicScraper {
    private val client = HttpClient(OkHttp)
    private val gson = Gson()

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery&sp=EgIQAQ%253D%253D"

            val response: HttpResponse = client.get(url) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Accept-Language", "en-US,en;q=0.9")
            }

            val htmlContent = response.bodyAsText()
            val searchMarker = "var ytInitialData = "

            if (!htmlContent.contains(searchMarker)) {
                println("MUZIX_DEBUG: Could not locate configuration payload data on page.")
                return@withContext emptyList()
            }

            val startIndex = htmlContent.indexOf(searchMarker) + searchMarker.length

            var endIndex = htmlContent.indexOf(";</script>", startIndex)
            if (endIndex == -1) {
                endIndex = htmlContent.indexOf("</script>", startIndex)
            }
            if (endIndex == -1) return@withContext emptyList()

            var jsonString = htmlContent.substring(startIndex, endIndex).trim()
            if (jsonString.endsWith(";")) {
                jsonString = jsonString.dropLast(1)
            }

            println("MUZIX_DEBUG: Cracked open raw configuration JSON! Size: ${jsonString.length} characters")

            return@withContext parseYouTubeHtmlJson(jsonString)
        } catch (e: Exception) {
            println("MUZIX_DEBUG_ERROR: Network request processing failed.")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseYouTubeHtmlJson(jsonText: String): List<Song> {
        val songList = mutableListOf<Song>()
        try {
            val root = gson.fromJson(jsonText, JsonObject::class.java)
            val contents = root.getAsJsonObject("contents") ?: return emptyList()

            val sectionList = contents.getAsJsonObject("twoColumnSearchResultsRenderer")
                ?.getAsJsonObject("primaryContents")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            var contentsArray: com.google.gson.JsonArray? = null
            for (section in sectionList) {
                val itemSection = section.asJsonObject.getAsJsonObject("itemSectionRenderer")
                if (itemSection != null) {
                    contentsArray = itemSection.getAsJsonArray("contents")
                    break
                }
            }

            if (contentsArray == null || contentsArray.size() == 0) {
                println("MUZIX_DEBUG: No valid item section contents found.")
                return emptyList()
            }

            println("MUZIX_DEBUG: Processing ${contentsArray.size()} items from results array...")

            for (element in contentsArray) {
                val obj = element.asJsonObject
                val videoRenderer = obj.getAsJsonObject("videoRenderer") ?: continue

                val titleRuns = videoRenderer.getAsJsonObject("title")?.getAsJsonArray("runs")
                val title = if (titleRuns != null && titleRuns.size() > 0) {
                    titleRuns.get(0).asJsonObject.getAsJsonPrimitive("text")?.asString ?: "Unknown Track"
                } else {
                    "Unknown Track"
                }

                val ownerRuns = videoRenderer.getAsJsonObject("ownerText")?.getAsJsonArray("runs")
                val artist = if (ownerRuns != null && ownerRuns.size() > 0) {
                    ownerRuns.get(0).asJsonObject.getAsJsonPrimitive("text")?.asString ?: "Unknown Artist"
                } else {
                    "Unknown Artist"
                }

                val videoIdObj = videoRenderer.getAsJsonPrimitive("videoId")
                val videoId = videoIdObj?.asString ?: ""

                val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                if (videoId.isNotBlank()) {
                    songList.add(
                        Song(
                            id = videoId,
                            title = title,
                            artist = artist,
                            uri = "https://www.youtube.com/watch?v=$videoId",
                            artUri = thumbnailUrl,
                            duration = 0L,
                            isStreaming = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("MUZIX_DEBUG_PARSING_ERROR: Layout parsing broke down.")
            e.printStackTrace()
        }

        println("MUZIX_DEBUG: Successfully parsed ${songList.size} streaming songs out of the payload data!")
        return songList
    }
}