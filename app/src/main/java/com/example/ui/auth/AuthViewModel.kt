package com.example.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.util.ProfileManager
import com.example.ui.util.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val userId: String, val profile: UserProfile) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var profileManager: ProfileManager? = null

    fun init(context: Context) {
        if (profileManager != null) return
        profileManager = ProfileManager(context)
        viewModelScope.launch {
            profileManager?.userProfile?.collect { profile ->
                if (profile != null) {
                    _uiState.value = AuthUiState.Success(profile.id, profile)
                } else {
                    _uiState.value = AuthUiState.Idle
                }
            }
        }
    }

    fun attemptAutoSignIn(context: Context) {
        init(context)
    }

    fun createLocalProfile(context: Context, name: String, email: String) {
        val pm = profileManager ?: ProfileManager(context)
        profileManager = pm
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val profile = UserProfile(
                id = "local_${System.currentTimeMillis()}",
                name = name,
                email = email
            )
            pm.saveProfile(profile)
            _uiState.value = AuthUiState.Success(profile.id, profile)
        }
    }

    fun updateProfile(context: Context, name: String, email: String) {
        val pm = profileManager ?: ProfileManager(context)
        viewModelScope.launch {
            val current = pm.userProfile.first() ?: UserProfile()
            val updated = current.copy(name = name, email = email)
            pm.saveProfile(updated)
        }
    }

    fun signIn(context: Context) {
        _uiState.value = AuthUiState.Error("A sincronização online foi removida. Utilize o Perfil Local.")
    }

    fun signOut(context: Context) {
        val pm = profileManager ?: ProfileManager(context)
        viewModelScope.launch {
            pm.clearProfile()
            _uiState.value = AuthUiState.Idle
        }
    }
}
