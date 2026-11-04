package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {
    companion object {
        const val BASE_URL = "https://api.jamendo.com/v3.0/"
    }

    @GET("tracks")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 10,
        @Query("namesearch") name: String? = null,
        @Query("artist_name") artist: String? = null,
        @Query("search") query: String? = null,
        @Query("order") order: String = "relevance",
        @Query("include") include: String = "musicinfo stats",
        @Query("audiodownload_allowed") downloadAllowed: Boolean = true,
        @Query("audiodlformat") downloadFormat: String = "mp32"
    ): JamendoResponse<JamendoTrack>
}
