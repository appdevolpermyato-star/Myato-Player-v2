package com.example.ui.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Song
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val suggestions: List<String>) : AiUiState
    data class Error(val message: String) : AiUiState
}

class GeminiViewModel : ViewModel() {
    private val model by lazy {
        Firebase.vertexAI.generativeModel("gemini-3.8-flash")
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun getSuggestions(favoriteSongs: List<Song>) {
        if (favoriteSongs.isEmpty()) {
            _uiState.value = AiUiState.Error("Add some favorites first to get suggestions.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                val prompt = """
                    Based on these favorite songs:
                    ${favoriteSongs.joinToString("\n") { "${it.title} by ${it.artist}" }}
                    
                    Suggest 5 similar artists or songs that a music lover might enjoy.
                    Return the result as a raw JSON array of strings.
                    Example: ["Artist 1", "Artist 2", "Song 1", "Song 2", "Artist 3"]
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    model.generateContent(prompt)
                }

                val text = response.text
                if (!text.isNullOrBlank()) {
                    val jsonArray = JSONArray(text.replace("```json", "").replace("```", "").trim())
                    val suggestions = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        suggestions.add(jsonArray.getString(i))
                    }
                    _uiState.value = AiUiState.Success(suggestions)
                } else {
                    _uiState.value = AiUiState.Error("No suggestions found.")
                }
            } catch (e: Exception) {
                Log.e("AiViewModel", "AI Error", e)
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Failed to get suggestions")
            }
        }
    }
}
