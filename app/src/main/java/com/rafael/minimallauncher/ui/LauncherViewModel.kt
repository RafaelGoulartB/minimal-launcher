package com.rafael.minimallauncher.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafael.minimallauncher.R
import com.rafael.minimallauncher.data.AppUninstallLauncher
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.AppListControlsPosition
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.DailyUsage
import com.rafael.minimallauncher.data.FolderDeletionSnapshot
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherAccent
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.data.LauncherFont
import com.rafael.minimallauncher.data.LauncherItem
import com.rafael.minimallauncher.data.LauncherPreferences
import com.rafael.minimallauncher.data.LauncherPreferencesRepository
import com.rafael.minimallauncher.data.LauncherRepository
import com.rafael.minimallauncher.data.LauncherSettings
import com.rafael.minimallauncher.data.LauncherTextSize
import com.rafael.minimallauncher.data.UninstallResult
import com.rafael.minimallauncher.data.UsageStatsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel

@OptIn(kotlinx.coroutines.FlowPreview::class)
class LauncherViewModel(
    private val launcherRepository: LauncherRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val uninstallLauncher: AppUninstallLauncher,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: (String, Throwable?) -> Unit = { message, error -> Log.e(TAG, message, error) },
) : ViewModel() {
    private val eventsChannel = Channel<LauncherUiEvent>(Channel.BUFFERED)
    val events: Flow<LauncherUiEvent> = eventsChannel.receiveAsFlow()

    private val apps = MutableStateFlow(launcherRepository.loadCachedApps())
    private val searchQuery = MutableStateFlow("")
    private val dailyUsage = MutableStateFlow(DailyUsage())
    private val initialCatalog = LauncherUiStateMapper.mapCatalog(
        installedApps = apps.value,
        preferences = preferencesRepository.cachedPreferences,
    )
    private val preferences: Flow<LauncherPreferences> = preferencesRepository.preferences
        .catch { exception ->
            if (exception is CancellationException) throw exception
            logger("Unable to read launcher preferences", exception)
            showError(R.string.error_preferences_read)
            emit(preferencesRepository.cachedPreferences)
        }
    private val catalog: StateFlow<LauncherCatalog> = combine(apps, preferences) { installedApps, values ->
        LauncherUiStateMapper.mapCatalog(installedApps, values)
    }.flowOn(dispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = initialCatalog,
    )
    private val debouncedSearchQuery = searchQuery
        .map(String::trim)
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged { previous, current -> previous.equals(current, ignoreCase = true) }
    val uiState: StateFlow<LauncherUiState> = combine(catalog, debouncedSearchQuery, dailyUsage) {
        currentCatalog,
        query,
        usage,
        -> LauncherUiStateMapper.mapState(currentCatalog, query, usage)
    }.flowOn(dispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiStateMapper.mapState(initialCatalog, "", dailyUsage.value),
    )

    private var appsRefreshJob: Job? = null
    private var usageRefreshJob: Job? = null
    private var refreshRequested = false
    private var nextUndoToken = 0L
    private var pendingUndo: PendingUndo? = null
    private var undoExpiryJob: Job? = null

    init {
        refreshApps()
        refreshUsage()
    }

    fun refreshApps() {
        if (appsRefreshJob?.isActive == true) {
            refreshRequested = true
            return
        }
        appsRefreshJob = viewModelScope.launch {
            try {
                var installedApps: List<LauncherApp>? = null
                for (attempt in 0 until APP_LOAD_ATTEMPTS) {
                    try {
                        installedApps = launcherRepository.loadApps()
                        break
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        logger("Unable to refresh installed apps (attempt ${attempt + 1})", exception)
                        if (attempt < APP_LOAD_ATTEMPTS - 1) delay(APP_LOAD_RETRY_DELAY_MS)
                    }
                }

                val refreshedApps = installedApps
                if (refreshedApps == null) {
                    showError(R.string.error_apps_refresh)
                } else {
                    apps.value = refreshedApps
                    try {
                        preferencesRepository.reconcileInstalledApps(refreshedApps.map(LauncherApp::id))
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        logger("Unable to reconcile launcher preferences", exception)
                        showError(R.string.error_preferences_cleanup)
                    }
                }
            } finally {
                appsRefreshJob = null
                if (refreshRequested) {
                    refreshRequested = false
                    refreshApps()
                }
            }
        }
    }

    fun onPackageChanged() = refreshApps()

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun refreshUsage() {
        if (usageRefreshJob?.isActive == true) return
        usageRefreshJob = viewModelScope.launch {
            try {
                dailyUsage.value = usageStatsRepository.loadTodayUsage()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger("Unable to refresh usage stats", exception)
                showError(R.string.error_usage_refresh)
            } finally {
                usageRefreshJob = null
            }
        }
    }

    fun toggleFavorite(app: LauncherApp) {
        launchPreferenceUpdate { preferencesRepository.toggleHomeApp(app.id) }
    }

    fun toggleHomeItem(item: HomeItemRef) = launchPreferenceUpdate {
        preferencesRepository.toggleHomeItem(item)
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

    fun hideApp(appId: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.setAppHidden(appId, true)
                val token = registerUndo(PendingUndo.HideApp(nextToken(), appId))
                showSnackbar(R.string.app_hidden, token)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger("Unable to hide app", exception)
                showError(R.string.error_preferences_write)
            }
        }
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

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            try {
                val snapshot = preferencesRepository.deleteFolder(folderId) ?: return@launch
                val token = registerUndo(PendingUndo.DeleteFolder(nextToken(), snapshot))
                showSnackbar(R.string.folder_deleted, token)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger("Unable to delete folder", exception)
                showError(R.string.error_preferences_write)
            }
        }
    }

    fun undo(token: Long) {
        val operation = pendingUndo?.takeIf { it.token == token } ?: return
        pendingUndo = null
        undoExpiryJob?.cancel()
        viewModelScope.launch {
            try {
                when (operation) {
                    is PendingUndo.HideApp -> preferencesRepository.setAppHidden(operation.appId, false)
                    is PendingUndo.DeleteFolder -> preferencesRepository.restoreFolder(
                        operation.snapshot,
                        apps.value.mapTo(mutableSetOf(), LauncherApp::id),
                    )
                }
                showSnackbar(
                    when (operation) {
                        is PendingUndo.HideApp -> R.string.app_restored
                        is PendingUndo.DeleteFolder -> R.string.folder_restored
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger("Unable to undo launcher operation", exception)
                showError(R.string.error_preferences_write)
            }
        }
    }

    fun moveAppToFolder(appId: String, folderId: String?) = launchPreferenceUpdate {
        preferencesRepository.moveAppToFolder(appId, folderId)
    }

    fun requestUninstall(app: LauncherApp) {
        viewModelScope.launch {
            try {
                when (uninstallLauncher.requestUninstall(app)) {
                    UninstallResult.Started -> Unit
                    UninstallResult.Unavailable -> {
                        logger("No uninstall activity available for ${app.id}", null)
                        showError(R.string.error_uninstall_unavailable)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger("Unable to open uninstall flow", exception)
                showError(R.string.error_uninstall_unavailable)
            }
        }
    }

    fun updateClockFormat(value: ClockFormat) = updateSettings { it.copy(clockFormat = value) }

    fun setShowDate(value: Boolean) = updateSettings { it.copy(showDate = value) }

    fun setShowBattery(value: Boolean) = updateSettings { it.copy(showBattery = value) }

    fun setShowDailyUsage(value: Boolean) = updateSettings { it.copy(showDailyUsage = value) }

    fun setFocusSearchOnListOpen(value: Boolean) = updateSettings { it.copy(focusSearchOnListOpen = value) }

    fun setAppListControlsPosition(value: AppListControlsPosition) =
        updateSettings { it.copy(appListControlsPosition = value) }

    fun setFont(value: LauncherFont) = updateSettings { it.copy(font = value) }

    fun setTextSize(value: LauncherTextSize) = updateSettings { it.copy(textSize = value) }

    fun setAccent(value: LauncherAccent) = updateSettings { it.copy(accent = value) }

    private fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) =
        launchPreferenceUpdate { preferencesRepository.updateSettings(transform) }

    private fun launchPreferenceUpdate(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger("Unable to update launcher preferences", exception)
                showError(R.string.error_preferences_write)
            }
        }
    }

    private fun nextToken(): Long = ++nextUndoToken

    private fun registerUndo(operation: PendingUndo): LauncherSnackbarAction {
        pendingUndo = operation
        undoExpiryJob?.cancel()
        undoExpiryJob = viewModelScope.launch {
            delay(UNDO_TIMEOUT_MS)
            if (pendingUndo?.token == operation.token) pendingUndo = null
        }
        return LauncherSnackbarAction(operation.token)
    }

    private fun showSnackbar(@StringRes messageRes: Int, action: LauncherSnackbarAction? = null) {
        eventsChannel.trySend(LauncherUiEvent.Snackbar(messageRes = messageRes, action = action))
    }

    private fun showError(@StringRes messageRes: Int) {
        eventsChannel.trySend(LauncherUiEvent.Snackbar(messageRes = messageRes, isError = true))
    }

    private fun LauncherItem.homeRef(): HomeItemRef = when (this) {
        is AppItem -> HomeItemRef.App(id)
        is com.rafael.minimallauncher.data.FolderItem -> HomeItemRef.Folder(id)
    }

    private sealed interface PendingUndo {
        val token: Long

        data class HideApp(override val token: Long, val appId: String) : PendingUndo

        data class DeleteFolder(override val token: Long, val snapshot: FolderDeletionSnapshot) : PendingUndo
    }

    private companion object {
        const val TAG = "LauncherViewModel"
        const val APP_LOAD_ATTEMPTS = 3
        const val APP_LOAD_RETRY_DELAY_MS = 250L
        const val SEARCH_DEBOUNCE_MS = 200L
        const val UNDO_TIMEOUT_MS = 10_000L
    }
}
