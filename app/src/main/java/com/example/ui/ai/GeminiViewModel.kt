package com.example.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val suggestions: List<String>) : AiUiState
    data class Error(val message: String) : AiUiState
}

class GeminiViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun getSuggestions(favoriteSongs: List<Song>) {
        if (favoriteSongs.isEmpty()) {
            _uiState.value = AiUiState.Error("Adicione músicas aos favoritos para gerar recomendações locais.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                // Local recommendation logic (100% offline)
                val artists = favoriteSongs.map { it.artist }.distinct()
                val suggestions = mutableListOf<String>()
                
                if (artists.isNotEmpty()) {
                    suggestions.add("Mais de ${artists.random()}")
                    if (artists.size > 1) suggestions.add("Estilo ${artists.random()}")
                }
                
                val genres = listOf("Synthwave", "Lo-Fi", "Acoustic", "Jazz", "Electronic", "Rock", "Pop")
                suggestions.add("Explorar ${genres.random()}")
                suggestions.add("Mix de Relaxamento")
                suggestions.add("Favoritos da Semana")

                _uiState.value = AiUiState.Success(suggestions.take(5))
            } catch (e: Exception) {
                _uiState.value = AiUiState.Error("Erro ao gerar recomendações locais.")
            }
        }
    }
}
