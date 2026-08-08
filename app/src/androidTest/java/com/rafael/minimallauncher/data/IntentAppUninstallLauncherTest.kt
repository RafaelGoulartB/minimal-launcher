package com.rafael.minimallauncher.data

import android.content.ComponentName
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class IntentAppUninstallLauncherTest {
    @Test
    fun uninstallIntentTargetsPackageAndCanStartOutsideActivity() {
        val intent = buildUninstallIntent(
            LauncherApp(
                label = "Calendar",
                componentName = ComponentName("com.example.calendar", "MainActivity"),
            ),
        )

        assertEquals(Intent.ACTION_UNINSTALL_PACKAGE, intent.action)
        assertEquals("package:com.example.calendar", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
