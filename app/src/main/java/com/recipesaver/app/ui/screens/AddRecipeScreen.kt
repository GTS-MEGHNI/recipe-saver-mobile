package com.recipesaver.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.recipesaver.app.R
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeCategory
import com.recipesaver.app.ui.components.icon
import com.recipesaver.app.ui.components.labelRes

/**
 * Form for creating or editing a recipe. Ingredients and steps are entered as free text, one item
 * per line, and split into lists on save (mirroring how [RecipeDetailScreen] renders them back).
 * Photos are not managed here — they are attached from the detail screen's gallery.
 *
 * When [existing] is null the form starts empty and [initialCategory] pre-selects the category the
 * user was browsing when they tapped "add". When [existing] is non-null the form is pre-filled with
 * that recipe's values for editing (and [initialCategory] is ignored). Save is disabled until a
 * non-blank title is entered.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRecipeScreen(
    initialCategory: RecipeCategory?,
    onSave: (
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        cookTimeMinutes: Int?,
        category: RecipeCategory?,
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    existing: Recipe? = null,
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var ingredients by remember { mutableStateOf(existing?.ingredients?.joinToString("\n").orEmpty()) }
    var steps by remember { mutableStateOf(existing?.steps?.joinToString("\n").orEmpty()) }
    var cookTime by remember { mutableStateOf(existing?.cookTimeMinutes?.toString().orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: initialCategory) }

    val canSave = title.isNotBlank()

    val submit = {
        if (canSave) {
            onSave(
                title.trim(),
                ingredients.toLines(),
                steps.toLines(),
                cookTime.trim().toIntOrNull(),
                category,
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (existing == null) R.string.add_recipe_title else R.string.edit_recipe_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = submit, enabled = canSave) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.content_description_save_recipe),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.field_title)) },
                placeholder = { Text(stringResource(R.string.hint_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            CategorySelector(
                selected = category,
                onSelect = { category = if (category == it) null else it },
            )

            OutlinedTextField(
                value = cookTime,
                onValueChange = { input -> cookTime = input.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.field_cook_time)) },
                placeholder = { Text(stringResource(R.string.hint_cook_time)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text(stringResource(R.string.field_ingredients)) },
                placeholder = { Text(stringResource(R.string.hint_one_per_line)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it },
                label = { Text(stringResource(R.string.field_steps)) },
                placeholder = { Text(stringResource(R.string.hint_one_per_line)) },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(
    selected: RecipeCategory?,
    onSelect: (RecipeCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.field_category),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeCategory.entries.forEach { entry ->
                FilterChip(
                    selected = selected == entry,
                    onClick = { onSelect(entry) },
                    label = { Text(stringResource(entry.labelRes)) },
                    leadingIcon = {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

/** Splits free-text field input into a list: one trimmed, non-blank line per item. */
private fun String.toLines(): List<String> =
    lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
