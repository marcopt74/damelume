package com.marcopt74.damelume.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.marcopt74.damelume.R
import com.marcopt74.damelume.domain.model.Mode

val EaseSmoothOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

object LumeFonts {
    val Fraunces =
        FontFamily(
            Font(R.font.fraunces_medium, FontWeight.Medium),
            Font(R.font.fraunces_semibold, FontWeight.SemiBold),
        )

    val Outfit =
        FontFamily(
            Font(R.font.outfit_regular, FontWeight.Normal),
            Font(R.font.outfit_medium, FontWeight.Medium),
            Font(R.font.outfit_semibold, FontWeight.SemiBold),
        )
}

object LumeTypography {
    val title =
        TextStyle(
            fontFamily = LumeFonts.Fraunces,
            fontWeight = FontWeight.Medium,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.75).sp,
        )

    // The reference clock is Outfit (`font-sans`), not Fraunces. Fraunces is the word "Lume".
    val timer =
        TextStyle(
            fontFamily = LumeFonts.Outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 52.sp,
            lineHeight = 52.sp,
            letterSpacing = (-1.3).sp,
            fontFeatureSettings = "tnum, lnum",
        )

    val body =
        TextStyle(
            fontFamily = LumeFonts.Outfit,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )

    val bodySmall =
        TextStyle(
            fontFamily = LumeFonts.Outfit,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )

    val label =
        TextStyle(
            fontFamily = LumeFonts.Outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
}

@Immutable
data class ModeColors(
    val accent: Color,
    val accentText: Color,
    val button: Color,
    val onButton: Color,
    val soft: Color,
    val runningBackground: Color,
)

val LocalModeColors =
    staticCompositionLocalOf {
        modeColors(Mode.WORK)
    }

fun modeColors(mode: Mode): ModeColors =
    ModeColors(
        accent = Color(LumePalette.graphic(mode)),
        accentText = Color(LumePalette.text(mode)),
        button = Color(LumePalette.button(mode)),
        onButton = Color.White,
        soft = Color(LumePalette.soft(mode)),
        runningBackground = Color(LumePalette.runningBackground(mode)),
    )

private val LightColors =
    lightColorScheme(
        primary = Color(LumePalette.WORK_ACCENT),
        onPrimary = Color.White,
        background = Color(LumePalette.BG),
        onBackground = Color(LumePalette.FG),
        surface = Color(LumePalette.SURFACE),
        onSurface = Color(LumePalette.FG),
        surfaceVariant = Color(LumePalette.SURFACE),
        outline = Color(LumePalette.BORDER),
    )

@Composable
fun LumeTheme(
    mode: Mode,
    running: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = modeColors(mode)
    val target = if (running) colors.runningBackground else Color(LumePalette.BG)
    val background =
        animateColorAsState(
            targetValue = target,
            animationSpec =
                tween(
                    durationMillis = if (reducedMotion) 0 else 400,
                    easing = EaseSmoothOut,
                ),
            label = "runningBackground",
        )
    val scheme = LightColors.copy(background = background.value)
    CompositionLocalProvider(LocalModeColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography =
                MaterialTheme.typography.copy(
                    displayLarge = LumeTypography.timer,
                    headlineMedium = LumeTypography.title,
                    titleLarge = LumeTypography.title,
                    bodyLarge = LumeTypography.body,
                    bodyMedium = LumeTypography.bodySmall,
                    bodySmall = LumeTypography.bodySmall,
                    labelLarge = LumeTypography.label,
                    labelMedium = LumeTypography.label,
                    labelSmall = LumeTypography.bodySmall,
                ),
            content = content,
        )
    }
}
