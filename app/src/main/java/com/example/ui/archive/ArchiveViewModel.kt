package com.example.ui.archive

import android.net.Uri
import android.util.Log
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
                // Check if user entered an external URL (e.g. YouTube)
                if (isExternalServiceUrl(trimmed)) {
                    _uiState.value = ArchiveUiState.Error(
                        "Links de plataformas externas (como YouTube ou Spotify) não são compatíveis. " +
                        "O Myato Player realiza pesquisas e downloads legais diretamente do Internet Archive (archive.org). " +
                        "Experimente pesquisar pelo nome da música ou artista."
                    )
                    return@launch
                }

                // If user entered an Archive.org URL or item identifier directly, fetch its metadata immediately
                val extractedId = extractArchiveIdentifier(trimmed)
                if (extractedId != null) {
                    getMetadata(extractedId)
                    return@launch
                }

                // Query Internet Archive for audio files matching title, creator, or general terms
                val cleanTerm = trimmed.replace("\"", "")
                val archiveQuery = "mediatype:audio AND ($cleanTerm)"
                Log.i("MyatoArchive", "Iniciando pesquisa no Internet Archive: $archiveQuery")

                val response = api.search(query = archiveQuery, rows = 40)
                val docs = response.response?.docs ?: emptyList()
                Log.i("MyatoArchive", "Resultados encontrados: ${docs.size}")
                _uiState.value = ArchiveUiState.SearchSuccess(docs)
            } catch (e: Exception) {
                Log.e("MyatoArchive", "Erro na pesquisa: ${e.message}", e)
                _uiState.value = ArchiveUiState.Error("Erro na pesquisa: ${e.localizedMessage ?: "Falha na conexão"}")
            }
        }
    }

    fun getMetadata(identifier: String) {
        viewModelScope.launch {
            _uiState.value = ArchiveUiState.Loading
            val cleanId = identifier.trim()
            try {
                Log.i("MyatoArchiveDiagnostic", "=== CONSULTA DE METADADOS DO INTERNET ARCHIVE ===")
                Log.i("MyatoArchiveDiagnostic", "Identificador Solicitado: $cleanId")

                val response = api.getMetadata(cleanId)
                val meta = response.metadata
                val allFiles = response.files ?: emptyList()

                // Check if item exists: Archive.org returns empty object or null if ID doesn't exist
                val itemExists = (meta != null && !meta.identifier.isNullOrBlank()) || allFiles.isNotEmpty()
                if (!itemExists) {
                    val errorMsg = if (cleanId.matches(Regex("""^[a-zA-Z0-9_-]{11}$"""))) {
                        "O identificador '$cleanId' não foi encontrado no Internet Archive. " +
                        "Este código possui o formato de ID do YouTube. O Myato Player busca áudios exclusivamente no Internet Archive (archive.org). " +
                        "Tente pesquisar pelo nome da música ou artista."
                    } else {
                        "O identificador '$cleanId' não foi encontrado no catálogo do Internet Archive. " +
                        "Verifique se o código ou link está correto."
                    }
                    Log.w("MyatoArchiveDiagnostic", "Item não encontrado: $errorMsg")
                    _uiState.value = ArchiveUiState.Error(errorMsg)
                    return@launch
                }

                val safeMeta = meta ?: ArchiveMetadata(identifier = cleanId, title = cleanId)
                val audioFiles = allFiles
                    .filter { isAudioFile(it) }
                    .sortedWith(compareBy({ getAudioFormatPriority(it) }, { it.name }))

                Log.i("MyatoArchiveDiagnostic", "Título: '${safeMeta.title}', Artista: '${safeMeta.creator}', Álbum: '${safeMeta.album}'")
                Log.i("MyatoArchiveDiagnostic", "Total de arquivos no pacote: ${allFiles.size}")
                Log.i("MyatoArchiveDiagnostic", "Arquivos de áudio identificados: ${audioFiles.size}")
                audioFiles.take(5).forEach { f ->
                    Log.i("MyatoArchiveDiagnostic", "  -> ${f.name} (Formato: ${f.format}, Tamanho: ${f.size} bytes, Duração: ${f.length}s)")
                }

                if (audioFiles.isEmpty()) {
                    _uiState.value = ArchiveUiState.Error(
                        "O item '$cleanId' existe no Internet Archive, mas não possui arquivos de áudio compatíveis para download."
                    )
                } else {
                    _uiState.value = ArchiveUiState.MetadataSuccess(
                        identifier = cleanId,
                        metadata = safeMeta,
                        files = audioFiles
                    )
                }
            } catch (e: Exception) {
                Log.e("MyatoArchiveDiagnostic", "Erro ao buscar metadados de '$cleanId': ${e.message}", e)
                _uiState.value = ArchiveUiState.Error(
                    "Erro ao consultar o Internet Archive para o item '$cleanId': ${e.localizedMessage ?: "Verifique sua conexão de internet."}"
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
                name.endsWith(".gif") ||
                name.endsWith(".cue") ||
                name.endsWith(".log") ||
                name.endsWith(".pk") ||
                name.endsWith(".pk3") ||
                fmt.contains("metadata") ||
                fmt.contains("spectrogram") ||
                fmt.contains("item tile") ||
                fmt.contains("bittorrent") ||
                fmt.contains("text") ||
                fmt.contains("image")
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

    fun getAudioFormatPriority(file: ArchiveFile): Int {
        val name = file.name.lowercase()
        val fmt = file.format?.lowercase() ?: ""
        return when {
            name.endsWith(".mp3") || fmt.contains("vbr mp3") || fmt.contains("mp3") -> 1
            name.endsWith(".m4a") || name.endsWith(".aac") || fmt.contains("aac") || fmt.contains("m4a") -> 2
            name.endsWith(".flac") || fmt.contains("flac") -> 3
            name.endsWith(".ogg") || name.endsWith(".opus") || fmt.contains("ogg") || fmt.contains("opus") -> 4
            name.endsWith(".wav") || fmt.contains("wav") -> 5
            else -> 10
        }
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

    fun isExternalServiceUrl(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("youtube.com") ||
                lower.contains("youtu.be") ||
                lower.contains("spotify.com") ||
                lower.contains("music.apple.com") ||
                lower.contains("deezer.com")
    }

    fun extractArchiveIdentifier(input: String): String? {
        val trimmed = input.trim()
        if (isExternalServiceUrl(trimmed)) return null

        val detailsRegex = Regex("""archive\.org/details/([a-zA-Z0-9_\-\.]+)""")
        val metadataRegex = Regex("""archive\.org/metadata/([a-zA-Z0-9_\-\.]+)""")
        val downloadRegex = Regex("""archive\.org/download/([a-zA-Z0-9_\-\.]+)(?:/.*)?""")

        detailsRegex.find(trimmed)?.groupValues?.get(1)?.let { return it }
        metadataRegex.find(trimmed)?.groupValues?.get(1)?.let { return it }
        downloadRegex.find(trimmed)?.groupValues?.get(1)?.let { return it }

        // If it's a raw identifier (e.g. 20220908_20220908_1340 or soundcloud-232493501), no protocol or slashes
        if (!trimmed.contains("://") && !trimmed.contains("/") && !trimmed.contains(" ") && trimmed.length in 5..80) {
            return trimmed
        }
        return null
    }
}
