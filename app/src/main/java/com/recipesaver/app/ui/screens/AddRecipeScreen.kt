package com.recipesaver.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    onSave: (
        title: String,
        ingredients: String,
        steps: String,
        cookTimeMinutes: Int?,
        servings: Int?,
        tags: String?,
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var cookTimeMinutes by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_recipe_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.field_title)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text(stringResource(R.string.field_ingredients)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it },
                label = { Text(stringResource(R.string.field_steps)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = cookTimeMinutes,
                onValueChange = { cookTimeMinutes = it.filter { char -> char.isDigit() } },
                label = { Text(stringResource(R.string.field_cook_time)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = servings,
                onValueChange = { servings = it.filter { char -> char.isDigit() } },
                label = { Text(stringResource(R.string.field_servings)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text(stringResource(R.string.field_tags)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onSave(
                        title,
                        ingredients,
                        steps,
                        cookTimeMinutes.toIntOrNull(),
                        servings.toIntOrNull(),
                        tags.ifBlank { null },
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
