package com.rafael.minimallauncher.ui

import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.DailyUsage
import com.rafael.minimallauncher.data.FolderItem
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.data.LauncherItem
import com.rafael.minimallauncher.data.LauncherPreferences
import java.text.Collator
import java.util.Locale

data class LauncherUiState(
    val apps: List<LauncherApp> = emptyList(),
    val filteredApps: List<LauncherApp> = emptyList(),
    val favoriteApps: List<LauncherApp> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val favoriteFolderIds: Set<String> = emptySet(),
    val drawerItems: List<LauncherItem> = emptyList(),
    val homeItems: List<LauncherItem> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val preferences: LauncherPreferences = LauncherPreferences(),
    val dailyUsage: DailyUsage = DailyUsage(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
)

data class LauncherCatalog(
    val apps: List<LauncherApp>,
    val preferences: LauncherPreferences,
    val visibleAppItems: Map<String, AppItem>,
    val folderItems: Map<String, FolderItem>,
    val topLevelItems: List<LauncherItem>,
    val homeItems: List<LauncherItem>,
    val favoriteApps: List<LauncherApp>,
    val favoriteIds: Set<String>,
    val favoriteFolderIds: Set<String>,
    val filteredApps: List<LauncherApp>,
    val itemComparator: Comparator<LauncherItem>,
)

object LauncherUiStateMapper {
    fun mapCatalog(
        installedApps: List<LauncherApp>,
        preferences: LauncherPreferences,
        locale: Locale = Locale.getDefault(),
    ): LauncherCatalog {
        val collator = Collator.getInstance(locale)
        val itemComparator = Comparator<LauncherItem> { first, second ->
            collator.compare(first.label, second.label)
        }
        val visibleAppItems = installedApps
            .filterNot { it.id in preferences.hiddenAppIds }
            .associate { app -> app.id to AppItem(app, preferences.customNames[app.id] ?: app.label) }
        val folderItems = preferences.folders.associate { folder ->
            folder.id to FolderItem(
                folder = folder,
                apps = preferences.appFolders
                    .filterValues { it == folder.id }
                    .keys
                    .mapNotNull(visibleAppItems::get)
                    .sortedWith(compareBy(collator) { it.label }),
            )
        }
        val assignedAppIds = preferences.appFolders
            .filterValues(folderItems::containsKey)
            .keys
        val topLevelItems = (visibleAppItems.values.filterNot { it.id in assignedAppIds } + folderItems.values)
            .sortedWith(itemComparator)
        val homeItems = preferences.homeItems.mapNotNull { item ->
            when (item) {
                is HomeItemRef.App -> visibleAppItems[item.value]
                is HomeItemRef.Folder -> folderItems[item.value]
            }
        }
        val appsById = installedApps.associateBy(LauncherApp::id)
        val favoriteApps = preferences.homeItems.mapNotNull { item ->
            (item as? HomeItemRef.App)?.value?.let(appsById::get)
        }.filterNot { it.id in preferences.hiddenAppIds }
        val filteredApps = installedApps
            .filterNot { it.id in preferences.hiddenAppIds }
            .sortedWith(compareBy(collator) { preferences.customNames[it.id] ?: it.label })

        return LauncherCatalog(
            apps = installedApps,
            preferences = preferences,
            visibleAppItems = visibleAppItems,
            folderItems = folderItems,
            topLevelItems = topLevelItems,
            homeItems = homeItems,
            favoriteApps = favoriteApps,
            favoriteIds = preferences.homeItems
                .filterIsInstance<HomeItemRef.App>()
                .mapTo(mutableSetOf(), HomeItemRef.App::value),
            favoriteFolderIds = preferences.homeItems
                .filterIsInstance<HomeItemRef.Folder>()
                .mapTo(mutableSetOf(), HomeItemRef.Folder::value),
            filteredApps = filteredApps,
            itemComparator = itemComparator,
        )
    }

    fun mapState(
        catalog: LauncherCatalog,
        query: String,
        usage: DailyUsage,
    ): LauncherUiState {
        val normalizedQuery = query.trim()
        val drawerItems = if (normalizedQuery.isBlank()) {
            catalog.topLevelItems
        } else {
            (catalog.visibleAppItems.values.filter { it.label.contains(normalizedQuery, ignoreCase = true) } +
                catalog.folderItems.values.filter { it.label.contains(normalizedQuery, ignoreCase = true) })
                .sortedWith(catalog.itemComparator)
        }
        return LauncherUiState(
            apps = catalog.apps,
            filteredApps = if (normalizedQuery.isBlank()) {
                catalog.filteredApps
            } else {
                catalog.filteredApps.filter { app ->
                    (catalog.preferences.customNames[app.id] ?: app.label)
                        .contains(normalizedQuery, ignoreCase = true)
                }
            },
            favoriteApps = catalog.favoriteApps,
            favoriteIds = catalog.favoriteIds,
            favoriteFolderIds = catalog.favoriteFolderIds,
            drawerItems = drawerItems,
            homeItems = catalog.homeItems,
            folders = catalog.folderItems.values.sortedWith(catalog.itemComparator),
            preferences = catalog.preferences,
            dailyUsage = usage,
            searchQuery = normalizedQuery,
            isLoading = false,
        )
    }
}
