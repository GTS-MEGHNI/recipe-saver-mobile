package com.recipesaver.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A shimmering placeholder used as a skeleton loader while a remote image is still downloading, so a
 * grid of gallery photos shows animated boxes instead of blank space (which reads as "nothing here"
 * when many images load at once). Draws a light band sweeping across a muted base tinted from the
 * theme surface.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)

    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer-progress",
    )

    Box(
        modifier = modifier
            .background(base)
            .drawWithCache {
                val bandWidth = size.width * 0.6f
                // Sweep the highlight band from off the left edge to off the right edge.
                val start = -bandWidth + (size.width + bandWidth) * progress
                val brush =
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        start = Offset(start, 0f),
                        end = Offset(start + bandWidth, size.height),
                    )
                onDrawBehind { drawRect(brush) }
            }
            .fillMaxSize(),
    )
}
