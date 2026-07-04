package com.recipesaver.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recipesaver.app.data.local.entities.RecipeCategory
import com.recipesaver.app.data.preferences.ThemePreferences
import com.recipesaver.app.data.remote.ImageUploader
import com.recipesaver.app.data.remote.NetworkModule
import com.recipesaver.app.data.repository.DefaultRecipeRepository
import com.recipesaver.app.ui.components.labelRes
import com.recipesaver.app.ui.screens.AddRecipeScreen
import com.recipesaver.app.ui.screens.CategoryScreen
import com.recipesaver.app.ui.screens.RecipeDetailScreen
import com.recipesaver.app.ui.screens.RecipeListScreen
import com.recipesaver.app.ui.screens.SettingsScreen
import com.recipesaver.app.ui.theme.RecipeSaverTheme
import com.recipesaver.app.ui.viewmodel.RecipeMessage
import com.recipesaver.app.ui.viewmodel.RecipeViewModel
import com.recipesaver.app.ui.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RecipeViewModel by viewModels {
        RecipeViewModel.Factory(
            DefaultRecipeRepository(
                NetworkModule.createApiService(),
                ImageUploader(applicationContext),
            ),
        )
    }

    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModel.Factory(ThemePreferences(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by themeViewModel.theme.collectAsState()
            val darkMode by themeViewModel.darkMode.collectAsState()
            RecipeSaverTheme(appTheme = appTheme, darkMode = darkMode) {
                RecipeSaverApp(viewModel, themeViewModel)
            }
        }
    }
}

@Composable
private fun RecipeSaverApp(
    viewModel: RecipeViewModel,
    themeViewModel: ThemeViewModel,
) {
    val navController = rememberNavController()

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, context.getString(message.stringRes()), Toast.LENGTH_SHORT).show()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "categories",
        modifier = Modifier.fillMaxSize(),
    ) {
        composable("categories") {
            val recipes by viewModel.recipes.collectAsState()
            val counts = remember(recipes) {
                recipes.mapNotNull { it.category }.groupingBy { it }.eachCount()
            }
            CategoryScreen(
                recipeCounts = counts,
                onCategoryClick = { category -> navController.navigate("list/${category.name}") },
                onSettingsClick = { navController.navigate("settings") },
            )
        }
        composable(
            route = "list/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType }),
        ) { backStackEntry ->
            val category =
                backStackEntry.arguments?.getString("category")?.let { name ->
                    runCatching { RecipeCategory.valueOf(name) }.getOrNull()
                } ?: return@composable
            val recipes by viewModel.recipes.collectAsState()
            val filtered = remember(recipes, category) { recipes.filter { it.category == category } }
            RecipeListScreen(
                title = stringResource(category.labelRes),
                recipes = filtered,
                onRecipeClick = { id -> navController.navigate("detail/$id") },
                onAddClick = { navController.navigate("add/${category.name}") },
                onSettingsClick = { navController.navigate("settings") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "add/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType }),
        ) { backStackEntry ->
            val initialCategory =
                backStackEntry.arguments?.getString("category")?.let { name ->
                    runCatching { RecipeCategory.valueOf(name) }.getOrNull()
                }
            AddRecipeScreen(
                initialCategory = initialCategory,
                onSave = { title, ingredients, steps, cookTimeMinutes, category ->
                    viewModel.addRecipe(title, ingredients, steps, cookTimeMinutes, category) { newId ->
                        navController.navigate("detail/$newId") {
                            popUpTo("add/{category}") { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable("settings") {
            val appTheme by themeViewModel.theme.collectAsState()
            val darkMode by themeViewModel.darkMode.collectAsState()
            SettingsScreen(
                selectedTheme = appTheme,
                onThemeSelected = themeViewModel::setTheme,
                selectedDarkMode = darkMode,
                onDarkModeSelected = themeViewModel::setDarkMode,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "detail/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: return@composable
            LaunchedEffect(recipeId) { viewModel.openRecipe(recipeId) }
            val recipe by viewModel.detail.collectAsState()
            val uploadingCount by viewModel.pendingUploads.collectAsState()

            recipe?.let { loaded ->
                RecipeDetailScreen(
                    recipe = loaded,
                    images = loaded.images,
                    uploadingCount = uploadingCount,
                    onAddImages = { uris -> viewModel.addImages(recipeId, uris) },
                    onDeleteImage = { image -> viewModel.deleteImage(image.recipeId, image.id) },
                    onSetCover = { uri -> viewModel.setCover(recipeId, uri) },
                    onEdit = { navController.navigate("edit/$recipeId") },
                    onDelete = {
                        viewModel.deleteRecipe(loaded)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(
            route = "edit/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: return@composable
            LaunchedEffect(recipeId) { viewModel.openRecipe(recipeId) }
            val recipe by viewModel.detail.collectAsState()

            recipe?.takeIf { it.id == recipeId }?.let { loaded ->
                AddRecipeScreen(
                    initialCategory = loaded.category,
                    existing = loaded,
                    onSave = { title, ingredients, steps, cookTimeMinutes, category ->
                        viewModel.updateRecipe(recipeId, title, ingredients, steps, cookTimeMinutes, category)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Maps a mutation outcome to its localized toast text. */
private fun RecipeMessage.stringRes(): Int =
    when (this) {
        RecipeMessage.RecipeSaved -> R.string.toast_recipe_saved
        RecipeMessage.RecipeDeleted -> R.string.toast_recipe_deleted
        RecipeMessage.ImageAdded -> R.string.toast_image_added
        RecipeMessage.ImageDeleted -> R.string.toast_image_deleted
        RecipeMessage.CoverUpdated -> R.string.toast_cover_updated
        RecipeMessage.SaveFailed -> R.string.toast_save_failed
        RecipeMessage.DeleteFailed -> R.string.toast_delete_failed
        RecipeMessage.ImageFailed -> R.string.toast_image_failed
        RecipeMessage.CoverFailed -> R.string.toast_cover_failed
    }
