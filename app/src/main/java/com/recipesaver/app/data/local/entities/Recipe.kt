package com.recipesaver.app.data.local.entities

/**
 * A recipe as the app consumes it. This is a plain domain model built from the API's `RecipeResource`
 * (the app is API-backed with no local database — see architecture.md §12). Image fields hold
 * Coil-loadable HTTP URLs served by the backend.
 */
data class Recipe(
    val id: Long = 0,
    val title: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val cookTimeMinutes: Int? = null,
    val category: RecipeCategory? = null,
    /** Coil-loadable URL for the cover photo, or null to render the monogram fallback. */
    val coverImageUri: String? = null,
    /** The recipe's gallery photos, ordered by position. Empty when none. */
    val images: List<RecipeImage> = emptyList(),
    val createdAt: Long = 0,
)
