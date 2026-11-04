package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.Song
import com.example.data.network.ArchiveDoc
import com.example.data.network.ArchiveFile
import com.example.data.network.ArchiveMetadata
import com.example.ui.archive.ArchiveUiState
import com.example.ui.archive.ArchiveViewModel
import com.example.ui.music.MusicViewModel
import com.example.ui.music.PlaybackState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: MusicViewModel, archiveViewModel: ArchiveViewModel) {
    val downloads by viewModel.allDownloads.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val archiveState by archiveViewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var searchIAInput by remember { mutableStateOf("") }
    var showAdvancedInput by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Biblioteca, 1: Ativos, 2: Pesquisar IA

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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioUris(uris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // HEADER WITH REPAIR ACTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Downloads & Internet Archive",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
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

        // 1. TABS
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
        }

        // 2. TAB CONTENT
        when (selectedTab) {
            0 -> {
                // Completed downloaded songs in library
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
                                text = "Nenhuma música baixada",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pesquise no Internet Archive ou insira um link direto para baixar áudios autorizados.",
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
                // Active downloads & Direct Link Input
                Column(modifier = Modifier.fillMaxSize()) {
                    // Direct Link Input Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Baixar por Link Direto ou Item",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                placeholder = { Text("Cole o link direto (.mp3, .flac) ou ID do Internet Archive") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            AnimatedVisibility(visible = showAdvancedInput) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = titleInput,
                                        onValueChange = { titleInput = it },
                                        placeholder = { Text("Nome da música (opcional)") },
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
                                            // Check if user entered an Internet Archive link or identifier
                                            val archiveId = archiveViewModel.extractArchiveIdentifier(trimmed)
                                            val isDirectAudioUrl = trimmed.endsWith(".mp3", ignoreCase = true) ||
                                                    trimmed.endsWith(".flac", ignoreCase = true) ||
                                                    trimmed.endsWith(".wav", ignoreCase = true) ||
                                                    trimmed.endsWith(".ogg", ignoreCase = true) ||
                                                    trimmed.endsWith(".m4a", ignoreCase = true)

                                            if (archiveId != null && !isDirectAudioUrl) {
                                                // It is an Archive item or details URL! Resolve via metadata:
                                                selectedTab = 2 // Switch to Archive search/details tab
                                                archiveViewModel.getMetadata(archiveId)
                                                urlInput = ""
                                                titleInput = ""
                                            } else {
                                                viewModel.startDownload(
                                                    url = trimmed,
                                                    title = titleInput.ifBlank { null }
                                                )
                                                urlInput = ""
                                                titleInput = ""
                                            }
                                        }
                                    },
                                    enabled = urlInput.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Baixar")
                                }
                                IconButton(onClick = { showAdvancedInput = !showAdvancedInput }) {
                                    Icon(
                                        if (showAdvancedInput) Icons.Default.ExpandLess else Icons.Default.Tune,
                                        contentDescription = "Personalizar"
                                    )
                                }
                                IconButton(onClick = { launcher.launch(arrayOf("audio/*")) }) {
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
                                "Nenhum download ativo no momento.",
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
        }
    }
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
                        Text("Arquivos de áudio disponíveis:", style = MaterialTheme.typography.labelLarge)
                        if (files.size > 1 && isAuthorized) {
                            TextButton(onClick = { files.forEach { onDownload(it) } }) {
                                Text("Baixar Tudo", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(files) { file ->
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
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
