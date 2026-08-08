package com.rafael.minimallauncher.ui

import android.content.ComponentName
import com.rafael.minimallauncher.R
import com.rafael.minimallauncher.data.AppUninstallLauncher
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.DailyUsage
import com.rafael.minimallauncher.data.FolderDeletionSnapshot
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherAccent
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.data.LauncherFont
import com.rafael.minimallauncher.data.LauncherFolder
import com.rafael.minimallauncher.data.LauncherPreferences
import com.rafael.minimallauncher.data.LauncherPreferencesRepository
import com.rafael.minimallauncher.data.LauncherRepository
import com.rafael.minimallauncher.data.LauncherSettings
import com.rafael.minimallauncher.data.LauncherTextSize
import com.rafael.minimallauncher.data.UninstallResult
import com.rafael.minimallauncher.data.UsageStatsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val app = LauncherApp(
        "Calendar",
        ComponentName("", ""),
        stableIdOverride = "calendar",
    )

    @Test
    fun refreshReconcilesPreferencesAfterUpdatingApps() = runTest {
        val preferences = FakePreferencesRepository()
        val repository = FakeLauncherRepository(cached = emptyList(), loaded = listOf(app))
        val viewModel = createViewModel(repository, preferences)

        advanceUntilIdle()

        assertEquals(listOf(app.id), preferences.reconciledIds)
    }

    @Test
    fun successfulEmptyRefreshReplacesCachedAppsAndCleansReferences() = runTest {
        val preferences = FakePreferencesRepository()
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app), loaded = emptyList()),
            preferences,
        )
        val stateJob = backgroundScope.launch { viewModel.uiState.collect() }

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.apps.isEmpty())
        assertEquals(emptyList<String>(), preferences.reconciledIds)
        stateJob.cancel()
    }

    @Test
    fun repeatedRefreshFailureEmitsVisibleError() = runTest {
        val repository = FakeLauncherRepository(cached = listOf(app), failure = true)
        val viewModel = createViewModel(repository, FakePreferencesRepository())

        val eventDeferred = async { viewModel.events.first() }
        advanceUntilIdle()
        val event = eventDeferred.await()

        assertTrue(event is LauncherUiEvent.Snackbar && event.isError)
        assertEquals(listOf(app), viewModel.uiState.value.apps)
        assertEquals(3, repository.loadAttempts)
    }

    @Test
    fun hidingAppCanBeUndone() = runTest {
        val preferences = FakePreferencesRepository()
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app), loaded = listOf(app)),
            preferences,
        )
        runCurrent()

        viewModel.hideApp(app.id)
        runCurrent()
        val event = viewModel.events.first() as LauncherUiEvent.Snackbar
        viewModel.undo(event.action!!.token)
        runCurrent()

        assertTrue(app.id !in preferences.current.hiddenAppIds)
    }

    @Test
    fun dragOrderUsesPersistedHomeIndices() = runTest {
        val second = LauncherApp("Notes", ComponentName("", ""), stableIdOverride = "notes")
        val initial = LauncherPreferences(homeItems = listOf(HomeItemRef.App(app.id), HomeItemRef.App(second.id)))
        val preferences = FakePreferencesRepository(initial)
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app, second), loaded = listOf(app, second)),
            preferences,
        )
        advanceUntilIdle()

        viewModel.moveHomeItem(0, 1)
        advanceUntilIdle()

        assertEquals(0 to 1, preferences.lastMove)
    }

    @Test
    fun repeatedHomeToggleUsesLatestPersistedValue() = runTest {
        val preferences = FakePreferencesRepository()
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app), loaded = listOf(app)),
            preferences,
        )
        runCurrent()

        viewModel.toggleHomeItem(HomeItemRef.App(app.id))
        viewModel.toggleHomeItem(HomeItemRef.App(app.id))
        advanceUntilIdle()

        assertTrue(preferences.current.homeItems.isEmpty())
    }

    @Test
    fun searchUsesCustomNameAfterDebounce() = runTest {
        val renamed = app.copy(label = "Calendar", stableIdOverride = app.id)
        val preferences = FakePreferencesRepository(
            LauncherPreferences(customNames = mapOf(app.id to "Planning")),
        )
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(renamed), loaded = listOf(renamed)),
            preferences,
        )
        val stateJob = backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.setSearchQuery("plan")
        runCurrent()
        assertEquals("", viewModel.uiState.value.searchQuery)
        advanceTimeBy(199)
        runCurrent()
        assertEquals("", viewModel.uiState.value.searchQuery)
        advanceTimeBy(1)
        runCurrent()

        assertEquals("plan", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(renamed), viewModel.uiState.value.filteredApps)
        stateJob.cancel()
    }

    @Test
    fun usageFailureEmitsVisibleErrorAndKeepsLastUsage() = runTest {
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app), loaded = listOf(app)),
            FakePreferencesRepository(),
            usageStatsRepository = FakeUsageStatsRepository(failure = true),
        )
        val eventDeferred = async { viewModel.events.first() }

        advanceUntilIdle()

        val event = eventDeferred.await()
        assertTrue(event is LauncherUiEvent.Snackbar && event.isError)
        assertEquals(DailyUsage(), viewModel.uiState.value.dailyUsage)
    }

    @Test
    fun uninstallFailureEmitsVisibleError() = runTest {
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app), loaded = listOf(app)),
            FakePreferencesRepository(),
            uninstallLauncher = FakeUninstallLauncher(UninstallResult.Unavailable),
        )
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.requestUninstall(app)
        runCurrent()

        val event = eventDeferred.await() as LauncherUiEvent.Snackbar
        assertEquals(R.string.error_uninstall_unavailable, event.messageRes)
        assertTrue(event.isError)
    }

    @Test
    fun preferenceReadFailureEmitsVisibleErrorAndUsesCache() = runTest {
        val preferences = FakePreferencesRepository(
            initial = LauncherPreferences(customNames = mapOf(app.id to "Cached")),
            preferenceFailure = true,
        )
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app), loaded = listOf(app)),
            preferences,
        )
        val stateJob = backgroundScope.launch { viewModel.uiState.collect() }
        val eventDeferred = async { viewModel.events.first() }

        advanceUntilIdle()

        val event = eventDeferred.await() as LauncherUiEvent.Snackbar
        assertEquals(R.string.error_preferences_read, event.messageRes)
        assertEquals("Cached", viewModel.uiState.value.preferences.customNames[app.id])
        stateJob.cancel()
    }

    @Test
    fun preferenceWriteFailureEmitsVisibleError() = runTest {
        val preferences = FakePreferencesRepository(writeFailure = true)
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app), loaded = listOf(app)),
            preferences,
        )
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.hideApp(app.id)
        runCurrent()

        val event = eventDeferred.await() as LauncherUiEvent.Snackbar
        assertEquals(R.string.error_preferences_write, event.messageRes)
        assertTrue(event.isError)
    }

    @Test
    fun deletingFolderShowsUndoAndRestoresSnapshot() = runTest {
        val snapshot = FolderDeletionSnapshot(
            folder = LauncherFolder("tools", "Tools"),
            memberAppIds = setOf(app.id),
            homeIndex = 0,
        )
        val preferences = FakePreferencesRepository(deleteSnapshot = snapshot)
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app), loaded = listOf(app)),
            preferences,
        )
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.deleteFolder(snapshot.folder.id)
        runCurrent()
        val event = eventDeferred.await() as LauncherUiEvent.Snackbar

        assertEquals(R.string.folder_deleted, event.messageRes)
        assertTrue(event.action != null)
        viewModel.undo(event.action!!.token)
        runCurrent()

        assertEquals(snapshot, preferences.restoredSnapshot)
    }

    @Test
    fun undoExpiresAfterSnackbarDuration() = runTest {
        val preferences = FakePreferencesRepository()
        val viewModel = createViewModel(
            FakeLauncherRepository(cached = listOf(app), loaded = listOf(app)),
            preferences,
        )
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.hideApp(app.id)
        runCurrent()
        val event = eventDeferred.await() as LauncherUiEvent.Snackbar
        advanceTimeBy(10_001)
        runCurrent()
        viewModel.undo(event.action!!.token)
        runCurrent()

        assertTrue(app.id in preferences.current.hiddenAppIds)
    }

    private fun createViewModel(
        repository: FakeLauncherRepository,
        preferences: FakePreferencesRepository,
        usageStatsRepository: UsageStatsRepository = FakeUsageStatsRepository(),
        uninstallLauncher: AppUninstallLauncher = FakeUninstallLauncher(),
    ) = LauncherViewModel(
        launcherRepository = repository,
        preferencesRepository = preferences,
        usageStatsRepository = usageStatsRepository,
        uninstallLauncher = uninstallLauncher,
        dispatcher = mainDispatcherRule.dispatcher,
        logger = { _, _ -> },
    )

    private class FakeLauncherRepository(
        private val cached: List<LauncherApp>,
        private val loaded: List<LauncherApp> = emptyList(),
        private val failure: Boolean = false,
    ) : LauncherRepository {
        var loadAttempts = 0

        override fun loadCachedApps() = cached

        override suspend fun loadApps(): List<LauncherApp> {
            loadAttempts++
            if (failure) error("refresh failed")
            return loaded
        }
    }

    private class FakeUsageStatsRepository(private val failure: Boolean = false) : UsageStatsRepository {
        override suspend fun loadTodayUsage(): DailyUsage {
            if (failure) error("usage failed")
            return DailyUsage()
        }
    }

    private class FakeUninstallLauncher(
        private val result: UninstallResult = UninstallResult.Started,
    ) : AppUninstallLauncher {
        override fun requestUninstall(app: LauncherApp) = result
    }

    private class FakePreferencesRepository(
        initial: LauncherPreferences = LauncherPreferences(),
        private val deleteSnapshot: FolderDeletionSnapshot? = null,
        private val preferenceFailure: Boolean = false,
        private val writeFailure: Boolean = false,
    ) : LauncherPreferencesRepository {
        private val state = MutableStateFlow(initial)
        val current: LauncherPreferences get() = state.value
        var reconciledIds: List<String> = emptyList()
        var lastMove: Pair<Int, Int>? = null
        var restoredSnapshot: FolderDeletionSnapshot? = null

        override val cachedPreferences: LauncherPreferences get() = state.value
        override val preferences: Flow<LauncherPreferences> = if (preferenceFailure) {
            flow { error("preferences read failed") }
        } else {
            state
        }

        override suspend fun reconcileInstalledApps(orderedAppIds: List<String>) {
            reconciledIds = orderedAppIds
        }

        override suspend fun toggleHomeApp(appId: String) = toggleHomeItem(HomeItemRef.App(appId))
        override suspend fun toggleHomeItem(item: HomeItemRef) {
            state.value = state.value.copy(
                homeItems = if (item in state.value.homeItems) {
                    state.value.homeItems - item
                } else {
                    state.value.homeItems + item
                },
            )
        }
        override suspend fun addHomeItem(item: HomeItemRef) = Unit
        override suspend fun removeHomeItem(item: HomeItemRef) = Unit
        override suspend fun moveHomeItem(fromIndex: Int, toIndex: Int) {
            lastMove = fromIndex to toIndex
        }
        override suspend fun renameApp(appId: String, name: String?) = Unit
        override suspend fun setAppHidden(appId: String, hidden: Boolean) {
            if (writeFailure) error("preferences write failed")
            state.value = state.value.copy(
                hiddenAppIds = if (hidden) state.value.hiddenAppIds + appId else state.value.hiddenAppIds - appId,
            )
        }
        override suspend fun setAppBlocked(appId: String, blocked: Boolean) = Unit
        override suspend fun createFolder(name: String, appId: String?) = Unit
        override suspend fun renameFolder(folderId: String, name: String) = Unit
        override suspend fun deleteFolder(folderId: String): FolderDeletionSnapshot? = deleteSnapshot
        override suspend fun restoreFolder(snapshot: FolderDeletionSnapshot, installedAppIds: Set<String>) {
            restoredSnapshot = snapshot
        }
        override suspend fun moveAppToFolder(appId: String, folderId: String?) = Unit
        override suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
            state.value = state.value.copy(settings = transform(state.value.settings))
        }
    }
}
