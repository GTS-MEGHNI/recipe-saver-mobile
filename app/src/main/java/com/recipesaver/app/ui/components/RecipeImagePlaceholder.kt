package com.recipesaver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.recipesaver.app.ui.theme.MonogramTints

/**
 * Monogram fill shown in the cover-photo slot when a recipe has no image yet: the recipe's initial
 * in serif on a warm tint chosen deterministically from [seed] (usually the recipe id). Fills its
 * [modifier] bounds, so the caller controls the size/shape via the enclosing Box.
 */
@Composable
fun RecipeImagePlaceholder(
    title: String,
    seed: Long,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 48.sp,
) {
    val tint = MonogramTints[(seed.mod(MonogramTints.size.toLong())).toInt()]
    val initial = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier.background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
        )
    }
}
