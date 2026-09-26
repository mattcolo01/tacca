package it.matteocolombo.tacca.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.SettingRules
import it.matteocolombo.tacca.ui.components.BrandMark
import it.matteocolombo.tacca.ui.components.EmptyState
import it.matteocolombo.tacca.ui.components.FilterPill
import it.matteocolombo.tacca.ui.components.GroupedList
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SearchField
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.TintedIconBox
import it.matteocolombo.tacca.ui.components.topLevelContentPadding
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

data class HomeActions(
    val onOpen: (Long) -> Unit,
    val onNew: () -> Unit,
    val onSettings: () -> Unit,
    val onSelectMethod: (Long?) -> Unit,
    val onClearCoffeeFilter: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onCloseSearch: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onClearFilters: () -> Unit,
) {
    companion object {
        val Noop = HomeActions({}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
fun HomeEntry(onOpen: (Long) -> Unit, onNew: (Long?, Long?) -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state,
        HomeActions(
            onOpen = onOpen,
            onNew = { onNew(state.coffeeFilter?.id, state.selectedMethodId) },
            onSettings = onSettings,
            onSelectMethod = vm::selectMethod,
            onClearCoffeeFilter = vm::clearCoffeeFilter,
            onOpenSearch = vm::openSearch,
            onCloseSearch = vm::closeSearch,
            onQueryChange = vm::onQueryChange,
            onClearFilters = vm::clearFilters,
        ),
        modifier,
    )
}

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val c = TaccaTheme.colors
    val base = topLevelContentPadding()
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(state.searchOpen) {
        if (state.searchOpen) runCatching { searchFocus.requestFocus() }
    }
    BackHandler(enabled = state.searchOpen) { actions.onCloseSearch() }
    Box(modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = base.calculateTopPadding(), bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    if (state.searchOpen) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SquareIconButton(R.drawable.ic_back, stringResource(R.string.action_close), actions.onCloseSearch)
                            SearchField(
                                query = state.query,
                                onQueryChange = actions.onQueryChange,
                                placeholder = stringResource(R.string.home_search_hint),
                                label = stringResource(R.string.action_search),
                                modifier = Modifier.weight(1f),
                                focusRequester = searchFocus,
                                onClear = { actions.onQueryChange("") },
                            )
                        }
                    } else {
                        ScreenHeader(title = stringResource(R.string.home_title), top = { BrandMark() }) {
                            SquareIconButton(R.drawable.ic_search, stringResource(R.string.action_search), actions.onOpenSearch)
                            SquareIconButton(R.drawable.ic_settings, stringResource(R.string.action_settings), actions.onSettings)
                        }
                    }
                }
            }
            if (state.hasCalibrations) {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.coffeeFilter?.let { coffee ->
                            item {
                                val removeLabel = stringResource(R.string.home_remove_coffee_filter)
                                FilterPill(
                                    label = coffee.name, selected = true, onClick = actions.onClearCoffeeFilter,
                                    leadingIcon = R.drawable.ic_bean, trailingIcon = R.drawable.ic_close,
                                    modifier = Modifier.widthIn(max = 240.dp).semantics { contentDescription = removeLabel },
                                )
                            }
                        }
                        item {
                            FilterPill(stringResource(R.string.home_filter_all), state.selectedMethodId == null, { actions.onSelectMethod(null) })
                        }
                        items(state.chips, key = { it.id }) { m ->
                            FilterPill(methodName(m), state.selectedMethodId == m.id, { actions.onSelectMethod(m.id) })
                        }
                    }
                }
            }
            when {
                state.loading -> Unit
                !state.hasCalibrations -> item {
                    EmptyState(
                        icon = R.drawable.ic_tab_calibrations,
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.home_empty_body) + if (state.needsSetup) "\n" + stringResource(R.string.home_empty_setup) else "",
                        modifier = Modifier.fillMaxWidth(),
                        actionLabel = stringResource(R.string.home_new_calibration),
                        onAction = actions.onNew,
                    )
                }
                state.groups.isEmpty() -> item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.home_no_results), style = TaccaType.title, color = c.textMuted)
                        TextButton(onClick = actions.onClearFilters) {
                            Text(stringResource(R.string.home_clear_filters), style = TaccaType.label, color = c.accentText)
                        }
                    }
                }
            }
            items(state.groups, key = { it.coffee.id }) { group ->
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(R.drawable.ic_bean), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                        Text(group.coffee.name, style = TaccaType.title, color = c.text)
                    }
                    GroupedList(Modifier.fillMaxWidth()) {
                        group.rows.forEach { cal -> CalibrationRow(cal) { actions.onOpen(cal.id) } }
                    }
                }
            }
        }
        if (state.hasCalibrations) {
            val fabLabel = stringResource(R.string.home_new_calibration)
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp).size(60.dp)
                    .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = c.text.copy(alpha = 0.35f), spotColor = c.text.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(22.dp)).background(c.accent)
                    .clickable(role = Role.Button, onClick = actions.onNew)
                    .semantics { contentDescription = fabLabel },
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = c.onAccent, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
private fun CalibrationRow(cal: Calibration, onClick: () -> Unit) {
    val c = TaccaTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(c.surface).clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TintedIconBox(cal.method.icon.drawableRes(), c.tint(cal.method.tint))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(methodName(cal.method), style = TaccaType.rowTitle, color = c.text)
            Text(cal.grinder.name, style = TaccaType.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (SettingRules.showsTurns(cal.setting, cal.grinder.spec)) {
                ValueColumn(cal.setting.turns.toString(), stringResource(R.string.unit_turns), c.text)
                Box(Modifier.width(1.dp).height(28.dp).background(c.outline))
            }
            ValueColumn(cal.setting.steps.toString(), stringResource(R.string.unit_steps), c.accentText)
        }
    }
}

@Composable
private fun ValueColumn(value: String, unit: String, color: Color) {
    Column(Modifier.widthIn(min = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = TaccaType.numberM, color = color)
        Text(unit.uppercase(), style = TaccaType.unit, color = TaccaTheme.colors.textMuted)
    }
}
