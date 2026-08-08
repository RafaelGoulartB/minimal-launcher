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

private val SettingsSurface = Color(0xFF101010)
private val SettingsSurfaceRaised = Color(0xFF1A1A1A)
private val SettingsOutline = Color(0xFF2A2A2A)
private val SettingsSecondaryText = Color(0xFFA6A6A6)
private val SettingsMutedText = Color(0xFF737373)
private val SettingsDestructive = Color(0xFFFF8A80)
private val SettingsCardShape = RoundedCornerShape(20.dp)

private sealed interface SettingsEditor {
    data class App(val id: String, val name: String) : SettingsEditor
    data class Folder(val id: String, val name: String) : SettingsEditor
}

private data class SettingsAction(
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp),
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

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    val backLabel = stringResource(R.string.settings_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, SettingsOutline, CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .clearAndSetSemantics {
                    contentDescription = backLabel
                    role = Role.Button
                    onClick(label = backLabel) {
                        onBack()
                        true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "‹",
                color = MaterialTheme.colorScheme.primary,
                fontSize = launcherSp(34.sp),
                lineHeight = launcherSp(34.sp),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = launcherSp(30.sp),
                lineHeight = launcherSp(34.sp),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.settings_description),
                color = SettingsSecondaryText,
                fontSize = launcherSp(14.sp),
                lineHeight = launcherSp(20.sp),
            )
        }
    }
}

@Composable
private fun SettingsSectionHeading(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 26.dp, end = 4.dp, bottom = 12.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = launcherSp(20.sp),
            lineHeight = launcherSp(25.sp),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = description,
            color = SettingsSecondaryText,
            fontSize = launcherSp(14.sp),
            lineHeight = launcherSp(20.sp),
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsSurface, SettingsCardShape)
            .border(1.dp, SettingsOutline, SettingsCardShape),
        content = content,
    )
}

