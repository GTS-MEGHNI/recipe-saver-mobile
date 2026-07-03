package com.recipesaver.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val cookTimeMinutes: Int? = null,
    val category: RecipeCategory? = null,
    /** Coil-loadable URI for the cover photo (a `file://` path in internal storage, or an
     *  `android.resource://` URI for bundled sample images). Null renders a monogram fallback. */
    val coverImageUri: String? = null,
    val createdAt: Long,
)
