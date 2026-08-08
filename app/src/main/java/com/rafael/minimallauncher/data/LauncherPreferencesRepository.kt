package com.rafael.minimallauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private const val DATASTORE_NAME = "launcher_preferences"
private val Context.launcherPreferencesDataStore by preferencesDataStore(name = DATASTORE_NAME)
private val LEGACY_FAVORITES = stringSetPreferencesKey("favorite_components")
private val HOME_ITEMS = stringPreferencesKey("home_items_v2")
private val CUSTOM_NAMES = stringPreferencesKey("custom_names")
private val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
private val BLOCKED_APPS = stringSetPreferencesKey("blocked_apps")
private val FOLDERS = stringPreferencesKey("folders")
private val APP_FOLDERS = stringPreferencesKey("app_folders")
private val CLOCK_FORMAT = stringPreferencesKey("clock_format")
private val SHOW_DATE = booleanPreferencesKey("show_date")
private val SHOW_BATTERY = booleanPreferencesKey("show_battery")
private val SHOW_DAILY_USAGE = booleanPreferencesKey("show_daily_usage")
private val FOCUS_SEARCH_ON_LIST_OPEN = booleanPreferencesKey("focus_search_on_list_open")
private val FONT = stringPreferencesKey("font")
private val TEXT_SIZE = stringPreferencesKey("text_size")
private val ACCENT = stringPreferencesKey("accent")

interface LauncherPreferencesRepository {
    val cachedPreferences: LauncherPreferences
    val preferences: Flow<LauncherPreferences>

    suspend fun reconcileInstalledApps(orderedAppIds: List<String>)
    suspend fun toggleHomeApp(appId: String)
    suspend fun toggleHomeItem(item: HomeItemRef)
    suspend fun addHomeItem(item: HomeItemRef)
    suspend fun removeHomeItem(item: HomeItemRef)
    suspend fun moveHomeItem(fromIndex: Int, toIndex: Int)
    suspend fun renameApp(appId: String, name: String?)
    suspend fun setAppHidden(appId: String, hidden: Boolean)
    suspend fun setAppBlocked(appId: String, blocked: Boolean)
    suspend fun createFolder(name: String, appId: String? = null)
    suspend fun renameFolder(folderId: String, name: String)
    suspend fun deleteFolder(folderId: String): FolderDeletionSnapshot?
    suspend fun restoreFolder(snapshot: FolderDeletionSnapshot, installedAppIds: Set<String>)
    suspend fun moveAppToFolder(appId: String, folderId: String?)
    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings)
}

class DataStoreLauncherPreferencesRepository(context: Context) : LauncherPreferencesRepository {
    private val applicationContext = context.applicationContext
    private val cache = applicationContext.getSharedPreferences(PREFERENCES_CACHE_NAME, Context.MODE_PRIVATE)
    private val dataStore = applicationContext.launcherPreferencesDataStore
    private val mutationMutex = Mutex()

    override val cachedPreferences: LauncherPreferences
        get() = decodeCachedPreferences()

    override val preferences: Flow<LauncherPreferences> = dataStore.data
        .map(::decodePreferences)
        .onEach(::cachePreferences)

    override suspend fun reconcileInstalledApps(orderedAppIds: List<String>) = mutationMutex.withLock {
        dataStore.edit { values ->
            val reconciled = LauncherPreferencesReconciler.reconcile(decodePreferences(values), orderedAppIds)
            values[CUSTOM_NAMES] = LauncherPreferencesCodec.encodeMap(reconciled.customNames)
            values[HIDDEN_APPS] = reconciled.hiddenAppIds
            values[BLOCKED_APPS] = reconciled.blockedAppIds
            values[APP_FOLDERS] = LauncherPreferencesCodec.encodeMap(reconciled.appFolders)
            values[LEGACY_FAVORITES] = reconciled.legacyFavoriteIds
            values[HOME_ITEMS] = LauncherPreferencesCodec.encodeHomeItems(reconciled.homeItems)
        }
        Unit
    }

    override suspend fun toggleHomeApp(appId: String) = updateHomeItems { current ->
        current.toggle(HomeItemRef.App(appId))
    }

