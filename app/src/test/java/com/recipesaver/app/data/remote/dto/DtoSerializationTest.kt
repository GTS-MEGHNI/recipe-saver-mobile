package com.recipesaver.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the wire contract with the Laravel API: responses arrive wrapped in a `data` envelope
 * with camelCase keys and possibly-unknown extra keys, and request bodies omit null/optional fields.
 * Mirrors the `Json` config used in NetworkModule.
 */
class DtoSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Test
    fun `decodes the data envelope with camelCase keys and ignores unknown fields`() {
        val body =
            """
            {
              "data": {
                "id": 3,
                "title": "Limonade",
                "ingredients": ["citron", "eau"],
                "steps": ["presser", "mélanger"],
                "cookTimeMinutes": 5,
                "category": "DRINKS",
                "coverImageUrl": "http://host/storage/c.jpg",
                "images": [{"id": 9, "url": "http://host/storage/i.jpg", "position": 0}],
                "createdAt": 1000,
                "extraServerFieldWeIgnore": true
              }
            }
            """.trimIndent()

        val recipe = json.decodeFromString<ApiData<RecipeDto>>(body).data

        assertEquals(3L, recipe.id)
        assertEquals("Limonade", recipe.title)
        assertEquals(5, recipe.cookTimeMinutes)
        assertEquals("DRINKS", recipe.category)
        assertEquals(1, recipe.images.size)
        assertEquals(9L, recipe.images[0].id)
    }

    @Test
    fun `missing optional fields fall back to defaults`() {
        val recipe = json.decodeFromString<ApiData<RecipeDto>>("""{"data":{"id":1,"title":"X"}}""").data

        assertEquals(emptyList<String>(), recipe.ingredients)
        assertEquals(emptyList<String>(), recipe.steps)
        assertEquals(null, recipe.cookTimeMinutes)
        assertEquals(emptyList<RecipeImageDto>(), recipe.images)
    }

    @Test
    fun `request body includes the category enum name`() {
        val out =
            json.encodeToString(
                RecipeRequestDto.serializer(),
                RecipeRequestDto(
                    title = "Cake",
                    ingredients = listOf("farine"),
                    steps = listOf("mélanger"),
                    cookTimeMinutes = 30,
                    category = "CAKE",
                ),
            )

        assertTrue(out.contains("\"title\":\"Cake\""))
        assertTrue(out.contains("\"category\":\"CAKE\""))
        assertTrue(out.contains("\"cookTimeMinutes\":30"))
    }

    @Test
    fun `request body omits null optional fields`() {
        val out =
            json.encodeToString(
                RecipeRequestDto.serializer(),
                RecipeRequestDto(
                    title = "Cake",
                    ingredients = listOf("farine"),
                    steps = listOf("mélanger"),
                    cookTimeMinutes = null,
                    category = null,
                ),
            )

        assertFalse(out.contains("cookTimeMinutes"))
        assertFalse(out.contains("category"))
    }
}
