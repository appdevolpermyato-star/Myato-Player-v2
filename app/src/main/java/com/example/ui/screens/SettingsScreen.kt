package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.R
import com.example.service.FloatingPlayerPrefs
import com.example.service.FloatingPlayerService
import com.example.ui.music.MusicViewModel
import com.example.ui.theme.*
import com.example.ui.util.MpStreetLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onOpenEqualizer: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToFolders: () -> Unit
) {
    val context = LocalContext.current
    val sleepRemaining by viewModel.sleepTimerRemaining.collectAsState()
    val currentSpeed by viewModel.playbackSpeed.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentSong = (playbackState as? com.example.ui.music.PlaybackState.Playing)?.currentSong

    // Floating Player State from Prefs
    var isFloatingEnabled by remember { mutableStateOf(FloatingPlayerPrefs.isEnabled(context)) }
    var isAutoStart by remember { mutableStateOf(FloatingPlayerPrefs.isAutoStart(context)) }
    var bubbleSize by remember { mutableIntStateOf(FloatingPlayerPrefs.getBubbleSize(context)) }
    var panelSizePercent by remember { mutableIntStateOf(FloatingPlayerPrefs.getPanelSizePercent(context)) }
    var panelOpacityPercent by remember { mutableIntStateOf(FloatingPlayerPrefs.getPanelOpacityPercent(context)) }
    var hasOverlayPermission by remember { mutableStateOf(FloatingPlayerPrefs.canDrawOverlays(context)) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showBubbleSizeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDuplicatesDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    var duplicates by remember { mutableStateOf<List<com.example.data.model.Song>>(emptyList()) }

    // Re-check overlay permission when screen resumes
    LaunchedEffect(Unit) {
        hasOverlayPermission = FloatingPlayerPrefs.canDrawOverlays(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderSection("Ajustes")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
        ) {
            // 1. APARÊNCIA & TEMA STREET CULTURE
            item {
                SettingsCategoryHeader("APARÊNCIA & IDENTIDADE")
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MpStreetLogo(size = 46.dp, showGlow = true)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Street Culture Premium",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Preto profundo • Roxo luminoso • Branco urbano",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2. SEÇÃO ESPECIAL: MYATO FLOATING PLAYER
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategoryHeader("MYATO FLOATING PLAYER (SOBREPOSIÇÃO)")
            }

            // Permission Card if not granted
            if (!hasOverlayPermission) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2A1535),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, PurplePrimary)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = PurpleBright,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Permissão de Sobreposição Necessária",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Para a bolinha MP flutuar sobre outros aplicativos, autorize a permissão 'Sobrepor a outros apps' no Android.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    FloatingPlayerPrefs.openOverlaySettings(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                            ) {
                                Text("Autorizar no Android", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Ativar / Desativar Bolinha
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.PictureInPictureAlt,
                    title = "Ativar Bolinha Flutuante MP",
                    subtitle = if (hasOverlayPermission) "Exibir bolinha arrastável sobre outros apps" else "Requer permissão de sobreposição",
                    checked = isFloatingEnabled && hasOverlayPermission,
                    onCheckedChange = { checked ->
                        if (!hasOverlayPermission) {
                            FloatingPlayerPrefs.openOverlaySettings(context)
                        } else {
                            isFloatingEnabled = checked
                            FloatingPlayerPrefs.setEnabled(context, checked)
                            if (checked) {
                                FloatingPlayerService.start(context)
                                snackbarMessage = "Floating Player iniciado!"
                            } else {
                                FloatingPlayerService.stop(context)
                                snackbarMessage = "Floating Player desativado"
                            }
                        }
                    }
                )
            }

            // Iniciar Automaticamente
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.PlayCircleOutline,
                    title = "Iniciar Automaticamente",
                    subtitle = "Abrir a bolinha MP automaticamente ao abrir o app",
                    checked = isAutoStart,
                    onCheckedChange = { checked ->
                        isAutoStart = checked
                        FloatingPlayerPrefs.setAutoStart(context, checked)
                    }
                )
            }

            // Ajustar Tamanho da Bolinha
            item {
                val sizeLabel = when (bubbleSize) {
                    48 -> "Pequeno (48dp)"
                    64 -> "Grande (64dp)"
                    else -> "Normal (56dp)"
                }
                SettingsItem(
                    icon = Icons.Default.AspectRatio,
                    title = "Tamanho da Bolinha MP",
                    subtitle = sizeLabel,
                    onClick = { showBubbleSizeDialog = true }
                )
            }

            // SEÇÃO: PERSONALIZAÇÃO DO MINI MENU
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsCategoryHeader("PERSONALIZAÇÃO DO MINI MENU")
            }

            item {
                MiniMenuCustomizationCard(
                    sizePercent = panelSizePercent,
                    opacityPercent = panelOpacityPercent,
                    currentSong = currentSong,
                    onSizeChange = { newSize ->
                        panelSizePercent = newSize
                        FloatingPlayerPrefs.setPanelSizePercent(context, newSize)
                        FloatingPlayerService.updateCustomization(context)
                    },
                    onOpacityChange = { newOpacity ->
                        panelOpacityPercent = newOpacity
                        FloatingPlayerPrefs.setPanelOpacityPercent(context, newOpacity)
                        FloatingPlayerService.updateCustomization(context)
                    },
                    onRestoreDefaults = {
                        FloatingPlayerPrefs.restoreDefaults(context)
                        panelSizePercent = 100
                        panelOpacityPercent = 100
                        FloatingPlayerService.updateCustomization(context)
                        snackbarMessage = "Padrão restaurado: 100% tamanho • 100% opaco"
                    }
                )
            }

            // Restaurar Posição Padrão
            item {
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Restaurar Posição da Bolinha",
                    subtitle = "Reposicionar a bolinha no canto lateral superior",
                    onClick = {
                        FloatingPlayerPrefs.resetPosition(context)
                        if (FloatingPlayerService.isRunning) {
                            FloatingPlayerService.stop(context)
                            FloatingPlayerService.start(context)
                        }
                        snackbarMessage = "Posição restaurada!"
                    }
                )
            }

            // Testar / Abrir Painel Flutuante
            item {
                SettingsItem(
                    icon = Icons.Default.OpenInNew,
                    title = "Testar / Abrir Painel Flutuante",
                    subtitle = "Expande o controle flutuante imediatamente",
                    onClick = {
                        if (FloatingPlayerPrefs.canDrawOverlays(context)) {
                            FloatingPlayerService.start(context)
                            val expandIntent = android.content.Intent(context, FloatingPlayerService::class.java).apply {
                                action = FloatingPlayerService.ACTION_EXPAND
                            }
                            context.startService(expandIntent)
                        } else {
                            FloatingPlayerPrefs.openOverlaySettings(context)
                        }
                    }
                )
            }

            // 3. ÁUDIO & REPRODUÇÃO
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategoryHeader("ÁUDIO & REPRODUÇÃO")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Equalizador de Áudio",
                    subtitle = "Ajustar bandas, graves (Bass Boost) e virtualizador",
                    onClick = onOpenEqualizer
                )
            }

            item {
                val speedLabel = when (currentSpeed) {
                    1.0f -> "Normal (1.0x)"
                    else -> "${currentSpeed}x"
                }
                SettingsItem(
                    icon = Icons.Default.Speed,
                    title = "Velocidade de Reprodução",
                    subtitle = speedLabel,
                    onClick = { showSpeedDialog = true }
                )
            }

            // 4. BIBLIOTECA & DADOS
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategoryHeader("BIBLIOTECA & ESTATÍSTICAS")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.BarChart,
                    title = "Estatísticas & Myato Wrapped",
                    subtitle = "Músicas mais tocadas e retrospectiva",
                    onClick = onNavigateToStats
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Navegador de Pastas",
                    subtitle = "Explorar arquivos de música por diretórios locais",
                    onClick = onNavigateToFolders
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Detectar Duplicatas",
                    subtitle = "Encontrar arquivos repetidos na biblioteca",
                    onClick = {
                        viewModel.viewModelScope.launch {
                            duplicates = viewModel.detectDuplicates()
                            showDuplicatesDialog = true
                        }
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Backup & Restauração",
                    subtitle = "Salvar ou restaurar playlists e favoritos (JSON Local)",
                    onClick = {
                        snackbarMessage = "Backup local mantido na memória segura do dispositivo."
                    }
                )
            }

            // 5. TEMPORIZADOR DE SONO
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategoryHeader("TEMPORIZADOR")
            }

            item {
                val timerSubtitle = if (sleepRemaining != null) {
                    val min = sleepRemaining!! / 60
                    val sec = sleepRemaining!! % 60
                    "Pausa em ${min}m ${sec}s"
                } else {
                    "Desativado"
                }

                SettingsItem(
                    icon = Icons.Default.Bedtime,
                    title = "Temporizador de Sono (Sleep Timer)",
                    subtitle = timerSubtitle,
                    onClick = { showSleepTimerDialog = true }
                )
            }

            // 6. MANUTENÇÃO & ARMAZENAMENTO
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategoryHeader("MANUTENÇÃO")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "Re-escanear Arquivos Locais",
                    subtitle = "Procurar novas faixas de áudio no aparelho",
                    onClick = {
                        viewModel.scanLocalFiles()
                        snackbarMessage = "Buscando novas faixas de áudio..."
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.CleaningServices,
                    title = "Limpar Cache Temporário",
                    subtitle = "Liberar espaço em cache e arquivos transitórios",
                    onClick = {
                        viewModel.clearCache()
                        snackbarMessage = "Cache limpo com sucesso!"
                    }
                )
            }

            // 7. SOBRE O APP
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategoryHeader("SOBRE")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Sobre o Myato Player",
                    subtitle = "Versão 1.0.0 Pro • Street Culture Premium",
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    // SNACKBAR MESSAGE
    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2500)
            snackbarMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(msg)
            }
        }
    }

    // BUBBLE SIZE DIALOG
    if (showBubbleSizeDialog) {
        val sizes = listOf(
            Pair("Pequena (48dp)", 48),
            Pair("Normal (56dp)", 56),
            Pair("Grande (64dp)", 64)
        )
        AlertDialog(
            onDismissRequest = { showBubbleSizeDialog = false },
            title = { Text("Tamanho da Bolinha Flutuante") },
            text = {
                Column {
                    sizes.forEach { (label, size) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    bubbleSize = size
                                    FloatingPlayerPrefs.setBubbleSize(context, size)
                                    if (FloatingPlayerService.isRunning) {
                                        FloatingPlayerService.stop(context)
                                        FloatingPlayerService.start(context)
                                    }
                                    showBubbleSizeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = bubbleSize == size,
                                onClick = {
                                    bubbleSize = size
                                    FloatingPlayerPrefs.setBubbleSize(context, size)
                                    if (FloatingPlayerService.isRunning) {
                                        FloatingPlayerService.stop(context)
                                        FloatingPlayerService.start(context)
                                    }
                                    showBubbleSizeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBubbleSizeDialog = false }) { Text("Fechar") }
            }
        )
    }


    // SPEED DIALOG
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Velocidade de Reprodução") },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSpeed == speed,
                                onClick = {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Fechar") }
            }
        )
    }

    // SLEEP TIMER DIALOG
    if (showSleepTimerDialog) {
        val timerOptions = listOf(
            Pair("Desativar", 0),
            Pair("15 minutos", 15),
            Pair("30 minutos", 30),
            Pair("45 minutos", 45),
            Pair("60 minutos", 60),
            Pair("90 minutos", 90)
        )

        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Temporizador de Sono") },
            text = {
                Column {
                    Text(
                        text = "A música será pausada automaticamente ao término do tempo definido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    timerOptions.forEach { (label, minutes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (minutes == 0) {
                                        viewModel.cancelSleepTimer()
                                    } else {
                                        viewModel.setSleepTimer(minutes)
                                    }
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = if (minutes == 0) sleepRemaining == null else (sleepRemaining != null && sleepRemaining!! <= minutes * 60L && sleepRemaining!! > (minutes - 15) * 60L),
                                onClick = {
                                    if (minutes == 0) {
                                        viewModel.cancelSleepTimer()
                                    } else {
                                        viewModel.setSleepTimer(minutes)
                                    }
                                    showSleepTimerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // DUPLICATES DIALOG
    if (showDuplicatesDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicatesDialog = false },
            title = { Text("Duplicatas Detectadas") },
            text = {
                if (duplicates.isEmpty()) {
                    Text("Nenhuma música duplicada encontrada.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(duplicates.size) { index ->
                            val song = duplicates[index]
                            ListItem(
                                headlineContent = { Text(song.title, style = MaterialTheme.typography.bodyMedium) },
                                supportingContent = { Text(song.artist, style = MaterialTheme.typography.bodySmall) },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.deleteSong(song) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.Red)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDuplicatesDialog = false }) { Text("Fechar") }
            }
        )
    }

    // ABOUT DIALOG
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MpStreetLogo(size = 36.dp, showGlow = false)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Myato Player")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Versão 1.0.0 • Street Culture Pro",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reprodutor de música premium inspirado na cultura urbana e Street Art. Equipado com equalizador profissional, letras sincronizadas e controle flutuante (Floating Player).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "100% Offline • Sem anúncios • Áudio de alta fidelidade.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x22A855F7)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x33A855F7))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x22A855F7))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x33A855F7))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = White,
                    checkedTrackColor = PurplePrimary,
                    uncheckedThumbColor = GrayMuted,
                    uncheckedTrackColor = GraphiteDark
                )
            )
        }
    }
}

