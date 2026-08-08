package com.rafael.minimallauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

internal class LauncherPackageChangeReceiver(
    private val onPackageChanged: () -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REPLACED,
            -> onPackageChanged()
        }
    }
}

internal fun launcherPackageIntentFilter(): IntentFilter = IntentFilter().apply {
    addAction(Intent.ACTION_PACKAGE_ADDED)
    addAction(Intent.ACTION_PACKAGE_REMOVED)
    addAction(Intent.ACTION_PACKAGE_CHANGED)
    addAction(Intent.ACTION_PACKAGE_REPLACED)
    addDataScheme("package")
}
