package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SoundToggle(
    enabled: Boolean,
    description: String,
    accent: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = if (enabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
        contentDescription = null,
        tint = if (enabled) accent else Color(0xFF6D6962),
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(48.dp)
                .semantics { contentDescription = description }
                .toggleable(
                    value = enabled,
                    role = Role.Switch,
                    onValueChange = { onToggle() },
                ),
    )
}
