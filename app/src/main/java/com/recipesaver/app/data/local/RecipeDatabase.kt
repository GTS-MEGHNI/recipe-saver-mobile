package com.recipesaver.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.recipesaver.app.data.local.entities.Recipe
import com.recipesaver.app.data.local.entities.RecipeImage

@Database(entities = [Recipe::class, RecipeImage::class], version = 4)
@TypeConverters(Converters::class)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        private const val DATABASE_NAME = "recipe_saver.db"

        /**
         * v1 → v2: drop `servings` and `tags`, add `category`. SQLite can't drop columns in this
         * Room version, so the table is recreated and rows copied across. The old `tags` value is
         * carried into `category` since it's the closest semantic match.
         */
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE recipes_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            ingredients TEXT NOT NULL,
                            steps TEXT NOT NULL,
                            cookTimeMinutes INTEGER,
                            category TEXT,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO recipes_new (id, title, ingredients, steps, cookTimeMinutes, category, createdAt)
                        SELECT id, title, ingredients, steps, cookTimeMinutes, tags, createdAt FROM recipes
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE recipes")
                    db.execSQL("ALTER TABLE recipes_new RENAME TO recipes")
                }
            }

        /** v2 → v3: add the optional `coverImageUri` column for a per-recipe cover photo. */
        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE recipes ADD COLUMN coverImageUri TEXT")
                }
            }

        /** v3 → v4: add the `recipe_images` table (one-to-many gallery), cascading on recipe delete. */
        private val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS recipe_images (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            recipeId INTEGER NOT NULL,
                            filePath TEXT NOT NULL,
                            position INTEGER NOT NULL,
                            FOREIGN KEY(recipeId) REFERENCES recipes(id) ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_recipe_images_recipeId ON recipe_images(recipeId)",
                    )
                }
            }

        @Volatile
        private var instance: RecipeDatabase? = null

        fun getInstance(context: Context): RecipeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecipeDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
