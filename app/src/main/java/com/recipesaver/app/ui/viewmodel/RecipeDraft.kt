package com.recipesaver.app.ui.viewmodel

import com.recipesaver.app.data.local.entities.RecipeCategory

/**
 * In-progress "new recipe" form state, held by [RecipeViewModel] so it survives leaving and
 * re-entering the add screen within a session (the screen's own state is destroyed when its
 * back-stack entry is popped). Fields are kept as the raw text the form edits — ingredients/steps
 * are one-item-per-line strings, split into lists only on save. Reset once a recipe is saved.
 */
data class RecipeDraft(
    val title: String = "",
    val cookTime: String = "",
    val ingredients: String = "",
    val steps: String = "",
    val category: RecipeCategory? = null,
) {
    /** True when nothing has been entered yet, so the add screen can apply its default category. */
    val isEmpty: Boolean
        get() =
            title.isEmpty() &&
                cookTime.isEmpty() &&
                ingredients.isEmpty() &&
                steps.isEmpty() &&
                category == null
}
