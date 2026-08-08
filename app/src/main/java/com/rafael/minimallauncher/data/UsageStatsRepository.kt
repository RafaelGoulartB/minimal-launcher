package com.rafael.minimallauncher.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class DailyUsage(
    val hasAccess: Boolean = false,
    val durationMillis: Long? = null,
)

interface UsageStatsRepository {
    suspend fun loadTodayUsage(): DailyUsage
}

class AndroidUsageStatsRepository(private val context: Context) : UsageStatsRepository {
    override suspend fun loadTodayUsage(): DailyUsage = withContext(Dispatchers.Default) {
        if (!hasUsageAccess()) return@withContext DailyUsage()
        val end = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        // Read the previous day only to establish whether an activity was already in the
        // foreground at midnight. The calculator clips every interval to today's bounds.
        val historyStart = today.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(historyStart, end)
        val transitions = buildList {
            if (events != null) {
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    event.toUsageTransition()?.let(::add)
                }
            }
        }
        val duration = calculateUsageDuration(start, end, transitions)
        DailyUsage(hasAccess = true, durationMillis = duration)
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

internal enum class UsageTransitionType {
    FOREGROUND,
    BACKGROUND,
    RESET,
}

internal data class UsageTransition(
    val timestampMillis: Long,
    val type: UsageTransitionType,
    val activityId: String = "",
)

private fun UsageEvents.Event.toUsageTransition(): UsageTransition? {
    val type = when (eventType) {
        UsageEvents.Event.ACTIVITY_RESUMED -> UsageTransitionType.FOREGROUND
        UsageEvents.Event.ACTIVITY_PAUSED,
        UsageEvents.Event.ACTIVITY_STOPPED,
        -> UsageTransitionType.BACKGROUND
        UsageEvents.Event.SCREEN_NON_INTERACTIVE,
        UsageEvents.Event.KEYGUARD_SHOWN,
        UsageEvents.Event.DEVICE_SHUTDOWN,
        -> UsageTransitionType.RESET
        else -> return null
    }
    val activityId = "$packageName/${className.orEmpty()}"
    return UsageTransition(timeStamp, type, activityId)
}

/**
 * Calculates the union of foreground activity intervals inside [windowStart, windowEnd].
 * Counting the union prevents split-screen activities and system overlays from inflating the
 * device's elapsed screen time beyond the time that has passed since midnight.
 */
internal fun calculateUsageDuration(
    windowStart: Long,
    windowEnd: Long,
    transitions: List<UsageTransition>,
): Long {
    if (windowEnd <= windowStart) return 0L

    val foregroundActivities = mutableSetOf<String>()
    var cursor = windowStart
    var duration = 0L

    transitions.sortedBy(UsageTransition::timestampMillis).forEach { transition ->
        if (transition.timestampMillis >= windowEnd) return@forEach

        if (transition.timestampMillis >= windowStart) {
            val eventTime = transition.timestampMillis.coerceAtLeast(cursor)
            if (foregroundActivities.isNotEmpty()) duration += eventTime - cursor
            cursor = eventTime
        }

        when (transition.type) {
            UsageTransitionType.FOREGROUND -> foregroundActivities += transition.activityId
            UsageTransitionType.BACKGROUND -> foregroundActivities -= transition.activityId
            UsageTransitionType.RESET -> foregroundActivities.clear()
        }
    }

    if (foregroundActivities.isNotEmpty()) duration += windowEnd - cursor
    return duration.coerceIn(0L, windowEnd - windowStart)
}
