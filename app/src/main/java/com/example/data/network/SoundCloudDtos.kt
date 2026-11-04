package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SoundCloudTrackResponse(
    val collection: List<SoundCloudTrack>,
    val next_href: String? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudTrack(
    val id: Long,
    val title: String,
    val duration: Long, // in ms
    @Json(name = "artwork_url") val artworkUrl: String? = null,
    val user: SoundCloudUser,
    val downloadable: Boolean = false,
    @Json(name = "download_url") val downloadUrl: String? = null,
    @Json(name = "stream_url") val streamUrl: String? = null,
    val media: SoundCloudMedia? = null,
    @Json(name = "permalink_url") val permalinkUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudUser(
    val id: Long,
    val username: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudMedia(
    val transcodings: List<SoundCloudTranscoding>? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudTranscoding(
    val url: String,
    val preset: String,
    val duration: Long,
    val snipped: Boolean,
    val format: SoundCloudFormat,
    val quality: String
)

@JsonClass(generateAdapter = true)
data class SoundCloudFormat(
    val protocol: String,
    @Json(name = "mime_type") val mimeType: String
)

@JsonClass(generateAdapter = true)
data class SoundCloudPlaylistResponse(
    val collection: List<SoundCloudPlaylist>,
    val next_href: String? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudPlaylist(
    val id: Long,
    val title: String,
    @Json(name = "artwork_url") val artworkUrl: String? = null,
    val user: SoundCloudUser,
    @Json(name = "track_count") val trackCount: Int,
    val tracks: List<SoundCloudTrack>? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudStreamResponse(
    val url: String
)
