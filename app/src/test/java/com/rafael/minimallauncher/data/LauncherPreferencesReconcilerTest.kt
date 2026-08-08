package com.rafael.minimallauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPreferencesReconcilerTest {
    @Test
    fun reconcileRemovesOnlyReferencesToMissingApps() {
        val preferences = LauncherPreferences(
            homeItems = listOf(
                HomeItemRef.App("installed"),
                HomeItemRef.App("removed"),
                HomeItemRef.Folder("tools"),
                HomeItemRef.Folder("missing-folder"),
            ),
            customNames = mapOf("installed" to "Keep", "removed" to "Drop"),
            hiddenAppIds = setOf("installed", "removed"),
            blockedAppIds = setOf("installed", "removed"),
            folders = listOf(LauncherFolder("tools", "Tools")),
            appFolders = mapOf(
                "installed" to "tools",
                "removed" to "tools",
                "orphaned" to "missing-folder",
            ),
            legacyFavoriteIds = setOf("installed", "removed"),
        )

        val result = LauncherPreferencesReconciler.reconcile(preferences, listOf("installed"))

        assertEquals(listOf(HomeItemRef.App("installed"), HomeItemRef.Folder("tools")), result.homeItems)
        assertEquals(mapOf("installed" to "Keep"), result.customNames)
        assertEquals(setOf("installed"), result.hiddenAppIds)
        assertEquals(setOf("installed"), result.blockedAppIds)
        assertEquals(mapOf("installed" to "tools"), result.appFolders)
        assertEquals(setOf("installed"), result.legacyFavoriteIds)
        assertFalse(result.needsFavoriteMigration)
    }

    @Test
    fun reconcileMigratesLegacyFavoritesInInstalledOrder() {
        val preferences = LauncherPreferences(
            needsFavoriteMigration = true,
            legacyFavoriteIds = setOf("second", "first", "removed"),
        )

        val result = LauncherPreferencesReconciler.reconcile(
            preferences,
            listOf("first", "second"),
        )

        assertEquals(
            listOf(HomeItemRef.App("first"), HomeItemRef.App("second")),
            result.homeItems,
        )
        assertTrue(result.legacyFavoriteIds == setOf("first", "second"))
        assertFalse(result.needsFavoriteMigration)
    }
}
