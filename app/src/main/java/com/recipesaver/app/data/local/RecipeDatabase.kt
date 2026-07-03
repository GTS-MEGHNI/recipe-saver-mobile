package com.recipesaver.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.recipesaver.app.data.local.entities.Recipe

@Database(entities = [Recipe::class], version = 1)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        const val DATABASE_NAME = "recipe_saver.db"
    }
}
