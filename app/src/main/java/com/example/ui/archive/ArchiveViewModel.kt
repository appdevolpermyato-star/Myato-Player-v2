package com.example.ui.archive

import android.net.Uri
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
    data class MetadataSuccess(
        val identifier: String,
        val metadata: ArchiveMetadata,
        val files: List<ArchiveFile>
    ) : ArchiveUiState
    data class Error(val message: String) : ArchiveUiState
}

class ArchiveViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ArchiveUiState>(ArchiveUiState.Idle)
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private val api = RetrofitInstance.archiveApi

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _uiState.value = ArchiveUiState.Loading
            try {
                // If the user entered an Archive.org URL or item identifier directly, fetch its metadata immediately
                val extractedId = extractArchiveIdentifier(trimmed)
                if (extractedId != null) {
                    getMetadata(extractedId)
                    return@launch
                }

                // Query for audio files matching title, creator, or general terms
                val cleanTerm = trimmed.replace("\"", "")
                val archiveQuery = "mediatype:audio AND ($cleanTerm)"
                val response = api.search(query = archiveQuery, rows = 40)
                val docs = response.response?.docs ?: emptyList()
                _uiState.value = ArchiveUiState.SearchSuccess(docs)
            } catch (e: Exception) {
                _uiState.value = ArchiveUiState.Error("Erro na pesquisa: ${e.localizedMessage ?: "Falha na conexão"}")
            }
        }
    }

    fun getMetadata(identifier: String) {
        viewModelScope.launch {
            _uiState.value = ArchiveUiState.Loading
            try {
                val cleanId = identifier.trim()
                val response = api.getMetadata(cleanId)
                val meta = response.metadata ?: ArchiveMetadata(identifier = cleanId, title = cleanId)
                val allFiles = response.files ?: emptyList()

                val audioFiles = allFiles.filter { isAudioFile(it) }

                if (audioFiles.isEmpty()) {
                    _uiState.value = ArchiveUiState.Error(
                        "O item '$cleanId' não possui arquivos de áudio compatíveis para download."
                    )
                } else {
                    _uiState.value = ArchiveUiState.MetadataSuccess(
                        identifier = cleanId,
                        metadata = meta,
                        files = audioFiles
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ArchiveUiState.Error(
                    "Erro ao buscar metadados do item '$identifier': ${e.localizedMessage ?: "Verifique a conexão"}"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ArchiveUiState.Idle
    }

    fun isAudioFile(file: ArchiveFile): Boolean {
        val name = file.name.lowercase()
        val fmt = file.format?.lowercase() ?: ""

        val isBlacklisted = name.endsWith(".xml") ||
                name.endsWith(".json") ||
                name.endsWith(".sqlite") ||
                name.endsWith(".png") ||
                name.endsWith(".jpg") ||
                name.endsWith(".jpeg") ||
                name.endsWith(".txt") ||
                name.endsWith(".pdf") ||
                name.endsWith(".torrent") ||
                name.endsWith(".zip") ||
                fmt.contains("metadata") ||
                fmt.contains("spectrogram") ||
                fmt.contains("item tile") ||
                fmt.contains("bittorrent")
        if (isBlacklisted) return false

        val isAudioExtension = name.endsWith(".mp3") ||
                name.endsWith(".flac") ||
                name.endsWith(".wav") ||
                name.endsWith(".ogg") ||
                name.endsWith(".m4a") ||
                name.endsWith(".aac") ||
                name.endsWith(".opus") ||
                name.endsWith(".aiff") ||
                name.endsWith(".wma")

        val isAudioFormat = fmt.contains("mp3") ||
                fmt.contains("flac") ||
                fmt.contains("wave") ||
                fmt.contains("wav") ||
                fmt.contains("ogg") ||
                fmt.contains("vorbis") ||
                fmt.contains("audio") ||
                fmt.contains("aac") ||
                fmt.contains("m4a") ||
                fmt.contains("opus")

        val size = file.size?.toLongOrNull() ?: 0L
        return (isAudioExtension || isAudioFormat) && size > 1024L
    }

    fun isLicenseAuthorized(licenseUrl: String?): Boolean {
        if (licenseUrl.isNullOrBlank()) return true // Public catalog items on Archive.org
        val lower = licenseUrl.lowercase()
        return lower.contains("creativecommons.org") ||
                lower.contains("publicdomain") ||
                lower.contains("open") ||
                lower.contains("zero") ||
                lower.contains("gpl") ||
                lower.contains("mit") ||
                lower.contains("apache")
    }

    private fun encodeUriComponent(s: String): String {
        return try {
            Uri.encode(s)
        } catch (_: Throwable) {
            java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        }
    }

    fun buildDownloadUrl(identifier: String, fileName: String): String {
        val cleanId = encodeUriComponent(identifier.trim())
        val encodedFileName = fileName.trim().split("/").joinToString("/") { segment ->
            encodeUriComponent(segment)
        }
        return "https://archive.org/download/$cleanId/$encodedFileName"
    }

    fun extractArchiveIdentifier(input: String): String? {
        val trimmed = input.trim()
        val detailsRegex = Regex("""archive\.org/details/([a-zA-Z0-9_\-\.]+)""")
        val metadataRegex = Regex("""archive\.org/metadata/([a-zA-Z0-9_\-\.]+)""")
        val downloadRegex = Regex("""archive\.org/download/([a-zA-Z0-9_\-\.]+)(?:/.*)?""")

        detailsRegex.find(trimmed)?.groupValues?.get(1)?.let { return it }
        metadataRegex.find(trimmed)?.groupValues?.get(1)?.let { return it }
        downloadRegex.find(trimmed)?.groupValues?.get(1)?.let { return it }

        // If it's a raw identifier (e.g. 1ybpHiqnQY4 or soundcloud-232493501), no protocol or slashes
        if (!trimmed.contains("://") && !trimmed.contains("/") && !trimmed.contains(" ") && trimmed.length in 5..80) {
            return trimmed
        }
        return null
    }
}
