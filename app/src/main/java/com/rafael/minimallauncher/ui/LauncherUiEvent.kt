package com.rafael.minimallauncher.ui

import androidx.annotation.StringRes

data class LauncherSnackbarAction(
    val token: Long,
)

sealed interface LauncherUiEvent {
    data class Snackbar(
        @StringRes val messageRes: Int,
        val action: LauncherSnackbarAction? = null,
        val isError: Boolean = false,
    ) : LauncherUiEvent
}