    override suspend fun toggleHomeItem(item: HomeItemRef) = updateHomeItems { current ->
        current.toggle(item)
    }

    override suspend fun addHomeItem(item: HomeItemRef) = updateHomeItems { current ->
        if (item in current) current else current + item
    }

    override suspend fun removeHomeItem(item: HomeItemRef) = updateHomeItems { it - item }

    override suspend fun moveHomeItem(fromIndex: Int, toIndex: Int) = updateHomeItems { current ->
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) {
            current
        } else {
            current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    override suspend fun renameApp(appId: String, name: String?) = editMap(CUSTOM_NAMES) { current ->
        if (name.isNullOrBlank()) current - appId else current + (appId to name.trim())
    }

    override suspend fun setAppHidden(appId: String, hidden: Boolean) = editSet(HIDDEN_APPS, appId, hidden)

    override suspend fun setAppBlocked(appId: String, blocked: Boolean) = editSet(BLOCKED_APPS, appId, blocked)

    override suspend fun createFolder(name: String, appId: String?) {
        mutationMutex.withLock {
            dataStore.edit { values ->
                val folder = LauncherFolder(UUID.randomUUID().toString(), name.trim())
                val folders = LauncherPreferencesCodec.decodeFolders(values[FOLDERS].orEmpty()) + folder
                values[FOLDERS] = LauncherPreferencesCodec.encodeFolders(folders)
                if (appId != null) {
                    val memberships = LauncherPreferencesCodec.decodeMap(values[APP_FOLDERS].orEmpty())
                    values[APP_FOLDERS] = LauncherPreferencesCodec.encodeMap(memberships + (appId to folder.id))
                }
            }
        }
    }

    override suspend fun renameFolder(folderId: String, name: String) {
        mutationMutex.withLock {
            dataStore.edit { values ->
                val folders = LauncherPreferencesCodec.decodeFolders(values[FOLDERS].orEmpty())
                    .map { if (it.id == folderId) it.copy(name = name.trim()) else it }
                values[FOLDERS] = LauncherPreferencesCodec.encodeFolders(folders)
            }
        }
    }

    override suspend fun deleteFolder(folderId: String): FolderDeletionSnapshot? = mutationMutex.withLock {
        var snapshot: FolderDeletionSnapshot? = null
        dataStore.edit { values ->
            val reduced = LauncherFolderPreferencesReducer.delete(decodePreferences(values), folderId)
            if (reduced != null) {
                val (updatedPreferences, deletedSnapshot) = reduced
                snapshot = deletedSnapshot
                values[FOLDERS] = LauncherPreferencesCodec.encodeFolders(updatedPreferences.folders)
                values[APP_FOLDERS] = LauncherPreferencesCodec.encodeMap(updatedPreferences.appFolders)
                values[HOME_ITEMS] = LauncherPreferencesCodec.encodeHomeItems(updatedPreferences.homeItems)
            }
        }
        snapshot
    }

    override suspend fun restoreFolder(
        snapshot: FolderDeletionSnapshot,
        installedAppIds: Set<String>,
    ) = mutationMutex.withLock {
        dataStore.edit { values ->
            val current = decodePreferences(values)
            val restored = LauncherFolderPreferencesReducer.restore(current, snapshot, installedAppIds)
            values[FOLDERS] = LauncherPreferencesCodec.encodeFolders(restored.folders)
            values[APP_FOLDERS] = LauncherPreferencesCodec.encodeMap(restored.appFolders)
            values[HOME_ITEMS] = LauncherPreferencesCodec.encodeHomeItems(restored.homeItems)
        }
        Unit
    }

    override suspend fun moveAppToFolder(appId: String, folderId: String?) = editMap(APP_FOLDERS) { current ->
        if (folderId == null) current - appId else current + (appId to folderId)
    }

    override suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        mutationMutex.withLock {
            dataStore.edit { values -> writeSettings(values, transform(decodeSettings(values))) }
        }
    }

    private suspend fun updateHomeItems(transform: (List<HomeItemRef>) -> List<HomeItemRef>) {
        mutationMutex.withLock {
            dataStore.edit { values ->
                val current = LauncherPreferencesCodec.decodeHomeItems(values[HOME_ITEMS].orEmpty())
                values[HOME_ITEMS] = LauncherPreferencesCodec.encodeHomeItems(transform(current))
            }
        }
    }

