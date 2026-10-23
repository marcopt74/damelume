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

private val Fraunces =
    FontFamily(
        Font(R.font.fraunces_medium, FontWeight.Medium),
        Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    )

private val Outfit =
    FontFamily(
        Font(R.font.outfit_regular, FontWeight.Normal),
        Font(R.font.outfit_medium, FontWeight.Medium),
        Font(R.font.outfit_semibold, FontWeight.SemiBold),
    )

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
                    bodyLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 16.sp),
                    bodyMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 14.sp),
                    labelLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 14.sp),
                    headlineMedium =
                        TextStyle(
                            fontFamily = Fraunces,
                            fontWeight = FontWeight.Medium,
                            fontSize = 30.sp,
                            letterSpacing = (-0.4).sp,
                        ),
                ),
            content = content,
        )
    }
}
