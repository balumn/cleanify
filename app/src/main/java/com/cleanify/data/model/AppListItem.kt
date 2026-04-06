package com.cleanify.data.model

import android.graphics.drawable.Drawable

data class AppListItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
)

