package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.ai.AiUiState
import com.example.ui.ai.GeminiViewModel
import com.example.ui.music.MusicViewModel
import com.example.ui.music.SortOption
import com.example.ui.util.PermissionHelper

enum class LibraryFilter {
    ALL,
    FAVORITES,
    ALBUMS,
    ARTISTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    aiViewModel: GeminiViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onRequestPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val allSongs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favoriteSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanMsg by viewModel.scanMessage.collectAsState()
    val aiState by aiViewModel.uiState.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }
    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }
    var songsForBatchPlaylist by remember { mutableStateOf<List<Song>?>(null) }

    var currentFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<String>() }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val hasAudioPermission = remember(allSongs) {
        PermissionHelper.hasAudioPermission(context)
    }

    // Handle back button when in selection mode
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedSongIds.clear()
    }

    // Filter by tab
    val baseList = remember(allSongs, favorites, currentFilter) {
        when (currentFilter) {
            LibraryFilter.ALL -> allSongs
            LibraryFilter.FAVORITES -> favorites
            LibraryFilter.ALBUMS -> allSongs // grouped below
            LibraryFilter.ARTISTS -> allSongs // grouped below
        }
    }

    // Filter and Sort
    val filteredSongs = remember(baseList, searchQuery, sortOption) {
        val query = searchQuery.trim().lowercase()
        val list = if (query.isEmpty()) {
            baseList
        } else {
            baseList.filter {
                it.title.lowercase().contains(query) ||
                it.artist.lowercase().contains(query) ||
                it.album.lowercase().contains(query)
            }
        }
        when (sortOption) {
            SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> list.sortedBy { it.artist.lowercase() }
            SortOption.DURATION -> list.sortedByDescending { it.duration }
            SortOption.DATE -> list.sortedByDescending { it.dateAdded }
        }
    }

    Scaffold(
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Queue Selected
                        IconButton(
                            onClick = {
                                val selectedSongs = allSongs.filter { selectedSongIds.contains(it.id) }
                                if (selectedSongs.isNotEmpty()) {
                                    viewModel.playQueue(selectedSongs, 0)
                                }
                                isSelectionMode = false
                                selectedSongIds.clear()
                            },
                            enabled = selectedSongIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Tocar Selecionadas", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Add to Playlist
                        IconButton(
                            onClick = {
                                val selectedSongs = allSongs.filter { selectedSongIds.contains(it.id) }
                                if (selectedSongs.isNotEmpty()) {
                                    songsForBatchPlaylist = selectedSongs
                                }
                            },
                            enabled = selectedSongIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = "Adicionar à Playlist")
                        }

                        // Favorite all
                        IconButton(
                            onClick = {
                                val selectedSongs = allSongs.filter { selectedSongIds.contains(it.id) }
                                selectedSongs.forEach { song ->
                                    if (!song.isFavorite) viewModel.toggleFavorite(song)
                                }
                                isSelectionMode = false
                                selectedSongIds.clear()
                            },
                            enabled = selectedSongIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Favoritar Selecionadas", tint = Color(0xFFFF2D55))
                        }

                        // Share
                        IconButton(
                            onClick = {
                                val selectedSongs = allSongs.filter { selectedSongIds.contains(it.id) }
                                if (selectedSongs.isNotEmpty()) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Ouvindo no Myato Player:\n" + selectedSongs.joinToString("\n") { "• ${it.title} - ${it.artist}" }
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Músicas"))
                                }
                                isSelectionMode = false
                                selectedSongIds.clear()
                            },
                            enabled = selectedSongIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar")
                        }

                        // Delete
                        IconButton(
                            onClick = { showBatchDeleteConfirm = true },
                            enabled = selectedSongIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir Selecionadas", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // HEADER (NORMAL OR SELECTION MODE)
            if (isSelectionMode) {
                SelectionHeader(
                    selectedCount = selectedSongIds.size,
                    totalCount = filteredSongs.size,
                    onSelectAll = {
                        selectedSongIds.clear()
                        selectedSongIds.addAll(filteredSongs.map { it.id })
                    },
                    onCancel = {
                        isSelectionMode = false
                        selectedSongIds.clear()
                    }
                )
            } else {
                HeaderWithSearch(
                    title = "Biblioteca",
                    songCount = filteredSongs.size,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.searchQuery.value = it },
                    onSortClick = { showSortMenu = true },
                    onRefreshClick = { viewModel.scanLocalFiles() },
                    isScanning = isScanning,
                    onSelectionModeClick = {
                        isSelectionMode = true
                    }
                )
            }

            // FILTER CHIPS ROW
            if (!isSelectionMode) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = currentFilter == LibraryFilter.ALL,
                            onClick = { currentFilter = LibraryFilter.ALL },
                            label = { Text("Todas (${allSongs.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = currentFilter == LibraryFilter.FAVORITES,
                            onClick = { currentFilter = LibraryFilter.FAVORITES },
                            label = { Text("Curtidas (${favorites.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = currentFilter == LibraryFilter.ALBUMS,
                            onClick = { currentFilter = LibraryFilter.ALBUMS },
                            label = { Text("Álbuns") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = currentFilter == LibraryFilter.ARTISTS,
                            onClick = { currentFilter = LibraryFilter.ARTISTS },
                            label = { Text("Artistas") }
                        )
                    }
                }
            }

            // Permission Banner if permission missing
            if (!hasAudioPermission) {
                PermissionBanner(onRequestPermissions)
            }

            // Scan Status Message
            AnimatedVisibility(visible = scanMsg != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = scanMsg.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // AI Smart Picks
            if (!isSelectionMode && searchQuery.isEmpty()) {
                AiSuggestionsRow(
                    state = aiState,
                    onGetSuggestions = { aiViewModel.getSuggestions(favorites) }
                )
            }

            // Main Songs List with Pull-To-Refresh
            PullToRefreshBox(
                isRefreshing = isScanning,
                onRefresh = { viewModel.scanLocalFiles() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredSongs.isEmpty()) {
                    EmptyLibraryState(
                        isSearching = searchQuery.isNotEmpty(),
                        onScanClick = { viewModel.scanLocalFiles() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            val isSelected = selectedSongIds.contains(song.id)
                            SelectableSongItem(
                                song = song,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedSongIds.remove(song.id)
                                        else selectedSongIds.add(song.id)
                                    } else {
                                        viewModel.playSong(song)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedSongIds.add(song.id)
                                    }
                                },
                                onFavoriteClick = { viewModel.toggleFavorite(song) },
                                onMoreClick = { selectedSongForMenu = song }
                            )
                        }
                    }
                }
            }
        }
    }

    // Sort Dropdown Dialog
    if (showSortMenu) {
        AlertDialog(
            onDismissRequest = { showSortMenu = false },
            title = { Text("Ordenar Músicas") },
            text = {
                Column {
                    SortOptionItem("Nome da Música", sortOption == SortOption.TITLE) {
                        viewModel.sortOption.value = SortOption.TITLE
                        showSortMenu = false
                    }
                    SortOptionItem("Artista", sortOption == SortOption.ARTIST) {
                        viewModel.sortOption.value = SortOption.ARTIST
                        showSortMenu = false
                    }
                    SortOptionItem("Duração", sortOption == SortOption.DURATION) {
                        viewModel.sortOption.value = SortOption.DURATION
                        showSortMenu = false
                    }
                    SortOptionItem("Adicionadas Recentemente", sortOption == SortOption.DATE) {
                        viewModel.sortOption.value = SortOption.DATE
                        showSortMenu = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortMenu = false }) { Text("Fechar") }
            }
        )
    }

    // Batch Delete Confirmation
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Excluir Músicas") },
            text = {
                Text("Deseja realmente remover as ${selectedSongIds.size} músicas selecionadas da biblioteca?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = allSongs.filter { selectedSongIds.contains(it.id) }
                        viewModel.deleteSongs(toDelete)
                        isSelectionMode = false
                        selectedSongIds.clear()
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    // Batch Add to Playlist Dialog
    songsForBatchPlaylist?.let { songs ->
        val playlists by viewModel.playlists.collectAsState()
        AlertDialog(
            onDismissRequest = { songsForBatchPlaylist = null },
            title = { Text("Adicionar à Playlist (${songs.size} faixas)") },
            text = {
                if (playlists.isEmpty()) {
                    Text("Nenhuma playlist encontrada. Crie uma na aba Playlists primeiro.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(playlists) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongsToPlaylist(pl.id, songs.map { it.id })
                                        songsForBatchPlaylist = null
                                        isSelectionMode = false
                                        selectedSongIds.clear()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(pl.name, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { songsForBatchPlaylist = null }) { Text("Fechar") }
            }
        )
    }

    // Song context menu
    selectedSongForMenu?.let { song ->
        SongActionDialog(
            song = song,
            onDismiss = { selectedSongForMenu = null },
            onPlay = {
                viewModel.playSong(song)
                selectedSongForMenu = null
            },
            onAddToPlaylist = {
                songForPlaylistDialog = song
                selectedSongForMenu = null
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(song)
                selectedSongForMenu = null
            },
            onDelete = {
                viewModel.deleteSong(song)
                selectedSongForMenu = null
            }
        )
    }

    // Add to playlist modal
    songForPlaylistDialog?.let { song ->
        AddToPlaylistDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { songForPlaylistDialog = null }
        )
    }
}

@Composable
fun SelectionHeader(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$selectedCount selecionadas",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            TextButton(onClick = onSelectAll) {
                Text(if (selectedCount == totalCount) "Desmarcar" else "Todas ($totalCount)")
            }
        }
    }
}

