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
    val dateAdded: Long = System.currentTimeMillis()
) {
    fun getUri(): Uri = Uri.parse(mediaUri)
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
