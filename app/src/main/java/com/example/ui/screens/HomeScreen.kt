package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Song
import com.example.service.FloatingPlayerPrefs
import com.example.service.FloatingPlayerService
import com.example.ui.music.MusicViewModel
import com.example.ui.music.PlaybackState
import com.example.ui.theme.*
import com.example.ui.util.MpStreetLogo

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val context = LocalContext.current
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 1. BRAND HEADER WITH MP STREET LOGO & QUICK ACTIONS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MpStreetLogo(
                        size = 40.dp,
                        showGlow = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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
                            text = "STREET CULTURE • HIGH FIDELITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Floating player trigger
                    IconButton(onClick = {
                        if (FloatingPlayerPrefs.canDrawOverlays(context)) {
                            if (FloatingPlayerService.isRunning) {
                                FloatingPlayerService.stop(context)
                            } else {
                                FloatingPlayerService.start(context)
                            }
                        } else {
                            FloatingPlayerPrefs.openOverlaySettings(context)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Floating Player MP",
                            tint = if (FloatingPlayerService.isRunning) PurpleBright else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Equalizer
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizador",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Sync local files
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

        // 2. HERO: CONTINUAR OUVINDO (STREET ART CARD)
        if (continueSong != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { viewModel.playSong(continueSong) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x55A855F7)),
                    tonalElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x33A855F7),
                                        Color(0xFF21172B)
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
                                    .size(70.dp)
                                    .shadow(12.dp, RoundedCornerShape(14.dp), spotColor = PurplePrimary),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66A855F7))
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
                                            modifier = Modifier.size(34.dp)
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
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = continueSong.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(8.dp, CircleShape, spotColor = PurplePrimary)
                            ) {
                                val isThisPlaying = (playbackState as? PlaybackState.Playing)?.let {
                                    it.currentSong.id == continueSong.id && it.isPlaying
                                } ?: false

                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(26.dp)
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
                QuickActionChip(
                    icon = Icons.Default.PictureInPictureAlt,
                    title = "Floating",
                    onClick = {
                        if (FloatingPlayerPrefs.canDrawOverlays(context)) {
                            FloatingPlayerService.start(context)
                        } else {
                            FloatingPlayerPrefs.openOverlaySettings(context)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. TOCADAS RECENTEMENTE (Horizontal Carousel)
        if (recentSongs.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title = "Tocadas Recentemente",
                    subtitle = "Suas últimas faixas reproduzidas",
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
                    subtitle = "Suas faixas em alta no dia a dia",
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
                    subtitle = "${favorites.size} músicas salvas na sua coleção",
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
                    title = "Álbuns em Destaque",
                    subtitle = "Organizados da sua biblioteca",
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

        // 8. TODAS AS MÚSICAS
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HomeSectionHeader(
                title = "Todas as Músicas",
                subtitle = "${allSongs.size} faixas locais prontas para tocar",
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
            .width(134.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier
                .size(134.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = PurplePrimary),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7))
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
                                listOf(GraphiteDark, BlackSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
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
            .width(134.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier
                .size(134.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = PurplePrimary),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7))
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
                                listOf(GraphiteDark, BlackPrimary)
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
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
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
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7))
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
