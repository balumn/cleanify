package com.cleanify.core.accessibility

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings

/**
 * Accessibility gate helpers.
 *
 * Cleanify must be enabled as an Accessibility Service for the automation to work.
 */
object AccessibilityUtils {
    fun isCleanifyAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val serviceComponent = ComponentName(context, com.cleanify.accessibility.CleanifyAccessibilityService::class.java)
        val expectedFull = serviceComponent.flattenToString()
        val expectedShort = serviceComponent.flattenToShortString()

        // ENABLED_ACCESSIBILITY_SERVICES is a colon-separated list of component names.
        return enabledServices
            .split(':')
            .map { it.trim() }
            .any { item ->
                item.equals(expectedFull, ignoreCase = true) ||
                    item.equals(expectedShort, ignoreCase = true)
            }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

