package com.marcopt74.damelume.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcopt74.damelume.R
import com.marcopt74.damelume.domain.model.Mode

val EaseSmoothOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)

val LocalReducedMotion = staticCompositionLocalOf { false }

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

object LumeType {
    val display =
        TextStyle(
            fontFamily = Fraunces,
            fontWeight = FontWeight.Medium,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.75).sp,
        )
    val tagline =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    val body =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    val status =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.2.sp,
        )
    val sessionCount =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 18.sp,
        )
    val sessionWord =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    val stepperLabel =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    val stepperValue =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontFeatureSettings = "tnum, lnum",
        )
    val stepperUnit =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    val button =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
    val buttonSmall =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    val chip =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )

    fun digits(sizeSp: Float): TextStyle =
        TextStyle(
            fontFamily = Outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = sizeSp.sp,
            lineHeight = sizeSp.sp,
            letterSpacing = (sizeSp * -0.025f).sp,
            fontFeatureSettings = "tnum, lnum",
        )
}

fun timerDigitSizeSp(screenWidthDp: Int): Float {
    val raw = screenWidthDp * 0.07f + 16f
    return raw.coerceIn(52f, 84f)
}

@Immutable
data class ModeColors(
    val accent: Color,
    val onAccent: Color,
    val soft: Color,
    val runningBackground: Color,
)

val LocalModeColors =
    staticCompositionLocalOf {
        modeColors(Mode.WORK)
    }

fun modeColors(mode: Mode): ModeColors =
    ModeColors(
        accent = Color(LumePalette.accent(mode)),
        onAccent = Color.White,
        soft = Color(LumePalette.soft(mode)),
        runningBackground = Color(LumePalette.runningBackground(mode)),
    )

fun Modifier.lumeShadowBorder(shape: Shape): Modifier =
    this
        .shadow(
            elevation = 2.dp,
            shape = shape,
            clip = false,
            ambientColor = Color(LumePalette.SHADOW),
            spotColor = Color(LumePalette.SHADOW),
        ).clip(shape)
        .border(width = 1.dp, color = Color(LumePalette.RING), shape = shape)

private val LightColors =
    lightColorScheme(
        primary = Color(LumePalette.WORK_ACCENT),
        onPrimary = Color.White,
        background = Color(LumePalette.BG),
        onBackground = Color(LumePalette.FG),
        surface = Color(LumePalette.BG),
        onSurface = Color(LumePalette.FG),
        surfaceVariant = Color(LumePalette.BG),
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
    CompositionLocalProvider(
        LocalModeColors provides colors,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography =
                MaterialTheme.typography.copy(
                    bodyLarge = LumeType.body.copy(fontSize = 16.sp, lineHeight = 24.sp),
                    bodyMedium = LumeType.body,
                    labelLarge = LumeType.buttonSmall,
                    headlineMedium = LumeType.display,
                ),
            content = content,
        )
    }
}

internal val PillShape = RoundedCornerShape(50)
internal val CardShape = RoundedCornerShape(16.dp)
internal val StepperShape = RoundedCornerShape(12.dp)
