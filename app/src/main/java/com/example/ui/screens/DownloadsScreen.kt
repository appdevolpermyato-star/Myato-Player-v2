package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.Song
import com.example.ui.music.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: MusicViewModel) {
    val downloads by viewModel.allDownloads.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var showAdvancedInput by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Concluídos, 1: Em Andamento

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

    // Curated royalty-free sample audio tracks for quick 1-tap download testing
    val sampleTracks = listOf(
        Triple("SoundHelix Synth 1", "Myato Cyberwave", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
        Triple("SoundHelix Ambient 2", "Deep Focus Sound", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
        Triple("SoundHelix Electro 3", "Neon Dreams", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
        Triple("SoundHelix Bass 4", "Future Bass Club", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // HEADER
        HeaderSection("Downloads & Importados")

        // 1. MUKA-STYLE URL DOWNLOADER INPUT CARD
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Baixar por Link Direto",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    TextButton(
                        onClick = { showAdvancedInput = !showAdvancedInput },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (showAdvancedInput) "Ocultar" else "Personalizar",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("Cole o link direto do áudio (MP3, FLAC, WAV, AAC...)") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
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
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmed = urlInput.trim()
                            if (trimmed.isNotBlank()) {
                                viewModel.startDownload(
                                    url = trimmed,
                                    title = titleInput.trim().ifEmpty { null }
                                )
                                urlInput = ""
                                titleInput = ""
                                selectedTab = 1 // Switch to in-progress tab
                            }
                        },
                        enabled = urlInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Iniciar Download")
                    }

                    OutlinedButton(
                        onClick = { launcher.launch(arrayOf("audio/*")) }
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importar")
                    }
                }
            }
        }

        // 2. QUICK 1-TAP DOWNLOAD PRESETS
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = "SUGESTÕES DE ÁUDIO PARA TESTAR",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sampleTracks) { (title, artist, url) ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.startDownload(url = url, title = title, artist = artist)
                                selectedTab = 1
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadForOffline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. TABS: CONCLUÍDOS / EM ANDAMENTO
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Concluídos (${downloadedSongs.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Em Andamento")
                        if (activeDownloads.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("${activeDownloads.size}")
                            }
                        }
                    }
                }
            )
        }

        // 4. TAB CONTENT
        if (selectedTab == 1) {
            // Active or paused downloads
            if (activeDownloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum download em andamento",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cole uma URL de áudio acima para iniciar um download.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
        } else {
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
                            text = "Baixe por link direto ou selecione arquivos de áudio do dispositivo.",
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
    }
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
