package com.example.ui.youtube

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.MusicDao
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.network.ArchiveDoc
import com.example.data.network.JamendoTrack
import com.example.data.network.RetrofitInstance
import com.example.data.network.YouTubeDurationHelper
import com.example.ui.util.LinkClassifier
import com.example.ui.util.RecognizedLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class YouTubeTrackInfo(
    val videoId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val position: Int? = null,
    val rawInput: String
)

data class YouTubePlaylistInfo(
    val playlistId: String,
    val title: String,
    val channelTitle: String,
    val totalItems: Int,
    val thumbnailUrl: String?,
    val rawInput: String
)

data class YouTubePlaylistItemState(
    val track: YouTubeTrackInfo,
    val isSelected: Boolean = true,
    val authorizedMatch: ArchiveDoc? = null,
    val jamendoMatch: JamendoTrack? = null,
    val linkedLocalUri: Uri? = null,
    val statusText: String = "Aguardando fonte autorizada",
    val isMatched: Boolean = false,
    val isImported: Boolean = false
)

sealed interface YouTubeUiState {
    data object Idle : YouTubeUiState
    data class Loading(val message: String) : YouTubeUiState
    data class VideoLoaded(
        val track: YouTubeTrackInfo,
        val archiveMatches: List<ArchiveDoc> = emptyList(),
        val jamendoMatches: List<JamendoTrack> = emptyList(),
        val isSearchingArchive: Boolean = false,
        val isSearchingJamendo: Boolean = false
    ) : YouTubeUiState
    data class PlaylistLoaded(
        val playlist: YouTubePlaylistInfo,
        val items: List<YouTubePlaylistItemState>,
        val isSearchingAll: Boolean = false,
        val importProgress: Float? = null,
        val statusMessage: String? = null
    ) : YouTubeUiState
    data class Error(val message: String, val canRetryOEmbed: Boolean = false) : YouTubeUiState
}

class YouTubeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<YouTubeUiState>(YouTubeUiState.Idle)
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    private val _customApiKey = MutableStateFlow<String?>(null)
    val customApiKey: StateFlow<String?> = _customApiKey.asStateFlow()

    private val youtubeApi = RetrofitInstance.youtubeApi
    private val youtubeOEmbedApi = RetrofitInstance.youtubeOEmbedApi
    private val archiveApi = RetrofitInstance.archiveApi
    private val jamendoApi = RetrofitInstance.jamendoApi

    fun setCustomApiKey(key: String?) {
        _customApiKey.value = key?.trim()?.ifBlank { null }
    }

    private fun getEffectiveApiKey(): String? {
        val custom = _customApiKey.value
        if (!custom.isNullOrBlank()) return custom

        return try {
            val keyField = BuildConfig::class.java.getField("YOUTUBE_API_KEY")
            val buildConfigKey = keyField.get(null) as? String
            if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "MY_YOUTUBE_API_KEY") {
                buildConfigKey
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun getJamendoClientId(): String? {
        return try {
            val keyField = BuildConfig::class.java.getField("JAMENDO_CLIENT_ID")
            val buildConfigKey = keyField.get(null) as? String
            if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "MY_JAMENDO_CLIENT_ID") {
                buildConfigKey
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun processInputLink(input: String) {
        val recognized = LinkClassifier.classify(input)
        when (recognized) {
            is RecognizedLink.YouTubeVideo -> {
                fetchVideoDetails(recognized.videoId, recognized.rawInput)
            }
            is RecognizedLink.YouTubePlaylist -> {
                fetchPlaylistDetails(recognized.playlistId, recognized.rawInput)
            }
            else -> {
                _uiState.value = YouTubeUiState.Error("O link informado não é um vídeo ou playlist do YouTube.")
            }
        }
    }

    fun fetchVideoDetails(videoId: String, rawUrl: String) {
        viewModelScope.launch {
            _uiState.value = YouTubeUiState.Loading("Consultando metadados do vídeo no YouTube...")
            val apiKey = getEffectiveApiKey()

            if (!apiKey.isNullOrBlank()) {
                try {
                    Log.i("MyatoYouTube", "Consultando vídeo $videoId via YouTube Data API v3")
                    val response = youtubeApi.getVideoDetails(id = videoId, key = apiKey)
                    val item = response.items?.firstOrNull()

                    if (item != null) {
                        val title = item.snippet?.title ?: "Vídeo do YouTube"
                        val artist = item.snippet?.channelTitle ?: "Artista do YouTube"
                        val thumb = item.snippet?.thumbnails?.getBestUrl()
                        val duration = YouTubeDurationHelper.parseIsoDuration(item.contentDetails?.duration)

                        val track = YouTubeTrackInfo(
                            videoId = videoId,
                            title = title,
                            artist = artist,
                            durationMs = duration,
                            thumbnailUrl = thumb,
                            rawInput = rawUrl
                        )
                        _uiState.value = YouTubeUiState.VideoLoaded(track = track)
                        // Search in authorized sources
                        searchAuthorizedSources(track)
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.w("MyatoYouTube", "YouTube Data API falhou para o vídeo $videoId: ${e.message}. Tentando oEmbed...")
                }
            }

            // Fallback: Official YouTube oEmbed API (does NOT require API key)
            try {
                Log.i("MyatoYouTube", "Consultando vídeo $videoId via oEmbed oficial")
                val oembed = youtubeOEmbedApi.getOEmbed(url = "https://www.youtube.com/watch?v=$videoId")
                val track = YouTubeTrackInfo(
                    videoId = videoId,
                    title = oembed.title ?: "Vídeo $videoId",
                    artist = oembed.author_name ?: "YouTube",
                    durationMs = 180000L,
                    thumbnailUrl = oembed.thumbnail_url,
                    rawInput = rawUrl
                )
                _uiState.value = YouTubeUiState.VideoLoaded(track = track)
                searchAuthorizedSources(track)
            } catch (e: Exception) {
                Log.e("MyatoYouTube", "Falha na consulta do vídeo: ${e.message}", e)
                _uiState.value = YouTubeUiState.Error(
                    "Não foi possível obter informações do vídeo '$videoId'. Verifique sua conexão e se o vídeo está público."
                )
            }
        }
    }

    fun searchAuthorizedSources(track: YouTubeTrackInfo) {
        searchArchiveForTrack(track)
        searchJamendoForTrack(track)
    }

    fun searchJamendoForTrack(track: YouTubeTrackInfo) {
        val current = _uiState.value
        if (current is YouTubeUiState.VideoLoaded) {
            _uiState.value = current.copy(isSearchingJamendo = true)
        }

        viewModelScope.launch {
            try {
                val clientId = getJamendoClientId()
                if (clientId.isNullOrBlank()) {
                    Log.w("MyatoYouTube", "Jamendo Client ID não configurado")
                    val now = _uiState.value
                    if (now is YouTubeUiState.VideoLoaded) {
                        _uiState.value = now.copy(isSearchingJamendo = false)
                    }
                    return@launch
                }

                val cleanTitle = cleanTitleForSearch(track.title)
                val cleanArtist = cleanArtistForSearch(track.artist)

                Log.i("MyatoYouTube", "Buscando áudio autorizado no Jamendo para '${track.title}'")
                
                // Try searching by name and artist
                val response = jamendoApi.searchTracks(
                    clientId = clientId,
                    name = cleanTitle,
                    artist = cleanArtist,
                    limit = 5
                )
                
                var tracks = response.results.filter { it.audiodownload_allowed }

                // If no exact match, try a broader search
                if (tracks.isEmpty()) {
                    val broadRes = jamendoApi.searchTracks(
                        clientId = clientId,
                        query = "$cleanTitle $cleanArtist",
                        limit = 5
                    )
                    tracks = broadRes.results.filter { it.audiodownload_allowed }
                }

                val now = _uiState.value
                if (now is YouTubeUiState.VideoLoaded) {
                    _uiState.value = now.copy(
                        jamendoMatches = tracks,
                        isSearchingJamendo = false
                    )
                }
            } catch (e: Exception) {
                Log.w("MyatoYouTube", "Falha na busca do Jamendo para ${track.title}: ${e.message}")
                val now = _uiState.value
                if (now is YouTubeUiState.VideoLoaded) {
                    _uiState.value = now.copy(isSearchingJamendo = false)
                }
            }
        }
    }

    fun fetchPlaylistDetails(playlistId: String, rawUrl: String) {
        viewModelScope.launch {
            _uiState.value = YouTubeUiState.Loading("Consultando playlist no YouTube...")
            val apiKey = getEffectiveApiKey()

            if (apiKey.isNullOrBlank()) {
                _uiState.value = YouTubeUiState.Error(
                    "Para consultar playlists completas do YouTube é necessária uma Chave da YouTube Data API v3.\n\n" +
                    "Você pode inserir sua chave de API gratuita do Google Cloud nas configurações abaixo."
                )
                return@launch
            }

            try {
                Log.i("MyatoYouTube", "Consultando playlist $playlistId via YouTube Data API v3")
                val playlistRes = youtubeApi.getPlaylistDetails(id = playlistId, key = apiKey)
                val playlistItem = playlistRes.items?.firstOrNull()

                if (playlistItem == null) {
                    _uiState.value = YouTubeUiState.Error("Playlist '$playlistId' não encontrada ou privada.")
                    return@launch
                }

                val pTitle = playlistItem.snippet?.title ?: "Playlist do YouTube"
                val pChannel = playlistItem.snippet?.channelTitle ?: "YouTube"
                val pThumb = playlistItem.snippet?.thumbnails?.getBestUrl()
                val pCount = playlistItem.contentDetails?.itemCount ?: 0

                val playlistInfo = YouTubePlaylistInfo(
                    playlistId = playlistId,
                    title = pTitle,
                    channelTitle = pChannel,
                    totalItems = pCount,
                    thumbnailUrl = pThumb,
                    rawInput = rawUrl
                )

                // Fetch playlist items (up to 50)
                _uiState.value = YouTubeUiState.Loading("Listando músicas da playlist '$pTitle'...")
                val itemsRes = youtubeApi.getPlaylistItems(playlistId = playlistId, key = apiKey, maxResults = 50)
                val entries = itemsRes.items ?: emptyList()

                val trackItems = entries.mapNotNull { entry ->
                    val vid = entry.snippet?.resourceId?.videoId ?: entry.contentDetails?.videoId ?: return@mapNotNull null
                    val tTitle = entry.snippet?.title ?: return@mapNotNull null
                    if (tTitle == "Private video" || tTitle == "Deleted video") return@mapNotNull null

                    val tArtist = entry.snippet?.channelTitle ?: pChannel
                    val tThumb = entry.snippet?.thumbnails?.getBestUrl() ?: pThumb

                    val track = YouTubeTrackInfo(
                        videoId = vid,
                        title = tTitle,
                        artist = tArtist,
                        durationMs = 180000L,
                        thumbnailUrl = tThumb,
                        position = entry.snippet?.position ?: 0,
                        rawInput = "https://www.youtube.com/watch?v=$vid"
                    )

                    YouTubePlaylistItemState(
                        track = track,
                        isSelected = true,
                        statusText = "Metadados obtidos (Aguardando áudio)"
                    )
                }

                _uiState.value = YouTubeUiState.PlaylistLoaded(
                    playlist = playlistInfo,
                    items = trackItems
                )
            } catch (e: Exception) {
                Log.e("MyatoYouTube", "Erro ao carregar playlist: ${e.message}", e)
                _uiState.value = YouTubeUiState.Error(
                    "Erro ao consultar a playlist: ${e.localizedMessage ?: "Verifique a chave de API e se a playlist está pública."}"
                )
            }
        }
    }

    fun searchArchiveForTrack(track: YouTubeTrackInfo) {
        val current = _uiState.value
        if (current is YouTubeUiState.VideoLoaded) {
            _uiState.value = current.copy(isSearchingArchive = true)
        }

        viewModelScope.launch {
            try {
                val cleanTitle = cleanTitleForSearch(track.title)
                val cleanArtist = cleanArtistForSearch(track.artist)
                val query = "mediatype:audio AND ($cleanTitle)"
                Log.i("MyatoYouTube", "Buscando áudio autorizado no Internet Archive para '${track.title}': $query")

                val response = archiveApi.search(query = query, rows = 10)
                val docs = response.response?.docs ?: emptyList()

                val now = _uiState.value
                if (now is YouTubeUiState.VideoLoaded) {
                    _uiState.value = now.copy(
                        archiveMatches = docs,
                        isSearchingArchive = false
                    )
                }
            } catch (e: Exception) {
                Log.w("MyatoYouTube", "Falha na busca do Archive para ${track.title}: ${e.message}")
                val now = _uiState.value
                if (now is YouTubeUiState.VideoLoaded) {
                    _uiState.value = now.copy(isSearchingArchive = false)
                }
            }
        }
    }

    fun toggleItemSelection(videoId: String) {
        val current = _uiState.value as? YouTubeUiState.PlaylistLoaded ?: return
        val updated = current.items.map { item ->
            if (item.track.videoId == videoId) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
        _uiState.value = current.copy(items = updated)
    }

    fun selectAll(select: Boolean) {
        val current = _uiState.value as? YouTubeUiState.PlaylistLoaded ?: return
        val updated = current.items.map { it.copy(isSelected = select) }
        _uiState.value = current.copy(items = updated)
    }

    fun batchSearchAuthorizedSources() {
        val current = _uiState.value as? YouTubeUiState.PlaylistLoaded ?: return
        _uiState.value = current.copy(isSearchingAll = true, statusMessage = "Buscando áudios autorizados no catálogo...")

        viewModelScope.launch {
            val updatedItems = current.items.toMutableList()
            val clientId = getJamendoClientId()
            
            for (i in updatedItems.indices) {
                val item = updatedItems[i]
                if (item.isSelected && !item.isMatched && !item.isImported && item.linkedLocalUri == null) {
                    try {
                        val cleanTitle = cleanTitleForSearch(item.track.title)
                        val cleanArtist = cleanArtistForSearch(item.track.artist)
                        
                        // 1. Try Jamendo first (often better metadata)
                        var jamendoMatch: JamendoTrack? = null
                        if (!clientId.isNullOrBlank()) {
                            val jamRes = jamendoApi.searchTracks(
                                clientId = clientId,
                                name = cleanTitle,
                                artist = cleanArtist,
                                limit = 1
                            )
                            jamendoMatch = jamRes.results.firstOrNull { it.audiodownload_allowed }
                        }

                        if (jamendoMatch != null) {
                            updatedItems[i] = item.copy(
                                jamendoMatch = jamendoMatch,
                                isMatched = true,
                                statusText = "Fonte autorizada encontrada no Jamendo"
                            )
                        } else {
                            // 2. Try Internet Archive
                            val query = "mediatype:audio AND ($cleanTitle)"
                            val res = archiveApi.search(query = query, rows = 1)
                            val archiveMatch = res.response?.docs?.firstOrNull()

                            if (archiveMatch != null) {
                                updatedItems[i] = item.copy(
                                    authorizedMatch = archiveMatch,
                                    isMatched = true,
                                    statusText = "Fonte autorizada encontrada no Archive"
                                )
                            } else {
                                updatedItems[i] = item.copy(
                                    statusText = "Sem correspondência automática (vincule arquivo local)"
                                )
                            }
                        }
                    } catch (_: Exception) {
                        updatedItems[i] = item.copy(statusText = "Falha na busca de fonte")
                    }
                }
            }
            _uiState.value = current.copy(
                items = updatedItems,
                isSearchingAll = false,
                statusMessage = "Busca concluída"
            )
        }
    }

    suspend fun createLocalPlaylistFromYouTube(
        musicDao: MusicDao,
        playlistInfo: YouTubePlaylistInfo,
        itemsToInclude: List<Song>
    ): String = withContext(Dispatchers.IO) {
        val playlistId = "yt_pl_${playlistInfo.playlistId}"
        val playlist = Playlist(
            id = playlistId,
            name = playlistInfo.title,
            description = "Importada do YouTube (${itemsToInclude.size} músicas)",
            coverUri = playlistInfo.thumbnailUrl,
            dateCreated = System.currentTimeMillis()
        )
        musicDao.insertPlaylist(playlist)

        if (itemsToInclude.isNotEmpty()) {
            val crossRefs = itemsToInclude.mapIndexed { index, song ->
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = song.id,
                    order = index
                )
            }
            musicDao.addSongsToPlaylist(crossRefs)
        }
        playlistId
    }

    suspend fun linkLocalAudioToTrack(
        track: YouTubeTrackInfo,
        audioUri: Uri,
        albumName: String?,
        musicDao: MusicDao
    ): Song = withContext(Dispatchers.IO) {
        val song = Song(
            id = "yt_${track.videoId}_${UUID.randomUUID().toString().take(6)}",
            title = track.title,
            artist = track.artist,
            album = albumName ?: "Importados do YouTube",
            duration = track.durationMs,
            mediaUri = audioUri.toString(),
            coverUri = track.thumbnailUrl,
            isFavorite = false,
            isDownloaded = true,
            dateAdded = System.currentTimeMillis()
        )
        musicDao.insertSong(song)
        song
    }

    fun resetState() {
        _uiState.value = YouTubeUiState.Idle
    }

    private fun cleanTitleForSearch(title: String): String {
        return title
            .replace(Regex("""(?i)\(official\s*(music)?\s*video\)"""), "")
            .replace(Regex("""(?i)\[official\s*(music)?\s*video\]"""), "")
            .replace(Regex("""(?i)\(lyric\s*video\)"""), "")
            .replace(Regex("""(?i)\[lyric\s*video\]"""), "")
            .replace(Regex("""(?i)\(audio\)"""), "")
            .replace(Regex("""(?i)\[audio\]"""), "")
            .replace(Regex("""(?i)\(clipe\s*oficial\)"""), "")
            .replace(Regex("""(?i)\[clipe\s*oficial\]"""), "")
            .replace(Regex("""(?i)\(hd\)"""), "")
            .replace(Regex("""(?i)\[hd\]"""), "")
            .replace(Regex("""(?i)\(4k\)"""), "")
            .replace(Regex("""[#|•]"""), " ")
            .replace("\"", "")
            .trim()
    }

    private fun cleanArtistForSearch(artist: String): String {
        return artist
            .replace(Regex("""(?i) - Topic"""), "")
            .replace(Regex("""(?i)VEVO"""), "")
            .trim()
    }
}
