package com.sumit.muzixx.data.network

import android.util.Log
import com.sumit.muzixx.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.util.concurrent.TimeUnit

class YouTubeAudioExtractor {

    companion object {

        private const val TAG = "YTExtractor"

        @Volatile
        private var initialized = false

        fun init() {

            if (initialized) return

            synchronized(this) {

                if (initialized) return

                try {

                    val okHttpClient = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()

                    NewPipe.init(object : Downloader() {

                        @Throws(IOException::class)
                        override fun execute(request: Request): Response {

                            val method =
                                request.httpMethod() ?: "GET"

                            val headers =
                                request.headers()
                                    ?.mapValues {
                                        it.value.joinToString(",")
                                    }
                                    ?.toHeaders()

                            val requestBuilder =
                                okhttp3.Request.Builder()
                                    .url(request.url())

                            if (headers != null) {
                                requestBuilder.headers(headers)
                            }

                            if (method.equals("POST", true)) {

                                val body =
                                    (request.dataToSend()
                                        ?: ByteArray(0))
                                        .toRequestBody()

                                requestBuilder.post(body)

                            } else {
                                requestBuilder.get()
                            }

                            val response =
                                okHttpClient
                                    .newCall(requestBuilder.build())
                                    .execute()

                            return Response(
                                response.code,
                                response.message,
                                response.headers.toMultimap(),
                                response.body?.string(),
                                response.request.url.toString()
                            )
                        }
                    })

                    initialized = true

                    Log.d(TAG, "NewPipe initialized")

                } catch (e: Exception) {

                    Log.e(TAG, "Initialization failed", e)
                }
            }
        }
    }

    suspend fun getSongFromVideoId(
        videoId: String
    ): Song? = withContext(Dispatchers.IO) {

        try {
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"

            val streamInfo = StreamInfo.getInfo(
                ServiceList.YouTube,
                videoUrl
            )

            val bestAudio = getBestAudioStream(streamInfo)

            if (bestAudio == null) {
                Log.e(TAG, "No audio stream found")
                return@withContext null
            }

            Song(
                id = videoId,
                title = streamInfo.name ?: "Unknown",
                artist = streamInfo.uploaderName ?: "Unknown Artist",
                uri = bestAudio.url ?: "",
                artUri = streamInfo.thumbnails?.firstOrNull()?.url,
                duration = streamInfo.duration * 1000L,
                isStreaming = true
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract song", e)
            null
        }
    }

    private fun getBestAudioStream(
        streamInfo: StreamInfo
    ): AudioStream? {

        return streamInfo.audioStreams
            ?.maxByOrNull { it.bitrate }
    }
}