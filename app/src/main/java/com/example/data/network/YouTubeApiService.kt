package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    }

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("id") id: String,
        @Query("key") key: String
    ): YouTubeVideoListResponse

    @GET("playlists")
    suspend fun getPlaylistDetails(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("id") id: String,
        @Query("key") key: String
    ): YouTubePlaylistListResponse

    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
        @Query("key") key: String
    ): YouTubePlaylistItemsResponse
}

interface YouTubeOEmbedService {
    companion object {
        const val BASE_URL = "https://www.youtube.com/"
    }

    @GET("oembed")
    suspend fun getOEmbed(
        @Query("url") url: String,
        @Query("format") format: String = "json"
    ): YouTubeOEmbedResponse
}
