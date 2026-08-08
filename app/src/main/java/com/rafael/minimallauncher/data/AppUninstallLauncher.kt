package com.rafael.minimallauncher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

sealed interface UninstallResult {
    data object Started : UninstallResult
    data object Unavailable : UninstallResult
}

interface AppUninstallLauncher {
    fun requestUninstall(app: LauncherApp): UninstallResult
}

@Suppress("DEPRECATION")
internal fun buildUninstallIntent(app: LauncherApp): Intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
    .setData(Uri.fromParts("package", app.componentName.packageName, null))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

class IntentAppUninstallLauncher(private val context: Context) : AppUninstallLauncher {
    override fun requestUninstall(app: LauncherApp): UninstallResult {
        return try {
            val intent = buildUninstallIntent(app)
            if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
                UninstallResult.Unavailable
            } else {
                context.startActivity(intent)
                UninstallResult.Started
            }
        } catch (_: ActivityNotFoundException) {
            UninstallResult.Unavailable
        } catch (_: SecurityException) {
            UninstallResult.Unavailable
        } catch (_: IllegalArgumentException) {
            UninstallResult.Unavailable
        }
    }
}
