package com.recipesaver.app.data.remote.dto

import com.recipesaver.app.BuildConfig
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeCategory
import com.recipesaver.app.data.local.entities.RecipeImage
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The API builds absolute image URLs from its `APP_URL` (often `localhost:8000`), which is not
 * reachable from an emulator/device. Rewriting each URL's scheme/host/port to match the configured
 * API base authority makes the images load regardless of how the server's `APP_URL` is set.
 */
private val baseAuthority = BuildConfig.API_BASE_URL.toHttpUrlOrNull()

private fun normalizeImageUrl(raw: String?): String? {
    if (raw == null) return null
    val base = baseAuthority ?: return raw
    val url = raw.toHttpUrlOrNull() ?: return raw
    return url.newBuilder()
        .scheme(base.scheme)
        .host(base.host)
        .port(base.port)
        .build()
        .toString()
}

fun RecipeDto.toDomain(): Recipe =
    Recipe(
        id = id,
        title = title,
        ingredients = ingredients,
        steps = steps,
        cookTimeMinutes = cookTimeMinutes,
        category = category?.let { name -> runCatching { RecipeCategory.valueOf(name) }.getOrNull() },
        coverImageUri = normalizeImageUrl(coverImageUrl),
        images = images.map { it.toDomain(id) },
        createdAt = createdAt,
    )

fun RecipeImageDto.toDomain(recipeId: Long): RecipeImage =
    RecipeImage(
        id = id,
        recipeId = recipeId,
        filePath = normalizeImageUrl(url) ?: url,
        position = position,
    )
