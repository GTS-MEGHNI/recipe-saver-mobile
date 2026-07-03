package com.recipesaver.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.recipesaver.app.R
import com.recipesaver.app.ui.theme.AppTheme
import com.recipesaver.app.ui.theme.Brick
import com.recipesaver.app.ui.theme.CoralBackground
import com.recipesaver.app.ui.theme.CoralPrimary
import com.recipesaver.app.ui.theme.CoralSecondary
import com.recipesaver.app.ui.theme.Cream
import com.recipesaver.app.ui.theme.DarkMode
import com.recipesaver.app.ui.theme.SageBackground
import com.recipesaver.app.ui.theme.SagePrimary
import com.recipesaver.app.ui.theme.SageSecondary
import com.recipesaver.app.ui.theme.Stone

/** Preview swatches + display name for one selectable theme, shown as a card in Settings. */
private data class ThemeOption(
    val theme: AppTheme,
    val nameRes: Int,
    val primary: Color,
    val secondary: Color,
    val background: Color,
)

private val themeOptions =
    listOf(
        ThemeOption(AppTheme.CORAL, R.string.theme_coral, CoralPrimary, CoralSecondary, CoralBackground),
        ThemeOption(AppTheme.SAGE, R.string.theme_sage, SagePrimary, SageSecondary, SageBackground),
        ThemeOption(AppTheme.NEUTRAL, R.string.theme_neutral, Brick, Stone, Cream),
    )

/** Icon + display name for one light/dark option, shown as a card in Settings. */
private data class ModeOption(
    val mode: DarkMode,
    val nameRes: Int,
    val icon: ImageVector,
)

private val modeOptions =
    listOf(
        ModeOption(DarkMode.SYSTEM, R.string.mode_system, Icons.Filled.BrightnessAuto),
        ModeOption(DarkMode.LIGHT, R.string.mode_light, Icons.Filled.LightMode),
        ModeOption(DarkMode.DARK, R.string.mode_dark, Icons.Filled.DarkMode),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    selectedDarkMode: DarkMode,
    onDarkModeSelected: (DarkMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "theme_header") {
                Text(
                    text = stringResource(R.string.settings_theme_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(themeOptions, key = { it.theme }) { option ->
                ThemeCard(
                    option = option,
                    selected = option.theme == selectedTheme,
                    onClick = { onThemeSelected(option.theme) },
                )
            }

            item(key = "display_header") {
                Text(
                    text = stringResource(R.string.settings_display_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            items(modeOptions, key = { it.mode }) { option ->
                ModeCard(
                    option = option,
                    selected = option.mode == selectedDarkMode,
                    onClick = { onDarkModeSelected(option.mode) },
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    option: ModeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(option.nameRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.content_description_theme_selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SwatchStrip(option)
            Text(
                text = stringResource(option.nameRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.content_description_theme_selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Three stacked color bars previewing a theme's primary / secondary / background. */
@Composable
private fun SwatchStrip(
    option: ThemeOption,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape),
    ) {
        listOf(option.primary, option.secondary, option.background).forEach { color ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(color),
            )
        }
    }
}
