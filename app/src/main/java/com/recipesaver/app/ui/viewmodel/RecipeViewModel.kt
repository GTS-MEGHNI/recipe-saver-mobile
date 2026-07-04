package com.recipesaver.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeCategory
import com.recipesaver.app.data.repository.RecipeRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // Number of gallery images currently being uploaded, so the detail screen can show that many
    // skeleton tiles while the (possibly slow) uploads are in flight — before the images exist on
    // the server and can be fetched back.
    private val _pendingUploads = MutableStateFlow(0)
    val pendingUploads: StateFlow<Int> = _pendingUploads.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Draft of the new-recipe form, retained across navigation so a half-filled recipe isn't lost
    // when the user leaves the add screen and comes back. Cleared once a recipe is saved.
    private val _draft = MutableStateFlow(RecipeDraft())
    val draft: StateFlow<RecipeDraft> = _draft.asStateFlow()

    // Guards against firing duplicate create requests when the save button is tapped repeatedly.
    private var saveInProgress = false

    // One-shot outcomes for success/error toasts. Buffered + drop-oldest so emitting never suspends
    // even when no collector is attached (e.g. during a screen transition).
    private val _messages =
        MutableSharedFlow<RecipeMessage>(
            extraBufferCapacity = 4,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val messages: SharedFlow<RecipeMessage> = _messages.asSharedFlow()

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

    /** Records the latest new-recipe form state so it survives navigating away from the add screen. */
    fun updateDraft(draft: RecipeDraft) {
        _draft.value = draft
    }

    fun addImages(
        recipeId: Long,
        uris: List<Uri>,
    ) {
        viewModelScope.launch {
            _pendingUploads.value += uris.size
            var anyFailed = false
            uris.forEach { uri ->
                runCatching { repository.addImage(recipeId, uri) }
                    .onFailure {
                        anyFailed = true
                        _errorMessage.value = it.message
                    }
                // Decrement per image so tiles clear progressively, even if some uploads fail.
                _pendingUploads.value -= 1
            }
            _messages.tryEmit(if (anyFailed) RecipeMessage.ImageFailed else RecipeMessage.ImageAdded)
            reloadDetail(recipeId)
        }
    }

    fun deleteImage(
        recipeId: Long,
        imageId: Long,
    ) {
        viewModelScope.launch {
            runCatching { repository.deleteImage(recipeId, imageId) }
                .onSuccess { _messages.tryEmit(RecipeMessage.ImageDeleted) }
                .onFailure { fail(it, RecipeMessage.ImageFailed) }
            reloadDetail(recipeId)
        }
    }

    fun setCover(
        recipeId: Long,
        uri: Uri,
    ) {
        viewModelScope.launch {
            runCatching { repository.setCover(recipeId, uri) }
                .onSuccess { _messages.tryEmit(RecipeMessage.CoverUpdated) }
                .onFailure { fail(it, RecipeMessage.CoverFailed) }
            reloadDetail(recipeId)
        }
    }

    /**
     * Creates a recipe and, on success, invokes [onCreated] with its new id so the caller can
     * navigate straight to its detail screen.
     */
    fun addRecipe(
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        cookTimeMinutes: Int?,
        category: RecipeCategory?,
        onCreated: (Long) -> Unit = {},
    ) {
        // Ignore repeat taps while a create is already in flight, so an impatient double-tap can't
        // post the same recipe several times.
        if (saveInProgress) return
        saveInProgress = true
        viewModelScope.launch {
            runCatching {
                repository.addRecipe(title, ingredients, steps, cookTimeMinutes, category)
            }.onSuccess { created ->
                _draft.value = RecipeDraft()
                refresh()
                _messages.tryEmit(RecipeMessage.RecipeSaved)
                onCreated(created.id)
            }.onFailure { fail(it, RecipeMessage.SaveFailed) }
            saveInProgress = false
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
            }.onSuccess {
                reloadDetail(id)
                _messages.tryEmit(RecipeMessage.RecipeSaved)
            }.onFailure { fail(it, RecipeMessage.SaveFailed) }
        }
    }

    /**
     * Toggles [recipe]'s favorite flag. Flips the cached state immediately so the heart responds
     * instantly, then persists it; a network failure reverts the flip and surfaces an error toast.
     */
    fun toggleFavorite(recipe: Recipe) {
        val target = !recipe.isFavorite
        applyFavorite(recipe.id, target)
        viewModelScope.launch {
            runCatching { repository.setFavorite(recipe, target) }
                .onFailure {
                    applyFavorite(recipe.id, recipe.isFavorite)
                    fail(it, RecipeMessage.SaveFailed)
                }
        }
    }

    /** Sets the favorite flag on the cached list and open recipe without a network round-trip. */
    private fun applyFavorite(
        id: Long,
        favorite: Boolean,
    ) {
        _recipes.value = _recipes.value.map { if (it.id == id) it.copy(isFavorite = favorite) else it }
        _detail.value = _detail.value?.let { if (it.id == id) it.copy(isFavorite = favorite) else it }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            runCatching { repository.deleteRecipe(recipe.id) }
                .onSuccess {
                    refresh()
                    _messages.tryEmit(RecipeMessage.RecipeDeleted)
                }.onFailure { fail(it, RecipeMessage.DeleteFailed) }
        }
    }

    /** Records the error for inline use and emits [message] so the UI shows an error toast. */
    private fun fail(
        error: Throwable,
        message: RecipeMessage,
    ) {
        _errorMessage.value = error.message
        _messages.tryEmit(message)
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
