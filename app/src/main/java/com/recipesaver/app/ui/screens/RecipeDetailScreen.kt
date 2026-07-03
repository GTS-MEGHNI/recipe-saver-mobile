package com.recipesaver.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.recipesaver.app.R
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeImage
import com.recipesaver.app.ui.components.RecipeImagePlaceholder
import com.recipesaver.app.ui.components.icon
import com.recipesaver.app.ui.components.labelRes

// Corner radius of the content sheet that rises over the bottom of the hero photo.
private val SheetCornerRadius = 28.dp

// Up to this many photos can be selected in a single trip through the picker.
private const val MAX_PICK = 10

/**
 * Full recipe view: a full-bleed cover hero the content "sheet" tucks into, followed by the title,
 * metadata pills, a photo gallery, a tinted ingredients card and numbered preparation steps. The
 * back control floats over the hero so the photo runs edge-to-edge under the status bar.
 */
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    images: List<RecipeImage>,
    onAddImages: (List<Uri>) -> Unit,
    onDeleteImage: (RecipeImage) -> Unit,
    onSetCover: (Uri) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Index of the photo shown full-screen, or null when the viewer is closed.
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    // Whether the delete-confirmation dialog is showing.
    var showDeleteDialog by remember { mutableStateOf(false) }

    val pickImages =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK),
        ) { uris ->
            if (uris.isNotEmpty()) onAddImages(uris)
        }
    val launchPicker = {
        pickImages.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val pickCover =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            if (uri != null) onSetCover(uri)
        }
    val launchCoverPicker = {
        pickCover.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // The sheet colour backs the whole list so the hero flows seamlessly into the content
        // below its rounded top edge.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            item(key = "hero") {
                RecipeHero(recipe)
            }

            item(key = "header") {
                RecipeHeader(recipe)
            }

            item(key = "photos_header") {
                SectionHeader(
                    title = stringResource(R.string.section_photos),
                    subtitle =
                        if (images.isEmpty()) {
                            ""
                        } else {
                            stringResource(R.string.label_photos_count, images.size)
                        },
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
            item(key = "gallery") {
                GalleryRow(
                    images = images,
                    onAddClick = launchPicker,
                    onImageClick = { index -> viewerIndex = index },
                )
            }

            item(key = "ingredients_header") {
                SectionHeader(
                    title = stringResource(R.string.section_ingredients),
                    subtitle = stringResource(R.string.label_ingredients_count, recipe.ingredients.size),
                    modifier = Modifier.padding(top = 28.dp),
                )
            }
            item(key = "ingredients_card") {
                IngredientsCard(recipe.ingredients)
            }

            item(key = "steps_header") {
                SectionHeader(
                    title = stringResource(R.string.section_steps),
                    subtitle = stringResource(R.string.label_steps_count, recipe.steps.size),
                    modifier = Modifier.padding(top = 28.dp),
                )
            }
            itemsIndexed(
                items = recipe.steps,
                key = { index, _ -> "step_$index" },
            ) { index, step ->
                StepRow(number = index + 1, text = step)
            }

            item(key = "footer_spacer") {
                Spacer(Modifier.height(40.dp))
            }
        }

        // Floating controls over the hero, offset below the status bar: back on the left, a
        // cover-photo picker on the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ScrimIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                onClick = onBack,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScrimIconButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.content_description_edit_recipe),
                    onClick = onEdit,
                )
                ScrimIconButton(
                    icon = Icons.Filled.AddAPhoto,
                    contentDescription = stringResource(R.string.content_description_set_cover),
                    onClick = launchCoverPicker,
                )
                ScrimIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.content_description_delete_recipe),
                    onClick = { showDeleteDialog = true },
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
            },
            title = { Text(stringResource(R.string.delete_recipe_dialog_title)) },
            text = { Text(stringResource(R.string.delete_recipe_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Full-screen swipeable viewer. Guarded so a stale index (after a delete) can't crash.
    viewerIndex?.takeIf { it in images.indices }?.let { startIndex ->
        FullScreenGallery(
            images = images,
            startIndex = startIndex,
            onDelete = { image ->
                onDeleteImage(image)
                viewerIndex = null
            },
            onClose = { viewerIndex = null },
        )
    }
}

@Composable
private fun RecipeHero(
    recipe: Recipe,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
    ) {
        val coverModel = rememberDetailCoverModel(recipe.coverImageUri)
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
                fontSize = MaterialTheme.typography.displayLarge.fontSize,
            )
        }

        // Light top scrim only, so the floating back button stays legible over bright photos.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.32f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // Rounded sheet edge that the photo tucks into, bridging into the content below.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(SheetCornerRadius)
                .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius))
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
private fun RecipeHeader(
    recipe: Recipe,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = recipe.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            recipe.cookTimeMinutes?.let { minutes ->
                MetaPill(
                    icon = Icons.Filled.Schedule,
                    label = stringResource(R.string.label_cook_time_short, minutes),
                )
            }
            recipe.category?.let { category ->
                MetaPill(
                    icon = category.icon,
                    label = stringResource(category.labelRes),
                )
            }
        }
    }
}

/** Horizontal strip of gallery thumbnails, led by an "add photos" tile. */
@Composable
private fun GalleryRow(
    images: List<RecipeImage>,
    onAddClick: () -> Unit,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "add_tile") {
            AddPhotoTile(onClick = onAddClick)
        }
        itemsIndexed(
            items = images,
            key = { _, image -> "img_${image.id}" },
        ) { index, image ->
            GalleryThumbnail(
                image = image,
                onClick = { onImageClick(index) },
            )
        }
    }
}

@Composable
private fun AddPhotoTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AddAPhoto,
            contentDescription = stringResource(R.string.content_description_add_photos),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun GalleryThumbnail(
    image: RecipeImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = image.filePath,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** All ingredients grouped in one tinted card so the list reads as a single unit. */
@Composable
private fun IngredientsCard(
    ingredients: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ingredients.forEach { ingredient ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = ingredient,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

/** Rounded pill with a leading icon, for recipe metadata below the title. */
@Composable
private fun MetaPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Circular translucent icon button that stays legible floating over the hero photo. */
@Composable
private fun ScrimIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.32f),
        modifier = modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
            )
        }
    }
}

/**
 * Full-screen, black-backed pager to swipe through the gallery. A top bar carries a close control,
 * the current position, and a delete control for the visible photo.
 */
@Composable
private fun FullScreenGallery(
    images: List<RecipeImage>,
    startIndex: Int,
    onDelete: (RecipeImage) -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(initialPage = startIndex) { images.size }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                AsyncImage(
                    model = images[page].filePath,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ScrimIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.content_description_close),
                    onClick = onClose,
                )
                Text(
                    text =
                        stringResource(
                            R.string.label_photo_position,
                            pagerState.currentPage + 1,
                            images.size,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
                ScrimIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.content_description_delete_photo),
                    onClick = { onDelete(images[pagerState.currentPage]) },
                )
            }
        }
    }
}

/**
 * Mirrors the list screen's cover resolution: `android.resource://…/drawable/<name>` URIs (bundled
 * sample photos) resolve to their Int resource id; any other value (a `file://` path from internal
 * storage) passes through. Null when there's no cover, so the caller shows the monogram fallback.
 */
@Composable
private fun rememberDetailCoverModel(uri: String?): Any? {
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
