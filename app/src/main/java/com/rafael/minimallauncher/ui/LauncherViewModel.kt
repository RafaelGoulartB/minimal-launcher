package com.rafael.minimallauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.FolderItem
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.data.LauncherItem
import com.rafael.minimallauncher.data.LauncherPreferences
import com.rafael.minimallauncher.data.LauncherPreferencesRepository
import com.rafael.minimallauncher.data.LauncherRepository
import com.rafael.minimallauncher.data.DailyUsage
import com.rafael.minimallauncher.data.UsageStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
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
    val isLoading: Boolean = true,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val launcherRepository = LauncherRepository(application)
    private val preferencesRepository = LauncherPreferencesRepository(application)
    private val usageStatsRepository = UsageStatsRepository(application)
    private val apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val dailyUsage = MutableStateFlow(DailyUsage())

    val uiState: StateFlow<LauncherUiState> = combine(
        apps,
        preferencesRepository.preferences,
        searchQuery.debounce(120),
        dailyUsage,
    ) { installedApps, preferences, query, usage ->
        val collator = Collator.getInstance(Locale.getDefault())
        val itemComparator = Comparator<LauncherItem> { first, second -> collator.compare(first.label, second.label) }
        val favoriteIds = preferences.homeItems.filterIsInstance<HomeItemRef.App>().mapTo(mutableSetOf()) { it.value }
        val favoriteFolderIds = preferences.homeItems.filterIsInstance<HomeItemRef.Folder>().mapTo(mutableSetOf()) { it.value }
        val appsById = installedApps.associateBy(LauncherApp::id)
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
        val assignedAppIds = preferences.appFolders.filterValues(folderItems::containsKey).keys
        val topLevelItems = (visibleAppItems.values.filterNot { it.id in assignedAppIds } + folderItems.values)
            .sortedWith(itemComparator)
        val drawerItems = if (query.isBlank()) {
            topLevelItems
        } else {
            (visibleAppItems.values.filter { it.label.contains(query, ignoreCase = true) } +
                folderItems.values.filter { it.label.contains(query, ignoreCase = true) })
                .sortedWith(itemComparator)
        }
        val homeItems = preferences.homeItems.mapNotNull { item ->
            when (item) {
                is HomeItemRef.App -> visibleAppItems[item.value]
                is HomeItemRef.Folder -> folderItems[item.value]
            }
        }
        val favoriteApps = preferences.homeItems.mapNotNull { item ->
            (item as? HomeItemRef.App)?.value?.let(appsById::get)
        }.filterNot { it.id in preferences.hiddenAppIds }
        LauncherUiState(
            apps = installedApps,
            filteredApps = installedApps.filter { app ->
                app.id !in preferences.hiddenAppIds &&
                    (query.isBlank() || (preferences.customNames[app.id] ?: app.label).contains(query, ignoreCase = true))
            }.sortedBy { preferences.customNames[it.id] ?: it.label },
            favoriteApps = favoriteApps,
            favoriteIds = favoriteIds,
            favoriteFolderIds = favoriteFolderIds,
            drawerItems = drawerItems,
            homeItems = homeItems,
            folders = folderItems.values.sortedWith(compareBy(collator) { it.label }),
            preferences = preferences,
            dailyUsage = usage,
            searchQuery = query,
            isLoading = false,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState(),
    )

    init {
        refreshApps()
        refreshUsage()
    }

    fun refreshApps() {
        viewModelScope.launch {
            val installedApps = launcherRepository.loadApps()
            apps.value = installedApps
            preferencesRepository.migrateLegacyFavorites(installedApps.map(LauncherApp::id))
        }
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun refreshUsage() {
        viewModelScope.launch { dailyUsage.value = usageStatsRepository.loadTodayUsage() }
    }

    fun toggleFavorite(app: LauncherApp) {
        viewModelScope.launch { preferencesRepository.toggleHomeApp(app.id) }
    }

    fun addHomeItem(item: HomeItemRef) = launchPreferenceUpdate { preferencesRepository.addHomeItem(item) }

    fun removeHomeItem(item: HomeItemRef) = launchPreferenceUpdate { preferencesRepository.removeHomeItem(item) }

    fun moveHomeItem(fromIndex: Int, toIndex: Int) = launchPreferenceUpdate {
        val currentState = uiState.value
        val fromItem = currentState.homeItems.getOrNull(fromIndex) ?: return@launchPreferenceUpdate
        val toItem = currentState.homeItems.getOrNull(toIndex) ?: return@launchPreferenceUpdate
        val persisted = currentState.preferences.homeItems
        val persistedFrom = persisted.indexOfFirst { it.stableId == fromItem.homeRef().stableId }
        val persistedTo = persisted.indexOfFirst { it.stableId == toItem.homeRef().stableId }
        preferencesRepository.moveHomeItem(persistedFrom, persistedTo)
    }

    fun renameApp(appId: String, name: String?) = launchPreferenceUpdate {
        preferencesRepository.renameApp(appId, name)
    }

    fun setAppHidden(appId: String, hidden: Boolean) = launchPreferenceUpdate {
        preferencesRepository.setAppHidden(appId, hidden)
    }

    fun setAppBlocked(appId: String, blocked: Boolean) = launchPreferenceUpdate {
        preferencesRepository.setAppBlocked(appId, blocked)
    }

    fun createFolder(name: String, appId: String? = null) = launchPreferenceUpdate {
        preferencesRepository.createFolder(name, appId)
    }

    fun renameFolder(folderId: String, name: String) = launchPreferenceUpdate {
        preferencesRepository.renameFolder(folderId, name)
    }

    fun deleteFolder(folderId: String) = launchPreferenceUpdate {
        preferencesRepository.deleteFolder(folderId)
    }

    fun moveAppToFolder(appId: String, folderId: String?) = launchPreferenceUpdate {
        preferencesRepository.moveAppToFolder(appId, folderId)
    }

    fun updateClockFormat(value: ClockFormat) = updateSettings { it.copy(clockFormat = value) }

    fun setShowDate(value: Boolean) = updateSettings { it.copy(showDate = value) }

    fun setShowBattery(value: Boolean) = updateSettings { it.copy(showBattery = value) }

    fun setShowDailyUsage(value: Boolean) = updateSettings { it.copy(showDailyUsage = value) }

    private fun updateSettings(transform: (com.rafael.minimallauncher.data.LauncherSettings) -> com.rafael.minimallauncher.data.LauncherSettings) =
        launchPreferenceUpdate { preferencesRepository.updateSettings(transform) }

    private fun launchPreferenceUpdate(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun LauncherItem.homeRef(): HomeItemRef = when (this) {
        is AppItem -> HomeItemRef.App(id)
        is FolderItem -> HomeItemRef.Folder(id)
    }
}
