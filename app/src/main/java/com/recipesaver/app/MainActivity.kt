package com.recipesaver.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recipesaver.app.data.files.ImageStorageManager
import com.recipesaver.app.data.local.RecipeDatabase
import com.recipesaver.app.data.preferences.ThemePreferences
import com.recipesaver.app.data.local.entities.RecipeCategory
import com.recipesaver.app.data.repository.RecipeRepository
import com.recipesaver.app.ui.components.labelRes
import com.recipesaver.app.ui.screens.CategoryScreen
import com.recipesaver.app.ui.screens.RecipeDetailScreen
import com.recipesaver.app.ui.screens.RecipeListScreen
import com.recipesaver.app.ui.screens.SettingsScreen
import com.recipesaver.app.ui.theme.RecipeSaverTheme
import com.recipesaver.app.ui.viewmodel.RecipeViewModel
import com.recipesaver.app.ui.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RecipeViewModel by viewModels {
        RecipeViewModel.Factory(
            RecipeRepository(
                RecipeDatabase.getInstance(applicationContext).recipeDao(),
                ImageStorageManager(applicationContext),
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
                // TODO: navigate to the add-recipe screen once it exists.
                onAddClick = {},
                onSettingsClick = { navController.navigate("settings") },
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
            // remember the flows so we don't build fresh StateFlows (and re-trigger their
            // initial emission) on every recomposition, which loops forever.
            val recipeFlow = remember(recipeId) { viewModel.getRecipe(recipeId) }
            val imagesFlow = remember(recipeId) { viewModel.getImages(recipeId) }
            val recipe by recipeFlow.collectAsState()
            val images by imagesFlow.collectAsState()

            recipe?.let { loaded ->
                RecipeDetailScreen(
                    recipe = loaded,
                    images = images,
                    onAddImages = { uris -> viewModel.addImages(recipeId, uris) },
                    onDeleteImage = viewModel::deleteImage,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
