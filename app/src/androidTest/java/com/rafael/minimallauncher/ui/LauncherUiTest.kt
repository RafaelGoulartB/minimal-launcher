package com.rafael.minimallauncher.ui

import android.content.ComponentName
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.LauncherApp
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

    private fun actions() = LauncherActions(
        onSearchChange = {},
        onAddHomeItem = {},
        onRemoveHomeItem = {},
        onMoveHomeItem = { _, _ -> },
        onRenameApp = { _, _ -> },
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
        onRefreshUsage = {},
    )
}
