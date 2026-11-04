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
import com.example.ui.music.MusicViewModel
import com.example.data.network.ArchiveDoc
import com.example.data.network.ArchiveFile
import com.example.data.network.ArchiveMetadata
import com.example.ui.archive.ArchiveUiState
import com.example.ui.archive.ArchiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: MusicViewModel, archiveViewModel: ArchiveViewModel) {
    val downloads by viewModel.allDownloads.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val archiveState by archiveViewModel.uiState.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var searchIAInput by remember { mutableStateOf("") }
    var showAdvancedInput by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Concluídos, 1: Ativos, 2: Pesquisar IA

    val activeDownloads = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.FAILED }
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
        // HEADER
        HeaderSection("Sistema de Downloads")

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
                // Completed downloaded and imported songs
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
                                text = "Use a aba 'Pesquisar IA' ou cole um link direto.",
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
                // Active downloads + Direct Link Input
                Column(modifier = Modifier.fillMaxSize()) {
                    // URL Input Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Baixar por Link Direto",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                placeholder = { Text("Cole o link direto (MP3, FLAC, WAV...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            AnimatedVisibility(visible = showAdvancedInput) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = titleInput,
                                    onValueChange = { titleInput = it },
                                    placeholder = { Text("Nome da música (opcional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val trimmed = urlInput.trim()
                                        if (trimmed.isNotBlank()) {
                                            viewModel.startDownload(trimmed, titleInput.ifBlank { null })
                                            urlInput = ""
                                            titleInput = ""
                                        }
                                    },
                                    enabled = urlInput.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Baixar Link")
                                }
                                IconButton(onClick = { showAdvancedInput = !showAdvancedInput }) {
                                    Icon(if (showAdvancedInput) Icons.Default.ExpandLess else Icons.Default.Tune, contentDescription = null)
                                }
                                IconButton(onClick = { launcher.launch(arrayOf("audio/*")) }) {
                                    Icon(Icons.Default.UploadFile, contentDescription = "Importar local")
                                }
                            }
                        }
                    }

                    if (activeDownloads.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Nenhum download ativo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp)
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
                // Internet Archive Search
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    OutlinedTextField(
                        value = searchIAInput,
                        onValueChange = { searchIAInput = it },
                        placeholder = { Text("Buscar no Internet Archive...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { archiveViewModel.search(searchIAInput) }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        when (archiveState) {
                            is ArchiveUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is ArchiveUiState.SearchSuccess -> {
                                val docs = (archiveState as ArchiveUiState.SearchSuccess).docs
                                if (docs.isEmpty()) {
                                    Text("Nenhum resultado encontrado.", modifier = Modifier.align(Alignment.Center))
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    metadata = state.metadata,
                                    files = state.files,
                                    isAuthorized = archiveViewModel.isLicenseAuthorized(state.metadata.licenseUrl),
                                    onDownload = { file ->
                                        val url = "https://archive.org/download/${state.metadata.identifier}/${file.name}"
                                        viewModel.startDownload(
                                            url = url,
                                            title = state.metadata.title ?: file.name,
                                            artist = state.metadata.creator ?: "Internet Archive"
                                        )
                                        selectedTab = 1
                                        archiveViewModel.resetState()
                                    },
                                    onDismiss = { archiveViewModel.resetState() }
                                )
                            }
                            is ArchiveUiState.Error -> {
                                Text("Erro: ${(archiveState as ArchiveUiState.Error).message}", color = MaterialTheme.colorScheme.error)
                            }
                            else -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.HistoryEdu, contentDescription = null, size = 48.dp, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Acervo Público Global", style = MaterialTheme.typography.titleMedium)
                                    Text("Pesquise por áudios de domínio público e licenciados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = doc.title ?: "Sem título", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = doc.creator ?: "Autor desconhecido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MetadataDialog(
    metadata: ArchiveMetadata,
    files: List<ArchiveFile>,
    isAuthorized: Boolean,
    onDownload: (ArchiveFile) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(metadata.title ?: "Detalhes do Item") },
        text = {
            Column {
                Text("Autor: ${metadata.creator ?: "Desconhecido"}", style = MaterialTheme.typography.bodySmall)
                Text("Licença: ${if (isAuthorized) "Autorizada (Domínio Público / CC)" else "Restrita ou Desconhecida"}", 
                    color = if (isAuthorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (files.isEmpty()) {
                    Text("Nenhum arquivo de áudio compatível encontrado.")
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Arquivos disponíveis:", style = MaterialTheme.typography.labelLarge)
                        if (files.size > 1 && isAuthorized) {
                            TextButton(onClick = { files.forEach { onDownload(it) } }) {
                                Text("Baixar Tudo", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(files) { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${file.format} • ${file.size?.toLongOrNull()?.let { it / 1024 / 1024 } ?: "?"} MB", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(
                                    onClick = { onDownload(file) },
                                    enabled = isAuthorized
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Baixar", tint = if (isAuthorized) MaterialTheme.colorScheme.primary else Color.Gray)
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
                        text = "${item.speedText} • $percentage%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
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
                        Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.error)
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
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            if (item.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    androidx.compose.material3.Icon(imageVector, contentDescription, modifier = Modifier.size(size), tint = tint)
}
