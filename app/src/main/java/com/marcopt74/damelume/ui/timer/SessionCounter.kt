package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marcopt74.damelume.ui.theme.LumePalette
import com.marcopt74.damelume.ui.theme.LumeType

@Composable
fun SessionCounter(
    count: Int,
    countLabel: String,
    countDescription: String,
    resetLabel: String,
    resetEnabled: Boolean,
    accent: Color,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dots = TimerUiState.dotsFor(count)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.semantics { contentDescription = countDescription },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = count.toString(),
                style = LumeType.sessionCount,
                color = Color(LumePalette.FG),
            )
            Text(
                text = countLabel,
                style = LumeType.sessionWord,
                color = Color(LumePalette.MUTED),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { index ->
                val filled = index < dots
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) accent else Color(LumePalette.FG).copy(alpha = 0.15f),
                            ),
                )
            }
        }
        Text(
            text = resetLabel,
            style = LumeType.sessionWord,
            color = Color(LumePalette.MUTED),
            modifier =
                Modifier
                    .alpha(if (resetEnabled) 1f else 0.4f)
                    .clickable(enabled = resetEnabled, onClick = onReset),
        )
    }
}
