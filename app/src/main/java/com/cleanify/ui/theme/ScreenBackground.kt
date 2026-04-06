package com.cleanify.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun CleanifyScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height
                val mid = lerp(scheme.background, scheme.surfaceVariant, 0.55f)
                val floor = lerp(scheme.background, scheme.primary, 0.085f)
                val glowA = lerp(scheme.tertiary, scheme.primary, 0.35f).copy(alpha = 0.14f)
                val glowB = CleanifyColor.Honey.copy(alpha = 0.09f)

                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to scheme.background,
                            0.42f to mid,
                            1f to floor,
                        ),
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowA, Color.Transparent),
                        center = Offset(w * 0.88f, h * 0.02f),
                        radius = w * 0.72f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowB, Color.Transparent),
                        center = Offset(w * 0.05f, h * 0.92f),
                        radius = h * 0.55f,
                    ),
                )
                drawRect(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to scheme.primary.copy(alpha = 0.03f),
                        1f to Color.Transparent,
                        start = Offset(0f, h),
                        end = Offset(w, 0f),
                    ),
                )
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
