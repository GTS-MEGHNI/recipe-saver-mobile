package com.recipesaver.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val ingredients: String,
    val steps: String,
    val cookTimeMinutes: Int? = null,
    val servings: Int? = null,
    val tags: String? = null,
    val createdAt: Long,
)
