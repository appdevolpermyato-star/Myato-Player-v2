package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.ui.auth.AuthViewModel
import com.example.ui.music.MusicViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.PermissionHelper
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var targetDestination by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val dest = intent.getStringExtra("target_screen")
        if (!dest.isNullOrBlank()) {
            targetDestination = dest
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dest = intent.getStringExtra("target_screen")
        if (!dest.isNullOrBlank()) {
            targetDestination = dest
        }

        val database = MusicDatabase.getDatabase(this)
        val repository = MusicRepository(this, database.musicDao())

        // Register for noisy audio (headset unplugged)
        registerReceiver(
            object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                    if (android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                        // Media3 / PlaybackService handles this automatically
                    }
                }
            },
            android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )

        // Auto-start floating player if enabled and permitted
        if (com.example.service.FloatingPlayerPrefs.isAutoStart(this) &&
            com.example.service.FloatingPlayerPrefs.canDrawOverlays(this)
        ) {
            com.example.service.FloatingPlayerService.start(this)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    LaunchedEffect(Unit) {
                        authViewModel.init(this@MainActivity)
                    }
                    val musicViewModel: MusicViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return MusicViewModel(repository) as T
                            }
                        }
                    )

                    var isSplashScreenVisible by remember { mutableStateOf(true) }

                    // Multi-permission launcher for Android 13+ (READ_MEDIA_AUDIO + POST_NOTIFICATIONS)
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissionsMap ->
                        val audioGranted = permissionsMap[PermissionHelper.getAudioPermission()] ?: false
                        if (audioGranted) {
                            musicViewModel.scanLocalFiles()
                        }
                    }

                    // Request permissions and initialize MediaController on startup
                    LaunchedEffect(Unit) {
                        musicViewModel.initController(this@MainActivity)
                        authViewModel.attemptAutoSignIn(this@MainActivity)

                        if (!PermissionHelper.hasAudioPermission(this@MainActivity)) {
                            permissionLauncher.launch(PermissionHelper.getStartupPermissions())
                        }

                        // Brief splash duration for premium feel
                        delay(1000)
                        isSplashScreenVisible = false
                    }

                    AnimatedContent(
                        targetState = isSplashScreenVisible,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                        },
                        label = "SplashScreenTransition"
                    ) { showSplash ->
                        if (showSplash) {
                            SplashScreen()
                        } else {
                            MainScreen(
                                musicViewModel = musicViewModel,
                                authViewModel = authViewModel,
                                initialDestination = targetDestination,
                                onDestinationConsumed = { targetDestination = null },
                                onRequestPermissions = {
                                    permissionLauncher.launch(PermissionHelper.getStartupPermissions())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF09090D),
                        Color(0xFF21172B),
                        Color(0xFF09090D)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            com.example.ui.util.MpStreetLogo(
                size = 92.dp,
                showGlow = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MYATO",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAYER",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp
                    ),
                    color = Color(0xFFF3F0F5)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "STREET CULTURE • HIGH FIDELITY",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp
            )
        }
    }
}
