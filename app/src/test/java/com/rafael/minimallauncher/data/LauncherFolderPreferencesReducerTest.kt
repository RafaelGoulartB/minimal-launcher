package com.rafael.minimallauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherFolderPreferencesReducerTest {
    private val folder = LauncherFolder("tools", "Tools")

    @Test
    fun deleteReturnsSnapshotAndMovesMembersOutOfFolder() {
        val preferences = LauncherPreferences(
            homeItems = listOf(
                HomeItemRef.App("calendar"),
                HomeItemRef.Folder(folder.id),
                HomeItemRef.App("notes"),
            ),
            folders = listOf(folder),
            appFolders = mapOf(
                "calendar" to folder.id,
                "notes" to folder.id,
            ),
        )

        val result = LauncherFolderPreferencesReducer.delete(preferences, folder.id)
        assertNotNull(result)
        val (updated, snapshot) = result!!

        assertEquals(folder, snapshot.folder)
        assertEquals(setOf("calendar", "notes"), snapshot.memberAppIds)
        assertEquals(1, snapshot.homeIndex)
        assertEquals(emptyList<LauncherFolder>(), updated.folders)
        assertEquals(emptyMap<String, String>(), updated.appFolders)
        assertEquals(
            listOf(HomeItemRef.App("calendar"), HomeItemRef.App("notes")),
            updated.homeItems,
        )
    }

    @Test
    fun restoreUsesOriginalHomePositionAndOnlyRestoresInstalledMembers() {
        val preferences = LauncherPreferences(
            homeItems = listOf(
                HomeItemRef.App("calendar"),
                HomeItemRef.App("notes"),
            ),
        )
        val snapshot = FolderDeletionSnapshot(
            folder = folder,
            memberAppIds = setOf("calendar", "removed"),
            homeIndex = 1,
        )

        val restored = LauncherFolderPreferencesReducer.restore(
            preferences = preferences,
            snapshot = snapshot,
            installedAppIds = setOf("calendar", "notes"),
        )

        assertEquals(listOf(folder), restored.folders)
        assertEquals(mapOf("calendar" to folder.id), restored.appFolders)
        assertEquals(
            listOf(
                HomeItemRef.App("calendar"),
                HomeItemRef.Folder(folder.id),
                HomeItemRef.App("notes"),
            ),
            restored.homeItems,
        )
    }

    @Test
    fun restoreDoesNotOverwriteConflictingFolderOrHomePlacement() {
        val conflictingFolder = LauncherFolder(folder.id, "New name")
        val preferences = LauncherPreferences(
            homeItems = listOf(HomeItemRef.Folder(folder.id)),
            folders = listOf(conflictingFolder),
            appFolders = mapOf("new-app" to folder.id),
        )
        val snapshot = FolderDeletionSnapshot(
            folder = folder,
            memberAppIds = setOf("old-app"),
            homeIndex = 0,
        )

        val restored = LauncherFolderPreferencesReducer.restore(
            preferences = preferences,
            snapshot = snapshot,
            installedAppIds = setOf("old-app", "new-app"),
        )

        assertEquals(listOf(conflictingFolder), restored.folders)
        assertEquals(mapOf("new-app" to folder.id), restored.appFolders)
        assertEquals(listOf(HomeItemRef.Folder(folder.id)), restored.homeItems)
    }

    @Test
    fun deleteUnknownFolderReturnsNull() {
        assertNull(LauncherFolderPreferencesReducer.delete(LauncherPreferences(), "missing"))
    }
}
