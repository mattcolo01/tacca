package it.matteocolombo.tacca.ui.coffees

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.EmptyState
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SearchField
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.components.TintedIconBox
import it.matteocolombo.tacca.ui.components.cascadeDeleteText
import it.matteocolombo.tacca.ui.components.topLevelContentPadding
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun CoffeesEntry(onShowOnHome: () -> Unit, modifier: Modifier = Modifier) {
    val vm: CoffeesViewModel = viewModel(factory = CoffeesViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    CoffeesScreen(
        state = state,
        onQueryChange = vm::onQueryChange,
        onAdd = vm::openNew,
        onCoffeeClick = { vm.showOnHome(it.id); onShowOnHome() },
        onCoffeeEdit = vm::openEdit,
        onSave = vm::save,
        onDelete = vm::delete,
        onDismissSheet = vm::dismissSheet,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoffeesScreen(
    state: CoffeesUiState,
    onQueryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCoffeeClick: (Coffee) -> Unit,
    onCoffeeEdit: (Coffee) -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = TaccaTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier.fillMaxSize().background(c.background),
        contentPadding = topLevelContentPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.coffees_title),
                subtitle = pluralStringResource(R.plurals.coffees_count, state.totalCount, state.totalCount),
            ) { SquareIconButton(R.drawable.ic_add, stringResource(R.string.coffees_add), onAdd, filled = true) }
        }
        if (state.totalCount > 0) {
            item {
                SearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    placeholder = stringResource(R.string.coffees_search_hint),
                    label = stringResource(R.string.coffees_search_label),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
        }
        when {
            !state.loading && state.totalCount == 0 -> item {
                EmptyState(R.drawable.ic_bean, stringResource(R.string.coffees_empty), null, actionLabel = stringResource(R.string.coffees_add), onAction = onAdd)
            }
            !state.loading && state.items.isEmpty() -> item {
                Text(stringResource(R.string.coffees_no_results), style = TaccaType.body, color = c.textMuted, modifier = Modifier.padding(8.dp))
            }
        }
        items(state.items, key = { it.coffee.id }) { item ->
            SurfaceCard(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .combinedClickable(onClick = { onCoffeeClick(item.coffee) }, onLongClick = { onCoffeeEdit(item.coffee) }),
            ) {
                Row(
                    Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    TintedIconBox(R.drawable.ic_bean, c.tint(item.tint), boxSize = 48.dp, iconSize = 24.dp, radius = 16.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.coffee.name, style = TaccaType.title, color = c.text)
                        Text(
                            if (item.calibrationCount == 0) stringResource(R.string.calibrations_none)
                            else stringResource(
                                R.string.coffee_subtitle,
                                pluralStringResource(R.plurals.calibrations_count, item.calibrationCount, item.calibrationCount),
                                item.methods.map { methodName(it) }.joinToString(", "),
                            ),
                            style = TaccaType.bodySmall, color = c.textMuted,
                        )
                    }
                    SquareIconButton(R.drawable.ic_more, stringResource(R.string.action_more), { onCoffeeEdit(item.coffee) })
                }
            }
        }
    }

    when (val sheet = state.sheet) {
        CoffeeSheetState.New -> CoffeeSheet(
            title = stringResource(R.string.coffee_sheet_title_new), initialName = "", existingNames = state.existingNames,
            onSave = onSave, onDismiss = onDismissSheet, saving = state.saving,
        )
        is CoffeeSheetState.Edit -> CoffeeSheet(
            title = stringResource(R.string.coffee_sheet_title_edit), initialName = sheet.coffee.name, existingNames = state.existingNames,
            onSave = onSave, onDismiss = onDismissSheet, saving = state.saving, onDelete = { confirmDelete = true },
        )
        null -> Unit
    }
    val editing = state.sheet as? CoffeeSheetState.Edit
    if (confirmDelete && editing != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title, editing.coffee.name),
            text = cascadeDeleteText(editing.calibrationCount),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
