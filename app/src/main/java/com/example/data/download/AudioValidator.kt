package com.example.data.download

import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

sealed interface AudioValidationResult {
    data class Valid(
        val durationMs: Long,
        val mimeType: String,
        val format: String
    ) : AudioValidationResult

    data class Invalid(val reason: String) : AudioValidationResult
}

object AudioValidator {

    fun isAudioContentType(contentType: String?): Boolean {
        if (contentType.isNullOrBlank()) return true // Some servers don't set it, will validate via magic bytes
        val lower = contentType.lowercase()
        if (lower.contains("text/html") || lower.contains("application/json") || lower.contains("text/plain") || lower.contains("text/xml")) {
            return false
        }
        return lower.startsWith("audio/") ||
                lower.contains("application/ogg") ||
                lower.contains("application/x-flac") ||
                lower.contains("application/octet-stream") ||
                lower.contains("binary/octet-stream")
    }

    fun validateDownloadedFile(
        file: File,
        expectedSize: Long? = null,
        expectedMd5: String? = null,
        expectedSha1: String? = null
    ): AudioValidationResult {
        if (!file.exists()) {
            return AudioValidationResult.Invalid("Arquivo não foi criado no disco.")
        }

        val fileSize = file.length()
        if (fileSize < 1024L) {
            return AudioValidationResult.Invalid("Arquivo muito pequeno ou vazio ($fileSize bytes).")
        }

        if (expectedSize != null && expectedSize > 0 && fileSize < expectedSize) {
            return AudioValidationResult.Invalid(
                "Download incompleto: foram baixados $fileSize bytes de um total esperado de $expectedSize bytes."
            )
        }

        // 1. Inspect initial bytes to reject HTML, XML or JSON error responses
        val headerBytes = ByteArray(128)
        var bytesRead = 0
        try {
            FileInputStream(file).use { fis ->
                bytesRead = fis.read(headerBytes)
            }
        } catch (e: Exception) {
            return AudioValidationResult.Invalid("Falha ao ler o cabeçalho do arquivo: ${e.message}")
        }

        if (bytesRead >= 8) {
            val headerText = String(headerBytes, 0, bytesRead.coerceAtMost(64), Charsets.ISO_8859_1).lowercase()
            if (headerText.contains("<!doctype") ||
                headerText.contains("<html") ||
                headerText.contains("<?xml") ||
                headerText.contains("{\"error\"") ||
                headerText.contains("{\"status\"")
            ) {
                return AudioValidationResult.Invalid(
                    "O conteúdo baixado é uma página web ou erro (HTML/JSON) e não um arquivo de áudio."
                )
            }
        }

        // 2. Validate MD5 hash if provided
        if (!expectedMd5.isNullOrBlank()) {
            val actualMd5 = calculateHash(file, "MD5")
            if (!actualMd5.equals(expectedMd5.trim(), ignoreCase = true)) {
                return AudioValidationResult.Invalid(
                    "Falha de integridade (MD5 divergente). Esperado: $expectedMd5, Obtido: $actualMd5"
                )
            }
        }

        // 3. Validate SHA-1 hash if provided
        if (!expectedSha1.isNullOrBlank()) {
            val actualSha1 = calculateHash(file, "SHA-1")
            if (!actualSha1.equals(expectedSha1.trim(), ignoreCase = true)) {
                return AudioValidationResult.Invalid(
                    "Falha de integridade (SHA-1 divergente). Esperado: $expectedSha1, Obtido: $actualSha1"
                )
            }
        }

        // 4. Validate audio container & extract duration using MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        var duration = 0L
        var mimeType = "audio/mpeg"
        var hasAudioTrack = false

        try {
            retriever.setDataSource(file.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val extractedMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val audioFlag = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)

            if (!durStr.isNullOrBlank()) {
                duration = durStr.toLongOrNull() ?: 0L
            }
            if (!extractedMime.isNullOrBlank()) {
                mimeType = extractedMime
            }
            if (audioFlag == "yes" || mimeType.startsWith("audio/") || duration > 0L) {
                hasAudioTrack = true
            }
        } catch (e: Exception) {
            Log.w("AudioValidator", "MediaMetadataRetriever failed on ${file.name}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        // 5. Fallback to magic bytes verification if MediaMetadataRetriever was indecisive
        val detectedFormat = detectAudioFormat(headerBytes, bytesRead, file.extension)
        if (!hasAudioTrack && detectedFormat == null) {
            return AudioValidationResult.Invalid(
                "O formato do arquivo não foi reconhecido como um formato de áudio válido."
            )
        }

        val finalFormat = detectedFormat ?: mimeType.substringAfter("audio/").uppercase()
        return AudioValidationResult.Valid(
            durationMs = duration,
            mimeType = mimeType,
            format = finalFormat
        )
    }

    private fun detectAudioFormat(header: ByteArray, length: Int, extension: String): String? {
        if (length < 4) return null

        // MP3: ID3v2 tag
        if (header[0] == 0x49.toByte() && header[1] == 0x44.toByte() && header[2] == 0x33.toByte()) {
            return "MP3"
        }
        // MP3: MPEG sync frame (0xFF followed by 0xFB, 0xF3, 0xF2, 0xE3, etc.)
        if (header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0) {
            return "MP3"
        }
        // FLAC: "fLaC"
        if (header[0] == 0x66.toByte() && header[1] == 0x4C.toByte() && header[2] == 0x61.toByte() && header[3] == 0x43.toByte()) {
            return "FLAC"
        }
        // OGG: "OggS"
        if (header[0] == 0x4F.toByte() && header[1] == 0x67.toByte() && header[2] == 0x67.toByte() && header[3] == 0x53.toByte()) {
            return "OGG"
        }
        // WAV: "RIFF" and "WAVE"
        if (length >= 12 &&
            header[0] == 0x52.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x46.toByte() &&
            header[8] == 0x57.toByte() && header[9] == 0x41.toByte() && header[10] == 0x56.toByte() && header[11] == 0x45.toByte()
        ) {
            return "WAV"
        }
        // MP4 / M4A / AAC: "ftyp" at offset 4
        if (length >= 8 &&
            header[4] == 0x66.toByte() && header[5] == 0x74.toByte() && header[6] == 0x79.toByte() && header[7] == 0x70.toByte()
        ) {
            return "M4A"
        }
        // ADTS AAC: 0xFFF1 or 0xFFF9
        if (header[0] == 0xFF.toByte() && ((header[1].toInt() and 0xF6) == 0xF0)) {
            return "AAC"
        }

        // Trust recognized audio extension if no contradiction
        val ext = extension.lowercase()
        return when (ext) {
            "mp3" -> "MP3"
            "flac" -> "FLAC"
            "wav" -> "WAV"
            "ogg" -> "OGG"
            "m4a" -> "M4A"
            "aac" -> "AAC"
            "opus" -> "OPUS"
            else -> null
        }
    }

    fun calculateHash(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun sanitizeFileName(raw: String): String {
        var clean = raw
            .replace(Regex("""[\\/:*?"<>|]"""), " - ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        while (clean.endsWith(".") || clean.endsWith(" ")) {
            clean = clean.dropLast(1)
        }
        if (clean.length > 90) {
            clean = clean.take(90).trim()
        }
        return clean.ifBlank { "Audio" }
    }

    fun getUniqueAudioFile(
        musicDir: File,
        title: String,
        artist: String?,
        extension: String
    ): File {
        val cleanTitle = sanitizeFileName(title)
        val cleanArtist = artist?.takeIf {
            it.isNotBlank() &&
            it != "Artista Desconhecido" &&
            it != "Internet Archive" &&
            it != "Download Myato" &&
            it != "Download da Web"
        }?.let { sanitizeFileName(it) }

        val baseName = if (cleanArtist != null && !cleanTitle.contains(cleanArtist, ignoreCase = true)) {
            "$cleanTitle - $cleanArtist"
        } else {
            cleanTitle
        }

        val cleanExt = if (extension.startsWith(".")) extension.lowercase() else ".${extension.lowercase()}"
        var file = File(musicDir, "$baseName$cleanExt")
        var counter = 1
        while (file.exists()) {
            file = File(musicDir, "$baseName ($counter)$cleanExt")
            counter++
        }
        return file
    }
}
