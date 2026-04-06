package com.cleanify.core

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import com.cleanify.data.model.AppListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads installed applications (including system apps).
 *
 * IMPORTANT: This can throw SecurityException if package visibility is restricted.
 * Since we declare QUERY_ALL_PACKAGES, it should work for most side-loaded setups.
 */
class InstalledAppsProvider(
    private val context: Context,
) {
    suspend fun loadAllInstalledApps(): Result<List<AppListItem>> = withContext(Dispatchers.Default) {
        try {
            val pm = context.packageManager

            val applications: List<ApplicationInfo> = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val items = applications
                .asSequence()
                .filter { it.enabled }
                .filter { it.packageName != context.packageName }
                .map { appInfo ->
                val label = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(appInfo.packageName)
                val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
                val isSystem = SystemAppClassifier.isSystem(appInfo)

                AppListItem(
                    packageName = appInfo.packageName,
                    label = label,
                    icon = icon,
                    isSystem = isSystem,
                )
                }
                .sortedWith(
                    compareBy<AppListItem> { it.isSystem }.thenBy { it.label.lowercase() },
                )
                .toList()

            Result.success(items)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

