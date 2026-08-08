package com.rafael.minimallauncher.data

import android.content.ComponentName

data class LauncherApp(
    val label: String,
    val componentName: ComponentName,
) {
    val id: String = componentName.flattenToString()
}

