package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // --- SONGS ---
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayedSongs(limit: Int = 10): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC, title ASC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int = 10): Flow<List<Song>>

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

    @Update
    suspend fun updateSong(song: Song)

    // --- PLAYLISTS ---
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: String, newName: String)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

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

    // --- HISTORY ---
    @Insert
    suspend fun insertHistoryEntry(entry: PlaybackHistory)

    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT :limit")
    fun getPlaybackHistory(limit: Int = 100): Flow<List<PlaybackHistory>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    // --- FAVORITE ALBUMS & ARTISTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteAlbum(album: FavoriteAlbum)

    @Query("DELETE FROM favorite_albums WHERE albumName = :name")
    suspend fun removeFavoriteAlbum(name: String)

    @Query("SELECT * FROM favorite_albums ORDER BY dateAdded DESC")
    fun getFavoriteAlbums(): Flow<List<FavoriteAlbum>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteArtist(artist: FavoriteArtist)

    @Query("DELETE FROM favorite_artists WHERE artistName = :name")
    suspend fun removeFavoriteArtist(name: String)

    @Query("SELECT * FROM favorite_artists ORDER BY dateAdded DESC")
    fun getFavoriteArtists(): Flow<List<FavoriteArtist>>

    // --- SYNCHRONOUS / AGGREGATE HELPERS ---
    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongByIdSync(songId: String): Song?

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsSync(): List<Song>

    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
    suspend fun getMostPlayedSongsSync(limit: Int): List<Song>

    @Query("SELECT SUM(playCount * duration) FROM songs")
    suspend fun getTotalPlaybackTimeSync(): Long

    @Query("SELECT artist FROM songs GROUP BY artist ORDER BY SUM(playCount) DESC LIMIT 1")
    suspend fun getTopArtistSync(): String?
}
