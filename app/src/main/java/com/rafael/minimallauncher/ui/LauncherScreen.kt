package com.rafael.minimallauncher.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherApp

data class LauncherActions(
    val onSearchChange: (String) -> Unit,
    val onAddHomeItem: (HomeItemRef) -> Unit,
    val onRemoveHomeItem: (HomeItemRef) -> Unit,
    val onMoveHomeItem: (Int, Int) -> Unit,
    val onRenameApp: (String, String?) -> Unit,
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
    val onRefreshUsage: () -> Unit,
)

@Composable
fun LauncherScreen(
    state: LauncherUiState,
    actions: LauncherActions,
) {
    var showSettings by remember { mutableStateOf(false) }
    BackHandler(enabled = showSettings) { showSettings = false }
    if (showSettings) {
        SettingsPage(state = state, actions = actions, onBack = { showSettings = false })
        return
    }
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
    }
}

internal fun uninstallApp(context: Context, app: LauncherApp) {
    val intent = Intent(Intent.ACTION_DELETE)
        .setData(android.net.Uri.parse("package:${app.componentName.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

internal fun showAppInfo(context: Context, app: LauncherApp) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(android.net.Uri.parse("package:${app.componentName.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

@Composable
fun MinimalLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
        ),
        content = content,
    )
}
