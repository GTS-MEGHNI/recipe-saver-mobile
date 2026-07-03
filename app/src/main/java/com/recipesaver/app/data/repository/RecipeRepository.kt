package com.recipesaver.app.data.repository

import com.recipesaver.app.data.local.RecipeDao
import com.recipesaver.app.data.local.entities.Recipe
import kotlinx.coroutines.flow.Flow

class RecipeRepository(
    private val recipeDao: RecipeDao,
) {
    fun getAllRecipes(): Flow<List<Recipe>> = recipeDao.getAll()

    fun getRecipe(id: Long): Flow<Recipe?> = recipeDao.getById(id)

    suspend fun addRecipe(recipe: Recipe): Long = recipeDao.insert(recipe)

    suspend fun updateRecipe(recipe: Recipe) = recipeDao.update(recipe)

    suspend fun deleteRecipe(recipe: Recipe) = recipeDao.delete(recipe)
}
