package it.matteocolombo.tacca.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

/** Home filter chip: 38dp pill, dark when selected. */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
) {
    val c = TaccaTheme.colors
    val bg = if (selected) c.text else c.surface
    val fg = if (selected) c.surface else c.text
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(38.dp)
                .clip(CircleShape)
                .background(bg)
                .border(1.dp, if (selected) c.text else c.outline, CircleShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leadingIcon?.let { Icon(painterResource(it), null, tint = fg, modifier = Modifier.size(16.dp)) }
            Text(label, style = TaccaType.label, color = fg, maxLines = 1)
            trailingIcon?.let { Icon(painterResource(it), null, tint = fg, modifier = Modifier.size(14.dp)) }
        }
    }
}

/** Small 30dp informational pill (grinder cards, detail header). */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    background: Color = TaccaTheme.colors.background,
) {
    Row(
        modifier = modifier.height(30.dp).clip(CircleShape).background(background).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let { Icon(painterResource(it), null, tint = TaccaTheme.colors.text, modifier = Modifier.size(14.dp)) }
        Text(text, style = TaccaType.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = TaccaTheme.colors.text, maxLines = 1)
    }
}