@Composable
fun MiniMenuCustomizationCard(
    sizePercent: Int,
    opacityPercent: Int,
    currentSong: com.example.data.model.Song?,
    onSizeChange: (Int) -> Unit,
    onOpacityChange: (Int) -> Unit,
    onRestoreDefaults: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Live Preview Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Preview,
                        contentDescription = null,
                        tint = PurpleBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pré-visualização em Tempo Real",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x33A855F7)
                ) {
                    Text(
                        text = "${sizePercent}% • ${opacityPercent}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PurpleBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Backdrop area showing transparency effect with street pattern
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D0B12))
                    .border(0.8.dp, Color(0x22A855F7), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background street art grid lines to make transparency visually obvious
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 28.dp.toPx()
                    val cols = (size.width / step).toInt() + 1
                    for (x in 0 until cols) {
                        drawLine(
                            color = androidx.compose.ui.graphics.Color(0x18A855F7),
                            start = Offset(x * step, 0f),
                            end = Offset(x * step, size.height),
                            strokeWidth = 1f
                        )
                    }
                    val rows = (size.height / step).toInt() + 1
                    for (y in 0 until rows) {
                        drawLine(
                            color = androidx.compose.ui.graphics.Color(0x18A855F7),
                            start = Offset(0f, y * step),
                            end = Offset(size.width, y * step),
                            strokeWidth = 1f
                        )
                    }
                }

                // Scaled Mini Menu Mockup
                val scale = sizePercent / 100f
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                ) {
                    MiniPanelPreviewMock(
                        opacityPercent = opacityPercent,
                        currentSong = currentSong
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. TAMANHO DO MINI MENU SLIDER
            val sizeLabel = when (sizePercent) {
                70 -> "70% — Compacto"
                80 -> "80% — Pequeno"
                90 -> "90% — Médio"
                100 -> "100% — Padrão"
                110 -> "110% — Grande"
                120 -> "120% — Extra grande"
                else -> "$sizePercent%"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tamanho do Mini Menu",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = White
                    )
                }
                Text(
                    text = sizeLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = PurpleBright
                )
            }

            Slider(
                value = sizePercent.toFloat(),
                onValueChange = { raw ->
                    val snapped = (Math.round(raw / 10f) * 10).toInt().coerceIn(70, 120)
                    if (snapped != sizePercent) {
                        onSizeChange(snapped)
                    }
                },
                valueRange = 70f..120f,
                steps = 4, // 80, 90, 100, 110
                colors = SliderDefaults.colors(
                    thumbColor = White,
                    activeTrackColor = PurplePrimary,
                    inactiveTrackColor = Color(0x33A855F7)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. TRANSPARÊNCIA DO MINI MENU SLIDER
            val opacityLabel = when (opacityPercent) {
                30 -> "30% — Muito transparente"
                40 -> "40% — Translúcido leve"
                50 -> "50% — Translúcido médio"
                60 -> "60% — Equilibrado"
                70 -> "70% — Suave"
                80 -> "80% — Quase sólido"
                90 -> "90% — Sólido suave"
                100 -> "100% — Totalmente opaco"
                else -> "$opacityPercent%"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Transparência do Mini Menu",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = White
                    )
                }
                Text(
                    text = opacityLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = PurpleBright
                )
            }

            Slider(
                value = opacityPercent.toFloat(),
                onValueChange = { raw ->
                    val snapped = (Math.round(raw / 10f) * 10).toInt().coerceIn(30, 100)
                    if (snapped != opacityPercent) {
                        onOpacityChange(snapped)
                    }
                },
                valueRange = 30f..100f,
                steps = 6, // 40, 50, 60, 70, 80, 90
                colors = SliderDefaults.colors(
                    thumbColor = White,
                    activeTrackColor = PurplePrimary,
                    inactiveTrackColor = Color(0x33A855F7)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. RESTAURAR PADRÃO BUTTON
            OutlinedButton(
                onClick = onRestoreDefaults,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55A855F7)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = PurpleBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Restaurar padrão (100% tamanho • 100% opaco)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun MiniPanelPreviewMock(
    opacityPercent: Int,
    currentSong: com.example.data.model.Song?
) {
    // Only the panel background opacity is modified!
    // Texts, buttons, icons, progress bar and album art stay 100% visible and sharp.
    // The purple border #A855F7 stays visible!
    val bgAlpha = opacityPercent / 100f

    Surface(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF121116).copy(alpha = bgAlpha),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PurplePrimary)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header: Official MP Logo + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.myato_logo),
                        contentDescription = "MP Logo",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MYATO FLOATING",
                            color = White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "STREET CULTURE",
                            color = PurpleBright,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = GrayMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Track info: Cover + Title + Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E1A26),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7))
                ) {
                    if (!currentSong?.coverUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentSong!!.coverUri,
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.myato_logo),
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "Street Art Anthem",
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = currentSong?.artist ?: "Myato Pro",
                        color = GrayMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar and times
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x33A855F7))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .fillMaxHeight()
                        .background(PurplePrimary)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "1:24", color = GrayMuted, fontSize = 9.sp)
                Text(text = "3:40", color = GrayMuted, fontSize = 9.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = PurplePrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF09090D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = GrayMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Mini shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MiniShortcutPill(modifier = Modifier.weight(1f), icon = Icons.Default.LibraryMusic, label = "Biblioteca")
                MiniShortcutPill(modifier = Modifier.weight(1f), icon = Icons.Default.Favorite, label = "Favoritos")
                MiniShortcutPill(modifier = Modifier.weight(1f), icon = Icons.Default.QueueMusic, label = "Playlists")
                MiniShortcutPill(modifier = Modifier.weight(1f), icon = Icons.Default.OpenInNew, label = "Abrir App", isHighlight = true)
            }
        }
    }
}

@Composable
private fun MiniShortcutPill(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isHighlight: Boolean = false
) {
    Surface(
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1B1624),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0x33A855F7))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlight) White else PurpleBright,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                color = if (isHighlight) PurpleBright else White,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
