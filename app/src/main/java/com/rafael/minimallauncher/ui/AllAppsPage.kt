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

private sealed interface EditorDialog {
    data class RenameApp(val item: AppItem) : EditorDialog
    data class RenameFolder(val item: FolderItem) : EditorDialog
    data class NewFolder(val app: AppItem) : EditorDialog
}

private sealed interface DestructiveAction {
    data class HideApp(val item: AppItem) : DestructiveAction
    data class UninstallApp(val app: LauncherApp, val label: String) : DestructiveAction
    data class DeleteFolder(val item: FolderItem) : DestructiveAction
}

private data class DrawerRow(
    val item: LauncherItem,
    val isFolderChild: Boolean = false,
)

private fun sectionLabel(label: String): String {
    val first = label.trim().firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else "#"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AllAppsPage(
    state: LauncherUiState,
    actions: LauncherActions,
    onOpenSettings: () -> Unit,
    isVisible: Boolean = true,
    appOpener: (android.content.Context, AppItem, Set<String>) -> Unit = ::openApp,
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
    var destructiveAction by remember { mutableStateOf<DestructiveAction?>(null) }
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
    val activeSectionLabel by remember(sections) {
        derivedStateOf {
            val visibleIndex = listState.firstVisibleItemIndex
            sections.lastOrNull { (_, startIndex) -> startIndex <= visibleIndex }?.first
        }
    }
    val isSearching = state.searchQuery.isNotBlank()
    val controlsAtTop = state.preferences.settings.appListControlsPosition == AppListControlsPosition.TOP
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val listToControlsSpacing = if (controlsAtTop) 18.dp else 6.dp
    val controlsBottomSpacing = if (controlsAtTop) 38.dp else 8.dp
    val listBottomPadding = when {
        controlsAtTop && isSearching -> 38.dp
        controlsAtTop -> 88.dp
        isSearching -> 8.dp
        showScrollToTop -> 72.dp
        else -> 8.dp
    }

    LaunchedEffect(isVisible, state.preferences.settings.focusSearchOnListOpen) {
        if (isVisible && state.preferences.settings.focusSearchOnListOpen) {
            searchFieldValue = searchFieldValue.copy(
                selection = TextRange(searchFieldValue.text.length),
            )
            withFrameNanos { }
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else if (!isVisible) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(state.searchQuery, isSearching) {
        if (isSearching && drawerRows.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(start = 20.dp, end = 4.dp),
    ) {
        if (controlsAtTop) {
            Spacer(Modifier.height(38.dp))
            AppListControls(
                searchFieldValue = searchFieldValue,
                searchFocusRequester = searchFocusRequester,
                onSearchChange = { value ->
                    searchFieldValue = value
                    actions.onSearchChange(value.text)
                },
                onClearSearch = {
                    searchFieldValue = TextFieldValue("")
                    actions.onSearchChange("")
                    searchFocusRequester.requestFocus()
                },
                onSearchSubmit = {
                    val result = LauncherUiStateMapper.singleLaunchableSearchResult(
                        installedApps = state.apps,
                        preferences = state.preferences,
                        query = searchFieldValue.text,
                    )
                    if (result != null) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        appOpener(context, result, state.preferences.blockedAppIds)
                    }
                },
                onOpenSettings = onOpenSettings,
            )
            Spacer(Modifier.height(18.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(if (controlsAtTop) Modifier else Modifier.statusBarsPadding()),
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = isSearching && !controlsAtTop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (isSearching) 0.dp else 24.dp),
                contentPadding = PaddingValues(bottom = listBottomPadding),
            ) {
                items(drawerRows, key = { row -> "${row.item.id}:${row.isFolderChild}" }) { row ->
                    when (val item = row.item) {
                        is FolderItem -> DrawerFolderRow(
                            item = item,
                            expanded = item.id in expandedFolderIds,
                            onClick = {
                                expandedFolderIds = if (item.id in expandedFolderIds) {
                                    expandedFolderIds - item.id
                                } else {
                                    expandedFolderIds + item.id
                                }
                            },
                            onLongClick = { selectedFolder = item },
                        )
                        is AppItem -> DrawerAppRow(
                            item = item,
                            isFolderChild = row.isFolderChild,
                            onClick = { appOpener(context, item, state.preferences.blockedAppIds) },
                            onLongClick = { selectedApp = item },
                        )
                    }
                }
            }
            if (!isSearching && sections.isNotEmpty()) {
                AlphabetRail(
                    sections = sections,
                    activeLabel = activeSectionLabel,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onSelect = { index ->
                        scope.launch {
                            if (index in drawerRows.indices) listState.scrollToItem(index)
                        }
                    },
                )
            }
            if (!isSearching && showScrollToTop) {
                ScrollToTopButton(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp),
                    onClick = {
                        scope.launch {
                            if (drawerRows.isNotEmpty()) listState.animateScrollToItem(0)
                        }
                    },
                )
            }
        }
        if (!controlsAtTop) {
            Spacer(Modifier.height(listToControlsSpacing))
            AppListControls(
                searchFieldValue = searchFieldValue,
                searchFocusRequester = searchFocusRequester,
                onSearchChange = { value ->
                    searchFieldValue = value
                    actions.onSearchChange(value.text)
                },
                onClearSearch = {
                    searchFieldValue = TextFieldValue("")
                    actions.onSearchChange("")
                    searchFocusRequester.requestFocus()
                },
                onSearchSubmit = {
                    val result = LauncherUiStateMapper.singleLaunchableSearchResult(
                        installedApps = state.apps,
                        preferences = state.preferences,
                        query = searchFieldValue.text,
                    )
                    if (result != null) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        appOpener(context, result, state.preferences.blockedAppIds)
                    }
                },
                onOpenSettings = onOpenSettings,
            )
            Spacer(Modifier.height(controlsBottomSpacing))
        }
    }

    selectedApp?.let { item ->
        AppManagementSheet(
            item = item,
            isOnHome = item.id in state.favoriteIds,
            isBlocked = item.id in state.preferences.blockedAppIds,
            onDismiss = { selectedApp = null },
            onHomeClick = {
                actions.onToggleHomeItem(HomeItemRef.App(item.id))
                selectedApp = null
            },
            onRename = {
                selectedApp = null
                editorDialog = EditorDialog.RenameApp(item)
            },
            onHide = {
                selectedApp = null
                destructiveAction = DestructiveAction.HideApp(item)
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
                destructiveAction = DestructiveAction.UninstallApp(item.app, item.label)
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
                actions.onToggleHomeItem(HomeItemRef.Folder(item.id))
                selectedFolder = null
            },
            onRename = {
                selectedFolder = null
                editorDialog = EditorDialog.RenameFolder(item)
            },
            onDelete = {
                selectedFolder = null
                destructiveAction = DestructiveAction.DeleteFolder(item)
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

    destructiveAction?.let { action ->
        when (action) {
            is DestructiveAction.HideApp -> DestructiveConfirmationDialog(
                title = stringResource(R.string.confirm_hide_title, action.item.label),
                message = stringResource(R.string.confirm_hide_message),
                confirmLabel = stringResource(R.string.confirm_hide_action),
                onDismiss = { destructiveAction = null },
                onConfirm = {
                    actions.onHideApp(action.item.id)
                    destructiveAction = null
                },
            )
            is DestructiveAction.DeleteFolder -> DestructiveConfirmationDialog(
                title = stringResource(R.string.confirm_delete_folder_title, action.item.label),
                message = stringResource(R.string.confirm_delete_folder_message),
                confirmLabel = stringResource(R.string.confirm_delete_folder_action),
                onDismiss = { destructiveAction = null },
                onConfirm = {
                    actions.onDeleteFolder(action.item.id)
                    destructiveAction = null
                },
            )
            is DestructiveAction.UninstallApp -> DestructiveConfirmationDialog(
                title = stringResource(R.string.confirm_uninstall_title, action.label),
                message = stringResource(R.string.confirm_uninstall_message),
                confirmLabel = stringResource(R.string.confirm_uninstall_action),
                onDismiss = { destructiveAction = null },
                onConfirm = {
                    actions.onRequestUninstall(action.app)
                    destructiveAction = null
                },
            )
        }
    }
}
