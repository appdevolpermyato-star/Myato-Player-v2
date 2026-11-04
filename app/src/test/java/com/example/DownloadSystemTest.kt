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
}
