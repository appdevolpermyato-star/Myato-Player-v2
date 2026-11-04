package com.example

import com.example.data.download.AudioValidationResult
import com.example.data.download.AudioValidator
import com.example.ui.archive.ArchiveViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadSystemTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSanitizeFileName() {
        val rawName = "Música: Teste / Exemplo * 2026? <Track> | Nome"
        val clean = AudioValidator.sanitizeFileName(rawName)
        assertFalse(clean.contains(":"))
        assertFalse(clean.contains("/"))
        assertFalse(clean.contains("*"))
        assertFalse(clean.contains("?"))
        assertFalse(clean.contains("<"))
        assertFalse(clean.contains(">"))
        assertFalse(clean.contains("|"))
    }

    @Test
    fun testUniqueFileNaming() {
        val dir = tempFolder.newFolder("music")
        val file1 = AudioValidator.getUniqueAudioFile(dir, "Moonlight Sonata", "Beethoven", ".mp3")
        assertEquals("Moonlight Sonata - Beethoven.mp3", file1.name)
        file1.createNewFile()

        val file2 = AudioValidator.getUniqueAudioFile(dir, "Moonlight Sonata", "Beethoven", ".mp3")
        assertEquals("Moonlight Sonata - Beethoven (1).mp3", file2.name)
    }

    @Test
    fun testAudioContentTypeRejection() {
        assertFalse(AudioValidator.isAudioContentType("text/html; charset=utf-8"))
        assertFalse(AudioValidator.isAudioContentType("application/json"))
        assertFalse(AudioValidator.isAudioContentType("text/plain"))
        assertFalse(AudioValidator.isAudioContentType("text/xml"))

        assertTrue(AudioValidator.isAudioContentType("audio/mpeg"))
        assertTrue(AudioValidator.isAudioContentType("audio/flac"))
        assertTrue(AudioValidator.isAudioContentType("audio/ogg"))
        assertTrue(AudioValidator.isAudioContentType("application/octet-stream"))
    }

    @Test
    fun testRejectHtmlAsAudio() {
        val htmlFile = tempFolder.newFile("fake_audio.mp3")
        htmlFile.writeText("<!DOCTYPE html><html><body>Error 404 Not Found</body></html>")

        val result = AudioValidator.validateDownloadedFile(htmlFile)
        assertTrue(result is AudioValidationResult.Invalid)
        val invalid = result as AudioValidationResult.Invalid
        assertTrue(invalid.reason.contains("HTML") || invalid.reason.contains("pequeno"))
    }

    @Test
    fun testHashValidation() {
        val testFile = tempFolder.newFile("sample.bin")
        testFile.writeBytes("MyatoPlayer-Audio-Test-Data-2026".toByteArray())

        val md5 = AudioValidator.calculateHash(testFile, "MD5")
        assertNotNull(md5)
        assertEquals(32, md5.length)

        val sha1 = AudioValidator.calculateHash(testFile, "SHA-1")
        assertNotNull(sha1)
        assertEquals(40, sha1.length)
    }

    @Test
    fun testExtractArchiveIdentifier() {
        val vm = ArchiveViewModel()
        assertEquals("1ybpHiqnQY4", vm.extractArchiveIdentifier("https://archive.org/details/1ybpHiqnQY4"))
        assertEquals("1ybpHiqnQY4", vm.extractArchiveIdentifier("https://archive.org/download/1ybpHiqnQY4"))
        assertEquals("1ybpHiqnQY4", vm.extractArchiveIdentifier("https://archive.org/metadata/1ybpHiqnQY4"))
        assertEquals("1ybpHiqnQY4", vm.extractArchiveIdentifier("1ybpHiqnQY4"))
        assertEquals("beethoven_sym5", vm.extractArchiveIdentifier("https://archive.org/details/beethoven_sym5/track.mp3"))
    }

    @Test
    fun testBuildDownloadUrlEncoding() {
        val vm = ArchiveViewModel()
        val url = vm.buildDownloadUrl("item_123", "01 - Track Name with Spaces.mp3")
        assertEquals("https://archive.org/download/item_123/01%20-%20Track%20Name%20with%20Spaces.mp3", url)
    }

    @Test
    fun testExternalServiceDetection() {
        val vm = ArchiveViewModel()
        assertTrue(vm.isExternalServiceUrl("https://www.youtube.com/watch?v=1ybpHiqnQY4"))
        assertTrue(vm.isExternalServiceUrl("https://youtu.be/1ybpHiqnQY4"))
        assertTrue(vm.isExternalServiceUrl("https://open.spotify.com/track/123"))
        assertFalse(vm.isExternalServiceUrl("https://archive.org/details/beethoven_sym5"))
        assertFalse(vm.isExternalServiceUrl("beethoven_sym5"))
        assertNull(vm.extractArchiveIdentifier("https://www.youtube.com/watch?v=1ybpHiqnQY4"))
    }

    @Test
    fun testAudioFormatPriority() {
        val vm = ArchiveViewModel()
        val mp3File = com.example.data.network.ArchiveFile(name = "track.mp3", format = "VBR MP3", size = "5000000")
        val m4aFile = com.example.data.network.ArchiveFile(name = "track.m4a", format = "AAC Audio", size = "5000000")
        val flacFile = com.example.data.network.ArchiveFile(name = "track.flac", format = "FLAC", size = "20000000")
        val oggFile = com.example.data.network.ArchiveFile(name = "track.ogg", format = "Ogg Vorbis", size = "4000000")
        val wavFile = com.example.data.network.ArchiveFile(name = "track.wav", format = "WAV", size = "30000000")

        assertTrue(vm.getAudioFormatPriority(mp3File) < vm.getAudioFormatPriority(m4aFile))
        assertTrue(vm.getAudioFormatPriority(m4aFile) < vm.getAudioFormatPriority(flacFile))
        assertTrue(vm.getAudioFormatPriority(flacFile) < vm.getAudioFormatPriority(oggFile))
        assertTrue(vm.getAudioFormatPriority(oggFile) < vm.getAudioFormatPriority(wavFile))
    }

    @Test
    fun testExcludeNonAudioFiles() {
        val vm = ArchiveViewModel()
        val xmlFile = com.example.data.network.ArchiveFile(name = "item_files.xml", format = "Metadata", size = "5000")
        val jsonFile = com.example.data.network.ArchiveFile(name = "item_meta.json", format = "Metadata", size = "2000")
        val torrentFile = com.example.data.network.ArchiveFile(name = "item_archive.torrent", format = "Archive BitTorrent", size = "10000")
        val jpgFile = com.example.data.network.ArchiveFile(name = "cover.jpg", format = "JPEG", size = "50000")
        val spectroFile = com.example.data.network.ArchiveFile(name = "track_spectrogram.png", format = "Spectrogram", size = "80000")
        val validMp3 = com.example.data.network.ArchiveFile(name = "01-symphony.mp3", format = "VBR MP3", size = "8000000")

        assertFalse(vm.isAudioFile(xmlFile))
        assertFalse(vm.isAudioFile(jsonFile))
        assertFalse(vm.isAudioFile(torrentFile))
        assertFalse(vm.isAudioFile(jpgFile))
        assertFalse(vm.isAudioFile(spectroFile))
        assertTrue(vm.isAudioFile(validMp3))
    }
}
