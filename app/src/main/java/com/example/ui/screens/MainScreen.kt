package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.auth.AuthUiState
import com.example.ui.auth.AuthViewModel
import com.example.ui.music.MusicViewModel
import com.example.ui.music.PlaybackState
import com.example.ui.util.UserProfile

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Screen("home", "Início", Icons.Default.Home)
    data object Library : Screen("library", "Biblioteca", Icons.Default.LibraryMusic)
    data object Playlists : Screen("playlists", "Playlists", Icons.Default.QueueMusic)
    data object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
    data object Search : Screen("search", "Busca", Icons.Default.Search)
    data object Stats : Screen("stats", "Estatísticas", Icons.Default.BarChart)
    data object Folders : Screen("folders", "Pastas", Icons.Default.Folder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    musicViewModel: MusicViewModel,
    authViewModel: AuthViewModel,
    initialDestination: String? = null,
    onDestinationConsumed: () -> Unit = {},
    onRequestPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    var showPlayer by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    val authState by authViewModel.uiState.collectAsState()
    val localProfile = (authState as? AuthUiState.Success)?.profile

    // Handle deep navigation from Floating Player shortcuts or external intents
    LaunchedEffect(initialDestination) {
        initialDestination?.let { dest ->
            when (dest) {
                "library" -> navController.navigate(Screen.Library.route) { launchSingleTop = true }
                "playlists" -> navController.navigate(Screen.Playlists.route) { launchSingleTop = true }
                "favorites" -> navController.navigate(Screen.Library.route) { launchSingleTop = true }
                "home" -> navController.navigate(Screen.Home.route) { launchSingleTop = true }
            }
            onDestinationConsumed()
        }
    }

    if (showPlayer) {
        PlayerScreen(
            viewModel = musicViewModel,
            onBack = { showPlayer = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.example.ui.util.MpStreetLogo(
                                size = 36.dp,
                                showGlow = true
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
                                    text = "STREET CULTURE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.2.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick Toggle Floating Player
                        IconButton(onClick = {
                            if (com.example.service.FloatingPlayerPrefs.canDrawOverlays(context)) {
                                if (com.example.service.FloatingPlayerService.isRunning) {
                                    com.example.service.FloatingPlayerService.stop(context)
                                } else {
                                    com.example.service.FloatingPlayerService.start(context)
                                }
                            } else {
                                com.example.service.FloatingPlayerPrefs.openOverlaySettings(context)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Floating Player MP",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Global Search Button
                        IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Pesquisar Músicas Online",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Quick Equalizer Button
                        IconButton(onClick = { showEqualizer = true }) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Equalizador",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Local Account Button
                        IconButton(onClick = { showAccountDialog = true }) {
                            if (localProfile?.photoUrl != null) {
                                AsyncImage(
                                    model = localProfile.photoUrl,
                                    contentDescription = "Perfil",
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = if (localProfile != null) Icons.Default.AccountCircle else Icons.Default.PersonOutline,
                                    contentDescription = "Perfil",
                                    tint = if (localProfile != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    NowPlayingBar(
                        viewModel = musicViewModel,
                        onExpand = { showPlayer = true }
                    )

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp
                    ) {
                        val items = listOf(
                            Screen.Home,
                            Screen.Library,
                            Screen.Playlists,
                            Screen.Settings
                        )
                        items.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = {
                                    Text(
                                        screen.title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = musicViewModel,
                        onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                        onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                        onOpenEqualizer = { showEqualizer = true }
                    )
                }
                composable(Screen.Library.route) {
                    LibraryScreen(
                        viewModel = musicViewModel,
                        onRequestPermissions = onRequestPermissions
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(viewModel = musicViewModel)
                }
                composable(Screen.Playlists.route) {
                    PlaylistsScreen(viewModel = musicViewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = musicViewModel,
                        onOpenEqualizer = { showEqualizer = true },
                        onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                        onNavigateToFolders = { navController.navigate(Screen.Folders.route) }
                    )
                }
                composable(Screen.Stats.route) {
                    StatsScreen(
                        viewModel = musicViewModel,
                        onSongClick = { musicViewModel.playSong(it) }
                    )
                }
                composable(Screen.Folders.route) {
                    FolderBrowserScreen(
                        viewModel = musicViewModel,
                        onSongClick = { musicViewModel.playSong(it) }
                    )
                }
            }
        }
    }

    // Equalizer BottomSheet
    if (showEqualizer) {
        EqualizerDialog(viewModel = musicViewModel, onDismiss = { showEqualizer = false })
    }

    // Local Account & Profile Dialog
    if (showAccountDialog) {
        val context = LocalContext.current
        AccountDialog(
            userProfile = localProfile,
            authState = authState,
            onCreateProfile = { name, email -> authViewModel.createLocalProfile(context, name, email) },
            onSignOut = { authViewModel.signOut(context) },
            onDismiss = { showAccountDialog = false }
        )
    }
}

@Composable
fun NowPlayingBar(viewModel: MusicViewModel, onExpand: () -> Unit) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()

    AnimatedVisibility(
        visible = playbackState is PlaybackState.Playing,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        if (playbackState is PlaybackState.Playing) {
            val playing = playbackState as PlaybackState.Playing
            val duration = if (playing.currentSong.duration > 0) playing.currentSong.duration else 1L
            val progressFraction = (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onExpand() },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40A855F7)),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x33A855F7))
                        ) {
                            if (!playing.currentSong.coverUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = playing.currentSong.coverUri,
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
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = playing.currentSong.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = playing.currentSong.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play / Pause circular action
                        Surface(
                            onClick = { viewModel.togglePlayPause() },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playing.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(onClick = { viewModel.skipToNext() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Próxima",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Bottom progress indicator line
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0x22A855F7)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountDialog(
    userProfile: UserProfile?,
    authState: AuthUiState,
    onCreateProfile: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (userProfile != null) "Seu Perfil Local" else "Criar Perfil Local")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (userProfile != null) {
                    Text(
                        text = "Usuário:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userProfile.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = userProfile.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhonelinkRing,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo 100% Offline Ativo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = {
                            onSignOut()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remover Perfil")
                    }
                } else {
                    Text(
                        text = "A sincronização em nuvem foi removida. Crie um perfil local para identificar seu Myato Player no dispositivo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (authState is AuthUiState.Loading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Button(
                            onClick = { onCreateProfile(name.ifBlank { "Usuário Myato" }, email) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Salvar Perfil")
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
