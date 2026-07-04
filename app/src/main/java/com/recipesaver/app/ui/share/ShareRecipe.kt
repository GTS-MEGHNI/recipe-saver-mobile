package com.recipesaver.app.ui.share

import android.content.Context
import android.content.Intent
import com.recipesaver.app.R
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.ui.components.labelRes

/**
 * Renders [recipe] as a formatted French plain-text block and hands it to the Android share sheet
 * (`ACTION_SEND`, `text/plain`). No backend, no images, no permissions — the recipient just gets a
 * readable copy in whatever app they pick. Photo sharing is a possible follow-up (needs a
 * FileProvider to expose internal-storage files as content URIs).
 */
fun shareRecipeAsText(
    context: Context,
    recipe: Recipe,
) {
    val text = buildRecipeShareText(context, recipe)
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, recipe.title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
    val chooser =
        Intent.createChooser(
            sendIntent,
            context.getString(R.string.share_recipe_chooser_title),
        )
    context.startActivity(chooser)
}

/** Builds the shared text so it reads naturally: title, meta line, ingredients, then numbered steps. */
private fun buildRecipeShareText(
    context: Context,
    recipe: Recipe,
): String =
    buildString {
        appendLine(recipe.title)

        val meta =
            buildList {
                recipe.cookTimeMinutes?.let {
                    add(context.getString(R.string.share_recipe_cook_time, it))
                }
                recipe.category?.let { add(context.getString(it.labelRes)) }
            }
        if (meta.isNotEmpty()) {
            appendLine(meta.joinToString(" · "))
        }

        if (recipe.ingredients.isNotEmpty()) {
            appendLine()
            appendLine(context.getString(R.string.section_ingredients))
            recipe.ingredients.forEach { appendLine("• $it") }
        }

        if (recipe.steps.isNotEmpty()) {
            appendLine()
            appendLine(context.getString(R.string.section_steps))
            recipe.steps.forEachIndexed { index, step ->
                appendLine("${index + 1}. $step")
            }
        }
    }.trim()
