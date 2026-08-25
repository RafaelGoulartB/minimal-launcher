package com.rafael.minimallauncher.ui

import android.content.ComponentName
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.DailyUsage
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.data.LauncherFolder
import com.rafael.minimallauncher.data.LauncherPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LauncherUiStateMapperTest {
    private val calendar = LauncherApp(
        "Calendar",
        ComponentName("", ""),
        stableIdOverride = "calendar",
    )
    private val notes = LauncherApp(
        "Notes",
        ComponentName("", ""),
        stableIdOverride = "notes",
    )

    @Test
    fun customNameIsUsedForSearchAndDisplay() {
        val preferences = LauncherPreferences(customNames = mapOf(calendar.id to "Planning"))
        val catalog = LauncherUiStateMapper.mapCatalog(listOf(calendar), preferences, Locale.US)

        val state = LauncherUiStateMapper.mapState(catalog, "plan", DailyUsage())

        assertEquals(listOf(calendar), state.filteredApps)
        assertEquals("Planning", (state.drawerItems.single() as AppItem).label)
    }

    @Test
    fun searchIgnoresDiacritics() {
        val accentedApp = LauncherApp(
            "Câmera",
            ComponentName("", ""),
            stableIdOverride = "camera",
        )
        val catalog = LauncherUiStateMapper.mapCatalog(listOf(accentedApp), LauncherPreferences(), Locale.US)

        val state = LauncherUiStateMapper.mapState(catalog, "camera", DailyUsage())

        assertEquals(listOf(accentedApp), state.filteredApps)
        assertEquals("Câmera", (state.drawerItems.single() as AppItem).label)
    }

    @Test
    fun singleLaunchableSearchResultRequiresOneMatchingAppAndNoMatchingFolder() {
        val preferences = LauncherPreferences(
            folders = listOf(LauncherFolder("camera-folder", "Camera tools")),
        )

        assertEquals(
            calendar,
            LauncherUiStateMapper.singleLaunchableSearchResult(
                installedApps = listOf(calendar, notes),
                preferences = LauncherPreferences(customNames = mapOf(calendar.id to "Calendário")),
                query = "calendario",
                locale = Locale.US,
            )?.app,
        )
        assertEquals(
            null,
            LauncherUiStateMapper.singleLaunchableSearchResult(
                installedApps = listOf(calendar),
                preferences = preferences,
                query = "camera",
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun blockedAppsRemainVisibleAndTheirStateIsPreserved() {
        val preferences = LauncherPreferences(blockedAppIds = setOf(calendar.id))
        val catalog = LauncherUiStateMapper.mapCatalog(listOf(calendar), preferences, Locale.US)

        val state = LauncherUiStateMapper.mapState(catalog, "", DailyUsage())

        assertTrue(state.drawerItems.any { it.id == calendar.id })
        assertEquals(setOf(calendar.id), state.preferences.blockedAppIds)
    }

    @Test
    fun hiddenAppsDoNotAppearInFoldersOrDrawer() {
        val preferences = LauncherPreferences(
            hiddenAppIds = setOf(notes.id),
            folders = listOf(LauncherFolder("tools", "Tools")),
            appFolders = mapOf(notes.id to "tools"),
            homeItems = listOf(HomeItemRef.Folder("tools")),
        )
        val catalog = LauncherUiStateMapper.mapCatalog(listOf(calendar, notes), preferences, Locale.US)

        val state = LauncherUiStateMapper.mapState(catalog, "", DailyUsage())

        val folder = state.drawerItems.filterIsInstance<com.rafael.minimallauncher.data.FolderItem>().single()
        assertEquals(emptyList<com.rafael.minimallauncher.data.AppItem>(), folder.apps)
        assertEquals(listOf(folder), state.homeItems)
    }
}
