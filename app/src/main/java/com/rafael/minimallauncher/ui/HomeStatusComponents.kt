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
internal fun ClockHeader(settings: LauncherSettings) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val batteryState = rememberBatteryState()
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
            fontSize = launcherSp(48.sp),
            fontWeight = FontWeight.Normal,
        )
        if (settings.showDate) {
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
                modifier = Modifier.offset(y = (-4).dp),
                color = Color.LightGray,
                fontSize = launcherSp(16.sp),
            )
        }
        if (settings.showBattery) {
            Spacer(Modifier.height(2.dp))
            BatteryIndicator(batteryState)
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
internal fun RefreshUsageWhileVisible(onRefresh: () -> Unit) {
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

internal data class HomeBatteryState(
    val percentage: Int? = null,
    val isCharging: Boolean = false,
)

internal fun homeBatteryState(level: Int, scale: Int, status: Int): HomeBatteryState = HomeBatteryState(
    percentage = if (level >= 0 && scale > 0) {
        (level * 100f / scale).toInt().coerceIn(0, 100)
    } else {
        null
    },
    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL,
)

@Composable
private fun BatteryIndicator(state: HomeBatteryState) {
    val accessibilityLabel = when {
        state.percentage == null -> stringResource(R.string.battery_status_unavailable)
        state.isCharging -> stringResource(R.string.battery_status_charging, state.percentage)
        else -> stringResource(R.string.battery_status, state.percentage)
    }
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = accessibilityLabel },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            state.percentage?.let { "$it%" } ?: "--%",
            color = Color.LightGray,
            fontSize = launcherSp(14.sp),
        )
        Canvas(modifier = Modifier.width(30.dp).height(14.dp)) {
            val strokeWidth = 1.8.dp.toPx()
            val tipWidth = 2.5.dp.toPx()
            val bodyWidth = size.width - tipWidth
            val inset = strokeWidth / 2
            val fillInset = 3.dp.toPx()
            val fillWidth = (bodyWidth - fillInset * 2) * ((state.percentage ?: 0) / 100f)
            drawRoundRect(
                Color.White,
                androidx.compose.ui.geometry.Offset(inset, inset),
                androidx.compose.ui.geometry.Size(bodyWidth - strokeWidth, size.height - strokeWidth),
                androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                style = Stroke(strokeWidth),
            )
            if (fillWidth > 0f) {
                drawRoundRect(
                    Color.White,
                    androidx.compose.ui.geometry.Offset(fillInset, fillInset),
                    androidx.compose.ui.geometry.Size(fillWidth, size.height - fillInset * 2),
                    androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
            }
            drawRoundRect(
                Color.White,
                androidx.compose.ui.geometry.Offset(bodyWidth - inset, size.height * 0.3f),
                androidx.compose.ui.geometry.Size(tipWidth, size.height * 0.4f),
                androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
            )
            if (state.isCharging) {
                val boltPath = Path().apply {
                    moveTo(bodyWidth * 0.56f, size.height * 0.10f)
                    lineTo(bodyWidth * 0.35f, size.height * 0.55f)
                    lineTo(bodyWidth * 0.50f, size.height * 0.55f)
                    lineTo(bodyWidth * 0.42f, size.height * 0.92f)
                    lineTo(bodyWidth * 0.71f, size.height * 0.42f)
                    lineTo(bodyWidth * 0.56f, size.height * 0.42f)
                    close()
                }
                val boltOnFilledArea = fillInset + fillWidth >= bodyWidth * 0.54f
                val boltColor = if (boltOnFilledArea) Color.Black else Color.White
                val boltOutline = if (boltOnFilledArea) Color.White else Color.Black
                drawPath(
                    path = boltPath,
                    color = boltOutline,
                    style = Stroke(width = 1.1.dp.toPx(), join = StrokeJoin.Round),
                )
                drawPath(path = boltPath, color = boltColor)
            }
        }
    }
}

@Composable
private fun rememberBatteryState(): HomeBatteryState {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    var batteryState by remember { mutableStateOf(HomeBatteryState()) }
    DisposableEffect(context, lifecycleOwner) {
        var registered = false
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN,
                )
                batteryState = homeBatteryState(level = level, scale = scale, status = status)
            }
        }
        fun register() {
            if (registered) return
            runCatching {
                val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("DEPRECATION")
                    context.registerReceiver(receiver, filter)
                }
                registered = true
                sticky?.let { receiver.onReceive(context, it) }
            }
        }
        fun unregister() {
            if (registered) runCatching { context.unregisterReceiver(receiver) }
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
    return batteryState
}
