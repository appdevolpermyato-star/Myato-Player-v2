package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface SoundCloudApiService {
    companion object {
        const val BASE_URL = "https://api-v2.soundcloud.com/"
    }

    @GET("search/tracks")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("client_id") clientId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): SoundCloudTrackResponse

    @GET("search/playlists")
    suspend fun searchPlaylists(
        @Query("q") query: String,
        @Query("client_id") clientId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): SoundCloudPlaylistResponse

    @GET
    suspend fun resolveStreamUrl(
        @Url url: String,
        @Query("client_id") clientId: String
    ): SoundCloudStreamResponse
}
