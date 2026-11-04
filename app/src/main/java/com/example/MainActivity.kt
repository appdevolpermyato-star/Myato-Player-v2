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
import com.example.ui.archive.ArchiveViewModel
import com.example.ui.music.MusicViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.PermissionHelper
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = MusicDatabase.getDatabase(this)
        val repository = MusicRepository(this, database.musicDao())

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
                    val archiveViewModel: ArchiveViewModel = viewModel()
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
                        delay(1200)
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
                                archiveViewModel = archiveViewModel,
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
                        Color(0xFF04070E),
                        Color(0xFF080E1B),
                        Color(0xFF020408)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Image(
                    painter = painterResource(id = R.drawable.myato_logo),
                    contentDescription = "Myato Player",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MYATO",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PLAYER",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Offline High-Fidelity Music Experience",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
