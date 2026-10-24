package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marcopt74.damelume.R
import com.marcopt74.damelume.ui.theme.LumePalette

@Composable
fun SoundToggle(
    enabled: Boolean,
    description: String,
    accent: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (enabled) accent else Color(LumePalette.FAINT)
    Box(
        modifier =
            modifier
                .size(44.dp)
                .semantics { contentDescription = description }
                .toggleable(
                    value = enabled,
                    role = Role.Switch,
                    onValueChange = { onToggle() },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides tint,
            ) {
                LumeSwapIcon(
                    visible = enabled,
                    painter = painterResource(R.drawable.ic_lume_volume),
                    modifier = Modifier.size(20.dp),
                )
                LumeSwapIcon(
                    visible = !enabled,
                    painter = painterResource(R.drawable.ic_lume_volume_off),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
