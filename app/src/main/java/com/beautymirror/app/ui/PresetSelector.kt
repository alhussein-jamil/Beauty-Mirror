package com.beautymirror.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beautymirror.app.R
import com.beautymirror.app.settings.BeautyPreset
import com.beautymirror.app.ui.theme.BmAccent
import com.beautymirror.app.ui.theme.BmBg
import com.beautymirror.app.ui.theme.BmTextMuted

@Composable
fun PresetSelector(
    selected: BeautyPreset,
    onSelect: (BeautyPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = listOf(
        BeautyPreset.OFF,
        BeautyPreset.NATURAL,
        BeautyPreset.SOFT,
        BeautyPreset.BRIGHT,
        BeautyPreset.STAGE,
        BeautyPreset.GLAM,
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { preset ->
            val active = preset == selected
            val label = stringResource(preset.labelRes())
            val description = stringResource(R.string.preset_content_desc, label)
            Text(
                text = label,
                color = if (active) BmBg else BmTextMuted,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) BmAccent else BmTextMuted.copy(alpha = 0.10f))
                    .semantics {
                        role = Role.Tab
                        this.selected = active
                        contentDescription = description
                    }
                    .clickable { onSelect(preset) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
        if (selected == BeautyPreset.CUSTOM) {
            Text(
                text = stringResource(R.string.preset_custom),
                color = BmBg,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(BmAccent)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}
