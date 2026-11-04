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
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("namesearch") name: String? = null,
        @Query("artist_name") artist: String? = null,
        @Query("album_name") album: String? = null,
        @Query("tags") tags: String? = null,
        @Query("search") search: String? = null,
        @Query("order") order: String = "relevance",
        @Query("include") include: String = "musicinfo stats licenses",
        @Query("audiodlformat") downloadFormat: String = "mp32"
    ): JamendoResponse<JamendoTrack>

    @GET("albums")
    suspend fun searchAlbums(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("name") name: String? = null,
        @Query("artist_name") artist: String? = null,
        @Query("search") search: String? = null,
        @Query("include") include: String = "stats licenses"
    ): JamendoResponse<JamendoAlbum>

    @GET("albums/tracks")
    suspend fun getAlbumTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("id") albumId: String,
        @Query("include") include: String = "musicinfo stats licenses",
        @Query("audiodlformat") downloadFormat: String = "mp32"
    ): JamendoResponse<JamendoAlbumTracks>

    @GET("playlists")
    suspend fun searchPlaylists(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("name") name: String? = null,
        @Query("search") search: String? = null,
        @Query("include") include: String = "stats"
    ): JamendoResponse<JamendoPlaylist>

    @GET("playlists/tracks")
    suspend fun getPlaylistTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("id") playlistId: String,
        @Query("include") include: String = "musicinfo stats licenses",
        @Query("audiodlformat") downloadFormat: String = "mp32"
    ): JamendoResponse<JamendoPlaylistTracks>
}
