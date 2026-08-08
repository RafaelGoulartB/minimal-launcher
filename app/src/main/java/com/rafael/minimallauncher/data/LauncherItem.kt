package com.rafael.minimallauncher.data

sealed interface LauncherItem {
    val id: String
    val label: String
}

data class AppItem(
    val app: LauncherApp,
    override val label: String = app.label,
) : LauncherItem {
    override val id: String = app.id
}

data class FolderItem(
    val folder: LauncherFolder,
    val apps: List<AppItem>,
) : LauncherItem {
    override val id: String = folder.id
    override val label: String = folder.name
}
