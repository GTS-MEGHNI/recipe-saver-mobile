package com.recipesaver.app.data.local.entities

/**
 * One photo in a recipe's gallery. Built from the API's `RecipeImageResource`; [filePath] holds the
 * backend HTTP URL Coil loads (the name is kept for source compatibility with the UI, which still
 * calls it `filePath`). [recipeId] is filled in from the parent recipe when mapping.
 */
data class RecipeImage(
    val id: Long = 0,
    val recipeId: Long,
    /** Coil-loadable URL pointing at the image served by the backend. */
    val filePath: String,
    /** Ordering within the gallery. */
    val position: Int,
)
