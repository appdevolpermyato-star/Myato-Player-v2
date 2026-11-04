package com.example.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UnifiedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val coverUrl: String?,
    val streamUrl: String?,
    val downloadUrl: String?,
    val isDownloadAllowed: Boolean,
    val platform: MusicPlatform
)

enum class MusicPlatform {
    JAMENDO, SOUNDCLOUD
}

enum class SearchSourceFilter {
    ALL, JAMENDO, SOUNDCLOUD
}

sealed interface MultiPlatformUiState {
    data object Idle : MultiPlatformUiState
    data object Loading : MultiPlatformUiState
    data class Success(
        val tracks: List<UnifiedTrack>,
        val query: String,
        val filter: SearchSourceFilter
    ) : MultiPlatformUiState
    data class Error(val message: String) : MultiPlatformUiState
}

class MultiPlatformSearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MultiPlatformUiState>(MultiPlatformUiState.Idle)
    val uiState: StateFlow<MultiPlatformUiState> = _uiState.asStateFlow()

    private val jamendoApi = RetrofitInstance.jamendoApi
    private val soundCloudApi = RetrofitInstance.soundCloudApi

    private fun getJamendoClientId(): String {
        return try {
            val keyField = BuildConfig::class.java.getField("JAMENDO_CLIENT_ID")
            val key = keyField.get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_JAMENDO_CLIENT_ID") key else "709fa152"
        } catch (_: Throwable) {
            "709fa152"
        }
    }

    private fun getSoundCloudClientId(): String {
        return try {
            val keyField = BuildConfig::class.java.getField("SOUNDCLOUD_CLIENT_ID")
            val key = keyField.get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_SOUNDCLOUD_CLIENT_ID") key else "a3119339a9c2db9c6d3dfcf3519c67bc"
        } catch (_: Throwable) {
            "a3119339a9c2db9c6d3dfcf3519c67bc"
        }
    }

    fun search(query: String, filter: SearchSourceFilter) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.value = MultiPlatformUiState.Idle
            return
        }

        viewModelScope.launch {
            _uiState.value = MultiPlatformUiState.Loading
            try {
                val unifiedList = mutableListOf<UnifiedTrack>()

                // 1. Fetch Jamendo if ALL or JAMENDO
                if (filter == SearchSourceFilter.ALL || filter == SearchSourceFilter.JAMENDO) {
                    try {
                        val jamResponse = jamendoApi.searchTracks(
                            clientId = getJamendoClientId(),
                            search = trimmed,
                            limit = 25
                        )
                        jamResponse.results.forEach { track ->
                            val stream = track.audio
                            val download = track.audiodownload?.takeIf { it.isNotBlank() } ?: track.audio
                            unifiedList.add(
                                UnifiedTrack(
                                    id = "jam_${track.id}",
                                    title = track.name,
                                    artist = track.artist_name,
                                    album = track.album_name ?: "Jamendo",
                                    durationMs = (track.duration * 1000L),
                                    coverUrl = track.album_image ?: track.image,
                                    streamUrl = stream,
                                    downloadUrl = download,
                                    isDownloadAllowed = track.audiodownload_allowed,
                                    platform = MusicPlatform.JAMENDO
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("MultiSearch", "Jamendo search failed: ${e.message}")
                    }
                }

                // 2. Fetch SoundCloud if ALL or SOUNDCLOUD
                if (filter == SearchSourceFilter.ALL || filter == SearchSourceFilter.SOUNDCLOUD) {
                    try {
                        val scClientId = getSoundCloudClientId()
                        val scResponse = soundCloudApi.searchTracks(
                            query = trimmed,
                            clientId = scClientId,
                            limit = 25
                        )
                        for (scTrack in scResponse.collection) {
                            var playableUrl = scTrack.streamUrl
                            if (playableUrl.isNullOrBlank() && scTrack.media?.transcodings != null) {
                                val mp3Transcoding = scTrack.media.transcodings.firstOrNull { it.format.protocol == "progressive" || it.format.mimeType.contains("audio") }
                                if (mp3Transcoding != null) {
                                    try {
                                        val resolved = soundCloudApi.resolveStreamUrl(mp3Transcoding.url, scClientId)
                                        playableUrl = resolved.url
                                    } catch (_: Exception) {}
                                }
                            }

                            val downloadUrl = if (scTrack.downloadable && !scTrack.downloadUrl.isNullOrBlank()) {
                                "${scTrack.downloadUrl}?client_id=$scClientId"
                            } else null

                            unifiedList.add(
                                UnifiedTrack(
                                    id = "sc_${scTrack.id}",
                                    title = scTrack.title,
                                    artist = scTrack.user.username,
                                    album = "SoundCloud",
                                    durationMs = scTrack.duration,
                                    coverUrl = scTrack.artworkUrl?.replace("large", "t500x500"),
                                    streamUrl = playableUrl,
                                    downloadUrl = downloadUrl,
                                    isDownloadAllowed = scTrack.downloadable && !downloadUrl.isNullOrBlank(),
                                    platform = MusicPlatform.SOUNDCLOUD
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("MultiSearch", "SoundCloud search failed: ${e.message}")
                    }
                }

                _uiState.value = MultiPlatformUiState.Success(
                    tracks = unifiedList,
                    query = trimmed,
                    filter = filter
                )
            } catch (e: Exception) {
                Log.e("MultiSearch", "Error in multi-search", e)
                _uiState.value = MultiPlatformUiState.Error("Erro na pesquisa: ${e.localizedMessage ?: "Falha de conexão"}")
            }
        }
    }

    fun resetState() {
        _uiState.value = MultiPlatformUiState.Idle
    }
}
