@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rafael.minimallauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.AppListControlsPosition
import com.rafael.minimallauncher.data.FolderItem
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherItem
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun AppManagementSheet(
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
internal fun FolderManagementSheet(
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
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Text(title, modifier = Modifier.weight(4f), textAlign = TextAlign.Center, fontSize = launcherSp(20.sp), fontWeight = FontWeight.SemiBold)
            Text("×", modifier = Modifier.weight(1f).clickable(onClick = onDismiss), textAlign = TextAlign.End, fontSize = launcherSp(34.sp))
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
        modifier = Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        color = Color.White,
        fontSize = launcherSp(19.sp),
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
        Text(folder.label, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, fontSize = launcherSp(25.sp), fontWeight = FontWeight.SemiBold)
        HorizontalDivider(color = Color.LightGray)
        if (folder.apps.isEmpty()) {
            Text("Empty folder", color = Color.Gray, modifier = Modifier.padding(32.dp), fontSize = launcherSp(20.sp))
        } else {
            folder.apps.forEach { item ->
                Text(
                    item.label,
                    modifier = Modifier.fillMaxWidth().height(60.dp).combinedClickable(
                        onClick = { openApp(context, item, blockedIds) },
                        onLongClick = { onLongClickApp(item) },
                    ).padding(horizontal = 32.dp, vertical = 12.dp),
                    color = Color.White,
                    fontSize = launcherSp(25.sp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun FolderPickerDialog(
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
internal fun NameDialog(
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
