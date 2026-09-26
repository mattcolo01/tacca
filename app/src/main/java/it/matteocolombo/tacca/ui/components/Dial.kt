package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.DialGeometry
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.ui.format.settingText
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun Dial(setting: GrindSetting, stepsPerTurn: Int?, modifier: Modifier = Modifier, size: Dp = 260.dp) {
    val c = TaccaTheme.colors
    val description = stringResource(R.string.detail_dial_description, settingText(setting))
    val hiddenTurns = (setting.turns - DialGeometry.MAX_RINGS).coerceAtLeast(0)
    Box(
        modifier.size(size).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val side = this.size.minDimension
            val s = side / 260f
            val model = DialGeometry.build(side, setting, stepsPerTurn)
            val center = Offset(side / 2f, side / 2f)
            model.ringRadii.forEach { r -> drawCircle(c.highlight, radius = r, center = center, style = Stroke(5f * s)) }
            model.ticks.forEach { t ->
                drawLine(
                    color = if (t.active) c.accent else c.tickInactive,
                    start = Offset(t.inner.x, t.inner.y),
                    end = Offset(t.outer.x, t.outer.y),
                    strokeWidth = (if (t.major) 3f else 2f) * s,
                    cap = StrokeCap.Round,
                )
            }
            model.pointer?.let { p ->
                val o = Offset(p.x, p.y)
                drawCircle(c.accent, radius = 8f * s, center = o)
                drawCircle(c.surface, radius = 8f * s, center = o, style = Stroke(3f * s))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (hiddenTurns > 0) Text(stringResource(R.string.detail_more_rings, hiddenTurns), style = TaccaType.bodySmall, color = c.textMuted)
            if (setting.turns > 0) {
                Text(pluralStringResource(R.plurals.detail_turns_plus, setting.turns, setting.turns), style = TaccaType.label.copy(fontSize = 15.sp), color = c.textMuted)
            }
            Text(setting.steps.toString(), style = TaccaType.numberXL, color = c.accentText)
            Text(stringResource(R.string.unit_steps).uppercase(), style = TaccaType.unit.copy(fontSize = 12.sp), color = c.textMuted)
        }
    }
}
