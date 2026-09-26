package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.ui.navigation.TopLevelDestination
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun TaccaBottomBar(selected: TopLevelDestination?, onSelect: (TopLevelDestination) -> Unit, modifier: Modifier = Modifier) {
    val c = TaccaTheme.colors
    Column(modifier.fillMaxWidth().background(c.surface).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = c.outline)
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 12.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TopLevelDestination.entries.forEach { dest ->
                val isSelected = dest == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(66.dp)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(dest) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    Box(
                        Modifier.size(width = 52.dp, height = 30.dp).clip(CircleShape).background(if (isSelected) c.highlight else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(dest.icon), contentDescription = null, tint = if (isSelected) c.text else c.textMuted, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        stringResource(dest.label),
                        style = TaccaType.nav.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold),
                        color = if (isSelected) c.text else c.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
