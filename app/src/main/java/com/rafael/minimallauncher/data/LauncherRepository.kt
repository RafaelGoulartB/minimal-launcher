package com.rafael.minimallauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class LauncherRepository(private val context: Context) {
    suspend fun loadApps(): List<LauncherApp> = withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packageManager = context.packageManager
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }

        val collator = Collator.getInstance(Locale.getDefault())
        activities
            .mapNotNull { resolveInfo ->
                // A single package with invalid resources must not take down the launcher.
                runCatching {
                    val activityInfo = resolveInfo.activityInfo ?: return@runCatching null
                    LauncherApp(
                        label = resolveInfo.loadLabel(packageManager).toString(),
                        componentName = activityInfo.run {
                            android.content.ComponentName(packageName, name)
                        },
                    )
                }.getOrNull()
            }
            .distinctBy(LauncherApp::id)
            .sortedWith(compareBy(collator) { it.label })
    }
}
