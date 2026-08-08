package com.rafael.minimallauncher

import android.app.Application
import android.content.BroadcastReceiver
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rafael.minimallauncher.data.AndroidUsageStatsRepository
import com.rafael.minimallauncher.data.DataStoreLauncherPreferencesRepository
import com.rafael.minimallauncher.data.IntentAppUninstallLauncher
import com.rafael.minimallauncher.data.PackageManagerLauncherRepository
import com.rafael.minimallauncher.ui.LauncherScreen
import com.rafael.minimallauncher.ui.LauncherActions
import com.rafael.minimallauncher.ui.LauncherPackageChangeReceiver
import com.rafael.minimallauncher.ui.LauncherViewModel
import com.rafael.minimallauncher.ui.launcherPackageIntentFilter
import com.rafael.minimallauncher.ui.MinimalLauncherTheme

class MainActivity : ComponentActivity() {
    private val launcherViewModel: LauncherViewModel by viewModels {
        LauncherViewModelFactory(application)
    }
    private var packageReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by launcherViewModel.uiState.collectAsStateWithLifecycle()
            MinimalLauncherTheme(settings = state.preferences.settings) {
                LauncherScreen(
                    state = state,
                    events = launcherViewModel.events,
                    actions = LauncherActions(
                        onSearchChange = launcherViewModel::setSearchQuery,
                        onToggleHomeItem = launcherViewModel::toggleHomeItem,
                        onAddHomeItem = launcherViewModel::addHomeItem,
                        onRemoveHomeItem = launcherViewModel::removeHomeItem,
                        onMoveHomeItem = launcherViewModel::moveHomeItem,
                        onRenameApp = launcherViewModel::renameApp,
                        onHideApp = launcherViewModel::hideApp,
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
                        onFocusSearchOnListOpenChange = launcherViewModel::setFocusSearchOnListOpen,
                        onFontChange = launcherViewModel::setFont,
                        onTextSizeChange = launcherViewModel::setTextSize,
                        onAccentChange = launcherViewModel::setAccent,
                        onRefreshUsage = launcherViewModel::refreshUsage,
                        onRequestUninstall = launcherViewModel::requestUninstall,
                        onUndo = launcherViewModel::undo,
                    ),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning from installation/uninstallation without a permanent receiver.
        launcherViewModel.refreshApps()
        launcherViewModel.refreshUsage()
    }

    override fun onStart() {
        super.onStart()
        val receiver = LauncherPackageChangeReceiver(launcherViewModel::onPackageChanged)
        packageReceiver = receiver
        ContextCompat.registerReceiver(
            this,
            receiver,
            launcherPackageIntentFilter(),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun onStop() {
        packageReceiver?.let { receiver ->
            runCatching { unregisterReceiver(receiver) }
        }
        packageReceiver = null
        super.onStop()
    }
}

class LauncherViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LauncherViewModel(
                launcherRepository = PackageManagerLauncherRepository(application),
                preferencesRepository = DataStoreLauncherPreferencesRepository(application),
                usageStatsRepository = AndroidUsageStatsRepository(application),
                uninstallLauncher = IntentAppUninstallLauncher(application),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
