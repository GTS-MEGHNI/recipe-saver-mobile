package com.recipesaver.app.data.local

import androidx.room.TypeConverter
import com.recipesaver.app.data.local.entities.RecipeCategory

/**
 * Persists `List<String>` fields (ingredients, steps) as a single newline-delimited TEXT column.
 * Each list element is one line item, so newline is a safe delimiter and no extra dependency
 * (JSON serializer) is needed. Blank entries are dropped on both directions.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(separator = "\n")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) {
            emptyList()
        } else {
            value.split("\n")
        }

    @TypeConverter
    fun fromCategory(category: RecipeCategory?): String? = category?.name

    /**
     * Reads the stored `category` TEXT back into an enum. Recognizes the current enum name first;
     * falls back to mapping legacy values that existing databases still hold — both the retired
     * `DESSERT` enum name and the even older free-text French labels (e.g. "Plat salé", "Dessert")
     * — so their chips survive the category changes. Retired desserts map to the closest current
     * category ([RecipeCategory.PASTRY]). Anything unknown → null.
     */
    @TypeConverter
    fun toCategory(value: String?): RecipeCategory? =
        value?.let { stored ->
            runCatching { RecipeCategory.valueOf(stored) }.getOrNull() ?: legacyCategory(stored)
        }

    private fun legacyCategory(value: String): RecipeCategory? =
        when {
            value.equals("DESSERT", ignoreCase = true) -> RecipeCategory.PASTRY
            value.startsWith("Plat", ignoreCase = true) -> RecipeCategory.FOOD
            value.startsWith("Boisson", ignoreCase = true) -> RecipeCategory.DRINKS
            else -> null
        }
}
