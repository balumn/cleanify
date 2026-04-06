package com.cleanify.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

@Composable
fun DrawableIcon(
    drawable: Drawable?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    if (drawable == null) return

    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }.coerceAtLeast(1)
    val drawableKey = drawable.constantState ?: drawable
    val bitmap = remember(drawableKey, sizePx) { drawableToBitmap(drawable, sizePx, sizePx) }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Image(
        bitmap = imageBitmap,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

private fun drawableToBitmap(
    drawable: Drawable,
    width: Int,
    height: Int,
): Bitmap {
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

