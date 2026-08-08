package com.rafael.minimallauncher.data

internal object LauncherFolderPreferencesReducer {
    fun delete(
        preferences: LauncherPreferences,
        folderId: String,
    ): Pair<LauncherPreferences, FolderDeletionSnapshot>? {
        val folder = preferences.folders.firstOrNull { it.id == folderId } ?: return null
        val snapshot = FolderDeletionSnapshot(
            folder = folder,
            memberAppIds = preferences.appFolders.filterValues { it == folderId }.keys,
            homeIndex = preferences.homeItems.indexOf(HomeItemRef.Folder(folderId)).takeIf { it >= 0 },
        )
        return preferences.copy(
            folders = preferences.folders.filterNot { it.id == folderId },
            appFolders = preferences.appFolders.filterValues { it != folderId },
            homeItems = preferences.homeItems.filterNot { it == HomeItemRef.Folder(folderId) },
        ) to snapshot
    }

    fun restore(
        preferences: LauncherPreferences,
        snapshot: FolderDeletionSnapshot,
        installedAppIds: Set<String>,
    ): LauncherPreferences {
        val existingFolder = preferences.folders.firstOrNull { it.id == snapshot.folder.id }
        val canRestoreSnapshot = existingFolder == null || existingFolder == snapshot.folder
        val folders = if (existingFolder != null) preferences.folders else preferences.folders + snapshot.folder
        val restoredMemberships = snapshot.memberAppIds
            .asSequence()
            .filter { canRestoreSnapshot }
            .filter { it in installedAppIds }
            .filter { it !in preferences.appFolders }
            .associateWith { snapshot.folder.id }
        val memberships = preferences.appFolders + restoredMemberships
        val homeItems = if (snapshot.homeIndex != null && HomeItemRef.Folder(snapshot.folder.id) !in preferences.homeItems) {
            preferences.homeItems.toMutableList().apply {
                add(snapshot.homeIndex.coerceIn(0, size), HomeItemRef.Folder(snapshot.folder.id))
            }
        } else {
            preferences.homeItems
        }
        return preferences.copy(
            folders = folders,
            appFolders = memberships,
            homeItems = homeItems,
        )
    }
}
