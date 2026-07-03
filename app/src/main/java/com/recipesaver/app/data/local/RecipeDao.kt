package com.recipesaver.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeImage
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt ASC")
    fun getAll(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun getById(id: Long): Flow<Recipe?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(recipe: Recipe): Long

    @Update
    suspend fun update(recipe: Recipe)

    @Delete
    suspend fun delete(recipe: Recipe)

    @Query("SELECT * FROM recipe_images WHERE recipeId = :recipeId ORDER BY position ASC")
    fun getImagesForRecipe(recipeId: Long): Flow<List<RecipeImage>>

    @Query("SELECT * FROM recipe_images WHERE recipeId = :recipeId ORDER BY position ASC")
    suspend fun getImagesForRecipeOnce(recipeId: Long): List<RecipeImage>

    /** Highest existing position for a recipe, or -1 when it has no images yet. */
    @Query("SELECT COALESCE(MAX(position), -1) FROM recipe_images WHERE recipeId = :recipeId")
    suspend fun maxImagePosition(recipeId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertImage(image: RecipeImage): Long

    @Delete
    suspend fun deleteImage(image: RecipeImage)
}
