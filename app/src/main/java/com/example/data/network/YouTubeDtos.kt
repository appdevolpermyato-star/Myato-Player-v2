package com.example.data.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    val items: List<YouTubeVideoItem>? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoItem(
    val id: String,
    val snippet: YouTubeSnippet? = null,
    val contentDetails: YouTubeVideoContentDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    val title: String? = null,
    val channelTitle: String? = null,
    val description: String? = null,
    val thumbnails: YouTubeThumbnails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoContentDetails(
    val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    val maxres: YouTubeThumbnail? = null,
    val standard: YouTubeThumbnail? = null,
    val high: YouTubeThumbnail? = null,
    val medium: YouTubeThumbnail? = null,
    val default: YouTubeThumbnail? = null
) {
    fun getBestUrl(): String? {
        return maxres?.url ?: standard?.url ?: high?.url ?: medium?.url ?: default?.url
    }
}

@JsonClass(generateAdapter = true)
data class YouTubeThumbnail(
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistListResponse(
    val items: List<YouTubePlaylistItemInfo>? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItemInfo(
    val id: String,
    val snippet: YouTubeSnippet? = null,
    val contentDetails: YouTubePlaylistContentDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistContentDetails(
    val itemCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItemsResponse(
    val nextPageToken: String? = null,
    val items: List<YouTubePlaylistItemEntry>? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItemEntry(
    val id: String,
    val snippet: YouTubePlaylistItemSnippet? = null,
    val contentDetails: YouTubePlaylistItemDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItemSnippet(
    val title: String? = null,
    val channelTitle: String? = null,
    val position: Int? = null,
    val resourceId: YouTubeResourceId? = null,
    val thumbnails: YouTubeThumbnails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
    val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItemDetails(
    val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeOEmbedResponse(
    val title: String? = null,
    val author_name: String? = null,
    val thumbnail_url: String? = null
)

object YouTubeDurationHelper {
    /**
     * Parses ISO 8601 duration (e.g. PT3M45S, PT1H2M30S, PT52S) to milliseconds.
     */
    fun parseIsoDuration(durationStr: String?): Long {
        if (durationStr.isNullOrBlank()) return 180000L
        return try {
            val regex = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""")
            val match = regex.matchEntire(durationStr.trim()) ?: return 180000L
            val hours = match.groupValues[1].toLongOrNull() ?: 0L
            val minutes = match.groupValues[2].toLongOrNull() ?: 0L
            val seconds = match.groupValues[3].toLongOrNull() ?: 0L
            ((hours * 3600) + (minutes * 60) + seconds) * 1000L
        } catch (_: Exception) {
            180000L
        }
    }
}
