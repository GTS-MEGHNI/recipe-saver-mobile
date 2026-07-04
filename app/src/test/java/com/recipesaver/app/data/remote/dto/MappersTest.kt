package com.recipesaver.app.data.remote.dto

import com.recipesaver.app.BuildConfig
import com.recipesaver.app.data.local.entities.RecipeCategory
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for the remote-DTO → domain mapping in Mappers.kt. */
class MappersTest {
    private val base = BuildConfig.API_BASE_URL.toHttpUrl()

    private fun dto(
        category: String? = null,
        coverImageUrl: String? = null,
        images: List<RecipeImageDto> = emptyList(),
    ) = RecipeDto(
        id = 7,
        title = "Tarte aux pommes",
        ingredients = listOf("pommes", "sucre"),
        steps = listOf("éplucher", "cuire"),
        cookTimeMinutes = 45,
        category = category,
        coverImageUrl = coverImageUrl,
        images = images,
        createdAt = 123L,
    )

    @Test
    fun `maps scalar fields straight through`() {
        val recipe = dto().toDomain()

        assertEquals(7L, recipe.id)
        assertEquals("Tarte aux pommes", recipe.title)
        assertEquals(listOf("pommes", "sucre"), recipe.ingredients)
        assertEquals(listOf("éplucher", "cuire"), recipe.steps)
        assertEquals(45, recipe.cookTimeMinutes)
        assertEquals(123L, recipe.createdAt)
    }

    @Test
    fun `valid category name maps to the enum`() {
        assertEquals(RecipeCategory.PASTRY, dto(category = "PASTRY").toDomain().category)
    }

    @Test
    fun `unknown category name maps to null instead of crashing`() {
        assertNull(dto(category = "NOT_A_CATEGORY").toDomain().category)
    }

    @Test
    fun `null category stays null`() {
        assertNull(dto(category = null).toDomain().category)
    }

    @Test
    fun `null cover url yields null cover uri`() {
        assertNull(dto(coverImageUrl = null).toDomain().coverImageUri)
    }

    @Test
    fun `cover url is rewritten to the configured api authority`() {
        val recipe = dto(coverImageUrl = "http://localhost:9999/storage/recipes/7/cover/a.jpg").toDomain()

        val out = recipe.coverImageUri!!.toHttpUrl()
        assertEquals(base.scheme, out.scheme)
        assertEquals(base.host, out.host)
        assertEquals(base.port, out.port)
        // Path (the part that actually locates the file) must be preserved.
        assertEquals("/storage/recipes/7/cover/a.jpg", out.encodedPath)
    }

    @Test
    fun `gallery images carry the parent recipe id and preserve position`() {
        val recipe =
            dto(
                images =
                    listOf(
                        RecipeImageDto(id = 1, url = "http://localhost:9999/storage/x.jpg", position = 0),
                        RecipeImageDto(id = 2, url = "http://localhost:9999/storage/y.jpg", position = 1),
                    ),
            ).toDomain()

        assertEquals(2, recipe.images.size)
        assertEquals(7L, recipe.images[0].recipeId)
        assertEquals(0, recipe.images[0].position)
        assertEquals(1, recipe.images[1].position)
        assertEquals("/storage/x.jpg", recipe.images[0].filePath.toHttpUrl().encodedPath)
    }
}
