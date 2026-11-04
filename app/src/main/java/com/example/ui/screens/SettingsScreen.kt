package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.music.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onOpenEqualizer: () -> Unit
) {
    val sleepRemaining by viewModel.sleepTimerRemaining.collectAsState()
    val currentSpeed by viewModel.playbackSpeed.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderSection("Configurações")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
        ) {
            // 1. ÁUDIO & REPRODUÇÃO
            item {
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

            // 2. TEMPORIZADOR DE SONO
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

            // 3. ARMAZENAMENTO & BIBLIOTECA
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategoryHeader("ARMAZENAMENTO & DADOS")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Biblioteca e Downloads",
                    subtitle = "${allSongs.size} faixas na biblioteca • ${downloadedSongs.size} arquivos baixados",
                    onClick = {}
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "Re-escanear Arquivos Locais",
                    subtitle = "Procurar novas músicas MP3, FLAC, WAV no aparelho",
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
                        snackbarMessage = "Cache temporário limpo com sucesso!"
                    }
                )
            }

            // 4. SOBRE O APP
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategoryHeader("SOBRE")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Sobre o Myato Player",
                    subtitle = "Versão 1.0.0 • Reprodução offline profissional",
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

    // ABOUT DIALOG
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.myato_logo),
                        contentDescription = "Myato Player Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Myato Player")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Versão 1.0.0 Pro",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reprodutor de música avançado para Android. Projetado para operação 100% offline, áudio de alta fidelidade, equalizador gráfico, organizador de biblioteca e gerenciador de downloads diretos de mídia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Desenvolvido com Jetpack Compose, Media3 ExoPlayer, Room Database e Material Design 3.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
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
