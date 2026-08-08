package com.rafael.minimallauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafael.minimallauncher.ui.LauncherScreen
import com.rafael.minimallauncher.ui.LauncherActions
import com.rafael.minimallauncher.ui.LauncherViewModel
import com.rafael.minimallauncher.ui.MinimalLauncherTheme

class MainActivity : ComponentActivity() {
    private val launcherViewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinimalLauncherTheme {
                val state by launcherViewModel.uiState.collectAsStateWithLifecycle()
                LauncherScreen(
                    state = state,
                    actions = LauncherActions(
                        onSearchChange = launcherViewModel::setSearchQuery,
                        onAddHomeItem = launcherViewModel::addHomeItem,
                        onRemoveHomeItem = launcherViewModel::removeHomeItem,
                        onMoveHomeItem = launcherViewModel::moveHomeItem,
                        onRenameApp = launcherViewModel::renameApp,
                        onSetAppHidden = launcherViewModel::setAppHidden,
                        onSetAppBlocked = launcherViewModel::setAppBlocked,
                        onCreateFolder = launcherViewModel::createFolder,
                        onRenameFolder = launcherViewModel::renameFolder,
                        onDeleteFolder = launcherViewModel::deleteFolder,
                        onMoveAppToFolder = launcherViewModel::moveAppToFolder,
                        onClockFormatChange = launcherViewModel::updateClockFormat,
                        onShowDateChange = launcherViewModel::setShowDate,
                        onShowBatteryChange = launcherViewModel::setShowBattery,
                        onShowDailyUsageChange = launcherViewModel::setShowDailyUsage,
                    ),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning from installation/uninstallation without a permanent receiver.
        launcherViewModel.refreshApps()
    }
}