@Composable
fun SelectableSongItem(
    song: Song,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (!song.coverUri.isNullOrEmpty()) {
                AsyncImage(
                    model = song.coverUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isSelectionMode) {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favoritar",
                    tint = if (song.isFavorite) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Mais opções",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit = {}
) {
    SelectableSongItem(
        song = song,
        isSelectionMode = false,
        isSelected = false,
        onClick = onClick,
        onLongClick = {},
        onFavoriteClick = onFavoriteClick,
        onMoreClick = onMoreClick
    )
}

@Composable
fun HeaderWithSearch(
    title: String,
    songCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSortClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isScanning: Boolean,
    onSelectionModeClick: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent)
                )
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$songCount faixas disponíveis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Pesquisar",
                        tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSelectionModeClick) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = "Selecionar Múltiplas",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSortClick) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Ordenar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefreshClick, enabled = !isScanning) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar Biblioteca",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Pesquisar música, artista ou álbum...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun PermissionBanner(onRequest: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permissão de Áudio Necessária",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Para encontrar e tocar seus arquivos de música locais (MP3, FLAC, etc.), o app precisa de permissão de acesso aos arquivos de áudio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Permitir Acesso")
            }
        }
    }
}

@Composable
fun EmptyLibraryState(isSearching: Boolean, onScanClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearching) "Nenhuma música corresponde à pesquisa" else "Nenhuma música encontrada",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Escaneie seus arquivos locais ou baixe músicas na aba Downloads.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isSearching) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onScanClick) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear Dispositivo")
                }
            }
        }
    }
}

@Composable
fun SortOptionItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SongActionDialog(
    song: Song,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                ActionItem(Icons.Default.PlayArrow, "Tocar Agora", onPlay)
                ActionItem(Icons.Default.PlaylistAdd, "Adicionar à Playlist", onAddToPlaylist)
                ActionItem(
                    if (song.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                    if (song.isFavorite) "Remover dos Favoritos" else "Adicionar aos Favoritos",
                    onToggleFavorite
                )
                ActionItem(Icons.Default.Delete, "Remover da Biblioteca", onDelete)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
fun ActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
fun AiSuggestionsRow(state: AiUiState, onGetSuggestions: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Smart Picks (IA)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(
                onClick = onGetSuggestions,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Sugerir", style = MaterialTheme.typography.bodySmall)
            }
        }

        when (state) {
            is AiUiState.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analisando favoritos localmente...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is AiUiState.Success -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(state.suggestions) { suggestion ->
                        SuggestionChip(suggestion)
                    }
                }
            }
            is AiUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            else -> {
                Text(
                    text = "Descubra novas recomendações baseadas nas suas músicas favoritas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SuggestionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
