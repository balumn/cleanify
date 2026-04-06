package com.cleanify.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cleanify.R

/** Roboto for all UI text; pair with [FontWeight.Bold] for the product name where needed. */
val CleanifyFontFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_medium, FontWeight.SemiBold),
    Font(R.font.roboto_bold, FontWeight.Bold),
)
