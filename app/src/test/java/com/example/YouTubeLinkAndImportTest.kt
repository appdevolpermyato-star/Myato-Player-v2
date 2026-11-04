package com.example

import com.example.data.network.YouTubeDurationHelper
import com.example.ui.util.LinkClassifier
import com.example.ui.util.RecognizedLink
import org.junit.Assert.*
import org.junit.Test

class YouTubeLinkAndImportTest {

    @Test
    fun testYouTubeVideoRecognition() {
        val watchUrl = "https://www.youtube.com/watch?v=1ybpHiqnQY4"
        val recognizedWatch = LinkClassifier.classify(watchUrl)
        assertTrue(recognizedWatch is RecognizedLink.YouTubeVideo)
        assertEquals("1ybpHiqnQY4", (recognizedWatch as RecognizedLink.YouTubeVideo).videoId)

        val shortUrl = "https://youtu.be/1ybpHiqnQY4"
        val recognizedShort = LinkClassifier.classify(shortUrl)
        assertTrue(recognizedShort is RecognizedLink.YouTubeVideo)
        assertEquals("1ybpHiqnQY4", (recognizedShort as RecognizedLink.YouTubeVideo).videoId)

        val shortsUrl = "https://www.youtube.com/shorts/1ybpHiqnQY4"
        val recognizedShorts = LinkClassifier.classify(shortsUrl)
        assertTrue(recognizedShorts is RecognizedLink.YouTubeVideo)
        assertEquals("1ybpHiqnQY4", (recognizedShorts as RecognizedLink.YouTubeVideo).videoId)

        val rawId = "1ybpHiqnQY4"
        val recognizedRaw = LinkClassifier.classify(rawId)
        assertTrue(recognizedRaw is RecognizedLink.YouTubeVideo)
        assertEquals("1ybpHiqnQY4", (recognizedRaw as RecognizedLink.YouTubeVideo).videoId)
    }

    @Test
    fun testYouTubePlaylistRecognition() {
        val playlistUrl = "https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"
        val recognized = LinkClassifier.classify(playlistUrl)
        assertTrue(recognized is RecognizedLink.YouTubePlaylist)
        assertEquals("PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI", (recognized as RecognizedLink.YouTubePlaylist).playlistId)

        val watchWithListUrl = "https://www.youtube.com/watch?v=1ybpHiqnQY4&list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"
        val recognizedWatchList = LinkClassifier.classify(watchWithListUrl)
        assertTrue(recognizedWatchList is RecognizedLink.YouTubePlaylist)
        assertEquals("PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI", (recognizedWatchList as RecognizedLink.YouTubePlaylist).playlistId)

        val rawPlaylistId = "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"
        val recognizedRaw = LinkClassifier.classify(rawPlaylistId)
        assertTrue(recognizedRaw is RecognizedLink.YouTubePlaylist)
        assertEquals("PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI", (recognizedRaw as RecognizedLink.YouTubePlaylist).playlistId)
    }

    @Test
    fun testDirectAudioRecognition() {
        val mp3Url = "https://example.com/audio/song.mp3"
        val recognizedMp3 = LinkClassifier.classify(mp3Url)
        assertTrue(recognizedMp3 is RecognizedLink.DirectAudio)
        assertEquals("MP3", (recognizedMp3 as RecognizedLink.DirectAudio).extension)

        val flacUrl = "https://example.com/audio/track.flac?download=true"
        val recognizedFlac = LinkClassifier.classify(flacUrl)
        assertTrue(recognizedFlac is RecognizedLink.DirectAudio)
        assertEquals("FLAC", (recognizedFlac as RecognizedLink.DirectAudio).extension)
    }

    @Test
    fun testArchiveItemRecognition() {
        val archiveUrl = "https://archive.org/details/20220908_20220908_1340"
        val recognized = LinkClassifier.classify(archiveUrl)
        assertTrue(recognized is RecognizedLink.ArchiveItem)
        assertEquals("20220908_20220908_1340", (recognized as RecognizedLink.ArchiveItem).identifier)
    }

    @Test
    fun testIsoDurationParsing() {
        // PT3M45S -> (3*60 + 45) * 1000 = 225000 ms
        val dur1 = YouTubeDurationHelper.parseIsoDuration("PT3M45S")
        assertEquals(225000L, dur1)

        // PT1H2M30S -> (3600 + 120 + 30) * 1000 = 3750000 ms
        val dur2 = YouTubeDurationHelper.parseIsoDuration("PT1H2M30S")
        assertEquals(3750000L, dur2)

        // PT52S -> 52000 ms
        val dur3 = YouTubeDurationHelper.parseIsoDuration("PT52S")
        assertEquals(52000L, dur3)

        // PT4M -> 240000 ms
        val dur4 = YouTubeDurationHelper.parseIsoDuration("PT4M")
        assertEquals(240000L, dur4)

        // Invalid or null -> default 180000 ms
        assertEquals(180000L, YouTubeDurationHelper.parseIsoDuration(null))
        assertEquals(180000L, YouTubeDurationHelper.parseIsoDuration(""))
        assertEquals(180000L, YouTubeDurationHelper.parseIsoDuration("invalid"))
    }
}
