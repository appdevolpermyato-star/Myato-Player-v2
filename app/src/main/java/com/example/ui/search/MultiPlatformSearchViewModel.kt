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
    val platform: MusicPlatform
)

enum class MusicPlatform {
    JAMENDO
}

enum class SearchSourceFilter {
    ALL, JAMENDO
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

    private fun getJamendoClientId(): String {
        return try {
            val keyField = BuildConfig::class.java.getField("JAMENDO_CLIENT_ID")
            val key = keyField.get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_JAMENDO_CLIENT_ID") key else "709fa152"
        } catch (_: Throwable) {
            "709fa152"
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
                            unifiedList.add(
                                UnifiedTrack(
                                    id = "jam_${track.id}",
                                    title = track.name,
                                    artist = track.artist_name,
                                    album = track.album_name ?: "Jamendo",
                                    durationMs = (track.duration * 1000L),
                                    coverUrl = track.album_image ?: track.image,
                                    streamUrl = stream,
                                    platform = MusicPlatform.JAMENDO
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("MultiSearch", "Jamendo search failed: ${e.message}")
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
