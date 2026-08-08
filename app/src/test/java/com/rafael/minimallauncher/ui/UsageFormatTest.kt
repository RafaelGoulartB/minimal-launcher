package com.rafael.minimallauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageFormatTest {
    @Test
    fun formatsMinutesAndHours() {
        assertEquals("0m", formatUsageDuration(0))
        assertEquals("42m", formatUsageDuration(42 * 60_000L))
        assertEquals("3h 23m", formatUsageDuration((3 * 60 + 23) * 60_000L))
    }

    @Test
    fun negativeDurationIsClamped() {
        assertEquals("0m", formatUsageDuration(-1))
    }
}
