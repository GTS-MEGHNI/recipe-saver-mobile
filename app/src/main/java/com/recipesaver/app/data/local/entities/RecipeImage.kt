package com.recipesaver.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One photo in a recipe's gallery (one-to-many: many images per [Recipe]). Only the *file path* to
 * the image binary in internal storage is stored here — never the image blob itself. Deleting a
 * recipe cascades to its images so no rows are orphaned.
 */
@Entity(
    tableName = "recipe_images",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class RecipeImage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: Long,
    /** Coil-loadable `file://` URI pointing at the downsampled JPEG in `filesDir`. */
    val filePath: String,
    /** Ordering within the gallery (0-based). */
    val position: Int,
)
