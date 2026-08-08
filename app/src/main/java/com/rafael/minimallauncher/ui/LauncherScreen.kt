package com.rafael.minimallauncher.ui

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rafael.minimallauncher.data.LauncherApp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun LauncherScreen(
    state: LauncherUiState,
    onSearchChange: (String) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) { page ->
        when (page) {
            0 -> HomePage(state, onToggleFavorite)
            else -> AllAppsPage(state, onSearchChange, onToggleFavorite)
        }
    }
}

@Composable
private fun HomePage(state: LauncherUiState, onToggleFavorite: (LauncherApp) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
    ) {
        ClockHeader()
        Spacer(Modifier.height(46.dp))
        if (state.isLoading) {
            Text("Loading apps…", color = Color.Gray)
        } else if (state.favoriteApps.isEmpty()) {
            Text(
                "Swipe left to add apps to Home.",
                color = Color.Gray,
                fontSize = 18.sp,
            )
        } else {
            AppList(
                apps = state.favoriteApps,
                favoriteIds = state.favoriteIds,
                onToggleFavorite = onToggleFavorite,
            )
        }
    }
}

@Composable
private fun AllAppsPage(
    state: LauncherUiState,
    onSearchChange: (String) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(54.dp))
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search apps") },
        )
        Spacer(Modifier.height(16.dp))
        AppList(
            apps = state.filteredApps,
            favoriteIds = state.favoriteIds,
            onToggleFavorite = onToggleFavorite,
        )
    }
}

@Composable
private fun AppList(
    apps: List<LauncherApp>,
    favoriteIds: Set<String>,
    onToggleFavorite: (LauncherApp) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(apps, key = LauncherApp::id) { app ->
            AppRow(
                app = app,
                isFavorite = app.id in favoriteIds,
                onOpen = { openApp(context, app) },
                onToggleFavorite = { onToggleFavorite(app) },
            )
        }
    }
}

@Composable
private fun AppRow(
    app: LauncherApp,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onOpen)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = app.label,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 26.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(
            onClick = onToggleFavorite,
            colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray),
        ) {
            Text(if (isFavorite) "Remove" else "Add")
        }
    }
}

@Composable
private fun ClockHeader() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val batteryPercentage = rememberBatteryPercentage()
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000 - (System.currentTimeMillis() % 60_000))
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 78.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color.White,
            fontSize = 58.sp,
            fontWeight = FontWeight.Normal,
        )
        Text(
            now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())),
            color = Color.LightGray,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(8.dp))
        BatteryIndicator(batteryPercentage)
    }
}

@Composable
private fun BatteryIndicator(percentage: Int?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = percentage?.let { "$it%" } ?: "--%",
            color = Color.LightGray,
            fontSize = 18.sp,
        )
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .width(62.dp)
                .height(28.dp),
        ) {
            val strokeWidth = 3.dp.toPx()
            val tipWidth = 5.dp.toPx()
            val bodyWidth = size.width - tipWidth
            val inset = strokeWidth / 2
            val bodyHeight = size.height - strokeWidth
            val cornerRadius = 7.dp.toPx()
            val fillInset = 5.dp.toPx()
            val fillWidth = ((bodyWidth - (fillInset * 2)) * ((percentage ?: 0) / 100f))

            drawRoundRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(bodyWidth - strokeWidth, bodyHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(strokeWidth),
            )
            if (fillWidth > 0f) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset(fillInset, fillInset),
                    size = androidx.compose.ui.geometry.Size(fillWidth, size.height - (fillInset * 2)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
            }
            drawRoundRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(bodyWidth - inset, size.height * 0.3f),
                size = androidx.compose.ui.geometry.Size(tipWidth, size.height * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
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
        var isRegistered = false
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                percentage = intent.toBatteryPercentage()
            }
        }

        fun registerReceiver() {
            if (isRegistered) return
            val stickyIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            percentage = stickyIntent?.toBatteryPercentage()
            isRegistered = true
        }

        fun unregisterReceiver() {
            if (!isRegistered) return
            context.unregisterReceiver(receiver)
            isRegistered = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> registerReceiver()
                Lifecycle.Event.ON_STOP -> unregisterReceiver()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            registerReceiver()
        }

        onDispose {
            unregisterReceiver()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return percentage
}

private fun Intent.toBatteryPercentage(): Int? {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    return (level * 100f / scale).toInt().coerceIn(0, 100)
}

private fun openApp(context: Context, app: LauncherApp) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(app.componentName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // The app may have been removed between listing and tapping it.
    }
}

@Composable
fun MinimalLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
        ),
        content = content,
    )
}
