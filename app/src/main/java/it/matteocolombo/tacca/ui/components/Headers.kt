package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: TextStyle = TaccaType.h1,
    top: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = TaccaTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            top?.invoke()
            Text(title, style = titleStyle, color = c.text, modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = TaccaType.body.copy(fontSize = TaccaType.label.fontSize), color = c.textMuted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun Legend(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = TaccaType.legend, color = TaccaTheme.colors.textMuted, modifier = modifier.padding(start = 4.dp))
}

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    val c = TaccaTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(R.drawable.ic_brand), contentDescription = null, tint = c.accentText, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.brand_name), style = TaccaType.brand, color = c.accentText)
    }
}
