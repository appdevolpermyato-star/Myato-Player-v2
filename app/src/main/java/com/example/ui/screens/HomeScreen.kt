package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Song
import com.example.ui.music.MusicViewModel
import com.example.ui.music.PlaybackState

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToLibrary: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val recentSongs by viewModel.recentlyPlayedSongs.collectAsState()
    val mostPlayed by viewModel.mostPlayedSongs.collectAsState()
    val favorites by viewModel.favoriteSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val currentPlayingSong = (playbackState as? PlaybackState.Playing)?.currentSong
    val continueSong = currentPlayingSong ?: recentSongs.firstOrNull() ?: allSongs.firstOrNull()

    // Distinct albums and artists from allSongs
    val uniqueAlbums = remember(allSongs) {
        allSongs.groupBy { it.album }.map { it.value.first() }.take(10)
    }
    val uniqueArtists = remember(allSongs) {
        allSongs.groupBy { it.artist }.map { it.value.first() }.take(10)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 1. BRAND HEADER WITH LOGO
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.myato_logo),
                        contentDescription = "Myato Player Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MYATO",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PLAYER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Música Offline & Downloader",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizador",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.scanLocalFiles() }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sincronizar Arquivos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. HERO: CONTINUAR OUVINDO
        if (continueSong != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.playSong(continueSong) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        Color(0xFF10192A)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(68.dp)
                                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                if (!continueSong.coverUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = continueSong.coverUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CONTINUAR OUVINDO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = continueSong.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${continueSong.artist} • ${continueSong.album}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                onClick = {
                                    if (currentPlayingSong?.id == continueSong.id) {
                                        viewModel.togglePlayPause()
                                    } else {
                                        viewModel.playSong(continueSong)
                                    }
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
                            ) {
                                val isThisPlaying = (playbackState as? PlaybackState.Playing)?.let {
                                    it.currentSong.id == continueSong.id && it.isPlaying
                                } ?: false

                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. QUICK SHORTCUTS ROW
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    icon = Icons.Default.Download,
                    title = "Baixar Link",
                    onClick = onNavigateToDownloads,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Default.LibraryMusic,
                    title = "Biblioteca",
                    onClick = onNavigateToLibrary,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Default.QueueMusic,
                    title = "Playlists",
                    onClick = onNavigateToPlaylists,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. TOCADAS RECENTEMENTE (Horizontal Carousel)
        if (recentSongs.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title = "Tocadas Recentemente",
                    subtitle = "Suas últimas faixas",
                    onSeeAll = onNavigateToLibrary
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentSongs, key = { "recent_${it.id}" }) { song ->
                        SongCarouselCard(
                            song = song,
                            onClick = { viewModel.playSong(song) }
                        )
                    }
                }
            }
        }

        // 5. MAIS REPRODUZIDAS (Horizontal Carousel)
        if (mostPlayed.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HomeSectionHeader(
                    title = "Mais Ouvidas",
                    subtitle = "Suas faixas favoritas do dia a dia",
                    onSeeAll = onNavigateToLibrary
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(mostPlayed, key = { "most_${it.id}" }) { song ->
                        SongCarouselCard(
                            song = song,
                            onClick = { viewModel.playSong(song) }
                        )
                    }
                }
            }
        }

        // 6. MÚSICAS FAVORITAS
        if (favorites.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HomeSectionHeader(
                    title = "Músicas Curtidas",
                    subtitle = "${favorites.size} músicas salvas",
                    onSeeAll = onNavigateToLibrary
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(favorites.take(10), key = { "fav_${it.id}" }) { song ->
                        SongCarouselCard(
                            song = song,
                            onClick = { viewModel.playSong(song) }
                        )
                    }
                }
            }
        }

        // 7. ÁLBUNS DA BIBLIOTECA
        if (uniqueAlbums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HomeSectionHeader(
                    title = "Álbuns Encontrados",
                    subtitle = "Organizados automaticamente",
                    onSeeAll = onNavigateToLibrary
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uniqueAlbums, key = { "album_${it.album}" }) { song ->
                        AlbumCard(
                            albumName = song.album,
                            artist = song.artist,
                            coverUri = song.coverUri,
                            onClick = {
                                val albumSongs = allSongs.filter { it.album == song.album }
                                if (albumSongs.isNotEmpty()) {
                                    viewModel.playQueue(albumSongs, 0)
                                }
                            }
                        )
                    }
                }
            }
        }

        // 8. ADICIONADAS RECENTEMENTE
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HomeSectionHeader(
                title = "Todas as Músicas",
                subtitle = "${allSongs.size} faixas locais disponíveis",
                onSeeAll = onNavigateToLibrary
            )
        }
        items(allSongs.take(8), key = { "all_${it.id}" }) { song ->
            SongListItem(
                song = song,
                onClick = { viewModel.playSong(song) },
                onFavoriteClick = { viewModel.toggleFavorite(song) },
                onMoreClick = { /* Can show options */ }
            )
        }
    }
}

@Composable
fun HomeSectionHeader(title: String, subtitle: String? = null, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(
            onClick = onSeeAll,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Ver tudo",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SongCarouselCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier
                .size(130.dp)
                .shadow(6.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E283C), Color(0xFF0F1522))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumCard(albumName: String, artist: String, coverUri: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier
                .size(130.dp)
                .shadow(6.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (!coverUri.isNullOrEmpty()) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = albumName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
