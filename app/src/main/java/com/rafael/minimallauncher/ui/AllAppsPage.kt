@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rafael.minimallauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.FolderItem
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherItem

private sealed interface EditorDialog {
    data class RenameApp(val item: AppItem) : EditorDialog
    data class RenameFolder(val item: FolderItem) : EditorDialog
    data class NewFolder(val app: AppItem) : EditorDialog
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AllAppsPage(state: LauncherUiState, actions: LauncherActions, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<AppItem?>(null) }
    var selectedFolder by remember { mutableStateOf<FolderItem?>(null) }
    var openedFolder by remember { mutableStateOf<FolderItem?>(null) }
    var folderPickerApp by remember { mutableStateOf<AppItem?>(null) }
    var editorDialog by remember { mutableStateOf<EditorDialog?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(54.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = actions.onSearchChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Search apps") },
            )
            TextButton(onClick = onOpenSettings) { Text("⚙", fontSize = 32.sp) }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            items(state.drawerItems, key = LauncherItem::id) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is AppItem -> openApp(context, item, state.preferences.blockedAppIds)
                                    is FolderItem -> openedFolder = item
                                }
                            },
                            onLongClick = {
                                when (item) {
                                    is AppItem -> selectedApp = item
                                    is FolderItem -> selectedFolder = item
                                }
                            },
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.label,
                        color = Color.White,
                        fontSize = 26.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    selectedApp?.let { item ->
        AppManagementSheet(
            item = item,
            isOnHome = item.id in state.favoriteIds,
            isBlocked = item.id in state.preferences.blockedAppIds,
            onDismiss = { selectedApp = null },
            onHomeClick = {
                if (item.id in state.favoriteIds) actions.onRemoveHomeItem(HomeItemRef.App(item.id))
                else actions.onAddHomeItem(HomeItemRef.App(item.id))
                selectedApp = null
            },
            onRename = {
                selectedApp = null
                editorDialog = EditorDialog.RenameApp(item)
            },
            onHide = {
                actions.onSetAppHidden(item.id, true)
                selectedApp = null
            },
            onBlock = {
                actions.onSetAppBlocked(item.id, item.id !in state.preferences.blockedAppIds)
                selectedApp = null
            },
            onFolder = {
                selectedApp = null
                folderPickerApp = item
            },
            onUninstall = {
                selectedApp = null
                uninstallApp(context, item.app)
            },
            onInfo = {
                selectedApp = null
                showAppInfo(context, item.app)
            },
        )
    }

    selectedFolder?.let { item ->
        FolderManagementSheet(
            item = item,
            isOnHome = item.id in state.favoriteFolderIds,
            onDismiss = { selectedFolder = null },
            onHomeClick = {
                if (item.id in state.favoriteFolderIds) actions.onRemoveHomeItem(HomeItemRef.Folder(item.id))
                else actions.onAddHomeItem(HomeItemRef.Folder(item.id))
                selectedFolder = null
            },
            onRename = {
                selectedFolder = null
                editorDialog = EditorDialog.RenameFolder(item)
            },
            onDelete = {
                actions.onDeleteFolder(item.id)
                selectedFolder = null
            },
        )
    }

    openedFolder?.let { folder ->
        FolderContentsSheet(
            folder = folder,
            blockedIds = state.preferences.blockedAppIds,
            onDismiss = { openedFolder = null },
            onLongClickApp = { app ->
                openedFolder = null
                selectedApp = app
            },
        )
    }

    folderPickerApp?.let { app ->
        FolderPickerDialog(
            app = app,
            folders = state.folders,
            currentFolderId = state.preferences.appFolders[app.id],
            onDismiss = { folderPickerApp = null },
            onSelect = { folderId ->
                actions.onMoveAppToFolder(app.id, folderId)
                folderPickerApp = null
            },
            onCreate = {
                folderPickerApp = null
                editorDialog = EditorDialog.NewFolder(app)
            },
        )
    }

    editorDialog?.let { dialog ->
        val initialValue = when (dialog) {
            is EditorDialog.RenameApp -> dialog.item.label
            is EditorDialog.RenameFolder -> dialog.item.label
            is EditorDialog.NewFolder -> ""
        }
        NameDialog(
            title = when (dialog) {
                is EditorDialog.NewFolder -> "New folder"
                else -> "Rename"
            },
            initialValue = initialValue,
            onDismiss = { editorDialog = null },
            onConfirm = { value ->
                when (dialog) {
                    is EditorDialog.RenameApp -> actions.onRenameApp(dialog.item.id, value)
                    is EditorDialog.RenameFolder -> actions.onRenameFolder(dialog.item.id, value)
                    is EditorDialog.NewFolder -> actions.onCreateFolder(value, dialog.app.id)
                }
                editorDialog = null
            },
        )
    }
}

