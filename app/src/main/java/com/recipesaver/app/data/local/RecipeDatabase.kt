package com.recipesaver.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.recipesaver.app.data.local.entities.Recipe

@Database(entities = [Recipe::class], version = 1)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        private const val DATABASE_NAME = "recipe_saver.db"

        @Volatile
        private var instance: RecipeDatabase? = null

        fun getInstance(context: Context): RecipeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecipeDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
