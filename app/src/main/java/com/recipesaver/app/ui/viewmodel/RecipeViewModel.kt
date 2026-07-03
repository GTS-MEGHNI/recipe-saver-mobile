package com.recipesaver.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeCategory
import com.recipesaver.app.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds UI state for recipes fetched from the API. Because the app is now pure-online (no local
 * database emitting Flows), the list and the open recipe are cached in [MutableStateFlow]s and
 * refreshed explicitly after every mutation. Network failures are swallowed into [errorMessage]
 * rather than crashing; the last good data stays on screen.
 */
class RecipeViewModel(
    private val repository: RecipeRepository,
) : ViewModel() {
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _detail = MutableStateFlow<Recipe?>(null)
    val detail: StateFlow<Recipe?> = _detail.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
    }

    /** Reloads the full recipe list from the server. */
    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { repository.getAllRecipes() }
                .onSuccess {
                    _recipes.value = it
                    _errorMessage.value = null
                }
                .onFailure { _errorMessage.value = it.message }
            _loading.value = false
        }
    }

    /** Loads a single recipe (with its gallery) into [detail]. Clears stale data while loading. */
    fun openRecipe(id: Long) {
        viewModelScope.launch {
            _detail.value = null
            runCatching { repository.getRecipe(id) }
                .onSuccess { _detail.value = it }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addImages(
        recipeId: Long,
        uris: List<Uri>,
    ) {
        viewModelScope.launch {
            runCatching {
                uris.forEach { repository.addImage(recipeId, it) }
            }.onFailure { _errorMessage.value = it.message }
            reloadDetail(recipeId)
        }
    }

    fun deleteImage(
        recipeId: Long,
        imageId: Long,
    ) {
        viewModelScope.launch {
            runCatching { repository.deleteImage(recipeId, imageId) }
                .onFailure { _errorMessage.value = it.message }
            reloadDetail(recipeId)
        }
    }

    fun setCover(
        recipeId: Long,
        uri: Uri,
    ) {
        viewModelScope.launch {
            runCatching { repository.setCover(recipeId, uri) }
                .onFailure { _errorMessage.value = it.message }
            reloadDetail(recipeId)
        }
    }

    fun addRecipe(
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        cookTimeMinutes: Int?,
        category: RecipeCategory?,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addRecipe(title, ingredients, steps, cookTimeMinutes, category)
            }.onSuccess { refresh() }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun updateRecipe(
        id: Long,
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        cookTimeMinutes: Int?,
        category: RecipeCategory?,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updateRecipe(id, title, ingredients, steps, cookTimeMinutes, category)
            }.onSuccess { reloadDetail(id) }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            runCatching { repository.deleteRecipe(recipe.id) }
                .onSuccess { refresh() }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    /** Re-fetches the open recipe and the list so gallery/cover changes show immediately. */
    private suspend fun reloadDetail(recipeId: Long) {
        runCatching { repository.getRecipe(recipeId) }
            .onSuccess { _detail.value = it }
            .onFailure { _errorMessage.value = it.message }
        runCatching { repository.getAllRecipes() }.onSuccess { _recipes.value = it }
    }

    class Factory(
        private val repository: RecipeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecipeViewModel(repository) as T
    }
}
