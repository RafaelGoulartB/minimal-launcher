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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
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
    var openedFolder by remember { mutableStateOf<FolderItem?>(null) }
    var displayedItems by remember { mutableStateOf(state.homeItems) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
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
            state.isLoading -> Text("Loading apps…", color = Color.Gray)
            displayedItems.isEmpty() -> Text("Swipe left to add apps to Home.", color = Color.Gray, fontSize = 18.sp)
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(displayedItems, key = LauncherItem::id) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .zIndex(if (draggedItemId == item.id) 1f else 0f)
                            .graphicsLayer { translationY = if (draggedItemId == item.id) dragOffset else 0f }
                            .background(if (draggedItemId == item.id) Color(0xFF161616) else Color.Transparent)
                            .pointerInput(item.id) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val earlyFinish = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                        waitForUpOrCancellation() to true
                                    }
                                    if (earlyFinish != null) {
                                        if (earlyFinish.first != null) {
                                            selectedItemId = null
                                            when (item) {
                                                is AppItem -> openApp(context, item, state.preferences.blockedAppIds)
                                                is FolderItem -> openedFolder = item
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
                                        val targetInfo = layout.visibleItemsInfo.firstOrNull { info ->
                                            info.key != item.id && draggedCenter in info.offset.toFloat()..(info.offset + info.size).toFloat()
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
                                        is FolderItem -> openedFolder = item
                                    }
                                    true
                                }
                                onLongClick {
                                    selectedItemId = item.id
                                    true
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.label,
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            fontSize = 21.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (selectedItemId == item.id) {
                            TextButton(
                                onClick = {
                                    val ref = when (item) {
                                        is AppItem -> HomeItemRef.App(item.id)
                                        is FolderItem -> HomeItemRef.Folder(item.id)
                                    }
                                    selectedItemId = null
                                    actions.onRemoveHomeItem(ref)
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray),
                            ) { Text("Remove") }
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
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                    ),
                color = Color.LightGray,
                fontSize = 16.sp,
            )
        }
    }
    openedFolder?.let { folder ->
        FolderContentsSheet(
            folder = folder,
            blockedIds = state.preferences.blockedAppIds,
            onDismiss = { openedFolder = null },
            onLongClickApp = {},
        )
    }
}

@Composable
private fun ClockHeader(settings: LauncherSettings) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val batteryPercentage = rememberBatteryPercentage()
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000 - (System.currentTimeMillis() % 60_000))
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 92.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val use24Hour = when (settings.clockFormat) {
            ClockFormat.SYSTEM -> DateFormat.is24HourFormat(context)
            ClockFormat.TWELVE_HOUR -> false
            ClockFormat.TWENTY_FOUR_HOUR -> true
        }
        Text(
            now.format(DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a")),
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Normal,
        )
        if (settings.showDate) {
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
                color = Color.LightGray,
                fontSize = 18.sp,
            )
        }
        if (settings.showBattery) {
            Spacer(Modifier.height(8.dp))
            BatteryIndicator(batteryPercentage)
        }
    }
}

internal fun formatUsageDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
private fun RefreshUsageWhileVisible(onRefresh: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner, onRefresh) {
        var refreshJob: Job? = null
        fun start() {
            if (refreshJob != null) return
            refreshJob = scope.launch {
                while (true) {
                    onRefresh()
                    delay(60_000)
                }
            }
        }
        fun stop() {
            refreshJob?.cancel()
            refreshJob = null
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose {
            stop()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun BatteryIndicator(percentage: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(percentage?.let { "$it%" } ?: "--%", color = Color.LightGray, fontSize = 16.sp)
        Canvas(modifier = Modifier.width(42.dp).height(19.dp)) {
            val strokeWidth = 2.2.dp.toPx()
            val tipWidth = 3.5.dp.toPx()
            val bodyWidth = size.width - tipWidth
            val inset = strokeWidth / 2
            val fillInset = 4.dp.toPx()
            val fillWidth = (bodyWidth - fillInset * 2) * ((percentage ?: 0) / 100f)
            drawRoundRect(
                Color.White,
                androidx.compose.ui.geometry.Offset(inset, inset),
                androidx.compose.ui.geometry.Size(bodyWidth - strokeWidth, size.height - strokeWidth),
                androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                style = Stroke(strokeWidth),
            )
            if (fillWidth > 0f) {
                drawRoundRect(
                    Color.White,
                    androidx.compose.ui.geometry.Offset(fillInset, fillInset),
                    androidx.compose.ui.geometry.Size(fillWidth, size.height - fillInset * 2),
                    androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                )
            }
            drawRoundRect(
                Color.White,
                androidx.compose.ui.geometry.Offset(bodyWidth - inset, size.height * 0.3f),
                androidx.compose.ui.geometry.Size(tipWidth, size.height * 0.4f),
                androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun rememberBatteryPercentage(): Int? {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    var percentage by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(context, lifecycleOwner) {
        var registered = false
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                percentage = if (level >= 0 && scale > 0) (level * 100f / scale).toInt().coerceIn(0, 100) else null
            }
        }
        fun register() {
            if (registered) return
            val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            sticky?.let { receiver.onReceive(context, it) }
            registered = true
        }
        fun unregister() {
            if (registered) context.unregisterReceiver(receiver)
            registered = false
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> register()
                Lifecycle.Event.ON_STOP -> unregister()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) register()
        onDispose {
            unregister()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return percentage
}