@Composable
private fun SettingChoice(
    title: String,
    description: String,
    choices: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
        SettingCopy(title = title, description = description)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEachIndexed { index, choice ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                SettingsSurfaceRaised
                            },
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else SettingsOutline,
                            shape = RoundedCornerShape(13.dp),
                        )
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        )
                        .padding(horizontal = 6.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = choice,
                        color = if (selected) MaterialTheme.colorScheme.primary else SettingsSecondaryText,
                        fontSize = launcherSp(13.sp),
                        lineHeight = launcherSp(17.sp),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSetting(selectedAccent: LauncherAccent, onSelect: (LauncherAccent) -> Unit) {
    val accents = LauncherAccent.entries.map { accent ->
        val fullName = when (accent) {
            LauncherAccent.MONOCHROME -> stringResource(R.string.accent_monochrome)
            LauncherAccent.BLUE -> stringResource(R.string.accent_blue)
            LauncherAccent.TEAL -> stringResource(R.string.accent_teal)
            LauncherAccent.AMBER -> stringResource(R.string.accent_amber)
            LauncherAccent.VIOLET -> stringResource(R.string.accent_violet)
        }
        val shortName = if (accent == LauncherAccent.MONOCHROME) {
            stringResource(R.string.accent_monochrome_short)
        } else {
            fullName
        }
        accent to shortName
    }
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
        SettingCopy(
            title = stringResource(R.string.appearance_accent),
            description = stringResource(R.string.settings_accent_description),
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            accents.forEach { (accent, shortName) ->
                val selected = accent == selectedAccent
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(accent) },
                        )
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (selected) accentColor(accent) else Color.Transparent,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .background(accentColor(accent), CircleShape)
                                .then(
                                    if (accent == LauncherAccent.MONOCHROME) {
                                        Modifier.border(1.dp, SettingsOutline, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }
                    Text(
                        text = shortName,
                        color = if (selected) Color.White else SettingsMutedText,
                        fontSize = launcherSp(11.sp),
                        lineHeight = launcherSp(15.sp),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun accentColor(accent: LauncherAccent): Color = when (accent) {
    LauncherAccent.MONOCHROME -> Color.White
    LauncherAccent.BLUE -> Color(0xFF90CAF9)
    LauncherAccent.TEAL -> Color(0xFF80CBC4)
    LauncherAccent.AMBER -> Color(0xFFFFCC80)
    LauncherAccent.VIOLET -> Color(0xFFCE93D8)
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            SettingCopy(title = title, description = description)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = SettingsSecondaryText,
                uncheckedTrackColor = SettingsSurfaceRaised,
                uncheckedBorderColor = SettingsOutline,
            ),
        )
    }
}

@Composable
private fun SettingCopy(title: String, description: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = launcherSp(17.sp),
        lineHeight = launcherSp(22.sp),
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = description,
        color = SettingsSecondaryText,
        fontSize = launcherSp(13.sp),
        lineHeight = launcherSp(18.sp),
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        thickness = 1.dp,
        color = SettingsOutline,
    )
}

@Composable
private fun UsageAccessCard(hasAccess: Boolean, onClick: () -> Unit) {
    val statusLabel = stringResource(
        if (hasAccess) R.string.settings_access_granted else R.string.settings_access_required,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsSurface, SettingsCardShape)
            .border(1.dp, SettingsOutline, SettingsCardShape)
            .clip(SettingsCardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            SettingCopy(
                title = stringResource(R.string.settings_usage_title),
                description = stringResource(
                    if (hasAccess) {
                        R.string.settings_usage_granted_description
                    } else {
                        R.string.settings_usage_required_description
                    },
                ),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = statusLabel,
                color = if (hasAccess) MaterialTheme.colorScheme.primary else SettingsSecondaryText,
                fontSize = launcherSp(11.sp),
                lineHeight = launcherSp(15.sp),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        color = if (hasAccess) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            SettingsSurfaceRaised
                        },
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_manage),
                color = MaterialTheme.colorScheme.primary,
                fontSize = launcherSp(13.sp),
                lineHeight = launcherSp(18.sp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ManagementGroupHeader(title: String, description: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SettingsSurface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            SettingCopy(title = title, description = description)
        }
        Box(
            modifier = Modifier
                .background(SettingsSurfaceRaised, CircleShape)
                .heightIn(min = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                color = SettingsSecondaryText,
                fontSize = launcherSp(13.sp),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun EmptyManagementRow(value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SettingsSurface,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            ),
    ) {
        HorizontalDivider(color = SettingsOutline)
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp),
            color = SettingsMutedText,
            fontSize = launcherSp(14.sp),
            lineHeight = launcherSp(20.sp),
        )
    }
}

@Composable
private fun ManagementItem(
    title: String,
    subtitle: String? = null,
    actions: List<SettingsAction>,
    isLast: Boolean,
) {
    val shape = if (isLast) {
        RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    } else {
        RoundedCornerShape(0.dp)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsSurface, shape),
    ) {
        HorizontalDivider(color = SettingsOutline)
        if (actions.size == 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ManagementCopy(
                    title = title,
                    subtitle = subtitle,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                InlineActionButton(action = actions.single())
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp)) {
                ManagementCopy(
                    title = title,
                    subtitle = subtitle,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    actions.forEach { action -> InlineActionButton(action = action) }
                }
            }
        }
    }
}

@Composable
private fun ManagementCopy(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = Color.White,
            fontSize = launcherSp(16.sp),
            lineHeight = launcherSp(21.sp),
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
            Spacer(Modifier.height(2.dp))
            Text(
                text = it,
                color = SettingsMutedText,
                fontSize = launcherSp(13.sp),
                lineHeight = launcherSp(18.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InlineActionButton(action: SettingsAction) {
    TextButton(
        onClick = action.onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier.heightIn(min = 44.dp),
    ) {
        Text(
            text = action.label,
            color = if (action.destructive) SettingsDestructive else MaterialTheme.colorScheme.primary,
            fontSize = launcherSp(13.sp),
            lineHeight = launcherSp(18.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SettingsNameDialog(initialValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SettingsSurfaceRaised,
        shape = SettingsCardShape,
        title = {
            Text(
                text = stringResource(R.string.settings_rename),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.settings_name)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(value.trim()) },
                enabled = value.isNotBlank(),
            ) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
