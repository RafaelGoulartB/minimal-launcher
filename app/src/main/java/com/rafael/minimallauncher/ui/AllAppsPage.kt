@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rafael.minimallauncher.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.FolderItem
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherItem
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private sealed interface EditorDialog {
    data class RenameApp(val item: AppItem) : EditorDialog
    data class RenameFolder(val item: FolderItem) : EditorDialog
    data class NewFolder(val app: AppItem) : EditorDialog
}

private data class DrawerRow(
    val item: LauncherItem,
    val isFolderChild: Boolean = false,
)

private fun sectionLabel(label: String): String {
    val first = label.trim().firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else "#"
}

@Composable
private fun SearchGlyph() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val stroke = 2.4.dp.toPx()
        drawCircle(
            color = Color(0xFFBEBEBE),
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.42f, size.height * 0.4f),
            style = Stroke(stroke),
        )
        drawLine(
            color = Color(0xFFBEBEBE),
            start = Offset(size.width * 0.61f, size.height * 0.6f),
            end = Offset(size.width * 0.84f, size.height * 0.83f),
            strokeWidth = stroke,
        )
    }
}

@Composable
private fun ClearSearchButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = "Clear search" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val stroke = 2.4.dp.toPx()
            drawLine(
                color = Color(0xFFBEBEBE),
                start = Offset(size.width * 0.2f, size.height * 0.2f),
                end = Offset(size.width * 0.8f, size.height * 0.8f),
                strokeWidth = stroke,
            )
            drawLine(
                color = Color(0xFFBEBEBE),
                start = Offset(size.width * 0.8f, size.height * 0.2f),
                end = Offset(size.width * 0.2f, size.height * 0.8f),
                strokeWidth = stroke,
            )
        }
    }
}

@Composable
private fun GearButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .semantics { contentDescription = "Settings" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension * 0.37f
            val innerRadius = size.minDimension * 0.14f
            val stroke = 2.8.dp.toPx()
            drawCircle(Color.White, outerRadius * 0.72f, center, style = Stroke(stroke))
            drawCircle(Color.White, innerRadius, center, style = Stroke(stroke))
            repeat(8) { index ->
                val angle = index * PI.toFloat() / 4f
                drawLine(
                    color = Color.White,
                    start = Offset(
                        center.x + cos(angle) * outerRadius * 0.68f,
                        center.y + sin(angle) * outerRadius * 0.68f,
                    ),
                    end = Offset(
                        center.x + cos(angle) * outerRadius,
                        center.y + sin(angle) * outerRadius,
                    ),
                    strokeWidth = stroke,
                )
            }
        }
    }
}

