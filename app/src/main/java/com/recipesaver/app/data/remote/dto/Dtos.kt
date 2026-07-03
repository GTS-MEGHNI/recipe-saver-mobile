package com.recipesaver.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Laravel API resources are wrapped in a top-level `data` key (both single resources and
 * collections), so every response body deserializes through this envelope.
 */
@Serializable
data class ApiData<T>(
    val data: T,
)

/** Wire shape of `RecipeResource` (camelCase keys mirror the Android domain model). */
@Serializable
data class RecipeDto(
    val id: Long,
    val title: String,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val cookTimeMinutes: Int? = null,
    val category: String? = null,
    val coverImageUrl: String? = null,
    val images: List<RecipeImageDto> = emptyList(),
    val createdAt: Long = 0,
)

/** Wire shape of `RecipeImageResource`. */
@Serializable
data class RecipeImageDto(
    val id: Long,
    val url: String,
    val position: Int,
)

/**
 * Request body for creating/updating a recipe. The server validates `ingredients`/`steps` as
 * non-empty arrays and `category` against its enum, so send the enum case name (e.g. "FOOD").
 */
@Serializable
data class RecipeRequestDto(
    val title: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val cookTimeMinutes: Int? = null,
    val category: String? = null,
)
