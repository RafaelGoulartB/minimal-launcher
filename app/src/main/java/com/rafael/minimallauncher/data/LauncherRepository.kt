package com.rafael.minimallauncher.data

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

interface LauncherRepository {
    fun loadCachedApps(): List<LauncherApp>

    suspend fun loadApps(): List<LauncherApp>
}

class PackageManagerLauncherRepository(private val context: Context) : LauncherRepository {
    private val cache = context.getSharedPreferences(APP_CACHE_NAME, Context.MODE_PRIVATE)

    override fun loadCachedApps(): List<LauncherApp> = LauncherPreferencesCodec
        .decodeMap(cache.getString(CACHED_APPS, null).orEmpty())
        .mapNotNull { (componentId, label) ->
            val component = ComponentName.unflattenFromString(componentId)
                ?: return@mapNotNull null
            LauncherApp(
                label = label,
                componentName = component,
            )
        }

    override suspend fun loadApps(): List<LauncherApp> = withContext(Dispatchers.Default) {
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
        val apps = activities
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

        // An empty result is valid when the last launchable app was removed. Exceptions
        // are handled by the ViewModel, so a successful query can safely replace the cache.
        cache.edit()
            .putString(CACHED_APPS, LauncherPreferencesCodec.encodeMap(apps.associate { it.id to it.label }))
            .apply()
        apps
    }

    private companion object {
        const val APP_CACHE_NAME = "launcher_app_cache"
        const val CACHED_APPS = "apps"
    }
}
