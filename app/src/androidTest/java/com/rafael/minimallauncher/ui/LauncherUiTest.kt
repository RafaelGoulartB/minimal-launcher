package com.rafael.minimallauncher.ui

import android.content.ComponentName
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import com.rafael.minimallauncher.R
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.AppListControlsPosition
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.FolderItem
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.data.LauncherFolder
import com.rafael.minimallauncher.data.LauncherPreferences
import com.rafael.minimallauncher.data.LauncherAccent
import com.rafael.minimallauncher.data.LauncherFont
import com.rafael.minimallauncher.data.LauncherTextSize
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class LauncherUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app = AppItem(
        LauncherApp("Calendar", ComponentName("com.example.calendar", "com.example.calendar.Main")),
    )

    @Test
    fun homeLongPressRevealsContextualRemoveOnly() {
        composeRule.setContent {
            MinimalLauncherTheme {
                HomePage(
                    state = LauncherUiState(homeItems = listOf(app), isLoading = false),
                    actions = actions(),
                )
            }
        }

        composeRule.onNodeWithText("Remove").assertDoesNotExist()
        composeRule.onNodeWithText("Calendar").performTouchInput { longClick() }
        composeRule.onNodeWithText("Remove").assertExists()
    }

    @Test
    fun drawerHasNoInlineFavoriteButtonsAndLongPressOpensManagement() {
        composeRule.setContent {
            MinimalLauncherTheme {
                AllAppsPage(
                    state = LauncherUiState(drawerItems = listOf(app), isLoading = false),
                    actions = actions(),
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Add").assertDoesNotExist()
        composeRule.onNodeWithText("Remove").assertDoesNotExist()
        composeRule.onNodeWithText("Calendar").performTouchInput { longClick() }
        composeRule.onNodeWithText("Add to Home").assertExists()
    }

    @Test
    fun homeLongPressAndDragRequestsReorder() {
        val secondApp = AppItem(
            LauncherApp("Camera", ComponentName("com.example.camera", "com.example.camera.Main")),
        )
        var requestedMove: Pair<Int, Int>? = null
        composeRule.setContent {
            MinimalLauncherTheme {
                HomePage(
                    state = LauncherUiState(homeItems = listOf(app, secondApp), isLoading = false),
                    actions = actions().copy(onMoveHomeItem = { from, to -> requestedMove = from to to }),
                )
            }
        }

        composeRule.onNodeWithText("Calendar").performTouchInput {
            down(center)
            advanceEventTime(650)
            moveTo(center + Offset(0f, 220f), 150)
            up()
        }
        composeRule.runOnIdle { assertEquals(0 to 1, requestedMove) }
    }

    @Test
    fun drawerExposesSettingsButton() {
        composeRule.setContent {
            MinimalLauncherTheme {
                AllAppsPage(
                    state = LauncherUiState(drawerItems = listOf(app), isLoading = false),
                    actions = actions(),
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Settings").assertExists()
    }

    @Test
    fun tappingFolderExpandsAppsInline() {
        val child = AppItem(
            LauncherApp("Notes", ComponentName("com.example.notes", "com.example.notes.Main")),
        )
        val folder = FolderItem(
            LauncherFolder("folder-id", "Tools"),
            apps = listOf(child),
        )
        composeRule.setContent {
            MinimalLauncherTheme {
                AllAppsPage(
                    state = LauncherUiState(drawerItems = listOf(folder), isLoading = false),
                    actions = actions(),
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Notes").assertDoesNotExist()
        composeRule.onNodeWithText("Tools").performClick()
        composeRule.onNodeWithText("Notes").assertExists()
        composeRule.onNodeWithContentDescription("Collapse folder").assertExists()
    }

    @Test
    fun homeFolderExpandsInlineLikeDrawer() {
        val child = AppItem(
            LauncherApp("Notes", ComponentName("com.example.notes", "com.example.notes.Main")),
        )
        val folder = FolderItem(
            LauncherFolder("folder-id", "Tools"),
            apps = listOf(child),
        )
        composeRule.setContent {
            MinimalLauncherTheme {
                HomePage(
                    state = LauncherUiState(homeItems = listOf(folder), isLoading = false),
                    actions = actions(),
                )
            }
        }

        composeRule.onNodeWithText("1 app").assertExists()
        composeRule.onNodeWithText("Notes").assertDoesNotExist()
        composeRule.onNodeWithText("Tools").performClick()
        composeRule.onNodeWithText("Notes").assertExists()
        composeRule.onNodeWithContentDescription("Collapse folder").assertExists()
    }

    @Test
    fun deletingFolderRequiresConfirmation() {
        val folder = FolderItem(LauncherFolder("folder-id", "Tools"), apps = listOf(app))
        var deleteRequested = false
        composeRule.setContent {
            MinimalLauncherTheme {
                AllAppsPage(
                    state = LauncherUiState(drawerItems = listOf(folder), folders = listOf(folder), isLoading = false),
                    actions = actions().copy(onDeleteFolder = { deleteRequested = true }),
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Tools").performTouchInput { longClick() }
        composeRule.onNodeWithText("Delete folder").performClick()
        composeRule.onNodeWithText(
            "The apps in this folder will return to the main app list.",
            substring = true,
        ).assertExists()
        composeRule.runOnIdle { assertEquals(false, deleteRequested) }
        composeRule.onNodeWithText("Delete folder").performClick()
        composeRule.runOnIdle { assertEquals(true, deleteRequested) }
    }

    @Test
    fun hidingAppRequiresConfirmation() {
        var hideRequested = false
        composeRule.setContent {
            MinimalLauncherTheme {
                AllAppsPage(
                    state = LauncherUiState(drawerItems = listOf(app), isLoading = false),
                    actions = actions().copy(onHideApp = { hideRequested = true }),
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Calendar").performTouchInput { longClick() }
        composeRule.onNodeWithText("Hide").performClick()
        composeRule.onNodeWithText(
            "This app will leave the launcher list.",
            substring = true,
        ).assertExists()
        composeRule.runOnIdle { assertEquals(false, hideRequested) }
        composeRule.onNodeWithText("Hide").performClick()
        composeRule.runOnIdle { assertEquals(true, hideRequested) }
    }

    @Test
    fun uninstallRequiresConfirmation() {
        var requestedApp: LauncherApp? = null
        composeRule.setContent {
            MinimalLauncherTheme {
                AllAppsPage(
                    state = LauncherUiState(drawerItems = listOf(app), isLoading = false),
                    actions = actions().copy(onRequestUninstall = { requestedApp = it }),
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Calendar").performTouchInput { longClick() }
        composeRule.onNodeWithText("Uninstall").performClick()
        composeRule.onNodeWithText("Uninstall Calendar?").assertExists()
        composeRule.onNodeWithText(
            "Android will open its uninstall screen for final confirmation.",
            substring = true,
        ).assertExists()
        composeRule.runOnIdle { assertEquals(null, requestedApp) }
        composeRule.onNodeWithText("Uninstall").performClick()
        composeRule.runOnIdle { assertEquals(app.app, requestedApp) }
    }

    @Test
    fun launcherSnackbarExposesUndoAction() {
        val events = kotlinx.coroutines.flow.MutableSharedFlow<LauncherUiEvent>(extraBufferCapacity = 1)
        var undoneToken: Long? = null
        composeRule.setContent {
            MinimalLauncherTheme {
                LauncherScreen(
                    state = LauncherUiState(isLoading = false),
                    actions = actions().copy(onUndo = { undoneToken = it }),
                    events = events,
                )
            }
        }

        composeRule.runOnIdle {
            events.tryEmit(LauncherUiEvent.Snackbar(R.string.app_hidden, LauncherSnackbarAction(7)))
        }
        composeRule.onNodeWithText("Undo").assertExists()
        composeRule.onNodeWithText("Undo").performClick()
        composeRule.runOnIdle { assertEquals(7L, undoneToken) }
    }

    @Test
    fun settingsFolderDeletionRequiresConfirmation() {
        val folder = FolderItem(LauncherFolder("folder-id", "Tools"), apps = listOf(app))
        var deleteRequested = false
        composeRule.setContent {
            MinimalLauncherTheme {
                SettingsPage(
                    state = LauncherUiState(
                        folders = listOf(folder),
                        preferences = LauncherPreferences(
                            folders = listOf(folder.folder),
                        ),
                        isLoading = false,
                    ),
                    actions = actions().copy(onDeleteFolder = { deleteRequested = true }),
                    onBack = {},
                )
            }
        }

        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Tools"))
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText(
            "The apps in this folder will return to the main app list.",
            substring = true,
        ).assertExists()
        composeRule.runOnIdle { assertEquals(false, deleteRequested) }
        composeRule.onNodeWithText("Delete folder").performClick()
        composeRule.runOnIdle { assertEquals(true, deleteRequested) }
    }

    @Test
    fun appearancePresetControlsDispatchChanges() {
        var selectedFont: LauncherFont? = null
        var selectedSize: LauncherTextSize? = null
        var selectedAccent: LauncherAccent? = null
        composeRule.setContent {
            MinimalLauncherTheme {
                SettingsPage(
                    state = LauncherUiState(preferences = LauncherPreferences(), isLoading = false),
                    actions = actions().copy(
                        onFontChange = { selectedFont = it },
                        onTextSizeChange = { selectedSize = it },
                        onAccentChange = { selectedAccent = it },
                    ),
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Serif").performClick()
        composeRule.onNodeWithText("Large").performClick()
        composeRule.onNodeWithText("Teal").performClick()

        composeRule.runOnIdle {
            assertEquals(LauncherFont.SERIF, selectedFont)
            assertEquals(LauncherTextSize.LARGE, selectedSize)
            assertEquals(LauncherAccent.TEAL, selectedAccent)
        }
    }

    @Test
    fun settingsBackButtonIsAccessibleAndNavigatesBack() {
        var backRequested = false
        composeRule.setContent {
            MinimalLauncherTheme {
                SettingsPage(
                    state = LauncherUiState(isLoading = false),
                    actions = actions(),
                    onBack = { backRequested = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.runOnIdle { assertEquals(true, backRequested) }
    }

    private fun actions() = LauncherActions(
        onToggleHomeItem = {},
        onSearchChange = {},
        onAddHomeItem = {},
        onRemoveHomeItem = {},
        onMoveHomeItem = { _, _ -> },
        onRenameApp = { _, _ -> },
        onHideApp = {},
        onSetAppHidden = { _, _ -> },
        onSetAppBlocked = { _, _ -> },
        onCreateFolder = { _, _ -> },
        onRenameFolder = { _, _ -> },
        onDeleteFolder = {},
        onMoveAppToFolder = { _, _ -> },
        onClockFormatChange = { _: ClockFormat -> },
        onShowDateChange = {},
        onShowBatteryChange = {},
        onShowDailyUsageChange = {},
        onFocusSearchOnListOpenChange = {},
        onAppListControlsPositionChange = { _: AppListControlsPosition -> },
        onFontChange = { _: LauncherFont -> },
        onTextSizeChange = { _: LauncherTextSize -> },
        onAccentChange = { _: LauncherAccent -> },
        onRefreshUsage = {},
        onRequestUninstall = {},
        onUndo = {},
    )
}
