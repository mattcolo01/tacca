package it.matteocolombo.tacca.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = TaccaTheme.colors
    Column(
        modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(c.highlight), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = c.accentText, modifier = Modifier.size(30.dp))
        }
        Text(title, style = TaccaType.title.copy(fontSize = 17.sp), color = c.text, textAlign = TextAlign.Center)
        body?.let { Text(it, style = TaccaType.body.copy(fontSize = 14.sp), color = c.textMuted, textAlign = TextAlign.Center) }
        if (actionLabel != null && onAction != null) {
            PrimaryButton(actionLabel, onAction, Modifier.widthIn(max = 280.dp))
        }
    }
}
