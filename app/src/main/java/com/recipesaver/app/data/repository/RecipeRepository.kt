package com.recipesaver.app.data.repository

import android.net.Uri
import com.recipesaver.app.data.files.ImageStorageManager
import com.recipesaver.app.data.local.RecipeDao
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeImage
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for recipe data. Hides both the Room database and the image file storage
 * from the ViewModel/UI, so image binaries and their DB rows always stay in sync (a gallery photo
 * is a file on disk *and* a row) and no caller has to coordinate the two.
 */
class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val imageStorage: ImageStorageManager,
) {
    fun getAllRecipes(): Flow<List<Recipe>> = recipeDao.getAll()

    fun getRecipe(id: Long): Flow<Recipe?> = recipeDao.getById(id)

    suspend fun addRecipe(recipe: Recipe): Long = recipeDao.insert(recipe)

    suspend fun updateRecipe(recipe: Recipe) = recipeDao.update(recipe)

    /** Deletes the recipe and, first, the image files its cascade-deleted rows point at. */
    suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.getImagesForRecipeOnce(recipe.id).forEach { imageStorage.deleteImage(it.filePath) }
        recipeDao.delete(recipe)
    }

    fun getImages(recipeId: Long): Flow<List<RecipeImage>> = recipeDao.getImagesForRecipe(recipeId)

    /** Saves [uri] into internal storage and appends it to the recipe's gallery. No-op if unreadable. */
    suspend fun addImage(
        recipeId: Long,
        uri: Uri,
        uniqueSuffix: Int,
    ) {
        val filePath = imageStorage.saveImage(uri, recipeId, uniqueSuffix) ?: return
        val position = recipeDao.maxImagePosition(recipeId) + 1
        recipeDao.insertImage(RecipeImage(recipeId = recipeId, filePath = filePath, position = position))
    }

    /** Removes a gallery photo: its DB row and the file it points at. */
    suspend fun deleteImage(image: RecipeImage) {
        recipeDao.deleteImage(image)
        imageStorage.deleteImage(image.filePath)
    }
}
