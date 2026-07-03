package com.recipesaver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.recipesaver.app.data.preferences.ThemePreferences
import com.recipesaver.app.ui.theme.AppTheme
import com.recipesaver.app.ui.theme.DarkMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes the selected [AppTheme] as a [StateFlow] and persists changes via [ThemePreferences]. */
class ThemeViewModel(
    private val preferences: ThemePreferences,
) : ViewModel() {
    val theme: StateFlow<AppTheme> =
        preferences.theme.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppTheme.NEUTRAL,
        )

    val darkMode: StateFlow<DarkMode> =
        preferences.darkMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DarkMode.SYSTEM,
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { preferences.setTheme(theme) }
    }

    fun setDarkMode(darkMode: DarkMode) {
        viewModelScope.launch { preferences.setDarkMode(darkMode) }
    }

    class Factory(
        private val preferences: ThemePreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ThemeViewModel(preferences) as T
    }
}