@Composable
private fun AppManagementSheet(
    item: AppItem,
    isOnHome: Boolean,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
    onHomeClick: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onBlock: () -> Unit,
    onFolder: () -> Unit,
    onUninstall: () -> Unit,
    onInfo: () -> Unit,
) {
    ManagementSheet(title = item.label, onDismiss = onDismiss) {
        ManagementAction(if (isOnHome) "Remove from Home" else "Add to Home", onHomeClick)
        ManagementAction("Rename", onRename)
        ManagementAction("Hide", onHide)
        ManagementAction(if (isBlocked) "Unblock" else "Block", onBlock)
        ManagementAction("Put in folder", onFolder)
        ManagementAction("Uninstall", onUninstall)
        ManagementAction("App info", onInfo)
    }
}

@Composable
private fun FolderManagementSheet(
    item: FolderItem,
    isOnHome: Boolean,
    onDismiss: () -> Unit,
    onHomeClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    ManagementSheet(title = item.label, onDismiss = onDismiss) {
        ManagementAction(if (isOnHome) "Remove from Home" else "Add to Home", onHomeClick)
        ManagementAction("Rename", onRename)
        ManagementAction("Delete folder", onDelete)
    }
}

@Composable
private fun ManagementSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        contentColor = Color.White,
        shape = RectangleShape,
        dragHandle = null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Text(title, modifier = Modifier.weight(4f), textAlign = TextAlign.Center, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            Text("×", modifier = Modifier.weight(1f).clickable(onClick = onDismiss), textAlign = TextAlign.End, fontSize = 42.sp)
        }
        HorizontalDivider(color = Color.LightGray)
        content()
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ManagementAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.fillMaxWidth().height(58.dp).clickable(onClick = onClick).padding(horizontal = 40.dp, vertical = 12.dp),
        color = Color.White,
        fontSize = 24.sp,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderContentsSheet(
    folder: FolderItem,
    blockedIds: Set<String>,
    onDismiss: () -> Unit,
    onLongClickApp: (AppItem) -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        contentColor = Color.White,
        shape = RectangleShape,
        dragHandle = null,
    ) {
        Text(folder.label, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(color = Color.LightGray)
        if (folder.apps.isEmpty()) {
            Text("Empty folder", color = Color.Gray, modifier = Modifier.padding(32.dp), fontSize = 20.sp)
        } else {
            folder.apps.forEach { item ->
                Text(
                    item.label,
                    modifier = Modifier.fillMaxWidth().height(60.dp).combinedClickable(
                        onClick = { openApp(context, item, blockedIds) },
                        onLongClick = { onLongClickApp(item) },
                    ).padding(horizontal = 32.dp, vertical = 12.dp),
                    color = Color.White,
                    fontSize = 25.sp,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FolderPickerDialog(
    app: AppItem,
    folders: List<FolderItem>,
    currentFolderId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text("Put ${app.label} in folder") },
        text = {
            Column {
                if (currentFolderId != null) TextButton(onClick = { onSelect(null) }) { Text("No folder") }
                folders.forEach { folder ->
                    TextButton(onClick = { onSelect(folder.id) }) {
                        Text(if (folder.id == currentFolderId) "${folder.label} ✓" else folder.label)
                    }
                }
                TextButton(onClick = onCreate) { Text("Create new folder") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NameDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true) },
        confirmButton = { Button(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }, enabled = value.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
