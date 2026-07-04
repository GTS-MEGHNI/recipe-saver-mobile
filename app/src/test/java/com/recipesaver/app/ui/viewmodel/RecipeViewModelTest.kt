package com.recipesaver.app.ui.viewmodel

import android.net.Uri
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeCategory
import com.recipesaver.app.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests the ViewModel's mutation → outcome behavior against a fake repository: success/error
 * [RecipeMessage]s and the new "created recipe id" callback that drives the redirect to detail.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun recipe(id: Long) =
        Recipe(id = id, title = "R$id", ingredients = listOf("a"), steps = listOf("b"))

    @Test
    fun `addRecipe emits RecipeSaved and reports the new id`() =
        runTest(dispatcher) {
            val repo = FakeRepository(createdRecipe = recipe(42))
            val viewModel = RecipeViewModel(repo)
            val messages = mutableListOf<RecipeMessage>()
            val job = launch { viewModel.messages.toList(messages) }
            advanceUntilIdle() // let init refresh + collector subscription settle

            var newId: Long? = null
            viewModel.addRecipe("R42", listOf("a"), listOf("b"), null, RecipeCategory.PASTRY) { newId = it }
            advanceUntilIdle()

            assertEquals(42L, newId)
            assertTrue(messages.contains(RecipeMessage.RecipeSaved))
            job.cancel()
        }

    @Test
    fun `addRecipe failure emits SaveFailed and does not report an id`() =
        runTest(dispatcher) {
            val repo = FakeRepository(createdRecipe = recipe(1), failMutations = true)
            val viewModel = RecipeViewModel(repo)
            val messages = mutableListOf<RecipeMessage>()
            val job = launch { viewModel.messages.toList(messages) }
            advanceUntilIdle()

            var newId: Long? = null
            viewModel.addRecipe("R1", listOf("a"), listOf("b"), null, null) { newId = it }
            advanceUntilIdle()

            assertNull(newId)
            assertTrue(messages.contains(RecipeMessage.SaveFailed))
            assertFalse(messages.contains(RecipeMessage.RecipeSaved))
            job.cancel()
        }

    @Test
    fun `deleteRecipe emits RecipeDeleted on success`() =
        runTest(dispatcher) {
            val repo = FakeRepository(createdRecipe = recipe(5))
            val viewModel = RecipeViewModel(repo)
            val messages = mutableListOf<RecipeMessage>()
            val job = launch { viewModel.messages.toList(messages) }
            advanceUntilIdle()

            viewModel.deleteRecipe(recipe(5))
            advanceUntilIdle()

            assertTrue(messages.contains(RecipeMessage.RecipeDeleted))
            job.cancel()
        }

    @Test
    fun `toggleFavorite flips the cached recipe`() =
        runTest(dispatcher) {
            val repo = FakeRepository(createdRecipe = recipe(7), existing = listOf(recipe(7)))
            val viewModel = RecipeViewModel(repo)
            advanceUntilIdle() // let init refresh populate the cache

            viewModel.toggleFavorite(recipe(7))
            advanceUntilIdle()

            assertTrue(viewModel.recipes.value.single { it.id == 7L }.isFavorite)
        }

    @Test
    fun `toggleFavorite reverts and reports failure when the call fails`() =
        runTest(dispatcher) {
            val repo =
                FakeRepository(
                    createdRecipe = recipe(7),
                    existing = listOf(recipe(7)),
                    failMutations = true,
                )
            val viewModel = RecipeViewModel(repo)
            val messages = mutableListOf<RecipeMessage>()
            val job = launch { viewModel.messages.toList(messages) }
            advanceUntilIdle()

            viewModel.toggleFavorite(recipe(7))
            advanceUntilIdle()

            assertFalse(viewModel.recipes.value.single { it.id == 7L }.isFavorite)
            assertTrue(messages.contains(RecipeMessage.SaveFailed))
            job.cancel()
        }

    /** Minimal in-memory fake; only the methods exercised here do anything meaningful. */
    private class FakeRepository(
        private val createdRecipe: Recipe,
        private val failMutations: Boolean = false,
        private val existing: List<Recipe> = emptyList(),
    ) : RecipeRepository {
        override suspend fun getAllRecipes(): List<Recipe> = existing

        override suspend fun getRecipe(id: Long): Recipe = createdRecipe

        override suspend fun addRecipe(
            title: String,
            ingredients: List<String>,
            steps: List<String>,
            cookTimeMinutes: Int?,
            category: RecipeCategory?,
        ): Recipe {
            if (failMutations) throw RuntimeException("boom")
            return createdRecipe
        }

        override suspend fun updateRecipe(
            id: Long,
            title: String,
            ingredients: List<String>,
            steps: List<String>,
            cookTimeMinutes: Int?,
            category: RecipeCategory?,
        ): Recipe {
            if (failMutations) throw RuntimeException("boom")
            return createdRecipe
        }

        override suspend fun deleteRecipe(id: Long) {
            if (failMutations) throw RuntimeException("boom")
        }

        override suspend fun setFavorite(
            recipe: Recipe,
            favorite: Boolean,
        ): Recipe {
            if (failMutations) throw RuntimeException("boom")
            return recipe.copy(isFavorite = favorite)
        }

        override suspend fun addImage(
            recipeId: Long,
            uri: Uri,
        ) = Unit

        override suspend fun deleteImage(
            recipeId: Long,
            imageId: Long,
        ) = Unit

        override suspend fun setCover(
            recipeId: Long,
            uri: Uri,
        ) = Unit

        override suspend fun deleteCover(recipeId: Long) = Unit
    }
}
