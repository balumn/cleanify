package com.cleanify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = CleanifyColor.Green,
    onPrimary = Color.White,
    primaryContainer = CleanifyColor.Green50,
    onPrimaryContainer = CleanifyColor.GreenDark,
    secondary = CleanifyColor.GreenDark,
    onSecondary = Color.White,
    secondaryContainer = CleanifyColor.Green25,
    onSecondaryContainer = CleanifyColor.Forest,
    tertiary = CleanifyColor.Mint,
    onTertiary = CleanifyColor.Forest,
    background = CleanifyColor.Background,
    onBackground = CleanifyColor.TextPrimary,
    surface = CleanifyColor.Surface,
    onSurface = CleanifyColor.TextPrimary,
    surfaceVariant = CleanifyColor.SurfaceVariant,
    onSurfaceVariant = CleanifyColor.TextSecondary,
    outline = CleanifyColor.Green.copy(alpha = 0.35f),
    outlineVariant = CleanifyColor.Green.copy(alpha = 0.18f),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private fun cleanifyTypography(): Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.35).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.15).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.04.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.015.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.35.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = CleanifyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun CleanifyTheme(content: @Composable () -> Unit) {
    val shapes = Shapes(
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

    MaterialTheme(
        colorScheme = LightScheme,
        typography = cleanifyTypography(),
        shapes = shapes,
        content = content,
    )
}
