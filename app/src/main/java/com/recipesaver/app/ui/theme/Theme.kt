package com.recipesaver.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Brick,
    onPrimary = BrickOn,
    primaryContainer = PeachContainer,
    onPrimaryContainer = PeachOnContainer,
    secondary = Stone,
    onSecondary = BrickOn,
    secondaryContainer = StoneContainer,
    onSecondaryContainer = StoneOnContainer,
    tertiary = Amber,
    onTertiary = BrickOn,
    background = Cream,
    onBackground = Ink,
    surface = CreamSurface,
    onSurface = Ink,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = Hairline,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrickLight,
    onPrimary = BrickDarkOn,
    primaryContainer = PeachContainerDark,
    onPrimaryContainer = PeachOnContainerDark,
    secondary = StoneLight,
    onSecondary = BrickDarkOn,
    secondaryContainer = StoneContainerDark,
    onSecondaryContainer = Parchment,
    tertiary = AmberLight,
    onTertiary = BrickDarkOn,
    background = Charcoal,
    onBackground = Parchment,
    surface = CharcoalSurface,
    onSurface = Parchment,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = ParchmentMuted,
    outline = ParchmentMuted,
)

private val CoralLightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = CoralOnPrimary,
    primaryContainer = CoralPrimaryContainer,
    onPrimaryContainer = CoralOnPrimaryContainer,
    secondary = CoralSecondary,
    onSecondary = CoralOnSecondary,
    secondaryContainer = CoralSecondaryContainer,
    onSecondaryContainer = CoralOnSecondaryContainer,
    background = CoralBackground,
    onBackground = CoralOnBackground,
    surface = CoralSurface,
    onSurface = CoralOnBackground,
    surfaceVariant = CoralSurfaceVariant,
    onSurfaceVariant = CoralOnSurfaceVariant,
    outline = CoralOutline,
)

private val CoralDarkColorScheme = darkColorScheme(
    primary = CoralPrimaryDark,
    onPrimary = CoralOnPrimaryDark,
    primaryContainer = CoralPrimaryContainerDark,
    onPrimaryContainer = CoralOnPrimaryContainerDark,
    secondary = CoralSecondaryDark,
    onSecondary = CoralOnSecondaryDark,
    secondaryContainer = CoralSecondaryContainerDark,
    onSecondaryContainer = CoralOnSecondaryContainerDark,
    background = CoralBackgroundDark,
    onBackground = CoralOnBackgroundDark,
    surface = CoralSurfaceDark,
    onSurface = CoralOnBackgroundDark,
    surfaceVariant = CoralSurfaceVariantDark,
    onSurfaceVariant = CoralOnSurfaceVariantDark,
    outline = CoralOutlineDark,
)

private val SageLightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageSecondaryContainer,
    onSecondaryContainer = SageOnSecondaryContainer,
    background = SageBackground,
    onBackground = SageOnBackground,
    surface = SageSurface,
    onSurface = SageOnBackground,
    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = SageOnSurfaceVariant,
    outline = SageOutline,
)

private val SageDarkColorScheme = darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = SageOnPrimaryDark,
    primaryContainer = SagePrimaryContainerDark,
    onPrimaryContainer = SageOnPrimaryContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = SageOnSecondaryDark,
    secondaryContainer = SageSecondaryContainerDark,
    onSecondaryContainer = SageOnSecondaryContainerDark,
    background = SageBackgroundDark,
    onBackground = SageOnBackgroundDark,
    surface = SageSurfaceDark,
    onSurface = SageOnBackgroundDark,
    surfaceVariant = SageSurfaceVariantDark,
    onSurfaceVariant = SageOnSurfaceVariantDark,
    outline = SageOutlineDark,
)

@Composable
fun RecipeSaverTheme(
    appTheme: AppTheme = AppTheme.NEUTRAL,
    darkMode: DarkMode = DarkMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }

    // Deliberately no dynamic color: the warm cookbook palettes are the brand and must not be
    // overridden by the device wallpaper's Material You scheme.
    val colorScheme = when (appTheme) {
        AppTheme.CORAL -> if (darkTheme) CoralDarkColorScheme else CoralLightColorScheme
        AppTheme.SAGE -> if (darkTheme) SageDarkColorScheme else SageLightColorScheme
        AppTheme.NEUTRAL -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
