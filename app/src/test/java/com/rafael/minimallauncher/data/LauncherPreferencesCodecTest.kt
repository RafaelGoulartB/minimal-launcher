package com.rafael.minimallauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherPreferencesCodecTest {
    @Test
    fun homeItems_roundTrip_orderAndTypes() {
        val items = listOf(
            HomeItemRef.App("com.example/.Main"),
            HomeItemRef.Folder("folder-1"),
            HomeItemRef.App("unicode/ação"),
        )

        assertEquals(items, LauncherPreferencesCodec.decodeHomeItems(LauncherPreferencesCodec.encodeHomeItems(items)))
    }

    @Test
    fun homeItems_decoder_ignoresMalformedAndDuplicateEntries() {
        val encoded = LauncherPreferencesCodec.encodeHomeItems(
            listOf(HomeItemRef.App("one"), HomeItemRef.App("one")),
        ) + "\ninvalid\na:not-base64%%%"

        assertEquals(listOf(HomeItemRef.App("one")), LauncherPreferencesCodec.decodeHomeItems(encoded))
    }

    @Test
    fun maps_roundTrip_tabsNewlinesAndUnicode() {
        val values = mapOf(
            "component/one" to "My\tApp",
            "component/two" to "Ação\nNova",
        )

        assertEquals(values, LauncherPreferencesCodec.decodeMap(LauncherPreferencesCodec.encodeMap(values)))
    }

    @Test
    fun folders_roundTrip_idsAndNames() {
        val folders = listOf(
            LauncherFolder("one", "Work"),
            LauncherFolder("two", "Música"),
        )

        assertEquals(folders.toSet(), LauncherPreferencesCodec.decodeFolders(LauncherPreferencesCodec.encodeFolders(folders)).toSet())
    }
}
