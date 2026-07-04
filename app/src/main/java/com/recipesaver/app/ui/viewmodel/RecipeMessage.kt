package com.recipesaver.app.ui.viewmodel

/**
 * One-shot user-facing outcome of a mutation, emitted by [RecipeViewModel] after each API call so
 * the UI can surface a success/error toast. Kept as semantic cases (not raw strings) so the French
 * copy lives in string resources on the UI side, keeping the ViewModel free of Android resources.
 */
sealed interface RecipeMessage {
    val isError: Boolean

    data object RecipeSaved : RecipeMessage {
        override val isError = false
    }

    data object RecipeDeleted : RecipeMessage {
        override val isError = false
    }

    data object ImageAdded : RecipeMessage {
        override val isError = false
    }

    data object ImageDeleted : RecipeMessage {
        override val isError = false
    }

    data object CoverUpdated : RecipeMessage {
        override val isError = false
    }

    data object SaveFailed : RecipeMessage {
        override val isError = true
    }

    data object DeleteFailed : RecipeMessage {
        override val isError = true
    }

    data object ImageFailed : RecipeMessage {
        override val isError = true
    }

    data object CoverFailed : RecipeMessage {
        override val isError = true
    }
}
