package com.rafael.minimallauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

private const val DATASTORE_NAME = "launcher_preferences"
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

class LauncherPreferencesRepository(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(DATASTORE_NAME) },
    )

    val preferences: Flow<LauncherPreferences> = dataStore.data
        .catch { exception ->
            if (exception is CancellationException) throw exception
            // HOME must remain usable even if the preferences file cannot be read.
            emit(emptyPreferences())
        }
        .map(::decodePreferences)

    suspend fun migrateLegacyFavorites(orderedAppIds: List<String>) {
        dataStore.edit { values ->
            if (HOME_ITEMS !in values) {
                val legacy = values[LEGACY_FAVORITES].orEmpty()
                val ordered = orderedAppIds.filter(legacy::contains).map(HomeItemRef::App)
                values[HOME_ITEMS] = LauncherPreferencesCodec.encodeHomeItems(ordered)
            }
        }
    }

    suspend fun toggleHomeApp(appId: String) = updateHomeItems { current ->
        val ref = HomeItemRef.App(appId)
        if (ref in current) current - ref else current + ref
    }

    suspend fun addHomeItem(item: HomeItemRef) = updateHomeItems { current ->
        if (item in current) current else current + item
    }

    suspend fun removeHomeItem(item: HomeItemRef) = updateHomeItems { it - item }

    suspend fun moveHomeItem(fromIndex: Int, toIndex: Int) = updateHomeItems { current ->
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) {
            current
        } else {
            current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    suspend fun renameApp(appId: String, name: String?) = editMap(CUSTOM_NAMES) { current ->
        if (name.isNullOrBlank()) current - appId else current + (appId to name.trim())
    }

    suspend fun setAppHidden(appId: String, hidden: Boolean) = editSet(HIDDEN_APPS, appId, hidden)

    suspend fun setAppBlocked(appId: String, blocked: Boolean) = editSet(BLOCKED_APPS, appId, blocked)

    suspend fun createFolder(name: String, appId: String? = null) {
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

    suspend fun renameFolder(folderId: String, name: String) {
        dataStore.edit { values ->
            val folders = LauncherPreferencesCodec.decodeFolders(values[FOLDERS].orEmpty())
                .map { if (it.id == folderId) it.copy(name = name.trim()) else it }
            values[FOLDERS] = LauncherPreferencesCodec.encodeFolders(folders)
        }
    }

    suspend fun deleteFolder(folderId: String) {
        dataStore.edit { values ->
            val folders = LauncherPreferencesCodec.decodeFolders(values[FOLDERS].orEmpty())
                .filterNot { it.id == folderId }
            val memberships = LauncherPreferencesCodec.decodeMap(values[APP_FOLDERS].orEmpty())
                .filterValues { it != folderId }
            val homeItems = LauncherPreferencesCodec.decodeHomeItems(values[HOME_ITEMS].orEmpty())
                .filterNot { it == HomeItemRef.Folder(folderId) }
            values[FOLDERS] = LauncherPreferencesCodec.encodeFolders(folders)
            values[APP_FOLDERS] = LauncherPreferencesCodec.encodeMap(memberships)
            values[HOME_ITEMS] = LauncherPreferencesCodec.encodeHomeItems(homeItems)
        }
    }

    suspend fun moveAppToFolder(appId: String, folderId: String?) = editMap(APP_FOLDERS) { current ->
        if (folderId == null) current - appId else current + (appId to folderId)
    }

    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        dataStore.edit { values -> writeSettings(values, transform(decodeSettings(values))) }
    }

    private suspend fun updateHomeItems(transform: (List<HomeItemRef>) -> List<HomeItemRef>) {
        dataStore.edit { values ->
            val current = LauncherPreferencesCodec.decodeHomeItems(values[HOME_ITEMS].orEmpty())
            values[HOME_ITEMS] = LauncherPreferencesCodec.encodeHomeItems(transform(current))
        }
    }

    private suspend fun editMap(
        key: Preferences.Key<String>,
        transform: (Map<String, String>) -> Map<String, String>,
    ) {
        dataStore.edit { values ->
            values[key] = LauncherPreferencesCodec.encodeMap(transform(LauncherPreferencesCodec.decodeMap(values[key].orEmpty())))
        }
    }

    private suspend fun editSet(key: Preferences.Key<Set<String>>, value: String, enabled: Boolean) {
        dataStore.edit { values ->
            val current = values[key].orEmpty()
            values[key] = if (enabled) current + value else current - value
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
    )

    private fun writeSettings(values: androidx.datastore.preferences.core.MutablePreferences, settings: LauncherSettings) {
        values[CLOCK_FORMAT] = settings.clockFormat.name
        values[SHOW_DATE] = settings.showDate
        values[SHOW_BATTERY] = settings.showBattery
        values[SHOW_DAILY_USAGE] = settings.showDailyUsage
        values[FOCUS_SEARCH_ON_LIST_OPEN] = settings.focusSearchOnListOpen
    }
}
