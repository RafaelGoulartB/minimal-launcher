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

@Composable
internal fun SettingsHeader(onBack: () -> Unit) {
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
internal fun SettingsSectionHeading(title: String, description: String) {
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
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsSurface, SettingsCardShape)
            .border(1.dp, SettingsOutline, SettingsCardShape),
        content = content,
    )
}

@Composable
internal fun SettingChoice(
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
internal fun AccentSetting(selectedAccent: LauncherAccent, onSelect: (LauncherAccent) -> Unit) {
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
internal fun SettingSwitch(
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
internal fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        thickness = 1.dp,
        color = SettingsOutline,
    )
}

@Composable
internal fun UsageAccessCard(hasAccess: Boolean, onClick: () -> Unit) {
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
internal fun ManagementGroupHeader(title: String, description: String, count: Int) {
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
internal fun EmptyManagementRow(value: String) {
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
internal fun ManagementItem(
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
internal fun SettingsNameDialog(initialValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
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
