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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun startDownload(url: String, customTitle: String? = null, customArtist: String? = null): String {
        val downloadId = UUID.randomUUID().toString().take(8)
        
        scope.launch {
            // Check for duplicates
            val existing = musicDao.getAllDownloadsOnce().find { it.url == url }
            if (existing != null && existing.status == DownloadStatus.COMPLETED) {
                // If already completed, just ensure it is in library (it should be)
                return@launch
            }

            val inferredName = customTitle?.takeIf { it.isNotBlank() }
                ?: extractFileNameFromUrl(url)
                ?: "Faixa_${System.currentTimeMillis() % 10000}"

            val item = DownloadItem(
                id = downloadId,
                url = url,
                title = inferredName,
                artist = customArtist?.takeIf { it.isNotBlank() } ?: "Download Myato",
                totalBytes = 0L,
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
                    speedText = "Retomando..."
                )
            )
            launchDownloadJob(item)
        }
    }

    fun cancelDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
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
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir

            val safeTitle = currentItem.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val extension = when {
                currentItem.url.contains(".flac", ignoreCase = true) -> ".flac"
                currentItem.url.contains(".wav", ignoreCase = true) -> ".wav"
                currentItem.url.contains(".aac", ignoreCase = true) -> ".aac"
                currentItem.url.contains(".m4a", ignoreCase = true) -> ".m4a"
                else -> ".mp3"
            }

            val targetFile = currentItem.localPath?.let { File(it) } ?: File(musicDir, "myato_${id}_$safeTitle$extension")
            val existingSize = if (targetFile.exists()) targetFile.length() else 0L

            try {
                val requestBuilder = Request.Builder()
                    .url(currentItem.url)
                    .header("User-Agent", "MyatoPlayer/1.0 (Android)")
                
                if (existingSize > 0) {
                    requestBuilder.header("Range", "bytes=$existingSize-")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                
                // If we asked for a Range but got 200 OK, it means server doesn't support resumption
                // or the file is being sent from the start.
                val actualResuming = response.code == 206 && existingSize > 0
                
                if (!response.isSuccessful && response.code != 206) {
                    throw Exception("Servidor respondeu com código ${response.code}")
                }

                val body = response.body ?: throw Exception("Corpo da resposta vazio")
                val contentLength = body.contentLength()
                val totalBytes = if (actualResuming) existingSize + contentLength else if (contentLength > 0) contentLength else currentItem.totalBytes.coerceAtLeast(5 * 1024 * 1024L)

                currentItem = currentItem.copy(
                    totalBytes = totalBytes,
                    localPath = targetFile.absolutePath,
                    status = DownloadStatus.DOWNLOADING,
                    downloadedBytes = if (actualResuming) existingSize else 0L
                )
                musicDao.updateDownload(currentItem)

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    inputStream = body.byteStream()
                    // If not actually resuming (got 200 OK), overwrite the file (append = false)
                    outputStream = FileOutputStream(targetFile, actualResuming)

                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = if (actualResuming) existingSize else 0L
                    var lastUpdateTime = System.currentTimeMillis()
                    var bytesSinceLastUpdate = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) {
                            throw CancellationException("Download cancelado ou pausado")
                        }
                        outputStream.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        bytesSinceLastUpdate += bytesRead

                        val now = System.currentTimeMillis()
                        val diff = now - lastUpdateTime
                        if (diff >= 800) {
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

                    // Extract audio duration
                    var duration = 180000L
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(targetFile.absolutePath)
                        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        durStr?.toLongOrNull()?.let { if (it > 0) duration = it }
                    } catch (e: Exception) {
                        Log.w("DownloadEngine", "Metadata extraction failed: ${e.message}")
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }

                    // Download completed successfully
                    currentItem = currentItem.copy(
                        downloadedBytes = totalDownloaded,
                        totalBytes = totalDownloaded,
                        status = DownloadStatus.COMPLETED,
                        speedText = "Concluído",
                        localPath = targetFile.absolutePath
                    )
                    musicDao.updateDownload(currentItem)

                    // Register into local library
                    val song = Song(
                        id = "dl_$id",
                        title = currentItem.title,
                        artist = currentItem.artist,
                        album = "Downloads",
                        duration = duration,
                        mediaUri = Uri.fromFile(targetFile).toString(),
                        coverUri = null,
                        isFavorite = false,
                        isDownloaded = true,
                        dateAdded = System.currentTimeMillis()
                    )
                    musicDao.insertSong(song)

                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }

            } catch (e: CancellationException) {
                Log.i("DownloadEngine", "Download $id was paused/cancelled")
            } catch (e: Exception) {
                Log.e("DownloadEngine", "Download failed for $id", e)
                currentItem = currentItem.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message ?: "Falha no download",
                    speedText = "Erro"
                )
                musicDao.updateDownload(currentItem)
            } finally {
                activeJobs.remove(id)
            }
        }

        activeJobs[id] = job
    }

    private fun extractFileNameFromUrl(url: String): String? {
        return try {
            val path = Uri.parse(url).lastPathSegment ?: return null
            val clean = path.substringBefore("?").substringBefore("#")
            clean.substringBeforeLast(".")
        } catch (_: Exception) {
            null
        }
    }
}
