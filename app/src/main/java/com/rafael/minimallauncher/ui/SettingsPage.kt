package com.rafael.minimallauncher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafael.minimallauncher.R
import com.rafael.minimallauncher.data.AppListControlsPosition
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.FolderItem
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherAccent
import com.rafael.minimallauncher.data.LauncherFont
import com.rafael.minimallauncher.data.LauncherTextSize

private sealed interface SettingsEditor {
    data class App(val id: String, val name: String) : SettingsEditor
    data class Folder(val id: String, val name: String) : SettingsEditor
}

internal data class SettingsAction(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
internal fun SettingsPage(state: LauncherUiState, actions: LauncherActions, onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = state.preferences
    var editor by remember { mutableStateOf<SettingsEditor?>(null) }
    var folderToDelete by remember { mutableStateOf<FolderItem?>(null) }
    val appsById = remember(state.apps) { state.apps.associateBy { it.id } }
    val hiddenAppIds = preferences.hiddenAppIds.sortedBy { id ->
        preferences.customNames[id] ?: appsById[id]?.label ?: id
    }
    val blockedAppIds = preferences.blockedAppIds.sortedBy { id ->
        preferences.customNames[id] ?: appsById[id]?.label ?: id
    }
    val customNames = preferences.customNames.entries.sortedBy { it.value }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 48.dp),
    ) {
        item(key = "settings-header") {
            SettingsHeader(onBack = onBack)
        }

        item(key = "appearance-heading") {
            SettingsSectionHeading(
                title = stringResource(R.string.settings_section_appearance),
                description = stringResource(R.string.settings_section_appearance_description),
            )
        }
        item(key = "appearance-card") {
            SettingsCard {
                SettingChoice(
                    title = stringResource(R.string.appearance_font),
                    description = stringResource(R.string.settings_font_description),
                    choices = LauncherFont.entries.map { font ->
                        when (font) {
                            LauncherFont.SYSTEM -> stringResource(R.string.font_system)
                            LauncherFont.SERIF -> stringResource(R.string.font_serif)
                            LauncherFont.MONOSPACE -> stringResource(R.string.font_monospace)
                        }
                    },
                    selectedIndex = LauncherFont.entries.indexOf(preferences.settings.font),
                    onSelect = { actions.onFontChange(LauncherFont.entries[it]) },
                )
                SettingsDivider()
                SettingChoice(
                    title = stringResource(R.string.appearance_text_size),
                    description = stringResource(R.string.settings_text_size_description),
                    choices = LauncherTextSize.entries.map { size ->
                        when (size) {
                            LauncherTextSize.SMALL -> stringResource(R.string.text_size_small)
                            LauncherTextSize.MEDIUM -> stringResource(R.string.text_size_medium)
                            LauncherTextSize.LARGE -> stringResource(R.string.text_size_large)
                        }
                    },
                    selectedIndex = LauncherTextSize.entries.indexOf(preferences.settings.textSize),
                    onSelect = { actions.onTextSizeChange(LauncherTextSize.entries[it]) },
                )
                SettingsDivider()
                AccentSetting(
                    selectedAccent = preferences.settings.accent,
                    onSelect = actions.onAccentChange,
                )
            }
        }

        item(key = "home-heading") {
            SettingsSectionHeading(
                title = stringResource(R.string.settings_section_home),
                description = stringResource(R.string.settings_section_home_description),
            )
        }
        item(key = "home-card") {
            SettingsCard {
                SettingChoice(
                    title = stringResource(R.string.settings_clock_format),
                    description = stringResource(R.string.settings_clock_format_description),
                    choices = ClockFormat.entries.map { format ->
                        when (format) {
                            ClockFormat.SYSTEM -> stringResource(R.string.settings_clock_system)
                            ClockFormat.TWELVE_HOUR -> stringResource(R.string.settings_clock_twelve_hour)
                            ClockFormat.TWENTY_FOUR_HOUR -> stringResource(R.string.settings_clock_twenty_four_hour)
                        }
                    },
                    selectedIndex = ClockFormat.entries.indexOf(preferences.settings.clockFormat),
                    onSelect = { actions.onClockFormatChange(ClockFormat.entries[it]) },
                )
                SettingsDivider()
                SettingSwitch(
                    title = stringResource(R.string.settings_show_date),
                    description = stringResource(R.string.settings_show_date_description),
                    checked = preferences.settings.showDate,
                    onChange = actions.onShowDateChange,
                )
                SettingsDivider()
                SettingSwitch(
                    title = stringResource(R.string.settings_show_battery),
                    description = stringResource(R.string.settings_show_battery_description),
                    checked = preferences.settings.showBattery,
                    onChange = actions.onShowBatteryChange,
                )
                SettingsDivider()
                SettingSwitch(
                    title = stringResource(R.string.settings_show_daily_usage),
                    description = stringResource(R.string.settings_show_daily_usage_description),
                    checked = preferences.settings.showDailyUsage,
                    onChange = actions.onShowDailyUsageChange,
                )
            }
        }

        item(key = "app-list-heading") {
            SettingsSectionHeading(
                title = stringResource(R.string.settings_section_app_list),
                description = stringResource(R.string.settings_section_app_list_description),
            )
        }
        item(key = "app-list-card") {
            SettingsCard {
                SettingChoice(
                    title = stringResource(R.string.app_list_controls_position),
                    description = stringResource(R.string.settings_controls_position_description),
                    choices = AppListControlsPosition.entries.map { position ->
                        when (position) {
                            AppListControlsPosition.TOP -> stringResource(R.string.position_top)
                            AppListControlsPosition.BOTTOM -> stringResource(R.string.position_bottom)
                        }
                    },
                    selectedIndex = AppListControlsPosition.entries.indexOf(
                        preferences.settings.appListControlsPosition,
                    ),
                    onSelect = {
                        actions.onAppListControlsPositionChange(AppListControlsPosition.entries[it])
                    },
                )
                SettingsDivider()
                SettingSwitch(
                    title = stringResource(R.string.settings_focus_search),
                    description = stringResource(R.string.settings_focus_search_description),
                    checked = preferences.settings.focusSearchOnListOpen,
                    onChange = actions.onFocusSearchOnListOpenChange,
                )
            }
        }

        item(key = "usage-heading") {
            SettingsSectionHeading(
                title = stringResource(R.string.settings_section_usage),
                description = stringResource(R.string.settings_section_usage_description),
            )
        }
        item(key = "usage-card") {
            UsageAccessCard(
                hasAccess = state.dailyUsage.hasAccess,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
        }

        item(key = "management-heading") {
            SettingsSectionHeading(
                title = stringResource(R.string.settings_section_management),
                description = stringResource(R.string.settings_section_management_description),
            )
        }

        item(key = "hidden-header") {
            ManagementGroupHeader(
                title = stringResource(R.string.settings_hidden_apps),
                description = stringResource(R.string.settings_hidden_apps_description),
                count = hiddenAppIds.size,
            )
        }
        if (hiddenAppIds.isEmpty()) {
            item(key = "hidden-empty") {
                EmptyManagementRow(stringResource(R.string.settings_no_hidden_apps))
            }
        } else {
            itemsIndexed(hiddenAppIds, key = { _, id -> "hidden:$id" }) { index, id ->
                ManagementItem(
                    title = preferences.customNames[id] ?: appsById[id]?.label ?: id,
                    actions = listOf(
                        SettingsAction(stringResource(R.string.settings_restore)) {
                            actions.onSetAppHidden(id, false)
                        },
                    ),
                    isLast = index == hiddenAppIds.lastIndex,
                )
            }
        }
        item(key = "hidden-spacer") { Spacer(Modifier.height(12.dp)) }

        item(key = "blocked-header") {
            ManagementGroupHeader(
                title = stringResource(R.string.settings_blocked_apps),
                description = stringResource(R.string.settings_blocked_apps_description),
                count = blockedAppIds.size,
            )
        }
        if (blockedAppIds.isEmpty()) {
            item(key = "blocked-empty") {
                EmptyManagementRow(stringResource(R.string.settings_no_blocked_apps))
            }
        } else {
            itemsIndexed(blockedAppIds, key = { _, id -> "blocked:$id" }) { index, id ->
                ManagementItem(
                    title = preferences.customNames[id] ?: appsById[id]?.label ?: id,
                    actions = listOf(
                        SettingsAction(stringResource(R.string.settings_unblock)) {
                            actions.onSetAppBlocked(id, false)
                        },
                    ),
                    isLast = index == blockedAppIds.lastIndex,
                )
            }
        }
        item(key = "blocked-spacer") { Spacer(Modifier.height(12.dp)) }

        item(key = "names-header") {
            ManagementGroupHeader(
                title = stringResource(R.string.settings_custom_names),
                description = stringResource(R.string.settings_custom_names_description),
                count = customNames.size,
            )
        }
        if (customNames.isEmpty()) {
            item(key = "names-empty") {
                EmptyManagementRow(stringResource(R.string.settings_no_custom_names))
            }
        } else {
            itemsIndexed(customNames, key = { _, entry -> "name:${entry.key}" }) { index, (id, name) ->
                val originalName = appsById[id]?.label
                ManagementItem(
                    title = name,
                    subtitle = originalName?.let {
                        stringResource(R.string.settings_original_name, it)
                    },
                    actions = listOf(
                        SettingsAction(stringResource(R.string.settings_edit)) {
                            editor = SettingsEditor.App(id, name)
                        },
                        SettingsAction(stringResource(R.string.settings_reset)) {
                            actions.onRenameApp(id, null)
                        },
                    ),
                    isLast = index == customNames.lastIndex,
                )
            }
        }
        item(key = "names-spacer") { Spacer(Modifier.height(12.dp)) }

        item(key = "folders-header") {
            ManagementGroupHeader(
                title = stringResource(R.string.settings_folders),
                description = stringResource(R.string.settings_folders_description),
                count = state.folders.size,
            )
        }
        if (state.folders.isEmpty()) {
            item(key = "folders-empty") {
                EmptyManagementRow(stringResource(R.string.settings_no_folders))
            }
        } else {
            itemsIndexed(state.folders, key = { _, folder -> "folder:${folder.id}" }) { index, folder ->
                val appCount = pluralStringResource(
                    R.plurals.settings_app_count,
                    folder.apps.size,
                    folder.apps.size,
                )
                val isOnHome = folder.id in state.favoriteFolderIds
                ManagementItem(
                    title = folder.label,
                    subtitle = if (isOnHome) {
                        stringResource(R.string.settings_folder_on_home, appCount)
                    } else {
                        appCount
                    },
                    actions = listOf(
                        SettingsAction(
                            label = stringResource(
                                if (isOnHome) R.string.settings_remove_home else R.string.settings_add_home,
                            ),
                            onClick = {
                                actions.onToggleHomeItem(HomeItemRef.Folder(folder.id))
                            },
                        ),
                        SettingsAction(stringResource(R.string.settings_rename)) {
                            editor = SettingsEditor.Folder(folder.id, folder.label)
                        },
                        SettingsAction(
                            label = stringResource(R.string.settings_delete),
                            destructive = true,
                            onClick = { folderToDelete = folder },
                        ),
                    ),
                    isLast = index == state.folders.lastIndex,
                )
            }
        }
    }

    editor?.let { value ->
        SettingsNameDialog(
            initialValue = when (value) {
                is SettingsEditor.App -> value.name
                is SettingsEditor.Folder -> value.name
            },
            onDismiss = { editor = null },
            onSave = { name ->
                when (value) {
                    is SettingsEditor.App -> actions.onRenameApp(value.id, name)
                    is SettingsEditor.Folder -> actions.onRenameFolder(value.id, name)
                }
                editor = null
            },
        )
    }

    folderToDelete?.let { folder ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.confirm_delete_folder_title, folder.label),
            message = stringResource(R.string.confirm_delete_folder_message),
            confirmLabel = stringResource(R.string.confirm_delete_folder_action),
            onDismiss = { folderToDelete = null },
            onConfirm = {
                actions.onDeleteFolder(folder.id)
                folderToDelete = null
            },
        )
    }
}
