package com.example.data.download

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.data.local.MusicDao
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.Song
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadEngine(
    private val context: Context,
    private val musicDao: MusicDao
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    // Additional download parameters stored in memory during active job
    private val jobMetadata = ConcurrentHashMap<String, DownloadJobMeta>()

    private data class DownloadJobMeta(
        val album: String?,
        val expectedSize: Long?,
        val expectedMd5: String?,
        val expectedSha1: String?,
        val expectedDuration: Long?
    )

    fun startDownload(
        url: String,
        customTitle: String? = null,
        customArtist: String? = null,
        customAlbum: String? = null,
        expectedSize: Long? = null,
        expectedMd5: String? = null,
        expectedSha1: String? = null,
        expectedDuration: Long? = null
    ): String {
        val downloadId = UUID.randomUUID().toString().take(8)

        jobMetadata[downloadId] = DownloadJobMeta(
            album = customAlbum,
            expectedSize = expectedSize,
            expectedMd5 = expectedMd5,
            expectedSha1 = expectedSha1,
            expectedDuration = expectedDuration
        )

        scope.launch {
            // Check for completed duplicate
            val existing = musicDao.getAllDownloadsOnce().find { it.url == url }
            if (existing != null && existing.status == DownloadStatus.COMPLETED) {
                if (existing.localPath != null && File(existing.localPath).exists()) {
                    return@launch
                }
            }

            val inferredName = customTitle?.takeIf { it.isNotBlank() }
                ?: extractFileNameFromUrl(url)
                ?: "Faixa_${System.currentTimeMillis() % 10000}"

            val inferredArtist = customArtist?.takeIf { it.isNotBlank() } ?: "Download Myato"

            val item = DownloadItem(
                id = downloadId,
                url = url,
                title = inferredName,
                artist = inferredArtist,
                totalBytes = expectedSize ?: 0L,
                downloadedBytes = 0L,
                status = DownloadStatus.DOWNLOADING,
                speedText = "Iniciando..."
            )

            musicDao.insertDownload(item)
            launchDownloadJob(item)
        }

        return downloadId
    }

    fun pauseDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            val item = musicDao.getDownloadById(id) ?: return@launch
            musicDao.updateDownload(
                item.copy(
                    status = DownloadStatus.PAUSED,
                    speedText = "Pausado"
                )
            )
        }
    }

    fun resumeDownload(id: String) {
        scope.launch {
            val item = musicDao.getDownloadById(id) ?: return@launch
            musicDao.updateDownload(
                item.copy(
                    status = DownloadStatus.DOWNLOADING,
                    speedText = "Retomando...",
                    errorMessage = null
                )
            )
            launchDownloadJob(item)
        }
    }

    fun cancelDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        jobMetadata.remove(id)
        scope.launch {
            val item = musicDao.getDownloadById(id)
            if (item?.localPath != null) {
                try {
                    File(item.localPath).delete()
                } catch (_: Exception) {}
            }
            musicDao.deleteDownloadById(id)
            musicDao.deleteSongById("dl_$id")
        }
    }

    fun deleteDownloadedFile(id: String) {
        cancelDownload(id)
    }

    private fun launchDownloadJob(initialItem: DownloadItem) {
        val id = initialItem.id
        activeJobs[id]?.cancel()

        val job = scope.launch {
            var currentItem = initialItem
            val meta = jobMetadata[id]

            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir

            val extension = determineExtension(currentItem.url)

            // Formulate clean, readable target file name (e.g. "Song Title - Artist.mp3")
            val targetFile = currentItem.localPath?.let { File(it) }
                ?: AudioValidator.getUniqueAudioFile(
                    musicDir = musicDir,
                    title = currentItem.title,
                    artist = currentItem.artist,
                    extension = extension
                )

            val existingSize = if (targetFile.exists()) targetFile.length() else 0L

            try {
                val requestBuilder = Request.Builder()
                    .url(currentItem.url)
                    .header("User-Agent", "MyatoPlayer/2.0 (Android; Linux)")
                    .header("Accept", "audio/*,application/octet-stream;q=0.9,*/*;q=0.5")

                if (existingSize > 0) {
                    requestBuilder.header("Range", "bytes=$existingSize-")
                }

                val response = client.newCall(requestBuilder.build()).execute()

                val actualResuming = response.code == 206 && existingSize > 0

                if (!response.isSuccessful && response.code != 206) {
                    throw Exception("Servidor respondeu com erro HTTP ${response.code}: ${response.message}")
                }

                // Verify Content-Type before writing bytes to avoid saving HTML error pages
                val contentType = response.header("Content-Type")
                if (!AudioValidator.isAudioContentType(contentType)) {
                    throw Exception("O link retornou conteúdo não-áudio ($contentType). Verifique se o link é direto para o arquivo de áudio.")
                }

                val body = response.body ?: throw Exception("Corpo da resposta HTTP vazio.")
                val contentLength = body.contentLength()
                val totalBytes = if (actualResuming) {
                    existingSize + contentLength
                } else if (contentLength > 0) {
                    contentLength
                } else {
                    meta?.expectedSize ?: currentItem.totalBytes.coerceAtLeast(3 * 1024 * 1024L)
                }

                currentItem = currentItem.copy(
                    totalBytes = totalBytes,
                    localPath = targetFile.absolutePath,
                    status = DownloadStatus.DOWNLOADING,
                    downloadedBytes = if (actualResuming) existingSize else 0L,
                    errorMessage = null
                )
                musicDao.updateDownload(currentItem)

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(targetFile, actualResuming)

                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = if (actualResuming) existingSize else 0L
                    var lastUpdateTime = System.currentTimeMillis()
                    var bytesSinceLastUpdate = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) {
                            throw CancellationException("Download interrompido pelo usuário")
                        }
                        outputStream.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        bytesSinceLastUpdate += bytesRead

                        val now = System.currentTimeMillis()
                        val diff = now - lastUpdateTime
                        if (diff >= 700) {
                            val speedKB = (bytesSinceLastUpdate / 1024.0) / (diff / 1000.0)
                            val speedText = if (speedKB >= 1024) {
                                String.format("%.1f MB/s", speedKB / 1024)
                            } else {
                                String.format("%.0f KB/s", speedKB)
                            }

                            currentItem = currentItem.copy(
                                downloadedBytes = totalDownloaded,
                                speedText = speedText
                            )
                            musicDao.updateDownload(currentItem)

                            lastUpdateTime = now
                            bytesSinceLastUpdate = 0L
                        }
                    }
                    outputStream.flush()
                } finally {
                    try { inputStream?.close() } catch (_: Exception) {}
                    try { outputStream?.close() } catch (_: Exception) {}
                }

                // --- VALIDATION STAGE ---
                val validation = AudioValidator.validateDownloadedFile(
                    file = targetFile,
                    expectedSize = meta?.expectedSize,
                    expectedMd5 = meta?.expectedMd5,
                    expectedSha1 = meta?.expectedSha1
                )

                if (validation is AudioValidationResult.Invalid) {
                    // Delete corrupt / HTML file
                    try { targetFile.delete() } catch (_: Exception) {}
                    throw Exception(validation.reason)
                }

                val validAudio = validation as AudioValidationResult.Valid

                // Extract or refine metadata from the validated audio file
                var finalTitle = currentItem.title
                var finalArtist = currentItem.artist
                var finalAlbum = meta?.album ?: "Downloads"
                var finalDuration = validAudio.durationMs

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(targetFile.absolutePath)
                    val id3Title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val id3Artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val id3Album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    val id3Duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()

                    if (!id3Title.isNullOrBlank() && (finalTitle.startsWith("myato_") || finalTitle.length < 3 || finalTitle.contains(Regex("""^[a-zA-Z0-9_\-]{8,20}$""")))) {
                        finalTitle = id3Title
                    }
                    if (!id3Artist.isNullOrBlank() && (finalArtist == "Download Myato" || finalArtist == "Artista Desconhecido")) {
                        finalArtist = id3Artist
                    }
                    if (!id3Album.isNullOrBlank()) {
                        finalAlbum = id3Album
                    }
                    if (finalDuration <= 0L && id3Duration != null && id3Duration > 0L) {
                        finalDuration = id3Duration
                    }
                } catch (_: Exception) {
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }

                if (finalDuration <= 0L) {
                    finalDuration = meta?.expectedDuration ?: 180000L
                }

                // Download completed successfully
                currentItem = currentItem.copy(
                    title = finalTitle,
                    artist = finalArtist,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    status = DownloadStatus.COMPLETED,
                    speedText = "Concluído",
                    localPath = targetFile.absolutePath,
                    errorMessage = null
                )
                musicDao.updateDownload(currentItem)

                // Register into local Room library for offline playback & playlists
                val song = Song(
                    id = "dl_$id",
                    title = finalTitle,
                    artist = finalArtist,
                    album = finalAlbum,
                    duration = finalDuration,
                    mediaUri = Uri.fromFile(targetFile).toString(),
                    coverUri = null,
                    isFavorite = false,
                    isDownloaded = true,
                    dateAdded = System.currentTimeMillis()
                )
                musicDao.insertSong(song)

            } catch (e: CancellationException) {
                Log.i("DownloadEngine", "Download $id was paused or cancelled")
            } catch (e: Exception) {
                Log.e("DownloadEngine", "Download failed for $id: ${e.message}", e)
                currentItem = currentItem.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message ?: "Falha no download do arquivo.",
                    speedText = "Erro"
                )
                musicDao.updateDownload(currentItem)
            } finally {
                activeJobs.remove(id)
                jobMetadata.remove(id)
            }
        }

        activeJobs[id] = job
    }

    private fun determineExtension(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".flac") -> ".flac"
            lower.contains(".wav") -> ".wav"
            lower.contains(".ogg") -> ".ogg"
            lower.contains(".m4a") -> ".m4a"
            lower.contains(".aac") -> ".aac"
            lower.contains(".opus") -> ".opus"
            else -> ".mp3"
        }
    }

    private fun extractFileNameFromUrl(url: String): String? {
        return try {
            val path = Uri.parse(url).lastPathSegment ?: return null
            val clean = path.substringBefore("?").substringBefore("#")
            val name = clean.substringBeforeLast(".")
            Uri.decode(name)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun repairExistingDownloads() = withContext(Dispatchers.IO) {
        try {
            val downloads = musicDao.getAllDownloadsOnce()
            for (item in downloads) {
                val path = item.localPath
                if (path.isNullOrBlank()) {
                    musicDao.updateDownload(
                        item.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = "Arquivo local não registrado. Toque para remover."
                        )
                    )
                    continue
                }

                val file = File(path)
                if (!file.exists()) {
                    musicDao.updateDownload(
                        item.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = "Arquivo não encontrado no armazenamento. Toque para remover."
                        )
                    )
                    continue
                }

                // Validate if file is really an audio file
                val validation = AudioValidator.validateDownloadedFile(file)
                if (validation is AudioValidationResult.Invalid) {
                    musicDao.updateDownload(
                        item.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = "Arquivo inválido ou corrompido: ${validation.reason}. Toque para remover."
                        )
                    )
                    // If corresponding song exists, mark it or remove it so it doesn't break player
                    musicDao.deleteSongById("dl_${item.id}")
                } else {
                    val validAudio = validation as AudioValidationResult.Valid
                    // Check if title or artist needs recovery from ID3 tags
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        val id3Title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        val id3Artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        val id3Album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

                        val isSuspiciousName = item.title.startsWith("myato_") ||
                                item.title.matches(Regex("""^[a-zA-Z0-9_\-]{8,25}$""")) ||
                                item.artist == "Download Myato"

                        if (isSuspiciousName && !id3Title.isNullOrBlank()) {
                            val newTitle = id3Title
                            val newArtist = id3Artist ?: item.artist
                            val newAlbum = id3Album ?: "Downloads"

                            musicDao.updateDownload(
                                item.copy(
                                    title = newTitle,
                                    artist = newArtist,
                                    status = DownloadStatus.COMPLETED,
                                    errorMessage = null
                                )
                            )

                            val existingSong = musicDao.getSongById("dl_${item.id}")
                            if (existingSong != null) {
                                musicDao.updateSong(
                                    existingSong.copy(
                                        title = newTitle,
                                        artist = newArtist,
                                        album = newAlbum,
                                        duration = if (validAudio.durationMs > 0) validAudio.durationMs else existingSong.duration
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadEngine", "Error repairing downloads: ${e.message}", e)
        }
    }
}
