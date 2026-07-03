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
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.recipesaver.app.data.local.RecipeDatabase
import com.recipesaver.app.data.repository.RecipeRepository
import com.recipesaver.app.ui.screens.AddRecipeScreen
import com.recipesaver.app.ui.screens.RecipeDetailScreen
import com.recipesaver.app.ui.screens.RecipeListScreen
import com.recipesaver.app.ui.theme.RecipeSaverTheme
import com.recipesaver.app.ui.viewmodel.RecipeViewModel

private const val ROUTE_LIST = "list"
private const val ROUTE_ADD = "add"
private const val ROUTE_DETAIL = "detail/{recipeId}"

class MainActivity : ComponentActivity() {
    private val viewModel: RecipeViewModel by viewModels {
        RecipeViewModel.Factory(
            RecipeRepository(RecipeDatabase.getInstance(applicationContext).recipeDao()),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeSaverTheme {
                RecipeSaverApp(viewModel)
            }
        }
    }
}

@Composable
private fun RecipeSaverApp(viewModel: RecipeViewModel) {
    val navController = rememberNavController()
    val recipes by viewModel.recipes.collectAsState()

    NavHost(navController = navController, startDestination = ROUTE_LIST) {
        composable(ROUTE_LIST) {
            RecipeListScreen(
                recipes = recipes,
                onAddRecipeClick = { navController.navigate(ROUTE_ADD) },
                onRecipeClick = { recipe -> navController.navigate("detail/${recipe.id}") },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(ROUTE_ADD) {
            AddRecipeScreen(
                onSave = { title, ingredients, steps, cookTimeMinutes, servings, tags ->
                    viewModel.addRecipe(title, ingredients, steps, cookTimeMinutes, servings, tags)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(ROUTE_DETAIL) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")?.toLongOrNull()
            val recipe = recipes.find { it.id == recipeId }
            if (recipe != null) {
                RecipeDetailScreen(
                    recipe = recipe,
                    onDelete = {
                        viewModel.deleteRecipe(recipe)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
