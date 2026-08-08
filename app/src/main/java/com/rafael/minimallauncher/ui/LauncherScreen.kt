package com.rafael.minimallauncher.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.AppListControlsPosition
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.data.LauncherAccent
import com.rafael.minimallauncher.data.LauncherFont
import com.rafael.minimallauncher.data.LauncherSettings
import com.rafael.minimallauncher.data.LauncherTextSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class LauncherActions(
    val onSearchChange: (String) -> Unit,
    val onToggleHomeItem: (HomeItemRef) -> Unit,
    val onAddHomeItem: (HomeItemRef) -> Unit,
    val onRemoveHomeItem: (HomeItemRef) -> Unit,
    val onMoveHomeItem: (Int, Int) -> Unit,
    val onRenameApp: (String, String?) -> Unit,
    val onHideApp: (String) -> Unit,
    val onSetAppHidden: (String, Boolean) -> Unit,
    val onSetAppBlocked: (String, Boolean) -> Unit,
    val onCreateFolder: (String, String?) -> Unit,
    val onRenameFolder: (String, String) -> Unit,
    val onDeleteFolder: (String) -> Unit,
    val onMoveAppToFolder: (String, String?) -> Unit,
    val onClockFormatChange: (ClockFormat) -> Unit,
    val onShowDateChange: (Boolean) -> Unit,
    val onShowBatteryChange: (Boolean) -> Unit,
    val onShowDailyUsageChange: (Boolean) -> Unit,
    val onFocusSearchOnListOpenChange: (Boolean) -> Unit,
    val onFontChange: (LauncherFont) -> Unit,
    val onTextSizeChange: (LauncherTextSize) -> Unit,
    val onAccentChange: (LauncherAccent) -> Unit,
    val onRefreshUsage: () -> Unit,
    val onRequestUninstall: (LauncherApp) -> Unit,
    val onUndo: (Long) -> Unit,
    val onAppListControlsPositionChange: (AppListControlsPosition) -> Unit = {},
)

@Composable
fun LauncherScreen(
    state: LauncherUiState,
    actions: LauncherActions,
    events: Flow<LauncherUiEvent> = emptyFlow(),
) {
    var showSettings by remember { mutableStateOf(false) }
    BackHandler(enabled = showSettings) { showSettings = false }
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val undoLabel = stringResource(com.rafael.minimallauncher.R.string.undo)
    LaunchedEffect(events, undoLabel) {
        events.collect { event ->
            when (event) {
                is LauncherUiEvent.Snackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(event.messageRes),
                        actionLabel = event.action?.let { undoLabel },
                        duration = if (event.action == null) SnackbarDuration.Short else SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed && event.action != null) {
                        actions.onUndo(event.action.token)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showSettings) {
            SettingsPage(state = state, actions = actions, onBack = { showSettings = false })
        } else {
            val pagerState = rememberPagerState(pageCount = { 2 })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().background(Color.Black),
            ) { page ->
                when (page) {
                    0 -> HomePage(state, actions)
                    else -> AllAppsPage(
                        state = state,
                        actions = actions,
                        onOpenSettings = { showSettings = true },
                        isVisible = pagerState.currentPage == 1,
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}

internal fun openApp(context: Context, item: AppItem, blockedIds: Set<String>) {
    if (item.id in blockedIds) {
        Toast.makeText(context, "${item.label} is blocked. Unblock it in Settings.", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(item.app.componentName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "App is no longer available.", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "Android did not allow this app to be opened.", Toast.LENGTH_SHORT).show()
    } catch (_: RuntimeException) {
        Toast.makeText(context, "App is no longer available.", Toast.LENGTH_SHORT).show()
    }
}

internal fun showAppInfo(context: Context, app: LauncherApp) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(android.net.Uri.parse("package:${app.componentName.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

@Composable
fun MinimalLauncherTheme(
    settings: LauncherSettings = LauncherSettings(),
    content: @Composable () -> Unit,
) {
    val appearance = launcherAppearance(settings)
    androidx.compose.runtime.CompositionLocalProvider(LocalLauncherAppearance provides appearance) {
        MaterialTheme(
            colorScheme = androidx.compose.material3.darkColorScheme(
                primary = appearance.accentColor,
                onPrimary = appearance.onAccentColor,
                secondary = appearance.accentColor,
                onSecondary = appearance.onAccentColor,
                background = Color.Black,
                onBackground = Color.White,
                surface = Color.Black,
                onSurface = Color.White,
            ),
            typography = androidx.compose.material3.Typography().scaledForLauncher(appearance),
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = appearance.fontFamily),
                content = content,
            )
        }
    }
}
