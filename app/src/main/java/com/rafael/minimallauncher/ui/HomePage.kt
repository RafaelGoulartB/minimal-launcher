@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rafael.minimallauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.ClockFormat
import com.rafael.minimallauncher.data.FolderItem
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherItem
import com.rafael.minimallauncher.data.LauncherSettings
import com.rafael.minimallauncher.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
internal fun HomePage(state: LauncherUiState, actions: LauncherActions) {
    val context = LocalContext.current
    var displayedItems by remember { mutableStateOf(state.homeItems) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var expandedFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var originalDragIndex by remember { mutableIntStateOf(-1) }
    var currentDragIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var movedDuringDrag by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.homeItems, draggedItemId) {
        if (draggedItemId == null) displayedItems = state.homeItems
        if (selectedItemId != null && state.homeItems.none { it.id == selectedItemId }) selectedItemId = null
        expandedFolderIds = expandedFolderIds.filter { id ->
            state.homeItems.any { it is FolderItem && it.id == id }
        }.toSet()
    }

    RefreshUsageWhileVisible(actions.onRefreshUsage)
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = if (state.preferences.settings.showDailyUsage) 58.dp else 0.dp),
    ) {
        ClockHeader(state.preferences.settings)
        Spacer(Modifier.height(56.dp))
        when {
            displayedItems.isEmpty() -> Text("Swipe left to add apps to Home.", color = Color.Gray, fontSize = launcherSp(18.sp))
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(displayedItems, key = LauncherItem::id) { item ->
                    val isSelected = selectedItemId == item.id
                    val removeButton = @Composable {
                        TextButton(
                            onClick = {
                                val ref = when (item) {
                                    is AppItem -> HomeItemRef.App(item.id)
                                    is FolderItem -> HomeItemRef.Folder(item.id)
                                }
                                selectedItemId = null
                                actions.onRemoveHomeItem(ref)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        ) { Text("Remove") }
                    }
                    val dragVisualModifier = Modifier
                        .zIndex(if (draggedItemId == item.id) 1f else 0f)
                        .graphicsLayer { translationY = if (draggedItemId == item.id) dragOffset else 0f }
                        .background(if (draggedItemId == item.id) Color(0xFF161616) else Color.Transparent)
                    val itemGestureModifier = Modifier
                        .pointerInput(item.id) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val earlyFinish = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                    waitForUpOrCancellation()
                                }
                                if (earlyFinish != null) {
                                    selectedItemId = null
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
                                    return@awaitEachGesture
                                }
                                val index = displayedItems.indexOfFirst { it.id == item.id }
                                if (index < 0) return@awaitEachGesture
                                draggedItemId = item.id
                                originalDragIndex = index
                                currentDragIndex = index
                                dragOffset = 0f
                                movedDuringDrag = false
                                selectedItemId = item.id

                                drag(down.id) { change ->
                                    val amount = change.positionChange()
                                    change.consume()
                                    dragOffset += amount.y
                                    if (abs(dragOffset) > viewConfiguration.touchSlop) {
                                        selectedItemId = null
                                        movedDuringDrag = true
                                    }
                                    val layout = listState.layoutInfo
                                    val draggedInfo = layout.visibleItemsInfo.firstOrNull { it.key == item.id }
                                        ?: return@drag
                                    val draggedCenter = draggedInfo.offset + draggedInfo.size / 2f + dragOffset
                                    val targetInfo = if (abs(dragOffset) > draggedInfo.size / 2f) {
                                        layout.visibleItemsInfo
                                            .filter { info -> info.key != item.id }
                                            .minByOrNull { info ->
                                                abs(draggedCenter - (info.offset + info.size / 2f))
                                            }
                                    } else {
                                        null
                                    }
                                    if (targetInfo != null && targetInfo.index != currentDragIndex) {
                                        val mutable = displayedItems.toMutableList()
                                        val from = mutable.indexOfFirst { it.id == item.id }
                                        val to = targetInfo.index.coerceIn(0, mutable.lastIndex)
                                        if (from >= 0 && to != from) {
                                            mutable.add(to, mutable.removeAt(from))
                                            displayedItems = mutable
                                            currentDragIndex = to
                                            dragOffset += draggedInfo.offset - targetInfo.offset
                                        }
                                    }
                                    val edge = 96.dp.toPx()
                                    when {
                                        draggedCenter < layout.viewportStartOffset + edge -> coroutineScope.launch { listState.scrollBy(-22.dp.toPx()) }
                                        draggedCenter > layout.viewportEndOffset - edge -> coroutineScope.launch { listState.scrollBy(22.dp.toPx()) }
                                    }
                                }
                                if (movedDuringDrag && originalDragIndex >= 0 && currentDragIndex >= 0) {
                                    actions.onMoveHomeItem(originalDragIndex, currentDragIndex)
                                }
                                draggedItemId = null
                                dragOffset = 0f
                            }
                        }
                        .semantics {
                            onClick {
                                selectedItemId = null
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
                                true
                            }
                            onLongClick {
                                selectedItemId = item.id
                                true
                            }
                        }

                    when (item) {
                        is FolderItem -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(dragVisualModifier),
                        ) {
                            DrawerFolderRow(
                                item = item,
                                expanded = item.id in expandedFolderIds,
                                modifier = itemGestureModifier,
                                trailingContent = if (isSelected) removeButton else null,
                            )
                            if (item.id in expandedFolderIds) {
                                item.apps.forEach { app ->
                                    DrawerAppRow(
                                        item = app,
                                        isFolderChild = true,
                                        onClick = {
                                            selectedItemId = null
                                            openApp(context, app, state.preferences.blockedAppIds)
                                        },
                                    )
                                }
                            }
                        }
                        is AppItem -> Row(
                            modifier = dragVisualModifier
                                .then(itemGestureModifier)
                                .fillMaxWidth()
                                .height(44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                item.label,
                                modifier = Modifier.weight(1f),
                                color = Color.White,
                                fontSize = launcherSp(21.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isSelected) removeButton()
                        }
                    }
                }
            }
        }
    }
        if (state.preferences.settings.showDailyUsage) {
            val usageText = if (state.dailyUsage.hasAccess) {
                "Today's Usage: ${formatUsageDuration(state.dailyUsage.durationMillis ?: 0L)}"
            } else {
                "Enable usage access"
            }
            Text(
                usageText,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .then(
                        if (state.dailyUsage.hasAccess) Modifier
                        else Modifier.clickable {
                            runCatching { context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            ) }
                        },
                    ),
                color = Color.LightGray,
                fontSize = launcherSp(16.sp),
            )
        }
    }
}
