package com.recipesaver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.recipesaver.app.data.local.entities.Recipe
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

    fun addRecipe(
        title: String,
        ingredients: String,
        steps: String,
        cookTimeMinutes: Int?,
        servings: Int?,
        tags: String?,
    ) {
        viewModelScope.launch {
            repository.addRecipe(
                Recipe(
                    title = title,
                    ingredients = ingredients,
                    steps = steps,
                    cookTimeMinutes = cookTimeMinutes,
                    servings = servings,
                    tags = tags,
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
