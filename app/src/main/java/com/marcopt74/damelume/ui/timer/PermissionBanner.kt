package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcopt74.damelume.R

@Composable
fun PermissionBanner(
    message: String,
    actionLabel: String,
    dismissLabel: String,
    onAction: () -> Unit,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF5F4F1),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message, color = Color(0xFF1C1B19), fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text(text = actionLabel)
                }
                if (onDismiss != null) {
                    TextButton(
                        onClick = onDismiss,
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .semantics { contentDescription = dismissLabel },
                    ) {
                        Text(text = stringResource(R.string.dismiss_symbol))
                    }
                }
            }
        }
    }
}