@Composable
private fun AlphabetRail(
    sections: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    fun selectAt(y: Float, height: Float) {
        if (height <= 0f) return
        val slot = (y / height * sections.size).toInt().coerceIn(sections.indices)
        onSelect(sections[slot].second)
    }
    Column(
        modifier = modifier
            .width(24.dp)
            .pointerInput(sections) {
                detectVerticalDragGestures(
                    onDragStart = { selectAt(it.y, size.height.toFloat()) },
                    onVerticalDrag = { change, _ -> selectAt(change.position.y, size.height.toFloat()) },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        sections.forEach { (label, index) ->
            Text(
                label,
                modifier = Modifier.clickable { onSelect(index) }.padding(vertical = 1.dp),
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun FolderGlyph() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = 1.8.dp.toPx()
        val left = 2.dp.toPx()
        val top = 6.dp.toPx()
        val right = size.width - 2.dp.toPx()
        val bottom = size.height - 5.dp.toPx()
        val tabRight = left + size.width * 0.38f
        drawLine(Color(0xFFBDBDBD), Offset(left, top), Offset(left + 6.dp.toPx(), top))
        drawLine(Color(0xFFBDBDBD), Offset(left + 6.dp.toPx(), top), Offset(tabRight, top + 4.dp.toPx()))
        drawLine(Color(0xFFBDBDBD), Offset(tabRight, top + 4.dp.toPx()), Offset(right, top + 4.dp.toPx()))
        drawRoundRect(
            color = Color(0xFFBDBDBD),
            topLeft = Offset(left, top + 2.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(stroke),
        )
    }
}

@Composable
private fun FolderExpandGlyph(expanded: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .semantics {
                contentDescription = if (expanded) "Collapse folder" else "Expand folder"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = 2.dp.toPx()
            val centerX = size.width / 2f
            val edgeX = 3.dp.toPx()
            val edgeY = if (expanded) size.height - 4.dp.toPx() else 4.dp.toPx()
            drawLine(
                Color(0xFFBDBDBD),
                Offset(edgeX, edgeY),
                Offset(centerX, if (expanded) 4.dp.toPx() else size.height - 4.dp.toPx()),
                strokeWidth = stroke,
            )
            drawLine(
                Color(0xFFBDBDBD),
                Offset(centerX, if (expanded) 4.dp.toPx() else size.height - 4.dp.toPx()),
                Offset(size.width - edgeX, edgeY),
                strokeWidth = stroke,
            )
        }
    }
}

@Composable
private fun ScrollToTopButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(52.dp)
            .background(Color.White, CircleShape)
            .semantics { contentDescription = "Scroll to top" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(25.dp)) {
            val stroke = 2.4.dp.toPx()
            val centerX = size.width / 2f
            val top = 3.dp.toPx()
            val bottom = size.height - 3.dp.toPx()
            drawLine(Color.Black, Offset(centerX, bottom), Offset(centerX, top), strokeWidth = stroke)
            drawLine(Color.Black, Offset(centerX, top), Offset(5.dp.toPx(), 9.dp.toPx()), strokeWidth = stroke)
            drawLine(Color.Black, Offset(centerX, top), Offset(size.width - 5.dp.toPx(), 9.dp.toPx()), strokeWidth = stroke)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AllAppsPage(
    state: LauncherUiState,
    actions: LauncherActions,
    onOpenSettings: () -> Unit,
    isVisible: Boolean = true,
) {
    val context = LocalContext.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var searchFieldValue by remember { mutableStateOf(TextFieldValue(state.searchQuery)) }
    var selectedApp by remember { mutableStateOf<AppItem?>(null) }
    var selectedFolder by remember { mutableStateOf<FolderItem?>(null) }
    var expandedFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var folderPickerApp by remember { mutableStateOf<AppItem?>(null) }
    var editorDialog by remember { mutableStateOf<EditorDialog?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val drawerRows = remember(state.drawerItems, expandedFolderIds, state.searchQuery) {
        state.drawerItems.flatMap { item ->
            buildList {
                add(DrawerRow(item))
                if (state.searchQuery.isBlank() && item is FolderItem && item.id in expandedFolderIds) {
                    addAll(item.apps.map { app -> DrawerRow(app, isFolderChild = true) })
                }
            }
        }
    }
    val sections = remember(drawerRows) {
        drawerRows.mapIndexed { index, row -> sectionLabel(row.item.label) to index }
            .distinctBy { it.first }
    }
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(isVisible, state.preferences.settings.focusSearchOnListOpen) {
        if (isVisible && state.preferences.settings.focusSearchOnListOpen) {
            withFrameNanos { }
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else if (!isVisible) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 4.dp)) {
        Spacer(Modifier.height(38.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            BasicTextField(
                value = searchFieldValue,
                onValueChange = { value ->
                    searchFieldValue = value
                    actions.onSearchChange(value.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .focusRequester(searchFocusRequester)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF1B1B1B)),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 24.sp),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchGlyph()
                        Spacer(Modifier.width(15.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchFieldValue.text.isEmpty()) {
                                Text("Search apps", color = Color(0xFF9A9A9A), fontSize = 24.sp)
                            }
                            innerTextField()
                        }
                        if (searchFieldValue.text.isNotEmpty()) {
                            ClearSearchButton {
                                searchFieldValue = TextFieldValue("")
                                actions.onSearchChange("")
                                searchFocusRequester.requestFocus()
                            }
                        }
                    }
                },
            )
            GearButton(onClick = onOpenSettings)
        }
        Spacer(Modifier.height(18.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (state.searchQuery.isBlank()) 24.dp else 0.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(drawerRows, key = { row -> "${row.item.id}:${row.isFolderChild}" }) { row ->
                    val item = row.item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(start = if (row.isFolderChild) 26.dp else 0.dp)
                            .combinedClickable(
                                onClick = {
                                    when (item) {
                                        is AppItem -> openApp(context, item, state.preferences.blockedAppIds)
                                        is FolderItem -> {
                                            expandedFolderIds = if (item.id in expandedFolderIds) {
                                                expandedFolderIds - item.id
                                            } else {
                                                expandedFolderIds + item.id
                                            }
                                        }
                                    }
                                },
                                onLongClick = {
                                    when (item) {
                                        is AppItem -> selectedApp = item
                                        is FolderItem -> selectedFolder = item
                                    }
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (row.isFolderChild) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(30.dp)
                                    .background(Color(0xFF555555)),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        if (item is FolderItem) {
                            FolderExpandGlyph(expanded = item.id in expandedFolderIds)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            item.label,
                            modifier = if (item is FolderItem) Modifier.weight(1f) else Modifier,
                            color = if (item is FolderItem) Color(0xFFE4E4E4) else Color.White,
                            fontSize = 21.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item is FolderItem) {
                            Spacer(Modifier.width(12.dp))
                            FolderGlyph()
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
            if (state.searchQuery.isBlank() && sections.isNotEmpty()) {
                AlphabetRail(
                    sections = sections,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onSelect = { index -> scope.launch { listState.scrollToItem(index) } },
                )
            }
            if (showScrollToTop) {
                ScrollToTopButton(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp),
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                )
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
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Text(title, modifier = Modifier.weight(4f), textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("×", modifier = Modifier.weight(1f).clickable(onClick = onDismiss), textAlign = TextAlign.End, fontSize = 34.sp)
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
        modifier = Modifier.fillMaxWidth().height(58.dp).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        color = Color.White,
        fontSize = 19.sp,
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
