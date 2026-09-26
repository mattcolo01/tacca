package it.matteocolombo.tacca.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.ui.theme.TaccaTheme

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, radius: Dp = 22.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(radius)
    Column(modifier.clip(shape).background(c.surface).border(1.dp, c.outline, shape), content = content)
}

/** Rows separated by 1dp hairlines inside one rounded container (mockup home groups). Rows must set their own surface background. */
@Composable
fun GroupedList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.clip(shape).background(c.outline).border(1.dp, c.outline, shape),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        content = content,
    )
}

@Composable
fun TintedIconBox(
    @DrawableRes icon: Int,
    background: Color,
    modifier: Modifier = Modifier,
    boxSize: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    radius: Dp = 14.dp,
) {
    Box(modifier.size(boxSize).clip(RoundedCornerShape(radius)).background(background), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = TaccaTheme.colors.text, modifier = Modifier.size(iconSize))
    }
}
