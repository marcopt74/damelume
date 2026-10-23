package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcopt74.damelume.R

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
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelColor = if (active) accent else Color(0xFF6D6962)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .alpha(if (enabled) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = labelColor,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 16.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrease,
                enabled = enabled && value > min,
                modifier = Modifier.minimumInteractiveComponentSize().size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = decreaseDescription,
                    tint = Color(0xFF6D6962),
                )
            }
            Text(
                text = stringResource(R.string.stepper_value, value),
                modifier = Modifier.size(width = 40.dp, height = 28.dp),
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1C1B19),
            )
            Text(
                text = unit,
                modifier = Modifier.size(width = 32.dp, height = 16.dp),
                fontSize = 12.sp,
                color = Color(0xFF9A958C),
            )
            IconButton(
                onClick = onIncrease,
                enabled = enabled && value < max,
                modifier = Modifier.minimumInteractiveComponentSize().size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = increaseDescription,
                    tint = Color(0xFF6D6962),
                )
            }
        }
    }
}
