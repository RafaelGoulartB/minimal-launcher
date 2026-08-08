package com.rafael.minimallauncher.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreLauncherPreferencesRepositoryTest {
    @Test
    fun repositoriesShareTheSameDataStoreInstance() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstRepository = DataStoreLauncherPreferencesRepository(context)
        val secondRepository = DataStoreLauncherPreferencesRepository(context)

        firstRepository.updateSettings { it.copy(showDate = false) }
        assertFalse(secondRepository.preferences.first().settings.showDate)

        secondRepository.updateSettings { it.copy(showDate = true) }
        assertTrue(firstRepository.preferences.first().settings.showDate)
    }
}
