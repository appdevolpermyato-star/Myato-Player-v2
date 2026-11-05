package com.example.data.download

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object YtDlpManager {
    private const val TAG = "YtDlpManager"

    suspend fun getInfo(url: String): VideoInfo = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url)
        YoutubeDL.getInstance().getInfo(request)
    }

    suspend fun downloadAudio(
        context: Context,
        url: String,
        onProgress: (progress: Float, etaInSeconds: Long, line: String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val downloadDir = File(context.getExternalFilesDir(null), "YtDlpDownloads")
        if (!downloadDir.exists()) downloadDir.mkdirs()

        val request = YoutubeDLRequest(url).apply {
            addOption("-x")
            addOption("--audio-format", "mp3")
            addOption("--audio-quality", "0")
            addOption("--no-mtime")
            addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")
        }

        val response: YoutubeDLResponse = YoutubeDL.getInstance().execute(request, "MyatoYtDlp") { progress, eta, line ->
            onProgress(progress, eta, line)
        }

        val files = downloadDir.listFiles()?.sortedByDescending { it.lastModified() }
        files?.firstOrNull { it.extension.equals("mp3", true) }
            ?: files?.firstOrNull()
            ?: throw Exception("Arquivo de áudio não encontrado após o download via yt-dlp.")
    }

    suspend fun getEngineVersion(context: Context): String = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().versionName(context) ?: "0.18.1"
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao obter versão yt-dlp: ${e.message}", e)
            "0.18.1"
        }
    }
}
