package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.local.MusicDao
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao
) {
    val allSongs: Flow<List<Song>> = musicDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = musicDao.getFavoriteSongs()
    val allPlaylists: Flow<List<Playlist>> = musicDao.getAllPlaylists()

    suspend fun scanLocalSongs(): Int = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val rawTitle = cursor.getString(titleColumn)
                    val displayName = cursor.getString(displayNameColumn) ?: "audio.mp3"
                    val title = if (!rawTitle.isNullOrBlank()) rawTitle else displayName.substringBeforeLast(".")
                    val artist = cursor.getString(artistColumn) ?: "Artista Desconhecido"
                    val album = cursor.getString(albumColumn) ?: "Álbum Desconhecido"
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    songs.add(
                        Song(
                            id = id.toString(),
                            title = title,
                            artist = if (artist == "<unknown>") "Artista Desconhecido" else artist,
                            album = if (album == "<unknown>") "Álbum Desconhecido" else album,
                            duration = if (duration > 0) duration else 180000L,
                            mediaUri = contentUri.toString(),
                            coverUri = artworkUri.toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error querying MediaStore", e)
        }

        if (songs.isNotEmpty()) {
            musicDao.insertSongs(songs)
        }
        songs.size
    }

    suspend fun importAudioUris(uris: List<Uri>): List<Song> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<Song>()
        for (uri in uris) {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                var title: String? = null
                var artist: String? = null
                var album: String? = null
                var duration: Long = 0L

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    duration = durStr?.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    Log.w("MusicRepository", "Could not retrieve metadata for $uri", e)
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }

                if (title.isNullOrBlank()) {
                    title = queryFileName(uri) ?: "Faixa Importada"
                }
                if (artist.isNullOrBlank()) artist = "Artista Local"
                if (album.isNullOrBlank()) album = "Importados"
                if (duration <= 0L) duration = 210000L // 3:30 min fallback

                val song = Song(
                    id = "import_" + UUID.randomUUID().toString().take(8),
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    mediaUri = uri.toString(),
                    coverUri = null,
                    isFavorite = false
                )
                musicDao.insertSong(song)
                imported.add(song)
            } catch (e: Exception) {
                Log.e("MusicRepository", "Error importing uri: $uri", e)
            }
        }
        imported
    }

    suspend fun seedSampleSongsIfEmpty() = withContext(Dispatchers.IO) {
        // High quality royalty-free demo tracks from SoundHelix to ensure immediate playable experience
        val samples = listOf(
            Song(
                id = "sample_1",
                title = "Neon Horizon (Synthwave)",
                artist = "Myato Originals",
                album = "Cyber Dreams FLAC",
                duration = 372000L,
                mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                coverUri = null,
                isFavorite = true
            ),
            Song(
                id = "sample_2",
                title = "Midnight Odyssey",
                artist = "Aura Wave",
                album = "Digital Pulse",
                duration = 423000L,
                mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                coverUri = null,
                isFavorite = false
            ),
            Song(
                id = "sample_3",
                title = "Electric Echoes",
                artist = "Crono Beats",
                album = "Future Bass",
                duration = 345000L,
                mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                coverUri = null,
                isFavorite = true
            )
        )
        musicDao.insertSongs(samples)
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name?.substringBeforeLast(".") ?: name
    }

    suspend fun toggleFavorite(songId: String, isFavorite: Boolean) {
        musicDao.updateFavoriteStatus(songId, isFavorite)
    }

    suspend fun deleteSong(song: Song) {
        musicDao.deleteSong(song)
    }

    suspend fun createPlaylist(name: String, description: String? = null): String {
        val id = UUID.randomUUID().toString()
        val playlist = Playlist(
            id = id,
            name = name,
            description = description
        )
        musicDao.insertPlaylist(playlist)
        return id
    }

    suspend fun updatePlaylistName(playlistId: String, newName: String) {
        musicDao.updatePlaylistName(playlistId, newName)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        musicDao.deletePlaylist(playlist)
    }

    fun getSongsInPlaylist(playlistId: String): Flow<List<Song>> {
        return musicDao.getSongsInPlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        musicDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>) {
        val crossRefs = songIds.mapIndexed { index, songId ->
            PlaylistSongCrossRef(playlistId, songId, order = index)
        }
        musicDao.addSongsToPlaylist(crossRefs)
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        musicDao.removeSongFromPlaylist(playlistId, songId)
    }
}
