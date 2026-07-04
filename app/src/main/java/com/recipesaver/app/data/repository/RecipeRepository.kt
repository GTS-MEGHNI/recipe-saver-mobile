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
 * dispatcher (the ViewModel does, via `viewModelScope`). Modeled as an interface so the ViewModel
 * can be unit-tested against a fake without any Android/network dependencies.
 */
interface RecipeRepository {
    suspend fun getAllRecipes(): List<Recipe>

    suspend fun getRecipe(id: Long): Recipe

    suspend fun addRecipe(
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        cookTimeMinutes: Int?,
        category: RecipeCategory?,
    ): Recipe

    suspend fun updateRecipe(
        id: Long,
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        cookTimeMinutes: Int?,
        category: RecipeCategory?,
    ): Recipe

    suspend fun deleteRecipe(id: Long)

    /** Flips a recipe's favorite flag on the server and returns the updated recipe. */
    suspend fun setFavorite(
        recipe: Recipe,
        favorite: Boolean,
    ): Recipe

    suspend fun addImage(
        recipeId: Long,
        uri: Uri,
    )

    suspend fun deleteImage(
        recipeId: Long,
        imageId: Long,
    )

    suspend fun setCover(
        recipeId: Long,
        uri: Uri,
    )

    suspend fun deleteCover(recipeId: Long)
}

/** Live implementation talking to the owner-run API. */
class DefaultRecipeRepository(
    private val api: RecipeApiService,
    private val imageUploader: ImageUploader,
) : RecipeRepository {
    override suspend fun getAllRecipes(): List<Recipe> = api.listRecipes().data.map { it.toDomain() }

    override suspend fun getRecipe(id: Long): Recipe = api.getRecipe(id).data.toDomain()

    override suspend fun addRecipe(
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

    override suspend fun updateRecipe(
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

    override suspend fun deleteRecipe(id: Long) {
        api.deleteRecipe(id)
    }

    /**
     * Persists a favorite toggle. Sends the recipe's current fields alongside the new [favorite]
     * flag (the API updates via a full recipe body); the server only touches `is_favorite` because
     * the other fields are unchanged.
     */
    override suspend fun setFavorite(
        recipe: Recipe,
        favorite: Boolean,
    ): Recipe =
        api.updateRecipe(
            recipe.id,
            RecipeRequestDto(
                title = recipe.title,
                ingredients = recipe.ingredients,
                steps = recipe.steps,
                cookTimeMinutes = recipe.cookTimeMinutes,
                category = recipe.category?.name,
                isFavorite = favorite,
            ),
        ).data.toDomain()

    /** Uploads [uri] into the recipe's gallery. No-op if the image can't be read. */
    override suspend fun addImage(
        recipeId: Long,
        uri: Uri,
    ) {
        val part = imageUploader.buildPart(uri) ?: return
        api.addImage(recipeId, part)
    }

    override suspend fun deleteImage(
        recipeId: Long,
        imageId: Long,
    ) {
        api.deleteImage(recipeId, imageId)
    }

    /** Sets or replaces the recipe's cover photo. No-op if the image can't be read. */
    override suspend fun setCover(
        recipeId: Long,
        uri: Uri,
    ) {
        val part = imageUploader.buildPart(uri) ?: return
        api.uploadCover(recipeId, part)
    }

    override suspend fun deleteCover(recipeId: Long) {
        api.deleteCover(recipeId)
    }
}
