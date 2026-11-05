package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.music.MusicViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MusicViewModel,
    onSongClick: (Song) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Histórico", "Mais Tocadas", "Minha Retrospectiva")

    val recentlyPlayed by viewModel.recentlyPlayedSongs.collectAsState()
    val mostPlayed by viewModel.mostPlayedSongs.collectAsState()
    
    var wrappedStats by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) {
            wrappedStats = viewModel.repository.getWrappedStats(Calendar.getInstance().get(Calendar.YEAR))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estatísticas & Histórico") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "StatsTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HistoryList(songs = recentlyPlayed, onSongClick = onSongClick)
                    1 -> MostPlayedList(songs = mostPlayed, onSongClick = onSongClick)
                    2 -> WrappedView(stats = wrappedStats)
                }
            }
        }
    }
}

@Composable
fun HistoryList(songs: List<Song>, onSongClick: (Song) -> Unit) {
    if (songs.isEmpty()) {
        EmptyState("Nenhum histórico de reprodução encontrado.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs) { song ->
                SongStatItem(song = song, detail = "Visto em: ${formatTimestamp(song.lastPlayed)}", onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
fun MostPlayedList(songs: List<Song>, onSongClick: (Song) -> Unit) {
    if (songs.isEmpty()) {
        EmptyState("Você ainda não ouviu músicas suficientes.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs) { song ->
                SongStatItem(song = song, detail = "${song.playCount} reproduções", onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
fun WrappedView(stats: Map<String, Any>?) {
    if (stats == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val topSongs = stats["topSongs"] as List<Song>
        val minutes = stats["totalMinutes"] as Long
        val topArtist = stats["topArtist"] as String
        val year = stats["year"] as Int

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color.Black)
                    )
                ),
            contentPadding = PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "MYATO WRAPPED $year",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                WrappedCard(
                    title = "Tempo de Reprodução",
                    value = "$minutes min",
                    icon = Icons.Default.Timer,
                    color = Color(0xFF4361EE)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                WrappedCard(
                    title = "Artista Favorito",
                    value = topArtist,
                    icon = Icons.Default.Person,
                    color = Color(0xFF7209B7)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "Suas 5 Músicas Mais Tocadas",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            items(topSongs) { song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.coverUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(song.title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WrappedCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

@Composable
fun SongStatItem(song: Song, detail: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUri,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
    }
}

fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "Nunca"
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(ts))
}
