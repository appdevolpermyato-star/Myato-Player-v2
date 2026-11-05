package com.example.data.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JamendoResponse<T>(
    val headers: JamendoHeaders,
    val results: List<T>
)

@JsonClass(generateAdapter = true)
data class JamendoHeaders(
    val status: String,
    val code: Int,
    val error_message: String? = null,
    val results_count: Int
)

@JsonClass(generateAdapter = true)
data class JamendoTrack(
    val id: String,
    val name: String,
    val duration: Int, // in seconds
    val artist_id: String? = null,
    val artist_name: String,
    val album_name: String? = null,
    val album_id: String? = null,
    val album_image: String? = null,
    val audio: String,
    val image: String? = null,
    val licenseccurl: String? = null,
    val stats: JamendoStats? = null
)

@JsonClass(generateAdapter = true)
data class JamendoAlbum(
    val id: String,
    val name: String,
    val releasedate: String? = null,
    val artist_id: String? = null,
    val artist_name: String,
    val image: String? = null,
    val zip: String? = null,
    val zip_allowed: Boolean = false,
    val shorturl: String? = null,
    val shareurl: String? = null,
    val tracks: List<JamendoTrack>? = null
)

@JsonClass(generateAdapter = true)
data class JamendoAlbumTracks(
    val id: String,
    val name: String,
    val artist_id: String? = null,
    val artist_name: String,
    val image: String? = null,
    val zip: String? = null,
    val zip_allowed: Boolean = false,
    val tracks: List<JamendoTrack>
)

@JsonClass(generateAdapter = true)
data class JamendoPlaylist(
    val id: String,
    val name: String,
    val creationdate: String? = null,
    val user_id: String? = null,
    val user_name: String? = null,
    val zip: String? = null,
    val image: String? = null,
    val shareurl: String? = null,
    val tracks: List<JamendoTrack>? = null
)

@JsonClass(generateAdapter = true)
data class JamendoPlaylistTracks(
    val id: String,
    val name: String,
    val user_name: String? = null,
    val zip: String? = null,
    val tracks: List<JamendoTrack>
)

@JsonClass(generateAdapter = true)
data class JamendoStats(
    val rate_listened_total: Int? = 0
)
