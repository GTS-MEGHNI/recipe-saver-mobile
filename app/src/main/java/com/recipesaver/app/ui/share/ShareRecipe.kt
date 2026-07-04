package com.recipesaver.app.ui.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.recipesaver.app.R
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.ui.components.labelRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

private val shareClient by lazy { OkHttpClient() }

/**
 * Renders [recipe] as a formatted French plain-text block and hands it to the Android share sheet
 * (`ACTION_SEND`, `text/plain`). The recipient gets a readable copy in whatever app they pick.
 */
fun shareRecipeAsText(
    context: Context,
    recipe: Recipe,
) {
    startTextShare(context, subject = recipe.title, text = buildRecipeShareText(context, recipe))
}

/**
 * Shares just the ingredient list as a shopping list — for texting "voici ce qu'il faut acheter" to
 * whoever does the shopping. Plain text, so it drops straight into WhatsApp/SMS/etc.
 */
fun shareShoppingList(
    context: Context,
    recipe: Recipe,
) {
    val subject = context.getString(R.string.shopping_list_subject, recipe.title)
    startTextShare(context, subject = subject, text = buildShoppingListText(context, recipe))
}

/**
 * Shares the recipe as its cover photo plus the full text. Downloads the (remote) cover into a cache
 * file, exposes it through the app's [FileProvider], and sends both via `ACTION_SEND`. Falls back to
 * a text-only share if the recipe has no cover or the download fails. Must be called from a
 * coroutine; the download runs on [Dispatchers.IO].
 */
suspend fun shareRecipeWithPhoto(
    context: Context,
    recipe: Recipe,
) {
    val coverUrl = recipe.coverImageUri
    val file = if (coverUrl != null) downloadCover(context, coverUrl) else null
    if (file == null) {
        shareRecipeAsText(context, recipe)
        return
    }

    val uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, recipe.title)
            putExtra(Intent.EXTRA_TEXT, buildRecipeShareText(context, recipe))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    startChooser(context, sendIntent)
}

/** Downloads [url] into a stable cache file, returning null on any failure. */
private suspend fun downloadCover(
    context: Context,
    url: String,
): File? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(dir, "recipe_cover.jpg")
            shareClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body ?: return@runCatching null
                file.outputStream().use { out -> body.byteStream().copyTo(out) }
            }
            file
        }.getOrNull()
    }

private fun startTextShare(
    context: Context,
    subject: String,
    text: String,
) {
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
    startChooser(context, sendIntent)
}

private fun startChooser(
    context: Context,
    sendIntent: Intent,
) {
    val chooser =
        Intent.createChooser(sendIntent, context.getString(R.string.share_recipe_chooser_title))
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

/** Builds a shopping-list message: a heading naming the recipe, then the bulleted ingredients. */
private fun buildShoppingListText(
    context: Context,
    recipe: Recipe,
): String =
    buildString {
        appendLine(context.getString(R.string.shopping_list_heading, recipe.title))
        appendLine()
        recipe.ingredients.forEach { appendLine("• $it") }
    }.trim()