    private fun List<HomeItemRef>.toggle(item: HomeItemRef): List<HomeItemRef> =
        if (item in this) this - item else this + item

    private suspend fun editMap(
        key: Preferences.Key<String>,
        transform: (Map<String, String>) -> Map<String, String>,
    ) {
        mutationMutex.withLock {
            dataStore.edit { values ->
                values[key] = LauncherPreferencesCodec.encodeMap(transform(LauncherPreferencesCodec.decodeMap(values[key].orEmpty())))
            }
        }
    }

    private suspend fun editSet(key: Preferences.Key<Set<String>>, value: String, enabled: Boolean) {
        mutationMutex.withLock {
            dataStore.edit { values ->
                val current = values[key].orEmpty()
                values[key] = if (enabled) current + value else current - value
            }
        }
    }

    private fun decodePreferences(values: Preferences): LauncherPreferences = LauncherPreferences(
        homeItems = LauncherPreferencesCodec.decodeHomeItems(values[HOME_ITEMS].orEmpty()),
        customNames = LauncherPreferencesCodec.decodeMap(values[CUSTOM_NAMES].orEmpty()),
        hiddenAppIds = values[HIDDEN_APPS].orEmpty(),
        blockedAppIds = values[BLOCKED_APPS].orEmpty(),
        folders = LauncherPreferencesCodec.decodeFolders(values[FOLDERS].orEmpty()),
        appFolders = LauncherPreferencesCodec.decodeMap(values[APP_FOLDERS].orEmpty()),
        settings = decodeSettings(values),
        needsFavoriteMigration = HOME_ITEMS !in values,
        legacyFavoriteIds = values[LEGACY_FAVORITES].orEmpty(),
    )

    private fun decodeSettings(values: Preferences) = LauncherSettings(
        clockFormat = runCatching { ClockFormat.valueOf(values[CLOCK_FORMAT].orEmpty()) }
            .getOrDefault(ClockFormat.SYSTEM),
        showDate = values[SHOW_DATE] ?: true,
        showBattery = values[SHOW_BATTERY] ?: true,
        showDailyUsage = values[SHOW_DAILY_USAGE] ?: true,
        focusSearchOnListOpen = values[FOCUS_SEARCH_ON_LIST_OPEN] ?: true,
        font = runCatching { LauncherFont.valueOf(values[FONT].orEmpty()) }
            .getOrDefault(LauncherFont.SYSTEM),
        textSize = runCatching { LauncherTextSize.valueOf(values[TEXT_SIZE].orEmpty()) }
            .getOrDefault(LauncherTextSize.MEDIUM),
        accent = runCatching { LauncherAccent.valueOf(values[ACCENT].orEmpty()) }
            .getOrDefault(LauncherAccent.MONOCHROME),
    )

    private fun writeSettings(values: androidx.datastore.preferences.core.MutablePreferences, settings: LauncherSettings) {
        values[CLOCK_FORMAT] = settings.clockFormat.name
        values[SHOW_DATE] = settings.showDate
        values[SHOW_BATTERY] = settings.showBattery
        values[SHOW_DAILY_USAGE] = settings.showDailyUsage
        values[FOCUS_SEARCH_ON_LIST_OPEN] = settings.focusSearchOnListOpen
        values[FONT] = settings.font.name
        values[TEXT_SIZE] = settings.textSize.name
        values[ACCENT] = settings.accent.name
    }

