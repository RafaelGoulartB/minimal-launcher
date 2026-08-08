package com.rafael.minimallauncher.data

import android.app.AppOpsManager
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
        val start = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val duration = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .sumOf { it.totalTimeInForeground.coerceAtLeast(0L) }
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
