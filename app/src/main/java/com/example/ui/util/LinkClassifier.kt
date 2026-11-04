package com.example.ui.util

import android.net.Uri

sealed interface RecognizedLink {
    val rawInput: String
    val displayType: String

    data class YouTubePlaylist(
        val playlistId: String,
        override val rawInput: String
    ) : RecognizedLink {
        override val displayType: String = "YouTube Playlist"
    }

    data class YouTubeVideo(
        val videoId: String,
        override val rawInput: String
    ) : RecognizedLink {
        override val displayType: String = "YouTube Vídeo"
    }

    data class ArchiveItem(
        val identifier: String,
        override val rawInput: String
    ) : RecognizedLink {
        override val displayType: String = "Internet Archive"
    }

    data class DirectAudio(
        val url: String,
        val extension: String,
        override val rawInput: String
    ) : RecognizedLink {
        override val displayType: String = "Áudio Direto ($extension)"
    }

    data class Unknown(
        override val rawInput: String
    ) : RecognizedLink {
        override val displayType: String = "Texto / Busca"
    }
}

object LinkClassifier {

    private val directAudioExtensions = setOf("mp3", "flac", "wav", "ogg", "m4a", "aac", "opus", "aiff", "wma")

    fun classify(input: String): RecognizedLink {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return RecognizedLink.Unknown("")

        // 1. Check for Direct Audio URL first
        val cleanUrl = trimmed.substringBefore("?").substringBefore("#")
        val ext = cleanUrl.substringAfterLast(".", "").lowercase()
        if (directAudioExtensions.contains(ext) && (trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            return RecognizedLink.DirectAudio(
                url = trimmed,
                extension = ext.uppercase(),
                rawInput = trimmed
            )
        }

        // 2. Check for YouTube Playlist
        // Format: youtube.com/playlist?list=ID or youtu.be/...list=ID or youtube.com/watch?v=...&list=ID
        val playlistRegex = Regex("""(?:youtube\.com/(?:playlist\?list=|watch\?.*[&?]list=)|youtu\.be/.*[&?]list=)([a-zA-Z0-9_-]{10,64})""", RegexOption.IGNORE_CASE)
        val playlistMatch = playlistRegex.find(trimmed)
        if (playlistMatch != null) {
            val playlistId = playlistMatch.groupValues[1]
            return RecognizedLink.YouTubePlaylist(playlistId = playlistId, rawInput = trimmed)
        }

        // Raw Playlist ID (e.g. starts with PL, UU, LL, RD or long ID with no slashes)
        if (!trimmed.contains("://") && !trimmed.contains("/") && (trimmed.startsWith("PL") || trimmed.startsWith("UU") || trimmed.startsWith("RD")) && trimmed.length in 12..64) {
            return RecognizedLink.YouTubePlaylist(playlistId = trimmed, rawInput = trimmed)
        }

        // 3. Check for YouTube Video
        // Formats:
        // - https://www.youtube.com/watch?v=VIDEO_ID
        // - https://youtu.be/VIDEO_ID
        // - https://www.youtube.com/shorts/VIDEO_ID
        // - https://www.youtube.com/embed/VIDEO_ID
        val videoUrlRegex = Regex("""(?:youtube\.com/(?:watch\?(?:.*&)?v=|shorts/|embed/|v/)|youtu\.be/)([a-zA-Z0-9_-]{11})""", RegexOption.IGNORE_CASE)
        val videoUrlMatch = videoUrlRegex.find(trimmed)
        if (videoUrlMatch != null) {
            val videoId = videoUrlMatch.groupValues[1]
            return RecognizedLink.YouTubeVideo(videoId = videoId, rawInput = trimmed)
        }

        // Raw 11-char YouTube ID (no spaces, no slashes, exactly 11 chars)
        if (!trimmed.contains("://") && !trimmed.contains("/") && trimmed.matches(Regex("""^[a-zA-Z0-9_-]{11}$"""))) {
            return RecognizedLink.YouTubeVideo(videoId = trimmed, rawInput = trimmed)
        }

        // 4. Check for Internet Archive
        val archiveDetailsRegex = Regex("""archive\.org/details/([a-zA-Z0-9_\-\.]+)""", RegexOption.IGNORE_CASE)
        val archiveMetadataRegex = Regex("""archive\.org/metadata/([a-zA-Z0-9_\-\.]+)""", RegexOption.IGNORE_CASE)
        val archiveDownloadRegex = Regex("""archive\.org/download/([a-zA-Z0-9_\-\.]+)""", RegexOption.IGNORE_CASE)

        archiveDetailsRegex.find(trimmed)?.groupValues?.get(1)?.let {
            return RecognizedLink.ArchiveItem(identifier = it, rawInput = trimmed)
        }
        archiveMetadataRegex.find(trimmed)?.groupValues?.get(1)?.let {
            return RecognizedLink.ArchiveItem(identifier = it, rawInput = trimmed)
        }
        archiveDownloadRegex.find(trimmed)?.groupValues?.get(1)?.let {
            return RecognizedLink.ArchiveItem(identifier = it, rawInput = trimmed)
        }

        return RecognizedLink.Unknown(rawInput = trimmed)
    }
}
