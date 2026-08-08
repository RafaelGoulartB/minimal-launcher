package com.rafael.minimallauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rafael.minimallauncher.data.FavoritesRepository
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.data.LauncherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class LauncherUiState(
    val apps: List<LauncherApp> = emptyList(),
    val filteredApps: List<LauncherApp> = emptyList(),
    val favoriteApps: List<LauncherApp> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val launcherRepository = LauncherRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<LauncherUiState> = combine(
        apps,
        favoritesRepository.favoriteIds,
        searchQuery.debounce(120),
    ) { installedApps, favoriteIds, query ->
        LauncherUiState(
            apps = installedApps,
            filteredApps = installedApps.filter { app ->
                query.isBlank() || app.label.contains(query, ignoreCase = true)
            },
            favoriteApps = installedApps.filter { it.id in favoriteIds },
            favoriteIds = favoriteIds,
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
    }

    fun refreshApps() {
        viewModelScope.launch {
            apps.value = launcherRepository.loadApps()
        }
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun toggleFavorite(app: LauncherApp) {
        viewModelScope.launch { favoritesRepository.toggle(app) }
    }
}
