package com.rafael.minimallauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import com.rafael.minimallauncher.data.LauncherAccent
import com.rafael.minimallauncher.data.LauncherFont
import com.rafael.minimallauncher.data.LauncherSettings
import com.rafael.minimallauncher.data.LauncherTextSize

data class LauncherAppearance(
    val fontFamily: FontFamily,
    val textScale: Float,
    val accentColor: Color,
    val onAccentColor: Color,
)

val LocalLauncherAppearance = staticCompositionLocalOf {
    LauncherAppearance(
        fontFamily = FontFamily.Default,
        textScale = 1f,
        accentColor = Color.White,
        onAccentColor = Color.Black,
    )
}

fun launcherAppearance(settings: LauncherSettings): LauncherAppearance {
    val fontFamily = when (settings.font) {
        LauncherFont.SYSTEM -> FontFamily.Default
        LauncherFont.SERIF -> FontFamily.Serif
        LauncherFont.MONOSPACE -> FontFamily.Monospace
    }
    val textScale = when (settings.textSize) {
        LauncherTextSize.SMALL -> 0.9f
        LauncherTextSize.MEDIUM -> 1f
        LauncherTextSize.LARGE -> 1.15f
    }
    val accentColor = when (settings.accent) {
        LauncherAccent.MONOCHROME -> Color.White
        LauncherAccent.BLUE -> Color(0xFF90CAF9)
        LauncherAccent.TEAL -> Color(0xFF80CBC4)
        LauncherAccent.AMBER -> Color(0xFFFFCC80)
        LauncherAccent.VIOLET -> Color(0xFFCE93D8)
    }
    return LauncherAppearance(
        fontFamily = fontFamily,
        textScale = textScale,
        accentColor = accentColor,
        onAccentColor = Color.Black,
    )
}

fun TextUnit.scaledBy(appearance: LauncherAppearance): TextUnit = this * appearance.textScale

internal fun Typography.scaledForLauncher(appearance: LauncherAppearance): Typography {
    fun TextStyle.adjust() = copy(
        fontFamily = appearance.fontFamily,
        fontSize = fontSize.scaleIfSpecified(appearance.textScale),
        lineHeight = lineHeight.scaleIfSpecified(appearance.textScale),
    )

    return copy(
        displayLarge = displayLarge.adjust(),
        displayMedium = displayMedium.adjust(),
        displaySmall = displaySmall.adjust(),
        headlineLarge = headlineLarge.adjust(),
        headlineMedium = headlineMedium.adjust(),
        headlineSmall = headlineSmall.adjust(),
        titleLarge = titleLarge.adjust(),
        titleMedium = titleMedium.adjust(),
        titleSmall = titleSmall.adjust(),
        bodyLarge = bodyLarge.adjust(),
        bodyMedium = bodyMedium.adjust(),
        bodySmall = bodySmall.adjust(),
        labelLarge = labelLarge.adjust(),
        labelMedium = labelMedium.adjust(),
        labelSmall = labelSmall.adjust(),
    )
}

private fun TextUnit.scaleIfSpecified(scale: Float): TextUnit = if (this == TextUnit.Unspecified) this else this * scale

@Composable
fun launcherSp(base: TextUnit): TextUnit = base.scaledBy(LocalLauncherAppearance.current)
