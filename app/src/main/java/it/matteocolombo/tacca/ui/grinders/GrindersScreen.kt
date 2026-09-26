package it.matteocolombo.tacca.ui.grinders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.components.DashedButton
import it.matteocolombo.tacca.ui.components.InfoChip
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.components.TintedIconBox
import it.matteocolombo.tacca.ui.components.topLevelContentPadding
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun GrindersEntry(onGrinderClick: (Long) -> Unit, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    val vm: GrindersViewModel = viewModel(factory = GrindersViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    GrindersScreen(state, onGrinderClick, onAddClick, modifier)
}

@Composable
fun GrindersScreen(state: GrindersUiState, onGrinderClick: (Long) -> Unit, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = TaccaTheme.colors
    LazyColumn(
        modifier.fillMaxSize().background(c.background),
        contentPadding = topLevelContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.grinders_title),
                subtitle = stringResource(R.string.grinders_subtitle),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(state.items, key = { it.grinder.id }) { item -> GrinderCard(item, onClick = { onGrinderClick(item.grinder.id) }) }
        item { DashedButton(stringResource(R.string.grinders_add), onAddClick, Modifier.fillMaxWidth().height(64.dp)) }
    }
}

@Composable
private fun GrinderCard(item: GrinderItem, onClick: () -> Unit) {
    val c = TaccaTheme.colors
    val spec = item.grinder.spec
    SurfaceCard(Modifier.fillMaxWidth().clickable(onClick = onClick), radius = 24.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TintedIconBox(R.drawable.ic_grinder, c.tint(item.tint), boxSize = 52.dp, iconSize = 28.dp, radius = 18.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.grinder.name, style = TaccaType.cardTitle, color = c.text)
                    Text(
                        if (item.calibrationCount == 0) stringResource(R.string.calibrations_none)
                        else pluralStringResource(R.plurals.calibrations_count, item.calibrationCount, item.calibrationCount),
                        style = TaccaType.bodySmall, color = c.textMuted,
                    )
                }
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val n = spec.stepsPerTurn
                if (spec.multiTurn) {
                    InfoChip(stringResource(R.string.grinder_chip_multi), icon = R.drawable.ic_multi_turn)
                    InfoChip(if (n != null) pluralStringResource(R.plurals.grinder_chip_per_turn, n, n) else stringResource(R.string.grinder_chip_unknown))
                } else {
                    InfoChip(stringResource(R.string.grinder_chip_single))
                    InfoChip(if (n != null) pluralStringResource(R.plurals.steps_count, n, n) else stringResource(R.string.grinder_chip_unknown))
                }
            }
        }
    }
}
