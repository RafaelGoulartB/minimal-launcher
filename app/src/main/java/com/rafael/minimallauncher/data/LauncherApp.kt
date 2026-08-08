package com.rafael.minimallauncher.data

import android.content.ComponentName

data class LauncherApp(
    val label: String,
    val componentName: ComponentName,
    internal val stableIdOverride: String? = null,
) {
    val id: String = stableIdOverride ?: componentName.flattenToString()
}
