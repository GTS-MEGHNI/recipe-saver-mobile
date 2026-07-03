package com.recipesaver.app.ui.theme

/**
 * User-selectable light/dark preference, persisted via DataStore alongside the [AppTheme]. [SYSTEM]
 * follows the device setting (`isSystemInDarkTheme`); [LIGHT]/[DARK] force that mode regardless.
 */
enum class DarkMode { SYSTEM, LIGHT, DARK }
