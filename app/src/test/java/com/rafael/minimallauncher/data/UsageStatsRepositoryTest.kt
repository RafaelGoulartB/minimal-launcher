package com.rafael.minimallauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageStatsRepositoryTest {
    @Test
    fun completedUsageFromYesterdayIsNotIncluded() {
        val transitions = listOf(
            foreground(at = 20, activity = "app"),
            background(at = 80, activity = "app"),
        )

        assertEquals(0L, calculateUsageDuration(100, 1_000, transitions))
    }

    @Test
    fun usageIsClippedToStartOfToday() {
        val transitions = listOf(
            foreground(at = 50, activity = "app"),
            background(at = 300, activity = "app"),
        )

        assertEquals(200L, calculateUsageDuration(100, 1_000, transitions))
    }

    @Test
    fun usageAfterEndOfWindowIsIgnored() {
        val transitions = listOf(
            foreground(at = 200, activity = "app"),
            background(at = 1_200, activity = "app"),
        )

        assertEquals(800L, calculateUsageDuration(100, 1_000, transitions))
    }

    @Test
    fun overlappingActivitiesAreCountedOnlyOnce() {
        val transitions = listOf(
            foreground(at = 200, activity = "first"),
            foreground(at = 300, activity = "second"),
            background(at = 500, activity = "first"),
            background(at = 700, activity = "second"),
        )

        assertEquals(500L, calculateUsageDuration(100, 1_000, transitions))
    }

    @Test
    fun screenOffClearsStaleForegroundActivities() {
        val transitions = listOf(
            foreground(at = 200, activity = "app"),
            UsageTransition(400, UsageTransitionType.RESET),
        )

        assertEquals(200L, calculateUsageDuration(100, 1_000, transitions))
    }

    @Test
    fun invalidWindowHasNoUsage() {
        assertEquals(0L, calculateUsageDuration(100, 100, listOf(foreground(50, "app"))))
    }

    private fun foreground(at: Long, activity: String) =
        UsageTransition(at, UsageTransitionType.FOREGROUND, activity)

    private fun background(at: Long, activity: String) =
        UsageTransition(at, UsageTransitionType.BACKGROUND, activity)
}
