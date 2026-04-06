package com.cleanify.core

import android.content.pm.ApplicationInfo

/**
 * Heuristic classification for the selection UI.
 *
 * We intentionally allow power users to decide anyway, but we provide a visual signal
 * to help avoid accidentally selecting OS-critical frameworks.
 */
object SystemAppClassifier {
    fun isSystem(appInfo: ApplicationInfo): Boolean {
        val flags = appInfo.flags
        val systemFlag = flags and ApplicationInfo.FLAG_SYSTEM
        val updatedSystemFlag = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        return systemFlag != 0 || updatedSystemFlag != 0
    }
}

