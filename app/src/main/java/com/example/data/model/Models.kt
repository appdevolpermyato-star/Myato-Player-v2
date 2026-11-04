package com.example.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val mediaUri: String,
    val coverUri: String? = null,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val genre: String? = null,
    val folder: String? = null,
    val isDownloaded: Boolean = false
) {
    fun getUri(): Uri {
        return when {
            mediaUri.startsWith("content://") || mediaUri.startsWith("http://") || mediaUri.startsWith("https://") -> Uri.parse(mediaUri)
            mediaUri.startsWith("file://") -> Uri.parse(mediaUri)
            else -> Uri.fromFile(java.io.File(mediaUri))
        }
    }
}

@Serializable
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val coverUri: String? = null,
    val isFavorite: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis()
)

@Serializable
enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Serializable
@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val artist: String = "Download da Web",
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val localPath: String? = null,
    val speedText: String = "",
    val errorMessage: String? = null,
    val dateCreated: Long = System.currentTimeMillis()
)
