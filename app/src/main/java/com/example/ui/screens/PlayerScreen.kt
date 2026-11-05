package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.service.FloatingPlayerPrefs
import com.example.service.FloatingPlayerService
import com.example.ui.music.MusicViewModel
import com.example.ui.music.PlaybackState
import com.example.ui.theme.*
import com.example.ui.util.LrcParser

@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsState()
    val isShuffle by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    if (playbackState is PlaybackState.Playing) {
        val playing = playbackState as PlaybackState.Playing
        val song = playing.currentSong

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF21172B),
                            Color(0xFF121116),
                            Color(0xFF09090D)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimizar",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOCANDO AGORA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    // Floating player shortcut
                    IconButton(onClick = {
                        if (FloatingPlayerPrefs.canDrawOverlays(context)) {
                            FloatingPlayerService.start(context)
                        } else {
                            FloatingPlayerPrefs.openOverlaySettings(context)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Floating Player",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Equalizer
                    IconButton(onClick = { showEqualizer = true }) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizador",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Cover Art / Lyrics Toggle Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showLyrics) {
                    LyricsView(
                        song = song,
                        currentPosition = currentPos,
                        onClose = { showLyrics = false }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { showLyrics = true }
                            .padding(vertical = 8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .shadow(
                                    24.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    spotColor = PurplePrimary
                                ),
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x66A855F7))
                        ) {
                            if (!song.coverUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = song.coverUri,
                                    contentDescription = "Capa do Álbum",
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
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Street visualizer bars
                        AnimatedVisualizerBars(isPlaying = playing.isPlaying)
                    }
                }
            }

            // Track info and Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = { showAddToPlaylist = true }) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Adicionar à playlist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = if (song.isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Progress Bar and Timers
            Column(modifier = Modifier.fillMaxWidth()) {
                val duration = if (song.duration > 0) song.duration else 1L
                val sliderValue = currentPos.coerceIn(0L, duration).toFloat()

                Slider(
                    value = sliderValue,
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color(0x33A855F7)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPos),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(song.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Aleatório",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(onClick = { viewModel.skipToPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Play / Pause Street Art FAB
                Surface(
                    onClick = { viewModel.togglePlayPause() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = androidx.compose.foundation.BorderStroke(2.dp, White),
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(16.dp, CircleShape, spotColor = PurpleBright)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playing.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing.isPlaying) "Pausar" else "Reproduzir",
                            modifier = Modifier.size(36.dp),
                            tint = BlackPrimary
                        )
                    }
                }

                // Next
                IconButton(onClick = { viewModel.skipToNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Próxima",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Repeat Mode
                IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                    val repeatIcon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF

                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repetir",
                        tint = if (isRepeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Actions: Queue & Lyrics button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showLyrics = !showLyrics },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showLyrics) "Ver Capa" else "Letras", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }

                TextButton(
                    onClick = { showQueue = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fila", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }

        // Modals
        if (showQueue) {
            QueueSheet(viewModel = viewModel, onDismiss = { showQueue = false })
        }

        if (showEqualizer) {
            EqualizerDialog(viewModel = viewModel, onDismiss = { showEqualizer = false })
        }

        if (showAddToPlaylist) {
            AddToPlaylistDialog(song = song, viewModel = viewModel, onDismiss = { showAddToPlaylist = false })
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhuma música selecionada",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnimatedVisualizerBars(isPlaying: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(26.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
        val heights = listOf(
            infiniteTransition.animateFloat(
                initialValue = 0.2f, targetValue = if (isPlaying) 1f else 0.2f,
                animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "h1"
            ),
            infiniteTransition.animateFloat(
                initialValue = 0.45f, targetValue = if (isPlaying) 0.9f else 0.3f,
                animationSpec = infiniteRepeatable(tween(330, easing = LinearEasing), RepeatMode.Reverse), label = "h2"
            ),
            infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = if (isPlaying) 1f else 0.2f,
                animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse), label = "h3"
            ),
            infiniteTransition.animateFloat(
                initialValue = 0.6f, targetValue = if (isPlaying) 0.85f else 0.25f,
                animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "h4"
            ),
            infiniteTransition.animateFloat(
                initialValue = 0.2f, targetValue = if (isPlaying) 0.95f else 0.2f,
                animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse), label = "h5"
            ),
            infiniteTransition.animateFloat(
                initialValue = 0.4f, targetValue = if (isPlaying) 0.88f else 0.25f,
                animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "h6"
            )
        )

        heights.forEach { animatedHeight ->
            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .fillMaxHeight(animatedHeight.value)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(PurpleBright, PurplePrimary)
                        )
                    )
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun LyricsView(
    song: Song,
    currentPosition: Long,
    onClose: () -> Unit
) {
    val lyrics = remember(song.id) {
        val mockLrc = """
            [00:00.00]${song.title}
            [00:05.00]${song.artist}
            [00:10.00]Prepare-se para a batida...
            [00:20.00]A música flui através de você.
            [00:30.00]Sentindo cada nota, cada acorde.
            [00:45.00]Myato Player: Street Culture • High Fidelity
            [01:00.00]Vibe urbana em alta resolução.
        """.trimIndent()
        LrcParser.parse(mockLrc)
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val activeLineIndex = lyrics.indexOfLast { it.timeMs <= currentPosition }.coerceAtLeast(0)

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            listState.animateScrollToItem(activeLineIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClose() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LETRAS SINCRONIZADAS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(lyrics.size) { index ->
                val line = lyrics[index]
                val isActive = index == activeLineIndex
                val color by animateColorAsState(
                    targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                    animationSpec = tween(300), label = "lyricColor"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.12f else 1.0f,
                    animationSpec = tween(300), label = "lyricScale"
                )

                Text(
                    text = line.text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 32.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    color = color,
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                )
            }
        }
    }
}
