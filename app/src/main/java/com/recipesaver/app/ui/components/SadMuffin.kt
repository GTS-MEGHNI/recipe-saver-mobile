package com.recipesaver.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A playful hand-drawn "sad muffin" used for empty states — a muffin with a droopy frown and a tear
 * to say "nothing here yet". Purely decorative (drawn with [Canvas]), so no image asset is bundled.
 * Colors come from the active Material 3 theme so it fits every
 * [com.recipesaver.app.ui.theme.AppTheme].
 */
@Composable
fun SadMuffin(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
) {
    val capColor = MaterialTheme.colorScheme.primary
    val linerColor = MaterialTheme.colorScheme.secondaryContainer
    val linerLineColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.35f)
    val faceColor = MaterialTheme.colorScheme.onSecondaryContainer
    val tearColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Muffin liner (the ridged paper cup): a trapezoid, wider at the top.
        val linerTopY = h * 0.46f
        val linerBottomY = h * 0.90f
        val liner = Path().apply {
            moveTo(w * 0.30f, linerTopY)
            lineTo(w * 0.70f, linerTopY)
            lineTo(w * 0.63f, linerBottomY)
            lineTo(w * 0.37f, linerBottomY)
            close()
        }
        drawPath(liner, color = linerColor)

        // Vertical ridges on the liner, fanning out slightly toward the top.
        listOf(0.42f, 0.50f, 0.58f).forEach { fx ->
            drawLine(
                color = linerLineColor,
                start = Offset(w * fx, linerTopY + h * 0.02f),
                end = Offset(w * (fx + (fx - 0.50f) * 0.18f), linerBottomY - h * 0.02f),
                strokeWidth = w * 0.012f,
                cap = StrokeCap.Round,
            )
        }

        // Muffin cap (the domed, overhanging top) as a bumpy blob.
        val cap = Path().apply {
            moveTo(w * 0.24f, linerTopY + h * 0.02f)
            cubicTo(w * 0.16f, h * 0.34f, w * 0.20f, h * 0.16f, w * 0.34f, h * 0.15f)
            cubicTo(w * 0.40f, h * 0.06f, w * 0.52f, h * 0.06f, w * 0.57f, h * 0.15f)
            cubicTo(w * 0.72f, h * 0.12f, w * 0.84f, h * 0.22f, w * 0.78f, h * 0.36f)
            cubicTo(w * 0.82f, h * 0.44f, w * 0.78f, h * 0.50f, w * 0.70f, linerTopY + h * 0.03f)
            lineTo(w * 0.30f, linerTopY + h * 0.03f)
            close()
        }
        drawPath(cap, color = capColor)

        // Sad face on the cap: two eyes and a downturned frown.
        val eyeR = w * 0.028f
        val eyeY = h * 0.30f
        drawCircle(faceColor, radius = eyeR, center = Offset(w * 0.42f, eyeY))
        drawCircle(faceColor, radius = eyeR, center = Offset(w * 0.58f, eyeY))

        // Frown: an upward-bulging arc (corners turned down) = a sad mouth.
        val mouth = Path().apply {
            moveTo(w * 0.43f, h * 0.42f)
            quadraticTo(w * 0.50f, h * 0.35f, w * 0.57f, h * 0.42f)
        }
        drawPath(
            mouth,
            color = faceColor,
            style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
        )

        // A single tear under the left eye.
        val tear = Path().apply {
            moveTo(w * 0.42f, eyeY + eyeR * 1.6f)
            quadraticTo(w * 0.395f, h * 0.40f, w * 0.42f, h * 0.42f)
            quadraticTo(w * 0.445f, h * 0.40f, w * 0.42f, eyeY + eyeR * 1.6f)
            close()
        }
        drawPath(tear, color = tearColor)
    }
}
