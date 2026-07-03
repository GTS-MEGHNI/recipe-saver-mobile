package com.recipesaver.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.recipesaver.app.ui.theme.AppTheme
import com.recipesaver.app.ui.theme.DarkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persists the selected [AppTheme] in DataStore (Preferences) — a UI-only setting that does not
 * belong in Room (architecture.md §9.2). Stored as the enum's name; unknown/missing values fall
 * back to [AppTheme.NEUTRAL], the recommended default.
 */
class ThemePreferences(
    private val context: Context,
) {
    private val themeKey = stringPreferencesKey("app_theme")
    private val darkModeKey = stringPreferencesKey("dark_mode")

    val theme: Flow<AppTheme> =
        context.dataStore.data.map { prefs ->
            prefs[themeKey]?.let { name -> runCatching { AppTheme.valueOf(name) }.getOrNull() }
                ?: AppTheme.NEUTRAL
        }

    /** Light/dark preference; unknown/missing values fall back to [DarkMode.SYSTEM]. */
    val darkMode: Flow<DarkMode> =
        context.dataStore.data.map { prefs ->
            prefs[darkModeKey]?.let { name -> runCatching { DarkMode.valueOf(name) }.getOrNull() }
                ?: DarkMode.SYSTEM
        }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs -> prefs[themeKey] = theme.name }
    }

    suspend fun setDarkMode(darkMode: DarkMode) {
        context.dataStore.edit { prefs -> prefs[darkModeKey] = darkMode.name }
    }
}
