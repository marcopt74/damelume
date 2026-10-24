@file:Suppress("MatchingDeclarationName")

package com.marcopt74.damelume.ui.timer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marcopt74.damelume.ui.theme.EaseOut
import com.marcopt74.damelume.ui.theme.LocalReducedMotion
import com.marcopt74.damelume.ui.theme.PillShape
import com.marcopt74.damelume.ui.theme.StepperShape
import com.marcopt74.damelume.ui.theme.lumeShadowBorder

enum class LumeButtonKind {
    Primary,
    Reset,
    Ghost,
    Stepper,
}

@Composable
fun LumeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: LumeButtonKind = LumeButtonKind.Ghost,
    enabled: Boolean = true,
    accent: Color = Color(0xFF2F6DF6),
    description: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduced) 0.96f else 1f,
        animationSpec = if (reduced) snap() else tween(durationMillis = 150, easing = EaseOut),
        label = "pressScale",
    )
    val shape = if (kind == LumeButtonKind.Stepper) StepperShape else PillShape
    val corner = if (kind == LumeButtonKind.Stepper) 12.dp else 28.dp
    val background = if (kind == LumeButtonKind.Primary) accent else Color.Transparent
    val contentColor =
        when (kind) {
            LumeButtonKind.Primary -> Color.White
            LumeButtonKind.Reset -> Color(0xFF1C1B19)
            LumeButtonKind.Ghost,
            LumeButtonKind.Stepper,
            -> Color(0xFF6D6962)
        }
    Row(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (enabled) 1f else 0.4f
                }.then(kindChrome(kind, shape))
                .focusRing(focused, accent, corner)
                .background(background, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics {
                    if (description != null) contentDescription = description
                }.then(kindPadding(kind)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun LumeSwapIcon(
    visible: Boolean,
    painter: Painter,
    modifier: Modifier = Modifier,
    nudge: Dp = 0.dp,
) {
    val reduced = LocalReducedMotion.current
    val spec = if (reduced) snap<Float>() else tween(durationMillis = 200, easing = EaseOut)
    val dpSpec = if (reduced) snap<Dp>() else tween(durationMillis = 200, easing = EaseOut)
    val scale by animateFloatAsState(if (visible) 1f else 0.25f, spec, label = "swapScale")
    val alpha by animateFloatAsState(if (visible) 1f else 0f, spec, label = "swapAlpha")
    val blur by animateDpAsState(if (visible) 0.dp else 8.dp, dpSpec, label = "swapBlur")
    Icon(
        painter = painter,
        contentDescription = null,
        modifier =
            modifier
                .offset(x = if (visible) nudge else 0.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }.then(if (blur > 0.5.dp) Modifier.blur(blur) else Modifier),
    )
}

private fun kindChrome(
    kind: LumeButtonKind,
    shape: androidx.compose.ui.graphics.Shape,
): Modifier =
    when (kind) {
        LumeButtonKind.Reset -> Modifier.size(56.dp).lumeShadowBorder(shape)
        LumeButtonKind.Stepper -> Modifier.size(36.dp)
        LumeButtonKind.Ghost -> Modifier.heightIn(min = 36.dp)
        LumeButtonKind.Primary -> Modifier
    }

private fun kindPadding(kind: LumeButtonKind): Modifier =
    when (kind) {
        LumeButtonKind.Primary -> Modifier.padding(horizontal = 32.dp)
        LumeButtonKind.Ghost -> Modifier.padding(horizontal = 12.dp)
        LumeButtonKind.Reset,
        LumeButtonKind.Stepper,
        -> Modifier
    }

private fun Modifier.focusRing(
    focused: Boolean,
    accent: Color,
    corner: Dp,
): Modifier =
    this.drawBehind {
        if (!focused) return@drawBehind
        val grow = 3.dp.toPx()
        drawRoundRect(
            color = accent.copy(alpha = 0.35f),
            topLeft = Offset(-grow, -grow),
            size = Size(size.width + grow * 2, size.height + grow * 2),
            cornerRadius = CornerRadius(corner.toPx() + grow),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
