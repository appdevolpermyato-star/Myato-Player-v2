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
    val audio: String, // MP3 download URL
    val audiodownload: String? = null,
    val audiodownload_allowed: Boolean = false,
    val image: String? = null,
    val stats: JamendoStats? = null
)

@JsonClass(generateAdapter = true)
data class JamendoStats(
    val rate_downloads_total: Int? = 0,
    val rate_listened_total: Int? = 0
)
