package com.example.data.local

import androidx.room.*
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // Songs
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayedSongs(limit: Int = 10): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC, title ASC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int = 10): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isDownloaded = 1 OR id LIKE 'dl_%' ORDER BY dateAdded DESC")
    fun getDownloadedSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: String, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :songId")
    suspend fun recordSongPlayed(songId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): Song?

    @Delete
    suspend fun deleteSong(song: Song)

    @Delete
    suspend fun deleteSongs(songs: List<Song>)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: String, newName: String)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    // Playlist Songs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongsToPlaylist(crossRefs: List<PlaylistSongCrossRef>)

    @Query("""
        SELECT songs.* FROM songs
        INNER JOIN playlist_songs ON songs.id = playlist_songs.songId
        WHERE playlist_songs.playlistId = :playlistId
        ORDER BY playlist_songs.`order` ASC
    """)
    fun getSongsInPlaylist(playlistId: String): Flow<List<Song>>

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    // Downloads
    @Query("SELECT * FROM downloads ORDER BY dateCreated DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY dateCreated DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem)

    @Update
    suspend fun updateDownload(download: DownloadItem)

    @Delete
    suspend fun deleteDownload(download: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadItem?

    @Query("SELECT * FROM downloads")
    suspend fun getAllDownloadsOnce(): List<DownloadItem>

    @Update
    suspend fun updateSong(song: Song)

    @Query("SELECT * FROM songs WHERE isDownloaded = 1 OR id LIKE 'dl_%'")
    suspend fun getDownloadedSongsOnce(): List<Song>
}
