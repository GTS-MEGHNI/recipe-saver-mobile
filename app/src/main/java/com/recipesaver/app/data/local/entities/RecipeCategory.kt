package com.recipesaver.app.data.local.entities

/**
 * Fixed set of recipe categories. Persisted by [com.recipesaver.app.data.local.Converters] as the
 * enum's `name` (TEXT), so the SQLite column type is unchanged from the earlier free-text
 * `category` — no schema migration is needed. French labels and icons live in the UI layer so this
 * stays a plain domain type.
 */
enum class RecipeCategory { DRINKS, FOOD, PASTRY, VERRINE, CAKE, CONFITURE }
