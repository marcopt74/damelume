package com.marcopt74.damelume.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import com.marcopt74.damelume.domain.model.AppLocale
import com.marcopt74.damelume.ui.theme.LumePalette
import com.marcopt74.damelume.ui.theme.LumeType
import com.marcopt74.damelume.ui.theme.PillShape
import com.marcopt74.damelume.ui.theme.lumeShadowBorder

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
                .lumeShadowBorder(PillShape)
                .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
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
                .height(28.dp)
                .clip(PillShape)
                .background(if (selected) accent else Color.Transparent)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(LumePalette.MUTED),
            style = LumeType.chip,
        )
    }
}
