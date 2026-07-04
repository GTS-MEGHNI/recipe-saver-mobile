package com.recipesaver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.recipesaver.app.R
import com.recipesaver.app.data.local.entities.Recipe

/**
 * Photo card summarising a [recipe]: a cover image (or monogram fallback) with the title and
 * metadata pills overlaid at the bottom, and a favorite heart the user can tap in the top-right
 * corner. Shared by the category list, search results, and the favorites list so they stay visually
 * identical.
 */
@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f),
        ) {
            val coverModel = rememberCoverModel(recipe.coverImageUri)
            if (coverModel != null) {
                AsyncImage(
                    model = coverModel,
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                RecipeImagePlaceholder(
                    title = recipe.title,
                    seed = recipe.id,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Scrim so the overlaid title stays legible over any photo.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.45f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.65f),
                            ),
                        ),
                    ),
            )

            FavoriteButton(
                isFavorite = recipe.isFavorite,
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recipe.cookTimeMinutes?.let { minutes ->
                        MetaChip(
                            icon = Icons.Filled.Schedule,
                            label = stringResource(R.string.label_cook_time_short, minutes),
                        )
                    }
                    recipe.category?.let { category ->
                        MetaChip(
                            icon = category.icon,
                            label = stringResource(category.labelRes),
                        )
                    }
                }
            }
        }
    }
}

/** Translucent circular heart toggle overlaid on the cover; filled when the recipe is a favorite. */
@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.32f),
        modifier = modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription =
                    stringResource(
                        if (isFavorite) {
                            R.string.content_description_unfavorite
                        } else {
                            R.string.content_description_favorite
                        },
                    ),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Resolves a stored cover URI into a model Coil can load. `android.resource://…/drawable/<name>`
 * URIs (bundled sample photos) are resolved to their Int resource id, which loads reliably; any
 * other value (an HTTP URL from the API) is passed through as-is. Null when there's no cover, so the
 * caller shows the monogram fallback.
 */
@Composable
private fun rememberCoverModel(uri: String?): Any? {
    if (uri.isNullOrBlank()) return null
    val context = LocalContext.current
    return remember(uri) {
        if (uri.startsWith("android.resource://")) {
            val name = uri.substringAfterLast("/drawable/").substringBefore('/')
            val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (resId != 0) resId else null
        } else {
            uri
        }
    }
}

/** Small translucent pill with a leading icon, used for metadata overlaid on the cover photo. */
@Composable
private fun MetaChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
