package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marcopt74.damelume.R
import com.marcopt74.damelume.ui.theme.LumePalette
import com.marcopt74.damelume.ui.theme.LumeType

@Composable
fun DurationStepper(
    label: String,
    value: Int,
    unit: String,
    min: Int,
    max: Int,
    enabled: Boolean,
    active: Boolean,
    accent: Color,
    decreaseDescription: String,
    increaseDescription: String,
    valueDescription: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.4f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = if (active) accent else Color(LumePalette.MUTED),
            style = LumeType.stepperLabel,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            LumeButton(
                onClick = onDecrease,
                kind = LumeButtonKind.Stepper,
                enabled = enabled && value > min,
                accent = accent,
                description = decreaseDescription,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lume_minus),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            MinuteField(
                value = value,
                enabled = enabled,
                accent = accent,
                description = valueDescription,
                onCommit = onValueChange,
            )
            LumeButton(
                onClick = onIncrease,
                kind = LumeButtonKind.Stepper,
                enabled = enabled && value < max,
                accent = accent,
                description = increaseDescription,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lume_plus),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = unit,
                style = LumeType.stepperUnit,
                color = Color(LumePalette.FAINT),
                modifier = Modifier.width(32.dp),
            )
        }
    }
}

@Composable
private fun MinuteField(
    value: Int,
    enabled: Boolean,
    accent: Color,
    description: String,
    onCommit: (Int) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(TextFieldValue(value.toString())) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var acceptBlur by remember { mutableStateOf(false) }

    fun close(commit: Boolean) {
        if (!editing) return
        val parsed = draft.text.toIntOrNull()
        editing = false
        acceptBlur = false
        keyboard?.hide()
        if (commit) applyTypedMinutes(parsed, value, onCommit)
    }

    LaunchedEffect(value, enabled) {
        if (!editing) draft = TextFieldValue(value.toString())
        if (!enabled) close(commit = false)
    }
    if (!editing) {
        Box(
            modifier =
                Modifier
                    .width(40.dp)
                    .height(36.dp)
                    .semantics { contentDescription = description }
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            val text = value.toString()
                            draft = TextFieldValue(text, selection = TextRange(0, text.length))
                            editing = true
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.toString(),
                style = LumeType.stepperValue,
                color = Color(LumePalette.FG),
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BasicTextField(
        value = draft,
        onValueChange = { next -> draft = next.digitsOnly(maxDigits = 3) },
        modifier =
            Modifier
                .width(40.dp)
                .height(36.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        acceptBlur = true
                    } else if (acceptBlur) {
                        close(commit = true)
                    }
                }.drawBehind {
                    val y = size.height - 1.dp.toPx()
                    drawLine(
                        color = accent,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }.semantics { contentDescription = description },
        textStyle = LumeType.stepperValue.copy(color = Color(LumePalette.FG), textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { close(commit = true) }),
        singleLine = true,
        cursorBrush = SolidColor(accent),
    )
}

private fun applyTypedMinutes(
    minutes: Int?,
    current: Int,
    onCommit: (Int) -> Unit,
) {
    if (minutes == null || minutes <= 0 || minutes == current) return
    onCommit(minutes)
}

private fun TextFieldValue.digitsOnly(maxDigits: Int): TextFieldValue {
    val digits = text.filter(Char::isDigit).take(maxDigits)
    val normalized = digits.trimStart('0').ifEmpty { if (digits.isEmpty()) "" else "0" }
    return TextFieldValue(normalized, selection = TextRange(normalized.length))
}
