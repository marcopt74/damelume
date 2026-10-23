package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcopt74.damelume.domain.model.AppLocale

@Composable
fun LocaleToggle(
    selected: AppLocale?,
    groupDescription: String,
    ptLabel: String,
    enLabel: String,
    accent: Color,
    onSelect: (AppLocale) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .semantics { contentDescription = groupDescription }
                .selectableGroup()
                .clip(RoundedCornerShape(50))
                .border(1.dp, Color(0x141C1B19), RoundedCornerShape(50))
                .padding(2.dp),
    ) {
        LocaleChip(
            label = ptLabel,
            selected = selected == AppLocale.PT,
            accent = accent,
            onClick = { onSelect(AppLocale.PT) },
        )
        LocaleChip(
            label = enLabel,
            selected = selected == AppLocale.EN,
            accent = accent,
            onClick = { onSelect(AppLocale.EN) },
        )
    }
}

@Composable
private fun LocaleChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) accent else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF6D6962),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
