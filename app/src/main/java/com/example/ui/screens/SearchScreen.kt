package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.music.MusicViewModel
import com.example.ui.search.MultiPlatformSearchViewModel
import com.example.ui.search.MultiPlatformUiState
import com.example.ui.search.MusicPlatform
import com.example.ui.search.SearchSourceFilter
import com.example.ui.search.UnifiedTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    multiSearchViewModel: MultiPlatformSearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val multiSearchState by multiSearchViewModel.uiState.collectAsState()
    var multiQuery by remember { mutableStateOf("") }
    var searchSourceFilter by remember { mutableStateOf(SearchSourceFilter.ALL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Explorar Músicas",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = multiQuery,
            onValueChange = { multiQuery = it },
            placeholder = { Text("Pesquisar no Jamendo...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = {
                    multiSearchViewModel.search(multiQuery, searchSourceFilter)
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
                selected = searchSourceFilter == SearchSourceFilter.ALL,
                onClick = {
                    searchSourceFilter = SearchSourceFilter.ALL
                    if (multiQuery.isNotBlank()) multiSearchViewModel.search(multiQuery, searchSourceFilter)
                },
                label = { Text("Tudo") }
            )
            FilterChip(
                selected = searchSourceFilter == SearchSourceFilter.JAMENDO,
                onClick = {
                    searchSourceFilter = SearchSourceFilter.JAMENDO
                    if (multiQuery.isNotBlank()) multiSearchViewModel.search(multiQuery, searchSourceFilter)
                },
                label = { Text("Jamendo") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (multiSearchState) {
                is MultiPlatformUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is MultiPlatformUiState.Success -> {
                    val state = multiSearchState as MultiPlatformUiState.Success
                    if (state.tracks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhuma música encontrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            items(state.tracks, key = { it.id }) { track ->
                                SearchTrackCard(
                                    track = track,
                                    onPlay = {
                                        if (!track.streamUrl.isNullOrEmpty()) {
                                            val song = Song(
                                                id = track.id,
                                                title = track.title,
                                                artist = track.artist,
                                                album = track.album ?: "Online",
                                                duration = track.durationMs,
                                                mediaUri = track.streamUrl,
                                                coverUri = track.coverUrl,
                                                isDownloaded = false
                                            )
                                            viewModel.playSong(song)
                                        } else {
                                            Toast.makeText(context, "Link de streaming indisponível.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                is MultiPlatformUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((multiSearchState as MultiPlatformUiState.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Explore milhões de músicas gratuitas", fontWeight = FontWeight.Medium)
                            Text("Jamendo API v3.0 Official", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTrackCard(
    track: UnifiedTrack,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                if (!track.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val platformIcon = if (track.platform == MusicPlatform.JAMENDO) Icons.Default.Language else Icons.Default.Cloud
                    val platformName = if (track.platform == MusicPlatform.JAMENDO) "Jamendo" else "SoundCloud"
                    Icon(platformIcon, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(platformName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Tocar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
