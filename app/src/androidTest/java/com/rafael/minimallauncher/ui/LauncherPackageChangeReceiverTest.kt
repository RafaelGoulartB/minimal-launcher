package com.rafael.minimallauncher.ui

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPackageChangeReceiverTest {
    @Test
    fun packageChangesNotifyLauncher() {
        var notifications = 0
        val receiver = LauncherPackageChangeReceiver { notifications++ }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        receiver.onReceive(context, Intent(Intent.ACTION_PACKAGE_ADDED))
        receiver.onReceive(context, Intent(Intent.ACTION_PACKAGE_REMOVED))
        receiver.onReceive(context, Intent(Intent.ACTION_PACKAGE_REPLACED))
        receiver.onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))

        assertEquals(3, notifications)
    }

    @Test
    fun packageFilterUsesPackageDataScheme() {
        assertTrue(launcherPackageIntentFilter().hasDataScheme("package"))
    }
}
