package com.rafael.minimallauncher.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "launcher_preferences"
private val FAVORITES = stringSetPreferencesKey("favorite_components")

class FavoritesRepository(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(DATASTORE_NAME) },
    )

    val favoriteIds: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[FAVORITES].orEmpty()
    }

    suspend fun toggle(app: LauncherApp) {
        dataStore.edit { preferences ->
            val current = preferences[FAVORITES].orEmpty()
            preferences[FAVORITES] = if (app.id in current) current - app.id else current + app.id
        }
    }
}

