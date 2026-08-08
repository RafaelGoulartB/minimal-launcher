package com.rafael.minimallauncher.data

import java.nio.charset.StandardCharsets
import java.util.Base64

enum class ClockFormat {
    SYSTEM,
    TWELVE_HOUR,
    TWENTY_FOUR_HOUR,
}

sealed interface HomeItemRef {
    val value: String
    val stableId: String

    data class App(override val value: String) : HomeItemRef {
        override val stableId: String = "app:$value"
    }

    data class Folder(override val value: String) : HomeItemRef {
        override val stableId: String = "folder:$value"
    }
}

data class LauncherFolder(
    val id: String,
    val name: String,
)

data class LauncherSettings(
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val showDailyUsage: Boolean = true,
    val focusSearchOnListOpen: Boolean = true,
)

data class LauncherPreferences(
    val homeItems: List<HomeItemRef> = emptyList(),
    val customNames: Map<String, String> = emptyMap(),
    val hiddenAppIds: Set<String> = emptySet(),
    val blockedAppIds: Set<String> = emptySet(),
    val folders: List<LauncherFolder> = emptyList(),
    val appFolders: Map<String, String> = emptyMap(),
    val settings: LauncherSettings = LauncherSettings(),
    val needsFavoriteMigration: Boolean = false,
    val legacyFavoriteIds: Set<String> = emptySet(),
)

internal object LauncherPreferencesCodec {
    fun encodeHomeItems(items: List<HomeItemRef>): String = items.joinToString("\n") { item ->
        when (item) {
            is HomeItemRef.App -> "a:${encode(item.value)}"
            is HomeItemRef.Folder -> "f:${encode(item.value)}"
        }
    }

    fun decodeHomeItems(value: String): List<HomeItemRef> = value.lineSequence()
        .filter(String::isNotBlank)
        .mapNotNull { line ->
            val encoded = line.substringAfter(':', missingDelimiterValue = "")
            if (encoded.isEmpty()) return@mapNotNull null
            val decoded = decodeOrNull(encoded) ?: return@mapNotNull null
            when (line.substringBefore(':')) {
                "a" -> HomeItemRef.App(decoded)
                "f" -> HomeItemRef.Folder(decoded)
                else -> null
            }
        }
        .distinctBy(HomeItemRef::stableId)
        .toList()

    fun encodeMap(values: Map<String, String>): String = values.entries
        .sortedBy { it.key }
        .joinToString("\n") { (key, value) -> "${encode(key)}\t${encode(value)}" }

    fun decodeMap(value: String): Map<String, String> = value.lineSequence()
        .mapNotNull { line ->
            val parts = line.split('\t', limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val key = decodeOrNull(parts[0]) ?: return@mapNotNull null
            val decodedValue = decodeOrNull(parts[1]) ?: return@mapNotNull null
            key to decodedValue
        }
        .toMap()

    fun encodeFolders(folders: List<LauncherFolder>): String = encodeMap(
        folders.associate { it.id to it.name },
    )

    fun decodeFolders(value: String): List<LauncherFolder> = decodeMap(value)
        .map { (id, name) -> LauncherFolder(id, name) }

    private fun encode(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeOrNull(value: String): String? = runCatching {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }.getOrNull()
}