    private fun cachePreferences(preferences: LauncherPreferences) {
        cache.edit()
            .putString(CACHED_HOME_ITEMS, LauncherPreferencesCodec.encodeHomeItems(preferences.homeItems))
            .putString(CACHED_CUSTOM_NAMES, LauncherPreferencesCodec.encodeMap(preferences.customNames))
            .putStringSet(CACHED_HIDDEN_APPS, preferences.hiddenAppIds)
            .putStringSet(CACHED_BLOCKED_APPS, preferences.blockedAppIds)
            .putString(CACHED_FOLDERS, LauncherPreferencesCodec.encodeFolders(preferences.folders))
            .putString(CACHED_APP_FOLDERS, LauncherPreferencesCodec.encodeMap(preferences.appFolders))
            .putString(CACHED_CLOCK_FORMAT, preferences.settings.clockFormat.name)
            .putBoolean(CACHED_SHOW_DATE, preferences.settings.showDate)
            .putBoolean(CACHED_SHOW_BATTERY, preferences.settings.showBattery)
            .putBoolean(CACHED_SHOW_DAILY_USAGE, preferences.settings.showDailyUsage)
            .putBoolean(CACHED_FOCUS_SEARCH, preferences.settings.focusSearchOnListOpen)
            .putString(CACHED_FONT, preferences.settings.font.name)
            .putString(CACHED_TEXT_SIZE, preferences.settings.textSize.name)
            .putString(CACHED_ACCENT, preferences.settings.accent.name)
            .apply()
    }

    private fun decodeCachedPreferences(): LauncherPreferences {
        if (!cache.contains(CACHED_HOME_ITEMS)) return LauncherPreferences()
        return LauncherPreferences(
            homeItems = LauncherPreferencesCodec.decodeHomeItems(cache.getString(CACHED_HOME_ITEMS, null).orEmpty()),
            customNames = LauncherPreferencesCodec.decodeMap(cache.getString(CACHED_CUSTOM_NAMES, null).orEmpty()),
            hiddenAppIds = cache.getStringSet(CACHED_HIDDEN_APPS, emptySet()).orEmpty(),
            blockedAppIds = cache.getStringSet(CACHED_BLOCKED_APPS, emptySet()).orEmpty(),
            folders = LauncherPreferencesCodec.decodeFolders(cache.getString(CACHED_FOLDERS, null).orEmpty()),
            appFolders = LauncherPreferencesCodec.decodeMap(cache.getString(CACHED_APP_FOLDERS, null).orEmpty()),
            settings = LauncherSettings(
                clockFormat = runCatching {
                    ClockFormat.valueOf(cache.getString(CACHED_CLOCK_FORMAT, null).orEmpty())
                }.getOrDefault(ClockFormat.SYSTEM),
                showDate = cache.getBoolean(CACHED_SHOW_DATE, true),
                showBattery = cache.getBoolean(CACHED_SHOW_BATTERY, true),
                showDailyUsage = cache.getBoolean(CACHED_SHOW_DAILY_USAGE, true),
                focusSearchOnListOpen = cache.getBoolean(CACHED_FOCUS_SEARCH, true),
                font = runCatching {
                    LauncherFont.valueOf(cache.getString(CACHED_FONT, null).orEmpty())
                }.getOrDefault(LauncherFont.SYSTEM),
                textSize = runCatching {
                    LauncherTextSize.valueOf(cache.getString(CACHED_TEXT_SIZE, null).orEmpty())
                }.getOrDefault(LauncherTextSize.MEDIUM),
                accent = runCatching {
                    LauncherAccent.valueOf(cache.getString(CACHED_ACCENT, null).orEmpty())
                }.getOrDefault(LauncherAccent.MONOCHROME),
            ),
        )
    }

    private companion object {
        const val PREFERENCES_CACHE_NAME = "launcher_preferences_cache"
        const val CACHED_HOME_ITEMS = "home_items"
        const val CACHED_CUSTOM_NAMES = "custom_names"
        const val CACHED_HIDDEN_APPS = "hidden_apps"
        const val CACHED_BLOCKED_APPS = "blocked_apps"
        const val CACHED_FOLDERS = "folders"
        const val CACHED_APP_FOLDERS = "app_folders"
        const val CACHED_CLOCK_FORMAT = "clock_format"
        const val CACHED_SHOW_DATE = "show_date"
        const val CACHED_SHOW_BATTERY = "show_battery"
        const val CACHED_SHOW_DAILY_USAGE = "show_daily_usage"
        const val CACHED_FOCUS_SEARCH = "focus_search"
        const val CACHED_FONT = "font"
        const val CACHED_TEXT_SIZE = "text_size"
        const val CACHED_ACCENT = "accent"
    }
}
