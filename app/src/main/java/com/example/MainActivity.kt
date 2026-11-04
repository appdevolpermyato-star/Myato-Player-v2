package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.ui.auth.AuthViewModel
import com.example.ui.music.MusicViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.PermissionHelper
import com.example.ui.util.configureAppCheck

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure App Check contract for Firebase AI Logic
        configureAppCheck(this, intent)

        val database = MusicDatabase.getDatabase(this)
        val repository = MusicRepository(this, database.musicDao())

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    val musicViewModel: MusicViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return MusicViewModel(repository) as T
                            }
                        }
                    )

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
                    }

                    // Always show MainScreen directly (sem necessidade de login obrigatório)
                    MainScreen(
                        musicViewModel = musicViewModel,
                        authViewModel = authViewModel,
                        onRequestPermissions = {
                            permissionLauncher.launch(PermissionHelper.getStartupPermissions())
                        }
                    )
                }
            }
        }
    }
}
