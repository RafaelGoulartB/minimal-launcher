package com.rafael.minimallauncher.data

internal object LauncherPreferencesReconciler {
    fun reconcile(
        preferences: LauncherPreferences,
        orderedAppIds: List<String>,
    ): LauncherPreferences {
        val installedIds = orderedAppIds.toSet()
        val folderIds = preferences.folders.mapTo(mutableSetOf(), LauncherFolder::id)
        val validHomeItems = preferences.homeItems.filter { item ->
            when (item) {
                is HomeItemRef.App -> item.value in installedIds
                is HomeItemRef.Folder -> item.value in folderIds
            }
        }
        val homeItems = if (preferences.needsFavoriteMigration) {
            orderedAppIds.filter(preferences.legacyFavoriteIds::contains).map(HomeItemRef::App)
        } else {
            validHomeItems
        }
        return preferences.copy(
            homeItems = homeItems,
            customNames = preferences.customNames.filterKeys(installedIds::contains),
            hiddenAppIds = preferences.hiddenAppIds.intersect(installedIds),
            blockedAppIds = preferences.blockedAppIds.intersect(installedIds),
            appFolders = preferences.appFolders.filter { (appId, folderId) ->
                appId in installedIds && folderId in folderIds
            },
            needsFavoriteMigration = false,
            legacyFavoriteIds = preferences.legacyFavoriteIds.intersect(installedIds),
        )
    }
}
