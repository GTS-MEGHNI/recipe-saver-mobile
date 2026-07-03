package com.recipesaver.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.recipesaver.app.R
import com.recipesaver.app.data.local.entities.RecipeCategory

/** UI-layer mapping of a [RecipeCategory] to its French label resource. */
val RecipeCategory.labelRes: Int
    get() =
        when (this) {
            RecipeCategory.DRINKS -> R.string.category_drinks
            RecipeCategory.FOOD -> R.string.category_food
            RecipeCategory.PASTRY -> R.string.category_pastry
            RecipeCategory.VERRINE -> R.string.category_verrine
            RecipeCategory.CAKE -> R.string.category_cake
            RecipeCategory.CONFITURE -> R.string.category_confiture
        }

/**
 * Drawable resource name for a category's cover photo, resolved at runtime by name (see the
 * category screen) so the app compiles before the images are added. Drop a matching file into
 * `res/drawable/` — e.g. `category_drinks.jpg`; a monogram placeholder shows until then.
 */
val RecipeCategory.coverDrawableName: String
    get() =
        when (this) {
            RecipeCategory.DRINKS -> "category_drink"
            RecipeCategory.FOOD -> "category_food"
            RecipeCategory.PASTRY -> "category_pastry"
            RecipeCategory.VERRINE -> "category_verrine"
            RecipeCategory.CAKE -> "category_cake"
            RecipeCategory.CONFITURE -> "category_confiture"
        }

/** UI-layer mapping of a [RecipeCategory] to its icon. */
val RecipeCategory.icon: ImageVector
    get() =
        when (this) {
            RecipeCategory.DRINKS -> Icons.Filled.LocalDrink
            RecipeCategory.FOOD -> Icons.Filled.Restaurant
            RecipeCategory.PASTRY -> Icons.Filled.BakeryDining
            RecipeCategory.VERRINE -> Icons.Filled.Icecream
            RecipeCategory.CAKE -> Icons.Filled.Cake
            RecipeCategory.CONFITURE -> Icons.Filled.BreakfastDining
        }
