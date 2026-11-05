package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.Song
import com.example.ui.music.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    viewModel: MusicViewModel,
    onSongClick: (Song) -> Unit
) {
    val songs by viewModel.allSongs.collectAsState()
    val folders = remember(songs) {
        songs.groupBy { it.folder ?: "Raiz" }
    }
    
    var currentFolder by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFolder ?: "Navegar por Pastas") },
                navigationIcon = {
                    if (currentFolder != null) {
                        IconButton(onClick = { currentFolder = null }) {
                            Icon(Icons.Default.Folder, contentDescription = "Voltar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (currentFolder == null) {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(folders.keys.toList()) { folderName ->
                    ListItem(
                        headlineContent = { Text(folderName, color = Color.White) },
                        supportingContent = { Text("${folders[folderName]?.size ?: 0} músicas", color = Color.Gray) },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { currentFolder = folderName },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        } else {
            val songsInFolder = folders[currentFolder] ?: emptyList()
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(songsInFolder) { song ->
                    ListItem(
                        headlineContent = { Text(song.title, color = Color.White) },
                        supportingContent = { Text(song.artist, color = Color.Gray) },
                        leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.clickable { onSongClick(song) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}
