package com.recipesaver.app.data.repository

import android.net.Uri
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeCategory
import com.recipesaver.app.data.remote.ImageUploader
import com.recipesaver.app.data.remote.RecipeApiService
import com.recipesaver.app.data.remote.dto.RecipeRequestDto
import com.recipesaver.app.data.remote.dto.toDomain

/**
 * Single source of truth for recipe data, backed entirely by the owner-run HTTP API (no local
 * database — see architecture.md §12). Hides the Retrofit service and image-upload details from the
 * ViewModel/UI, so the layers above stay unaware of how data is stored or fetched.
 *
 * Every method is a `suspend` call that hits the network; callers run them on a background
 * dispatcher (the ViewModel does, via `viewModelScope`).
 */
class RecipeRepository(
    private val api: RecipeApiService,
    private val imageUploader: ImageUploader,
) {
    suspend fun getAllRecipes(): List<Recipe> = api.listRecipes().data.map { it.toDomain() }

    suspend fun getRecipe(id: Long): Recipe = api.getRecipe(id).data.toDomain()

    suspend fun addRecipe(
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        cookTimeMinutes: Int?,
        category: RecipeCategory?,
    ): Recipe =
        api.createRecipe(
            RecipeRequestDto(
                title = title,
                ingredients = ingredients,
                steps = steps,
                cookTimeMinutes = cookTimeMinutes,
                category = category?.name,
            ),
        ).data.toDomain()

    suspend fun updateRecipe(
        id: Long,
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        cookTimeMinutes: Int?,
        category: RecipeCategory?,
    ): Recipe =
        api.updateRecipe(
            id,
            RecipeRequestDto(
                title = title,
                ingredients = ingredients,
                steps = steps,
                cookTimeMinutes = cookTimeMinutes,
                category = category?.name,
            ),
        ).data.toDomain()

    suspend fun deleteRecipe(id: Long) {
        api.deleteRecipe(id)
    }

    /** Uploads [uri] into the recipe's gallery. No-op if the image can't be read. */
    suspend fun addImage(
        recipeId: Long,
        uri: Uri,
    ) {
        val part = imageUploader.buildPart(uri) ?: return
        api.addImage(recipeId, part)
    }

    suspend fun deleteImage(
        recipeId: Long,
        imageId: Long,
    ) {
        api.deleteImage(recipeId, imageId)
    }

    /** Sets or replaces the recipe's cover photo. No-op if the image can't be read. */
    suspend fun setCover(
        recipeId: Long,
        uri: Uri,
    ) {
        val part = imageUploader.buildPart(uri) ?: return
        api.uploadCover(recipeId, part)
    }

    suspend fun deleteCover(recipeId: Long) {
        api.deleteCover(recipeId)
    }
}
