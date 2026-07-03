package com.recipesaver.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeCategory
import com.recipesaver.app.data.local.entities.RecipeImage
import com.recipesaver.app.data.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val repository: RecipeRepository,
) : ViewModel() {
    val recipes: StateFlow<List<Recipe>> =
        repository.getAllRecipes().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun getRecipe(id: Long): StateFlow<Recipe?> =
        repository.getRecipe(id).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun getImages(recipeId: Long): StateFlow<List<RecipeImage>> =
        repository.getImages(recipeId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun addImages(
        recipeId: Long,
        uris: List<Uri>,
    ) {
        viewModelScope.launch {
            uris.forEachIndexed { index, uri -> repository.addImage(recipeId, uri, index) }
        }
    }

    fun deleteImage(image: RecipeImage) {
        viewModelScope.launch { repository.deleteImage(image) }
    }

    fun addRecipe(
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        cookTimeMinutes: Int?,
        category: RecipeCategory?,
    ) {
        viewModelScope.launch {
            repository.addRecipe(
                Recipe(
                    title = title,
                    ingredients = ingredients,
                    steps = steps,
                    cookTimeMinutes = cookTimeMinutes,
                    category = category,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch { repository.deleteRecipe(recipe) }
    }

    class Factory(
        private val repository: RecipeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecipeViewModel(repository) as T
    }
}
