package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.Song
import com.example.data.network.ArchiveDoc
import com.example.data.network.ArchiveFile
import com.example.data.network.ArchiveMetadata
import com.example.data.network.JamendoAlbum
import com.example.data.network.JamendoPlaylist
import com.example.data.network.JamendoTrack
import com.example.ui.archive.ArchiveUiState
import com.example.ui.archive.ArchiveViewModel
import com.example.ui.jamendo.JamendoFilterCategory
import com.example.ui.jamendo.JamendoSearchType
import com.example.ui.jamendo.JamendoSearchViewModel
import com.example.ui.jamendo.JamendoUiState
import com.example.ui.music.MusicViewModel
import com.example.ui.music.PlaybackState
import com.example.ui.util.LinkClassifier
import com.example.ui.util.RecognizedLink
import com.example.ui.youtube.YouTubePlaylistInfo
import com.example.ui.youtube.YouTubePlaylistItemState
import com.example.ui.youtube.YouTubeTrackInfo
import com.example.ui.youtube.YouTubeUiState
import com.example.ui.youtube.YouTubeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: MusicViewModel,
    archiveViewModel: ArchiveViewModel,
    youTubeViewModel: YouTubeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val downloads by viewModel.allDownloads.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val archiveState by archiveViewModel.uiState.collectAsState()
    val youTubeState by youTubeViewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val customApiKey by youTubeViewModel.customApiKey.collectAsState()

    val jamendoSearchViewModel: JamendoSearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val jamendoState by jamendoSearchViewModel.uiState.collectAsState()

    var jamendoQuery by remember { mutableStateOf("") }
    var jamendoSearchType by remember { mutableStateOf(JamendoSearchType.TRACKS) }
    var jamendoCategory by remember { mutableStateOf(JamendoFilterCategory.ALL) }

    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var searchIAInput by remember { mutableStateOf("") }
    var showAdvancedInput by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Biblioteca, 1: Ativos, 2: Pesquisar IA
    var inputErrorMessage by remember { mutableStateOf<String?>(null) }

    var pendingTrackForLocalAudio by remember { mutableStateOf<YouTubeTrackInfo?>(null) }

    // Launcher for linking a local audio file to a YouTube track metadata
    val linkAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val track = pendingTrackForLocalAudio
        if (uri != null && track != null) {
            coroutineScope.launch {
                try {
                    val imported = youTubeViewModel.linkLocalAudioToTrack(
                        track = track,
                        audioUri = uri,
                        albumName = "Importados do YouTube",
                        musicDao = viewModel.repository.musicDao
                    )
                    Toast.makeText(context, "Áudio vinculado com sucesso: ${imported.title}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao vincular áudio: ${e.message}", Toast.LENGTH_LONG).show()
                }
                pendingTrackForLocalAudio = null
            }
        }
    }

    // Trigger repair on entering screen to verify existing and old downloads
    LaunchedEffect(Unit) {
        viewModel.repairExistingDownloads()
    }

    val activeDownloads = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.FAILED }
    }

    val failedDownloads = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.FAILED }
    }

    val multiFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioUris(uris)
        }
    }

    val recognizedLink = remember(urlInput) { LinkClassifier.classify(urlInput) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // HEADER WITH REPAIR AND API KEY ACTIONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Downloads & Importações",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showApiKeyDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Chave YouTube API",
                        tint = if (customApiKey != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.repairExistingDownloads() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Verificar e Reparar Arquivos",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // PLAYBACK ERROR BANNER (IF ANY)
        if (playbackState is PlaybackState.Error) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = (playbackState as PlaybackState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 1. TABS: Biblioteca | Ativos | Pesquisar IA | Jamendo
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Biblioteca (${downloadedSongs.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ativos")
                        if (activeDownloads.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("${activeDownloads.size}")
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Pesquisar IA") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Jamendo") }
            )
        }

        // 2. TAB CONTENT
        when (selectedTab) {
            0 -> {
                // Completed downloaded/imported songs in library
                if (downloadedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FileDownloadOff,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhuma música baixada ou importada",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cole links de vídeos/playlists do YouTube, consulte o Internet Archive ou adicione arquivos locais.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(downloadedSongs, key = { it.id }) { song ->
                            SongListItem(
                                song = song,
                                onClick = { viewModel.playSong(song) },
                                onFavoriteClick = { viewModel.toggleFavorite(song) },
                                onMoreClick = { /* More options */ }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Active downloads & Link / YouTube Importer
                Column(modifier = Modifier.fillMaxSize()) {
                    // Unified Link Recognition & Import Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Importar & Baixar Músicas",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Live Link Recognition Badge
                                if (urlInput.isNotBlank()) {
                                    val (badgeText, badgeColor, badgeIcon) = when (recognizedLink) {
                                        is RecognizedLink.YouTubeVideo -> Triple("YouTube Vídeo", Color(0xFFFF5252), Icons.Default.PlayCircleOutline)
                                        is RecognizedLink.YouTubePlaylist -> Triple("YouTube Playlist", Color(0xFFFF7043), Icons.Default.PlaylistPlay)
                                        is RecognizedLink.ArchiveItem -> Triple("Internet Archive", MaterialTheme.colorScheme.primary, Icons.Default.HistoryEdu)
                                        is RecognizedLink.DirectAudio -> Triple("Áudio Direto (${recognizedLink.extension})", Color(0xFF00E676), Icons.Default.AudioFile)
                                        is RecognizedLink.Unknown -> Triple("Texto / Busca", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Search)
                                    }

                                    Surface(
                                        color = badgeColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = badgeIcon,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = badgeText,
                                                color = badgeColor,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = {
                                    urlInput = it
                                    inputErrorMessage = null
                                },
                                placeholder = { Text("Cole link do YouTube, playlist, Archive.org ou .mp3") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (inputErrorMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = inputErrorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            AnimatedVisibility(visible = showAdvancedInput) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = titleInput,
                                        onValueChange = { titleInput = it },
                                        placeholder = { Text("Nome da música (opcional para link direto)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val trimmed = urlInput.trim()
                                        if (trimmed.isNotBlank()) {
                                            when (recognizedLink) {
                                                is RecognizedLink.YouTubeVideo -> {
                                                    youTubeViewModel.fetchVideoDetails(recognizedLink.videoId, trimmed)
                                                    urlInput = ""
                                                    inputErrorMessage = null
                                                }
                                                is RecognizedLink.YouTubePlaylist -> {
                                                    youTubeViewModel.fetchPlaylistDetails(recognizedLink.playlistId, trimmed)
                                                    urlInput = ""
                                                    inputErrorMessage = null
                                                }
                                                is RecognizedLink.ArchiveItem -> {
                                                    selectedTab = 2 // Switch to Archive search/details tab
                                                    archiveViewModel.getMetadata(recognizedLink.identifier)
                                                    urlInput = ""
                                                    inputErrorMessage = null
                                                }
                                                is RecognizedLink.DirectAudio -> {
                                                    viewModel.startDownload(
                                                        url = trimmed,
                                                        title = titleInput.ifBlank { null }
                                                    )
                                                    urlInput = ""
                                                    inputErrorMessage = null
                                                }
                                                is RecognizedLink.Unknown -> {
                                                    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                                                        viewModel.startDownload(url = trimmed, title = titleInput.ifBlank { null })
                                                        urlInput = ""
                                                    } else {
                                                        // Search Internet Archive
                                                        selectedTab = 2
                                                        archiveViewModel.search(trimmed)
                                                        urlInput = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = urlInput.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val buttonLabel = when (recognizedLink) {
                                        is RecognizedLink.YouTubeVideo -> "Consultar Vídeo"
                                        is RecognizedLink.YouTubePlaylist -> "Consultar Playlist"
                                        is RecognizedLink.ArchiveItem -> "Consultar Archive"
                                        is RecognizedLink.DirectAudio -> "Baixar Áudio"
                                        else -> "Identificar & Buscar"
                                    }
                                    Text(buttonLabel)
                                }
                                IconButton(onClick = { showAdvancedInput = !showAdvancedInput }) {
                                    Icon(
                                        if (showAdvancedInput) Icons.Default.ExpandLess else Icons.Default.Tune,
                                        contentDescription = "Personalizar"
                                    )
                                }
                                IconButton(onClick = { multiFileLauncher.launch(arrayOf("audio/*")) }) {
                                    Icon(
                                        Icons.Default.UploadFile,
                                        contentDescription = "Importar arquivos do celular"
                                    )
                                }
                            }
                        }
                    }

                    // Bulk clean failed downloads button if any
                    if (failedDownloads.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${failedDownloads.size} download(s) com erro",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(
                                onClick = {
                                    failedDownloads.forEach { viewModel.cancelDownload(it.id) }
                                }
                            ) {
                                Text("Limpar Erros", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (activeDownloads.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nenhum download em andamento.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            items(activeDownloads, key = { it.id }) { item ->
                                ActiveDownloadCard(
                                    item = item,
                                    onPause = { viewModel.pauseDownload(item.id) },
                                    onResume = { viewModel.resumeDownload(item.id) },
                                    onCancel = { viewModel.cancelDownload(item.id) }
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                // Internet Archive Search & Metadata Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchIAInput,
                        onValueChange = { searchIAInput = it },
                        placeholder = { Text("Pesquisar músicas no Internet Archive...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { archiveViewModel.search(searchIAInput) }) {
                                Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        when (archiveState) {
                            is ArchiveUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            is ArchiveUiState.SearchSuccess -> {
                                val docs = (archiveState as ArchiveUiState.SearchSuccess).docs
                                if (docs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Nenhum arquivo de áudio encontrado para essa pesquisa.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 120.dp)
                                    ) {
                                        items(docs) { doc ->
                                            ArchiveDocItem(
                                                doc = doc,
                                                onClick = { archiveViewModel.getMetadata(doc.identifier) }
                                            )
                                        }
                                    }
                                }
                            }
                            is ArchiveUiState.MetadataSuccess -> {
                                val state = archiveState as ArchiveUiState.MetadataSuccess
                                MetadataDialog(
                                    identifier = state.identifier,
                                    metadata = state.metadata,
                                    files = state.files,
                                    isAuthorized = archiveViewModel.isLicenseAuthorized(state.metadata.licenseUrl),
                                    onDownload = { file ->
                                        val url = archiveViewModel.buildDownloadUrl(state.identifier, file.name)
                                        val bestTitle = file.title?.takeIf { it.isNotBlank() }
                                            ?: state.metadata.title?.takeIf { it.isNotBlank() }
                                            ?: file.name.substringBeforeLast(".")
                                        val bestArtist = file.artist?.takeIf { it.isNotBlank() }
                                            ?: file.creator?.takeIf { it.isNotBlank() }
                                            ?: state.metadata.creator?.takeIf { it.isNotBlank() }
                                            ?: "Internet Archive"
                                        val bestAlbum = file.album?.takeIf { it.isNotBlank() }
                                            ?: state.metadata.album?.takeIf { it.isNotBlank() }
                                            ?: "Internet Archive"

                                        val expectedSize = file.size?.toLongOrNull()
                                        val expectedDuration = file.length?.toDoubleOrNull()?.let { (it * 1000).toLong() }

                                        viewModel.startDownload(
                                            url = url,
                                            title = bestTitle,
                                            artist = bestArtist,
                                            album = bestAlbum,
                                            expectedSize = expectedSize,
                                            expectedMd5 = file.md5,
                                            expectedSha1 = file.sha1,
                                            expectedDuration = expectedDuration
                                        )
                                        selectedTab = 1
                                        archiveViewModel.resetState()
                                    },
                                    onDismiss = { archiveViewModel.resetState() }
                                )
                            }
                            is ArchiveUiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = (archiveState as ArchiveUiState.Error).message,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            else -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HistoryEdu,
                                        contentDescription = null,
                                        modifier = Modifier.size(54.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Acervo Global Internet Archive",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Pesquise por títulos, compositores ou insira o identificador de áudios autorizados.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Jamendo Catalog Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = jamendoQuery,
                        onValueChange = { jamendoQuery = it },
                        placeholder = { Text("Pesquisar músicas, álbuns ou playlists no Jamendo...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType)
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = jamendoSearchType == JamendoSearchType.TRACKS,
                            onClick = {
                                jamendoSearchType = JamendoSearchType.TRACKS
                                jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType)
                            },
                            label = { Text("Músicas") }
                        )
                        FilterChip(
                            selected = jamendoSearchType == JamendoSearchType.ALBUMS,
                            onClick = {
                                jamendoSearchType = JamendoSearchType.ALBUMS
                                jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType)
                            },
                            label = { Text("Álbuns") }
                        )
                        FilterChip(
                            selected = jamendoSearchType == JamendoSearchType.PLAYLISTS,
                            onClick = {
                                jamendoSearchType = JamendoSearchType.PLAYLISTS
                                jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType)
                            },
                            label = { Text("Playlists") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf(
                            JamendoFilterCategory.ALL to "Geral",
                            JamendoFilterCategory.TRACK_NAME to "Nome",
                            JamendoFilterCategory.ARTIST to "Artista",
                            JamendoFilterCategory.ALBUM to "Álbum",
                            JamendoFilterCategory.GENRE to "Gênero"
                        )
                        categories.forEach { (cat, label) ->
                            SuggestionChip(
                                onClick = {
                                    jamendoCategory = cat
                                    jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType)
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        when (jamendoState) {
                            is JamendoUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            is JamendoUiState.TracksLoaded -> {
                                val state = jamendoState as JamendoUiState.TracksLoaded
                                if (state.tracks.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Nenhuma música encontrada no Jamendo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 120.dp)
                                    ) {
                                        items(state.tracks, key = { it.id }) { track ->
                                            JamendoTrackCard(
                                                track = track,
                                                onPlay = {
                                                    val song = Song(
                                                        id = "jam_${track.id}",
                                                        title = track.name,
                                                        artist = track.artist_name,
                                                        album = track.album_name ?: "Jamendo Music",
                                                        duration = (track.duration * 1000).toLong(),
                                                        mediaUri = track.audio,
                                                        coverUri = track.album_image ?: track.image
                                                    )
                                                    viewModel.playSong(song)
                                                },
                                                onDownload = {
                                                    if (!track.audiodownload_allowed) {
                                                        Toast.makeText(context, "Download bloqueado pelo artista (licença restrita).", Toast.LENGTH_LONG).show()
                                                        return@JamendoTrackCard
                                                    }
                                                    val url = track.audiodownload?.takeIf { it.isNotBlank() } ?: track.audio
                                                    viewModel.startDownload(
                                                        url = url,
                                                        title = track.name,
                                                        artist = track.artist_name,
                                                        album = track.album_name ?: "Jamendo",
                                                        expectedDuration = (track.duration * 1000).toLong()
                                                    )
                                                    selectedTab = 1
                                                    Toast.makeText(context, "Download iniciado: ${track.artist_name} - ${track.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                        if (state.hasMore) {
                                            item {
                                                Button(
                                                    onClick = {
                                                        jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType, state.offset + 20)
                                                    },
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                                ) {
                                                    Text("Carregar Mais")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is JamendoUiState.AlbumsLoaded -> {
                                val state = jamendoState as JamendoUiState.AlbumsLoaded
                                if (state.albums.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Nenhum álbum encontrado no Jamendo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 120.dp)
                                    ) {
                                        items(state.albums, key = { it.id }) { album ->
                                            JamendoAlbumCard(
                                                album = album,
                                                onClick = {
                                                    jamendoSearchViewModel.loadAlbumDetails(album.id)
                                                }
                                            )
                                        }
                                        if (state.hasMore) {
                                            item {
                                                Button(
                                                    onClick = {
                                                        jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType, state.offset + 20)
                                                    },
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                                ) {
                                                    Text("Carregar Mais")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is JamendoUiState.PlaylistsLoaded -> {
                                val state = jamendoState as JamendoUiState.PlaylistsLoaded
                                if (state.playlists.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Nenhuma playlist encontrada no Jamendo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 120.dp)
                                    ) {
                                        items(state.playlists, key = { it.id }) { playlist ->
                                            JamendoPlaylistCard(
                                                playlist = playlist,
                                                onClick = {
                                                    jamendoSearchViewModel.loadPlaylistDetails(playlist.id)
                                                }
                                            )
                                        }
                                        if (state.hasMore) {
                                            item {
                                                Button(
                                                    onClick = {
                                                        jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType, state.offset + 20)
                                                    },
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                                ) {
                                                    Text("Carregar Mais")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is JamendoUiState.AlbumDetailLoaded -> {
                                val details = jamendoState as JamendoUiState.AlbumDetailLoaded
                                JamendoAlbumDetailDialog(
                                    album = details.album,
                                    tracks = details.tracks,
                                    onDismiss = { jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType) },
                                    onDownloadTrack = { track ->
                                        if (!track.audiodownload_allowed) {
                                            Toast.makeText(context, "Download bloqueado para esta faixa.", Toast.LENGTH_SHORT).show()
                                            return@JamendoAlbumDetailDialog
                                        }
                                        val url = track.audiodownload?.takeIf { it.isNotBlank() } ?: track.audio
                                        viewModel.startDownload(
                                            url = url,
                                            title = track.name,
                                            artist = track.artist_name,
                                            album = details.album.name,
                                            expectedDuration = (track.duration * 1000).toLong()
                                        )
                                        Toast.makeText(context, "Download iniciado: ${track.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    onDownloadZip = {
                                        if (!details.album.zip.isNullOrBlank()) {
                                            viewModel.startDownload(
                                                url = details.album.zip!!,
                                                title = "${details.album.name} (Álbum ZIP)",
                                                artist = details.album.artist_name,
                                                album = details.album.name
                                            )
                                            selectedTab = 1
                                            Toast.makeText(context, "Download do álbum ZIP iniciado!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "ZIP indisponível para este álbum. Baixe as faixas individualmente.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            }
                            is JamendoUiState.PlaylistDetailLoaded -> {
                                val details = jamendoState as JamendoUiState.PlaylistDetailLoaded
                                JamendoPlaylistDetailDialog(
                                    playlist = details.playlist,
                                    tracks = details.tracks,
                                    onDismiss = { jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType) },
                                    onDownloadTrack = { track ->
                                        if (!track.audiodownload_allowed) {
                                            Toast.makeText(context, "Download bloqueado para esta faixa.", Toast.LENGTH_SHORT).show()
                                            return@JamendoPlaylistDetailDialog
                                        }
                                        val url = track.audiodownload?.takeIf { it.isNotBlank() } ?: track.audio
                                        viewModel.startDownload(
                                            url = url,
                                            title = track.name,
                                            artist = track.artist_name,
                                            album = details.playlist.name,
                                            expectedDuration = (track.duration * 1000).toLong()
                                        )
                                        Toast.makeText(context, "Download iniciado: ${track.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    onDownloadAllAuthorized = {
                                        var count = 0
                                        var skipped = 0
                                        details.tracks.forEach { track ->
                                            if (track.audiodownload_allowed) {
                                                val url = track.audiodownload?.takeIf { it.isNotBlank() } ?: track.audio
                                                viewModel.startDownload(
                                                    url = url,
                                                    title = track.name,
                                                    artist = track.artist_name,
                                                    album = details.playlist.name,
                                                    expectedDuration = (track.duration * 1000).toLong()
                                                )
                                                count++
                                            } else {
                                                skipped++
                                            }
                                        }
                                        Toast.makeText(context, "$count músicas autorizadas baixadas. $skipped ignoradas (sem permissão).", Toast.LENGTH_LONG).show()
                                        selectedTab = 1
                                        jamendoSearchViewModel.search(jamendoQuery, jamendoCategory, jamendoSearchType)
                                    }
                                )
                            }
                            is JamendoUiState.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text((jamendoState as JamendoUiState.Error).message, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = null,
                                            modifier = Modifier.size(54.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Catálogo Oficial Jamendo API v3.0", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Pesquise músicas, álbuns e playlists com licença Creative Commons.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS FOR YOUTUBE SYSTEM ---

    // 1. YouTube Loading Dialog
    if (youTubeState is YouTubeUiState.Loading) {
        val msg = (youTubeState as YouTubeUiState.Loading).message
        AlertDialog(
            onDismissRequest = { /* Loading in progress */ },
            confirmButton = {},
            title = { Text("Consultando YouTube") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }

    // 2. YouTube Video Inspector Dialog
    if (youTubeState is YouTubeUiState.VideoLoaded) {
        val state = youTubeState as YouTubeUiState.VideoLoaded
        YouTubeVideoInspectorDialog(
            track = state.track,
            archiveMatches = state.archiveMatches,
            jamendoMatches = state.jamendoMatches,
            isSearchingArchive = state.isSearchingArchive,
            isSearchingJamendo = state.isSearchingJamendo,
            onDismiss = { youTubeViewModel.resetState() },
            onSearchAuthorized = { youTubeViewModel.searchAuthorizedSources(state.track) },
            onLinkLocalAudio = {
                pendingTrackForLocalAudio = state.track
                linkAudioLauncher.launch(arrayOf("audio/*"))
            },
            onDownloadArchiveMatch = { doc ->
                archiveViewModel.getMetadata(doc.identifier)
                youTubeViewModel.resetState()
                selectedTab = 2
            },
            onDownloadJamendoMatch = { jamTrack ->
                // Prefer audiodownload field for direct download if available
                val downloadUrl = jamTrack.audiodownload?.takeIf { it.isNotBlank() } ?: jamTrack.audio
                viewModel.startDownload(
                    url = downloadUrl,
                    title = jamTrack.name,
                    artist = jamTrack.artist_name,
                    album = jamTrack.album_name,
                    expectedDuration = (jamTrack.duration * 1000).toLong()
                )
                youTubeViewModel.resetState()
                selectedTab = 1
            }
        )
    }

    // 3. YouTube Playlist Inspector Dialog
    if (youTubeState is YouTubeUiState.PlaylistLoaded) {
        val state = youTubeState as YouTubeUiState.PlaylistLoaded
        YouTubePlaylistInspectorDialog(
            playlist = state.playlist,
            items = state.items,
            isSearchingAll = state.isSearchingAll,
            statusMessage = state.statusMessage,
            onToggleItem = { youTubeViewModel.toggleItemSelection(it) },
            onSelectAll = { youTubeViewModel.selectAll(it) },
            onBatchSearchArchive = { youTubeViewModel.batchSearchAuthorizedSources() },
            onCreateLocalPlaylist = {
                coroutineScope.launch {
                    try {
                        val selectedItems = state.items.filter { it.isSelected }
                        
                        // 1. Create the empty playlist first
                        val plId = youTubeViewModel.createLocalPlaylistFromYouTube(
                            musicDao = viewModel.repository.musicDao,
                            playlistInfo = state.playlist,
                            itemsToInclude = emptyList()
                        )
                        
                        // 2. Start downloads for items that have matches
                        var downloadCount = 0
                        selectedItems.forEach { item ->
                            val jamTrack = item.jamendoMatch
                            val arcDoc = item.authorizedMatch
                            
                            if (jamTrack != null) {
                                val downloadUrl = jamTrack.audiodownload?.takeIf { it.isNotBlank() } ?: jamTrack.audio
                                viewModel.startDownload(
                                    url = downloadUrl,
                                    title = jamTrack.name,
                                    artist = jamTrack.artist_name,
                                    album = jamTrack.album_name,
                                    expectedDuration = (jamTrack.duration * 1000).toLong()
                                )
                                downloadCount++
                            } else if (arcDoc != null) {
                                // For Archive, we need metadata to get the actual file URL
                                // This is tricky for batch, so we might just point to the search
                                archiveViewModel.getMetadata(arcDoc.identifier)
                            }
                        }
                        
                        val msg = if (downloadCount > 0) {
                            "Playlist criada e $downloadCount downloads iniciados!"
                        } else {
                            "Playlist '${state.playlist.title}' criada! Vincule arquivos locais para populá-la."
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        youTubeViewModel.resetState()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro ao criar playlist: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onLinkItemAudio = { track ->
                pendingTrackForLocalAudio = track
                linkAudioLauncher.launch(arrayOf("audio/*"))
            },
            onDownloadArchiveMatch = { track, doc ->
                archiveViewModel.getMetadata(doc.identifier)
                youTubeViewModel.resetState()
                selectedTab = 2
            },
            onDownloadJamendoMatch = { track, jamTrack ->
                viewModel.startDownload(
                    url = jamTrack.audio,
                    title = jamTrack.name,
                    artist = jamTrack.artist_name,
                    album = jamTrack.album_name,
                    expectedDuration = (jamTrack.duration * 1000).toLong()
                )
                youTubeViewModel.resetState()
                selectedTab = 1
            },
            onDismiss = { youTubeViewModel.resetState() }
        )
    }

    // 4. YouTube Error Dialog
    if (youTubeState is YouTubeUiState.Error) {
        val err = (youTubeState as YouTubeUiState.Error).message
        AlertDialog(
            onDismissRequest = { youTubeViewModel.resetState() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulta do YouTube")
                }
            },
            text = {
                Column {
                    Text(text = err, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aviso: A YouTube Data API v3 é utilizada para identificação de títulos, canais, miniaturas e playlists. Ela não fornece arquivos MP3 diretamente.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { youTubeViewModel.resetState() }) {
                    Text("Entendi")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    youTubeViewModel.resetState()
                    showApiKeyDialog = true
                }) {
                    Text("Chave API")
                }
            }
        )
    }

    // 5. YouTube API Key Settings Dialog
    if (showApiKeyDialog) {
        YouTubeApiKeyDialog(
            currentKey = customApiKey ?: "",
            onSave = {
                youTubeViewModel.setCustomApiKey(it)
                showApiKeyDialog = false
                Toast.makeText(context, if (it.isNotBlank()) "Chave da YouTube API salva!" else "Chave redefinida para o padrão", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
fun YouTubeVideoInspectorDialog(
    track: YouTubeTrackInfo,
    archiveMatches: List<ArchiveDoc>,
    jamendoMatches: List<JamendoTrack>,
    isSearchingArchive: Boolean,
    isSearchingJamendo: Boolean,
    onDismiss: () -> Unit,
    onSearchAuthorized: () -> Unit,
    onLinkLocalAudio: () -> Unit,
    onDownloadArchiveMatch: (ArchiveDoc) -> Unit,
    onDownloadJamendoMatch: (JamendoTrack) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFFF5252).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.padding(4.dp).size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vídeo do YouTube",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Video Card: Thumbnail + Info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!track.thumbnailUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = track.title,
                                modifier = Modifier
                                    .size(72.dp, 54.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSearchAuthorized,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buscar áudio", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onLinkLocalAudio,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vincular local", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Results Section
                if (isSearchingArchive || isSearchingJamendo) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Buscando fontes autorizadas...", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    val hasMatches = archiveMatches.isNotEmpty() || jamendoMatches.isNotEmpty()
                    
                    if (hasMatches) {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            // Jamendo Results First (Higher Quality)
                            if (jamendoMatches.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Resultados Jamendo (Alta Qualidade)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                items(jamendoMatches) { jamTrack ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { onDownloadJamendoMatch(jamTrack) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!jamTrack.album_image.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = jamTrack.album_image,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = jamTrack.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = jamTrack.artist_name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Baixar",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Archive Results
                            if (archiveMatches.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Resultados Internet Archive",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                items(archiveMatches) { doc ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { onDownloadArchiveMatch(doc) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = doc.title ?: doc.identifier,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = doc.creator ?: "Internet Archive",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Baixar",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (!isSearchingArchive && !isSearchingJamendo) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Nenhuma fonte de áudio autorizada encontrada automaticamente. Tente buscar manualmente ou vincular um arquivo local.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun YouTubePlaylistInspectorDialog(
    playlist: YouTubePlaylistInfo,
    items: List<YouTubePlaylistItemState>,
    isSearchingAll: Boolean,
    statusMessage: String?,
    onToggleItem: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onBatchSearchArchive: () -> Unit,
    onCreateLocalPlaylist: () -> Unit,
    onLinkItemAudio: (YouTubeTrackInfo) -> Unit,
    onDownloadArchiveMatch: (YouTubeTrackInfo, ArchiveDoc) -> Unit,
    onDownloadJamendoMatch: (YouTubeTrackInfo, JamendoTrack) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedCount = items.count { it.isSelected }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFFF7043).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = Color(0xFFFF7043),
                        modifier = Modifier.padding(4.dp).size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.channelTitle} • ${items.size} vídeos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedCount == items.size && items.isNotEmpty(),
                            onCheckedChange = { onSelectAll(it) }
                        )
                        Text(
                            text = "Todas ($selectedCount/${items.size})",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Row {
                        IconButton(onClick = onBatchSearchArchive, enabled = !isSearchingAll) {
                            if (isSearchingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Default.ManageSearch,
                                    contentDescription = "Buscar fontes autorizadas",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onCreateLocalPlaylist) {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = "Salvar Playlist no Myato",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Track List
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                    items(items, key = { it.track.videoId }) { entry ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (entry.isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = entry.isSelected,
                                    onCheckedChange = { onToggleItem(entry.track.videoId) }
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.track.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val statusColor = when {
                                            entry.jamendoMatch != null -> MaterialTheme.colorScheme.primary
                                            entry.authorizedMatch != null -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        val statusIcon = when {
                                            entry.jamendoMatch != null -> Icons.Default.CheckCircle
                                            entry.authorizedMatch != null -> Icons.Default.LibraryMusic
                                            else -> null
                                        }

                                        if (statusIcon != null) {
                                            Icon(
                                                imageVector = statusIcon,
                                                contentDescription = null,
                                                tint = statusColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = entry.statusText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                when {
                                    entry.jamendoMatch != null -> {
                                        IconButton(onClick = { onDownloadJamendoMatch(entry.track, entry.jamendoMatch) }) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = "Baixar Jamendo",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    entry.authorizedMatch != null -> {
                                        IconButton(onClick = { onDownloadArchiveMatch(entry.track, entry.authorizedMatch) }) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = "Baixar Archive",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        IconButton(onClick = { onLinkItemAudio(entry.track) }) {
                                            Icon(
                                                Icons.Default.AttachFile,
                                                contentDescription = "Vincular local",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCreateLocalPlaylist) {
                Text("Criar Playlist no Myato")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun YouTubeApiKeyDialog(
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar YouTube Data API v3") },
        text = {
            Column {
                Text(
                    text = "A YouTube Data API v3 é utilizada para consultar metadados de vídeos e playlists completas.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Vídeos individuais funcionam sem chave (via oEmbed público).\n• Playlists completas requerem uma Chave de API gratuita do Google Cloud Console.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Chave da API (Google Cloud)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(keyInput) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ArchiveDocItem(doc: ArchiveDoc, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AudioFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title?.takeIf { it.isNotBlank() } ?: doc.identifier,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = doc.creator?.takeIf { it.isNotBlank() } ?: "Criador não especificado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MetadataDialog(
    identifier: String,
    metadata: ArchiveMetadata,
    files: List<ArchiveFile>,
    isAuthorized: Boolean,
    onDownload: (ArchiveFile) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                metadata.title?.takeIf { it.isNotBlank() } ?: "Item: $identifier",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                Text(
                    text = "Artista: ${metadata.creator?.takeIf { it.isNotBlank() } ?: "Desconhecido"}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!metadata.album.isNullOrBlank()) {
                    Text(
                        text = "Álbum: ${metadata.album}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "Licença: ${if (isAuthorized) "Autorizada (Domínio Público / Aberta)" else "Restrita ou Desconhecida"}",
                    color = if (isAuthorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (files.isEmpty()) {
                    Text(
                        text = "Nenhum arquivo de áudio compatível encontrado neste item.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Faixas disponíveis (${files.size}):", style = MaterialTheme.typography.labelLarge)
                        if (isAuthorized) {
                            Row {
                                if (files.isNotEmpty()) {
                                    TextButton(onClick = { onDownload(files.first()) }) {
                                        Text("Baixar Principal", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                if (files.size > 1) {
                                    TextButton(onClick = { files.forEach { onDownload(it) } }) {
                                        Text("Baixar Tudo", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        itemsIndexed(files) { index, file ->
                            val isRecommended = index == 0
                            val title = file.title?.takeIf { it.isNotBlank() } ?: file.name
                            val sizeMb = file.size?.toLongOrNull()?.let { "%.1f MB".format(it / 1024.0 / 1024.0) } ?: ""
                            val lengthSec = file.length?.toDoubleOrNull()?.let {
                                val mins = (it / 60).toInt()
                                val secs = (it % 60).toInt()
                                "%d:%02d".format(mins, secs)
                            } ?: ""
                            val details = listOfNotNull(file.format, sizeMb.ifBlank { null }, lengthSec.ifBlank { null }).joinToString(" • ")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isRecommended) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Recomendado",
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        details,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDownload(file) },
                                    enabled = isAuthorized
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Baixar",
                                        tint = if (isAuthorized) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
fun ActiveDownloadCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val total = if (item.totalBytes > 0) item.totalBytes else 1L
    val progress = (item.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.artist} • ${item.speedText} ($percentage%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.status == DownloadStatus.DOWNLOADING) {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.FAILED) {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Retomar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(onClick = onCancel) {
                        Icon(
                            if (item.status == DownloadStatus.FAILED) Icons.Default.Delete else Icons.Default.Close,
                            contentDescription = "Remover",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (item.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            if (!item.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun JamendoTrackCard(
    track: JamendoTrack,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val durationMin = track.duration / 60
    val durationSec = track.duration % 60
    val durationStr = "%d:%02d".format(durationMin, durationSec)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cover = track.album_image ?: track.image
            if (!cover.isNullOrEmpty()) {
                AsyncImage(
                    model = cover,
                    contentDescription = track.name,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist_name} • $durationStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (track.audiodownload_allowed) "Licença Autorizada (CC)" else "Download Restrito",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (track.audiodownload_allowed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    fontSize = 10.sp
                )
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Reproduzir", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDownload, enabled = track.audiodownload_allowed) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Baixar",
                    tint = if (track.audiodownload_allowed) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

@Composable
fun JamendoAlbumCard(
    album: JamendoAlbum,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!album.image.isNullOrEmpty()) {
                AsyncImage(
                    model = album.image,
                    contentDescription = album.name,
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist_name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (album.zip_allowed) {
                    Text("ZIP Disponível", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun JamendoPlaylistCard(
    playlist: JamendoPlaylist,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!playlist.image.isNullOrEmpty()) {
                AsyncImage(
                    model = playlist.image,
                    contentDescription = playlist.name,
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Criado por: ${playlist.user_name ?: "Jamendo"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun JamendoAlbumDetailDialog(
    album: JamendoAlbum,
    tracks: List<JamendoTrack>,
    onDismiss: () -> Unit,
    onDownloadTrack: (JamendoTrack) -> Unit,
    onDownloadZip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = album.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = album.artist_name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (album.zip_allowed && !album.zip.isNullOrBlank()) {
                    Button(
                        onClick = onDownloadZip,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar Álbum Completo (ZIP)")
                    }
                }
                Text("Faixas (${tracks.size}):", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(tracks, key = { it.id }) { track ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (track.audiodownload_allowed) "Autorizado" else "Restrito", style = MaterialTheme.typography.labelSmall, color = if (track.audiodownload_allowed) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 10.sp)
                            }
                            IconButton(onClick = { onDownloadTrack(track) }, enabled = track.audiodownload_allowed) {
                                Icon(Icons.Default.Download, contentDescription = "Baixar", tint = if (track.audiodownload_allowed) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
fun JamendoPlaylistDetailDialog(
    playlist: JamendoPlaylist,
    tracks: List<JamendoTrack>,
    onDismiss: () -> Unit,
    onDownloadTrack: (JamendoTrack) -> Unit,
    onDownloadAllAuthorized: () -> Unit
) {
    val authorizedCount = tracks.count { it.audiodownload_allowed }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = playlist.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = "Playlist • $authorizedCount/${tracks.size} faixas autorizadas para download", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (authorizedCount > 0) {
                    Button(
                        onClick = onDownloadAllAuthorized,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar Todas Autorizadas ($authorizedCount)")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(tracks, key = { it.id }) { track ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${track.artist_name} • ${if (track.audiodownload_allowed) "Autorizado" else "Restrito"}", style = MaterialTheme.typography.labelSmall, color = if (track.audiodownload_allowed) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 10.sp)
                            }
                            IconButton(onClick = { onDownloadTrack(track) }, enabled = track.audiodownload_allowed) {
                                Icon(Icons.Default.Download, contentDescription = "Baixar", tint = if (track.audiodownload_allowed) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}
