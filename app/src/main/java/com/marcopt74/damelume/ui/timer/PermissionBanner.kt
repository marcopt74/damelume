package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marcopt74.damelume.ui.theme.CardShape
import com.marcopt74.damelume.ui.theme.LumePalette
import com.marcopt74.damelume.ui.theme.LumeType
import com.marcopt74.damelume.ui.theme.lumeShadowBorder

@Composable
fun PermissionBanner(
    message: String,
    actionLabel: String,
    dismissLabel: String,
    onAction: () -> Unit,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .lumeShadowBorder(CardShape)
                .padding(16.dp),
    ) {
        Text(text = message, color = Color(LumePalette.FG), style = LumeType.body)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LumeButton(onClick = onAction, kind = LumeButtonKind.Ghost) {
                Text(text = actionLabel, style = LumeType.buttonSmall)
            }
            if (onDismiss != null) {
                LumeButton(onClick = onDismiss, kind = LumeButtonKind.Ghost) {
                    Text(text = dismissLabel, style = LumeType.buttonSmall)
                }
            }
        }
    }
}
