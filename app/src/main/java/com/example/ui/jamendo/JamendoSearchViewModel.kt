package com.example.ui.jamendo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.network.JamendoAlbum
import com.example.data.network.JamendoPlaylist
import com.example.data.network.JamendoTrack
import com.example.data.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class JamendoSearchType {
    TRACKS, ALBUMS, PLAYLISTS
}

enum class JamendoFilterCategory {
    ALL, TRACK_NAME, ARTIST, ALBUM, GENRE, PLAYLIST
}

sealed interface JamendoUiState {
    data object Idle : JamendoUiState
    data object Loading : JamendoUiState
    data class TracksLoaded(
        val tracks: List<JamendoTrack>,
        val hasMore: Boolean,
        val offset: Int
    ) : JamendoUiState
    data class AlbumsLoaded(
        val albums: List<JamendoAlbum>,
        val hasMore: Boolean,
        val offset: Int
    ) : JamendoUiState
    data class PlaylistsLoaded(
        val playlists: List<JamendoPlaylist>,
        val hasMore: Boolean,
        val offset: Int
    ) : JamendoUiState
    data class AlbumDetailLoaded(
        val album: JamendoAlbum,
        val tracks: List<JamendoTrack>
    ) : JamendoUiState
    data class PlaylistDetailLoaded(
        val playlist: JamendoPlaylist,
        val tracks: List<JamendoTrack>
    ) : JamendoUiState
    data class Error(val message: String) : JamendoUiState
}

class JamendoSearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<JamendoUiState>(JamendoUiState.Idle)
    val uiState: StateFlow<JamendoUiState> = _uiState.asStateFlow()

    private val api = RetrofitInstance.jamendoApi

    private var currentQuery = ""
    private var currentCategory = JamendoFilterCategory.ALL
    private var currentSearchType = JamendoSearchType.TRACKS

    private fun getClientId(): String {
        return try {
            val keyField = BuildConfig::class.java.getField("JAMENDO_CLIENT_ID")
            val key = keyField.get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_JAMENDO_CLIENT_ID") key else "709fa152"
        } catch (_: Throwable) {
            "709fa152"
        }
    }

    fun search(query: String, category: JamendoFilterCategory, searchType: JamendoSearchType, offset: Int = 0) {
        val trimmed = query.trim()
        currentQuery = trimmed
        currentCategory = category
        currentSearchType = searchType

        viewModelScope.launch {
            if (offset == 0) {
                _uiState.value = JamendoUiState.Loading
            }
            try {
                val clientId = getClientId()
                when (searchType) {
                    JamendoSearchType.TRACKS -> {
                        val response = when (category) {
                            JamendoFilterCategory.TRACK_NAME -> api.searchTracks(clientId = clientId, name = trimmed, offset = offset)
                            JamendoFilterCategory.ARTIST -> api.searchTracks(clientId = clientId, artist = trimmed, offset = offset)
                            JamendoFilterCategory.ALBUM -> api.searchTracks(clientId = clientId, album = trimmed, offset = offset)
                            JamendoFilterCategory.GENRE -> api.searchTracks(clientId = clientId, tags = trimmed, offset = offset)
                            else -> api.searchTracks(clientId = clientId, search = trimmed.ifBlank { null }, offset = offset)
                        }
                        val results = response.results
                        val hasMore = results.size >= 20
                        val existing = if (offset > 0 && _uiState.value is JamendoUiState.TracksLoaded) {
                            (_uiState.value as JamendoUiState.TracksLoaded).tracks
                        } else emptyList()

                        _uiState.value = JamendoUiState.TracksLoaded(
                            tracks = existing + results,
                            hasMore = hasMore,
                            offset = offset
                        )
                    }
                    JamendoSearchType.ALBUMS -> {
                        val response = when (category) {
                            JamendoFilterCategory.ARTIST -> api.searchAlbums(clientId = clientId, artist = trimmed, offset = offset)
                            JamendoFilterCategory.ALBUM -> api.searchAlbums(clientId = clientId, name = trimmed, offset = offset)
                            else -> api.searchAlbums(clientId = clientId, search = trimmed.ifBlank { null }, offset = offset)
                        }
                        val results = response.results
                        val hasMore = results.size >= 20
                        val existing = if (offset > 0 && _uiState.value is JamendoUiState.AlbumsLoaded) {
                            (_uiState.value as JamendoUiState.AlbumsLoaded).albums
                        } else emptyList()

                        _uiState.value = JamendoUiState.AlbumsLoaded(
                            albums = existing + results,
                            hasMore = hasMore,
                            offset = offset
                        )
                    }
                    JamendoSearchType.PLAYLISTS -> {
                        val response = when (category) {
                            JamendoFilterCategory.PLAYLIST -> api.searchPlaylists(clientId = clientId, name = trimmed, offset = offset)
                            else -> api.searchPlaylists(clientId = clientId, search = trimmed.ifBlank { null }, offset = offset)
                        }
                        val results = response.results
                        val hasMore = results.size >= 20
                        val existing = if (offset > 0 && _uiState.value is JamendoUiState.PlaylistsLoaded) {
                            (_uiState.value as JamendoUiState.PlaylistsLoaded).playlists
                        } else emptyList()

                        _uiState.value = JamendoUiState.PlaylistsLoaded(
                            playlists = existing + results,
                            hasMore = hasMore,
                            offset = offset
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("JamendoSearch", "Search error: ${e.message}", e)
                _uiState.value = JamendoUiState.Error("Erro na pesquisa: ${e.localizedMessage ?: "Falha de conexão"}")
            }
        }
    }

    fun loadAlbumDetails(albumId: String) {
        viewModelScope.launch {
            _uiState.value = JamendoUiState.Loading
            try {
                val clientId = getClientId()
                val response = api.getAlbumTracks(clientId = clientId, albumId = albumId)
                val albumData = response.results.firstOrNull()
                if (albumData != null) {
                    val albumObj = JamendoAlbum(
                        id = albumData.id,
                        name = albumData.name,
                        artist_name = albumData.artist_name,
                        image = albumData.image,
                        zip = albumData.zip,
                        zip_allowed = albumData.zip_allowed
                    )
                    _uiState.value = JamendoUiState.AlbumDetailLoaded(album = albumObj, tracks = albumData.tracks)
                } else {
                    _uiState.value = JamendoUiState.Error("Álbum não encontrado.")
                }
            } catch (e: Exception) {
                _uiState.value = JamendoUiState.Error("Erro ao carregar álbum: ${e.localizedMessage}")
            }
        }
    }

    fun loadPlaylistDetails(playlistId: String) {
        viewModelScope.launch {
            _uiState.value = JamendoUiState.Loading
            try {
                val clientId = getClientId()
                val response = api.getPlaylistTracks(clientId = clientId, playlistId = playlistId)
                val plData = response.results.firstOrNull()
                if (plData != null) {
                    val plObj = JamendoPlaylist(
                        id = plData.id,
                        name = plData.name,
                        user_name = plData.user_name,
                        zip = plData.zip
                    )
                    _uiState.value = JamendoUiState.PlaylistDetailLoaded(playlist = plObj, tracks = plData.tracks)
                } else {
                    _uiState.value = JamendoUiState.Error("Playlist não encontrada.")
                }
            } catch (e: Exception) {
                _uiState.value = JamendoUiState.Error("Erro ao carregar playlist: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _uiState.value = JamendoUiState.Idle
    }
}
