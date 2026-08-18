package com.example.next.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.next.data.session.Owner
import com.example.next.data.session.SessionManager
import com.example.next.data.session.ownerNow
import com.example.next.data.sync.RemoteSync
import com.example.next.models.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "next_settings")

/**
 * App settings backed by Jetpack DataStore (replaces the old SharedPreferences
 * based [com.example.next.ui.theme.ThemePreferences]). Exposes the current
 * [ThemeMode] as a Flow so the UI reacts to changes reactively.
 *
 * Sync: user-initiated changes are pushed to `users/{uid}/settings/settings`
 * (LWW on Firestore); cloud values are applied on owner activation
 * (pull-on-resume, cloud wins — see SyncCoordinator). The merge phase never
 * uploads guest settings. Pushes are best-effort: offline, the local value
 * simply stays local until the next successful push.
 */
class SettingsRepository(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val remoteSync: RemoteSync
) {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        prefs[themeModeKey]
            ?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
        val owner = sessionManager.ownerNow()
        if (Owner.isUser(owner)) {
            remoteSync.pushSettings(owner, mode)
        }
    }

    /**
     * Applies a theme coming FROM the cloud (pull-on-resume). Writes DataStore
     * directly so the value never echoes back to Firestore through the push
     * path (the push only happens on user-initiated changes).
     */
    suspend fun applyRemoteThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}