package com.example.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.network.ArchiveDoc
import com.example.data.network.ArchiveFile
import com.example.data.network.ArchiveMetadata
import com.example.data.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ArchiveUiState {
    data object Idle : ArchiveUiState
    data object Loading : ArchiveUiState
    data class SearchSuccess(val docs: List<ArchiveDoc>) : ArchiveUiState
    data class MetadataSuccess(val metadata: ArchiveMetadata, val files: List<ArchiveFile>) : ArchiveUiState
    data class Error(val message: String) : ArchiveUiState
}

class ArchiveViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ArchiveUiState>(ArchiveUiState.Idle)
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private val api = RetrofitInstance.archiveApi

    fun search(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = ArchiveUiState.Loading
            try {
                // Specific query for audio to ensure we get relevant results
                val archiveQuery = "mediatype:audio AND (title:\"$query\" OR creator:\"$query\")"
                val response = api.search(archiveQuery)
                _uiState.value = ArchiveUiState.SearchSuccess(response.response.docs)
            } catch (e: Exception) {
                _uiState.value = ArchiveUiState.Error("Erro na pesquisa: ${e.localizedMessage}")
            }
        }
    }

    fun getMetadata(identifier: String) {
        viewModelScope.launch {
            _uiState.value = ArchiveUiState.Loading
            try {
                val response = api.getMetadata(identifier)
                val audioFiles = response.files?.filter { 
                    it.format?.contains("MP3", ignoreCase = true) == true || 
                    it.format?.contains("VBR MP3", ignoreCase = true) == true ||
                    it.format?.contains("FLAC", ignoreCase = true) == true ||
                    it.format?.contains("WAV", ignoreCase = true) == true
                } ?: emptyList()
                
                _uiState.value = ArchiveUiState.MetadataSuccess(response.metadata, audioFiles)
            } catch (e: Exception) {
                _uiState.value = ArchiveUiState.Error("Erro ao buscar detalhes: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _uiState.value = ArchiveUiState.Idle
    }

    fun isLicenseAuthorized(licenseUrl: String?): Boolean {
        if (licenseUrl == null) return true // Archive.org usually allows personal use, but we should be careful.
        // Simple heuristic for common open licenses
        val authorized = licenseUrl.contains("creativecommons.org", ignoreCase = true) ||
                         licenseUrl.contains("publicdomain", ignoreCase = true)
        return authorized
    }
}
