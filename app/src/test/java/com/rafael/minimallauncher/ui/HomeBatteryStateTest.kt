package com.rafael.minimallauncher.ui

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeBatteryStateTest {
    @Test
    fun chargingStatusShowsChargingIndicator() {
        val state = homeBatteryState(
            level = 42,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
        )

        assertEquals(HomeBatteryState(percentage = 42, isCharging = true), state)
    }

    @Test
    fun fullStatusKeepsChargingIndicator() {
        val state = homeBatteryState(
            level = 100,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_FULL,
        )

        assertEquals(HomeBatteryState(percentage = 100, isCharging = true), state)
    }

    @Test
    fun dischargingStatusHidesChargingIndicator() {
        val state = homeBatteryState(
            level = 75,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
        )

        assertEquals(HomeBatteryState(percentage = 75, isCharging = false), state)
    }

    @Test
    fun invalidLevelKeepsPercentageUnavailable() {
        val state = homeBatteryState(
            level = -1,
            scale = -1,
            status = BatteryManager.BATTERY_STATUS_UNKNOWN,
        )

        assertEquals(HomeBatteryState(), state)
    }
}
