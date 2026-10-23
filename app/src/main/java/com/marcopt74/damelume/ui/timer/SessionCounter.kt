package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionCounter(
    count: Int,
    countLabel: String,
    countDescription: String,
    cycleWorkCount: Int,
    cycleDescription: String,
    resetLabel: String,
    resetEnabled: Boolean,
    accent: Color,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = Color(0xFF1C1B19),
            modifier = Modifier.semantics { contentDescription = countDescription },
        )
        Text(
            text = countLabel,
            fontSize = 13.sp,
            color = Color(0xFF6D6962),
        )
        Row(
            modifier =
                Modifier
                    .padding(top = 6.dp)
                    .semantics { contentDescription = cycleDescription },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(4) { index ->
                val filled = index < cycleWorkCount
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .background(
                                color = if (filled) accent else Color(0xFF1C1B19).copy(alpha = 0.15f),
                                shape = CircleShape,
                            ),
                )
            }
        }
        TextButton(
            onClick = onReset,
            enabled = resetEnabled,
            modifier = Modifier.minimumInteractiveComponentSize().alpha(if (resetEnabled) 1f else 0.4f),
        ) {
            Text(text = resetLabel, color = Color(0xFF9A4036))
        }
    }
}
