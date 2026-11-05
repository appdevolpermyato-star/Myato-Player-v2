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
    val isDownloaded: Boolean = false,
    
    // Technical metadata
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val format: String = "Unknown",
    val fileSize: Long = 0L
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
@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val playbackDuration: Long = 0L // How long the song was actually heard
)

@Serializable
@Entity(tableName = "favorite_albums")
data class FavoriteAlbum(
    @PrimaryKey val albumName: String,
    val artistName: String,
    val coverUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "favorite_artists")
data class FavoriteArtist(
    @PrimaryKey val artistName: String,
    val coverUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)
