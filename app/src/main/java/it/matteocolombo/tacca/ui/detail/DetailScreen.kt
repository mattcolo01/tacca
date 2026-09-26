package it.matteocolombo.tacca.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.ui.components.Dial
import it.matteocolombo.tacca.ui.components.EmptyState
import it.matteocolombo.tacca.ui.components.GroupedList
import it.matteocolombo.tacca.ui.components.Legend
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.components.TintedIconBox
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.format.settingText
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun DetailEntry(onBack: () -> Unit, onEdit: (Long) -> Unit, onOpen: (Long) -> Unit) {
    val vm: DetailViewModel = viewModel(factory = DetailViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    DetailScreen(state, onBack, onEdit, onOpen)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(state: DetailUiState, onBack: () -> Unit, onEdit: (Long) -> Unit, onOpen: (Long) -> Unit) {
    val c = TaccaTheme.colors
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val cal = state.calibration
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(R.drawable.ic_back, stringResource(R.string.action_back), onBack)
            if (cal != null) {
                // Visual pill is 44dp tall per the mockup; wrap it so the clickable hit area is >= 48dp.
                Box(
                    Modifier.heightIn(min = 48.dp).clickable { onEdit(cal.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier.height(44.dp).clip(RoundedCornerShape(16.dp)).background(c.surface)
                            .border(1.dp, c.outline, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.action_edit), style = TaccaType.label, color = c.text)
                    }
                }
            }
        }
        if (state.notFound) {
            EmptyState(R.drawable.ic_tab_calibrations, stringResource(R.string.detail_not_found), null)
            return@Column
        }
        if (cal == null) return@Column

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(cal.coffee.name, style = TaccaType.h1Small, color = c.text)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderChip(cal.method.icon.drawableRes(), methodName(cal.method), c.tint(cal.method.tint), bordered = false)
                HeaderChip(R.drawable.ic_grinder, cal.grinder.name, c.surface, bordered = true)
            }
        }

        SurfaceCard(Modifier.fillMaxWidth(), radius = 28.dp) {
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Dial(cal.setting, cal.grinder.spec.stepsPerTurn)
                val tiles = buildList {
                    add(stringResource(R.string.detail_step_zero))
                    if (cal.setting.turns > 0) add(pluralStringResource(R.plurals.detail_step_turns, cal.setting.turns, cal.setting.turns))
                    add(pluralStringResource(R.plurals.detail_step_steps, cal.setting.steps, cal.setting.steps))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tiles.forEachIndexed { i, text ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (i == tiles.lastIndex) c.highlight else c.background).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("${i + 1}", style = TaccaType.numberS.copy(fontSize = 13.sp), color = c.textMuted)
                            Text(text, style = TaccaType.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.text)
                        }
                    }
                }
            }
        }

        if (cal.notes.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Legend(stringResource(R.string.detail_notes))
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Text(cal.notes, style = TaccaType.body, color = c.text, modifier = Modifier.padding(16.dp))
                }
            }
        }

        if (state.sameCoffee.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Legend(stringResource(R.string.detail_same_coffee))
                GroupedList(Modifier.fillMaxWidth()) {
                    state.sameCoffee.forEach { other -> SameCoffeeRow(other) { onOpen(other.id) } }
                }
            }
        }
    }
}

@Composable
private fun HeaderChip(icon: Int, text: String, background: Color, bordered: Boolean) {
    val c = TaccaTheme.colors
    Row(
        Modifier.height(34.dp).clip(CircleShape).background(background)
            .then(if (bordered) Modifier.border(1.dp, c.outline, CircleShape) else Modifier)
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = c.text, modifier = Modifier.size(20.dp))
        Text(text, style = TaccaType.label, color = c.text)
    }
}

@Composable
private fun SameCoffeeRow(cal: Calibration, onClick: () -> Unit) {
    val c = TaccaTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).background(c.surface).clickable(onClick = onClick)
            .padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TintedIconBox(cal.method.icon.drawableRes(), c.tint(cal.method.tint), boxSize = 36.dp, iconSize = 20.dp, radius = 12.dp)
        Row(Modifier.weight(1f)) {
            Text("${methodName(cal.method)} · ", style = TaccaType.label, color = c.text)
            Text(cal.grinder.name, style = TaccaType.label.copy(fontWeight = FontWeight.Medium), color = c.textMuted, maxLines = 1)
        }
        Text(settingText(cal.setting), style = TaccaType.numberS, color = c.text)
    }
}
