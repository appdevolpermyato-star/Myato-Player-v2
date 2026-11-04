package com.example.ui.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_profile")

data class UserProfile(
    val id: String = "local_user",
    val name: String = "Usuário Myato",
    val email: String = "local@myato.app",
    val photoUrl: String? = null
)

class ProfileManager(private val context: Context) {
    private val ID_KEY = stringPreferencesKey("user_id")
    private val NAME_KEY = stringPreferencesKey("user_name")
    private val EMAIL_KEY = stringPreferencesKey("user_email")
    private val PHOTO_KEY = stringPreferencesKey("user_photo")

    val userProfile: Flow<UserProfile?> = context.dataStore.data.map { prefs ->
        val id = prefs[ID_KEY]
        if (id == null) null
        else UserProfile(
            id = id,
            name = prefs[NAME_KEY] ?: "Usuário Myato",
            email = prefs[EMAIL_KEY] ?: "",
            photoUrl = prefs[PHOTO_KEY]
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[ID_KEY] = profile.id
            prefs[NAME_KEY] = profile.name
            prefs[EMAIL_KEY] = profile.email
            profile.photoUrl?.let { prefs[PHOTO_KEY] = it }
        }
    }

    suspend fun clearProfile() {
        context.dataStore.edit { it.clear() }
    }
}
