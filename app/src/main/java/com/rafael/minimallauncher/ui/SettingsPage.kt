package com.rafael.minimallauncher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.HomeItemRef

private sealed interface SettingsEditor {
    data class App(val id: String, val name: String) : SettingsEditor
    data class Folder(val id: String, val name: String) : SettingsEditor
}

@Composable
internal fun SettingsPage(state: LauncherUiState, actions: LauncherActions, onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = state.preferences
    var editor by remember { mutableStateOf<SettingsEditor?>(null) }
    val appsById = state.apps.associateBy { it.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp),
    ) {
        item {
            Text(
                "‹  Settings",
                modifier = Modifier.fillMaxWidth().clickable(onClick = onBack).padding(start = 22.dp, top = 54.dp, bottom = 22.dp),
                color = Color.White,
                fontSize = 30.sp,
            )
        }
        item { SectionTitle("Appearance") }
        item {
            Text("Clock format", modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp), color = Color.White, fontSize = 20.sp)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ClockFormat.entries.forEach { format ->
                    TextButton(onClick = { actions.onClockFormatChange(format) }) {
                        Text(
                            when (format) {
                                ClockFormat.SYSTEM -> "System"
                                ClockFormat.TWELVE_HOUR -> "12-hour"
                                ClockFormat.TWENTY_FOUR_HOUR -> "24-hour"
                            },
                            color = if (preferences.settings.clockFormat == format) Color.White else Color.Gray,
                            fontWeight = if (preferences.settings.clockFormat == format) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
        item { SettingSwitch("Show date", preferences.settings.showDate, actions.onShowDateChange) }
        item { SettingSwitch("Show battery", preferences.settings.showBattery, actions.onShowBatteryChange) }
        item { SettingSwitch("Show today's usage", preferences.settings.showDailyUsage, actions.onShowDailyUsageChange) }
        item {
            SettingSwitch(
                "Focus search when opening app list",
                preferences.settings.focusSearchOnListOpen,
                actions.onFocusSearchOnListOpenChange,
            )
        }

        item { SectionTitle("Usage access") }
        item {
            SettingsActionRow(
                title = if (state.dailyUsage.hasAccess) "Usage access granted" else "Grant usage access",
                subtitle = if (state.dailyUsage.hasAccess) "Today's screen time is available." else "Required only for the Home usage summary.",
                action = if (state.dailyUsage.hasAccess) null else "Open",
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
            )
        }

        item { SectionTitle("Hidden apps") }
        if (preferences.hiddenAppIds.isEmpty()) {
            item { EmptySetting("No hidden apps") }
        } else {
            preferences.hiddenAppIds.sortedBy { appsById[it]?.label ?: it }.forEach { id ->
                item(key = "hidden:$id") {
                    SettingsActionRow(
                        title = preferences.customNames[id] ?: appsById[id]?.label ?: id,
                        action = "Restore",
                        onClick = { actions.onSetAppHidden(id, false) },
                    )
                }
            }
        }

        item { SectionTitle("Blocked apps") }
        if (preferences.blockedAppIds.isEmpty()) {
            item { EmptySetting("No blocked apps") }
        } else {
            preferences.blockedAppIds.sortedBy { appsById[it]?.label ?: it }.forEach { id ->
                item(key = "blocked:$id") {
                    SettingsActionRow(
                        title = preferences.customNames[id] ?: appsById[id]?.label ?: id,
                        action = "Unblock",
                        onClick = { actions.onSetAppBlocked(id, false) },
                    )
                }
            }
        }

        item { SectionTitle("Custom names") }
        if (preferences.customNames.isEmpty()) {
            item { EmptySetting("No renamed apps") }
        } else {
            preferences.customNames.entries.sortedBy { it.value }.forEach { (id, name) ->
                item(key = "name:$id") {
                    SettingsActionRow(
                        title = name,
                        subtitle = appsById[id]?.label,
                        action = "Edit",
                        secondaryAction = "Reset",
                        onClick = { editor = SettingsEditor.App(id, name) },
                        onSecondaryClick = { actions.onRenameApp(id, null) },
                    )
                }
            }
        }

        item { SectionTitle("Folders") }
        if (state.folders.isEmpty()) {
            item { EmptySetting("No folders") }
        } else {
            state.folders.forEach { folder ->
                item(key = "folder:${folder.id}") {
                    SettingsActionRow(
                        title = folder.label,
                        subtitle = "${folder.apps.size} apps",
                        action = if (folder.id in state.favoriteFolderIds) "Remove Home" else "Add Home",
                        secondaryAction = "Rename",
                        destructiveAction = "Delete",
                        onClick = {
                            val ref = HomeItemRef.Folder(folder.id)
                            if (folder.id in state.favoriteFolderIds) actions.onRemoveHomeItem(ref) else actions.onAddHomeItem(ref)
                        },
                        onSecondaryClick = { editor = SettingsEditor.Folder(folder.id, folder.label) },
                        onDestructiveClick = { actions.onDeleteFolder(folder.id) },
                    )
                }
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
}

@Composable
private fun SectionTitle(value: String) {
    Text(
        value,
        modifier = Modifier.fillMaxWidth().padding(start = 28.dp, top = 30.dp, bottom = 10.dp),
        color = Color.LightGray,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 28.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), color = Color.White, fontSize = 20.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    action: String? = null,
    secondaryAction: String? = null,
    destructiveAction: String? = null,
    onClick: () -> Unit = {},
    onSecondaryClick: () -> Unit = {},
    onDestructiveClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 20.sp)
            subtitle?.let { Text(it, color = Color.Gray, fontSize = 14.sp) }
        }
        action?.let { TextButton(onClick = onClick) { Text(it) } }
        secondaryAction?.let { TextButton(onClick = onSecondaryClick) { Text(it) } }
        destructiveAction?.let { TextButton(onClick = onDestructiveClick) { Text(it, color = Color(0xFFFF8A80)) } }
    }
}

@Composable
private fun EmptySetting(value: String) {
    Text(value, modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp), color = Color.DarkGray, fontSize = 17.sp)
}

@Composable
private fun SettingsNameDialog(initialValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text("Rename") },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true) },
        confirmButton = { Button(onClick = { onSave(value.trim()) }, enabled = value.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
