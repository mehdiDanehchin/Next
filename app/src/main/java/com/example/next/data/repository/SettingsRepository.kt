package com.example.next.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.next.models.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "next_settings")

/**
 * App settings backed by Jetpack DataStore (replaces the old SharedPreferences
 * based [com.example.next.ui.theme.ThemePreferences]). Exposes the current
 * [ThemeMode] as a Flow so the UI reacts to changes reactively.
 */
class SettingsRepository(private val context: Context) {

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
    }
}