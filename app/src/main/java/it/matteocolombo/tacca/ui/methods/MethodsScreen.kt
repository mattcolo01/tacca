package it.matteocolombo.tacca.ui.methods

import android.content.res.Resources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.DashedButton
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.cascadeDeleteText
import it.matteocolombo.tacca.ui.components.topLevelContentPadding
import it.matteocolombo.tacca.ui.format.builtInMethodDisplayNames
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.format.nameRes
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

/**
 * The names [MethodSheet] should treat as duplicates: [state]'s own custom names plus every
 * built-in method's localized display name (the view model itself has no [Resources] to localize
 * with), *except* the built-in name of the method currently being edited, if any. Without that
 * exclusion, editing a renamed built-in (e.g. a V60 with a custom name "Hario") and typing its
 * own built-in name back ("V60") would be flagged as a duplicate even though saving it actually
 * succeeds -- [customNameFor] treats that as reverting to the built-in default, not a collision.
 */
fun sheetExistingNames(state: MethodsUiState, res: Resources): Set<String> {
    val editingBuiltIn = (state.sheet as? MethodSheetState.Edit)?.method?.builtIn
    val ownBuiltInName = editingBuiltIn?.let { TextMatch.normalize(res.getString(it.nameRes())) }
    return state.existingNames + builtInMethodDisplayNames(res) - setOfNotNull(ownBuiltInName)
}

@Composable
fun MethodsEntry(onShowOnHome: () -> Unit, modifier: Modifier = Modifier) {
    val vm: MethodsViewModel = viewModel(factory = MethodsViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val res = LocalResources.current
    MethodsScreen(
        state = state.copy(existingNames = sheetExistingNames(state, res)),
        onMethodClick = { vm.showOnHome(it.id); onShowOnHome() },
        onMethodEdit = vm::openEdit,
        onAdd = vm::openNew,
        onSave = { name, icon, tint ->
            val builtIn = (state.sheet as? MethodSheetState.Edit)?.method?.builtIn
            vm.save(customNameFor(name, builtIn, builtIn?.let { res.getString(it.nameRes()) }), icon, tint)
        },
        onDelete = vm::delete,
        onDismissSheet = vm::dismissSheet,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MethodsScreen(
    state: MethodsUiState,
    onMethodClick: (Method) -> Unit,
    onMethodEdit: (Method) -> Unit,
    onAdd: () -> Unit,
    onSave: (String, MethodIcon, Tint) -> Unit,
    onDelete: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = TaccaTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().background(c.background),
        contentPadding = topLevelContentPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            ScreenHeader(
                title = stringResource(R.string.methods_title),
                subtitle = stringResource(R.string.methods_subtitle),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(state.items, key = { it.method.id }) { item ->
            val shape = RoundedCornerShape(26.dp)
            Column(
                Modifier.height(150.dp).clip(shape).background(c.tint(item.method.tint))
                    .combinedClickable(onClick = { onMethodClick(item.method) }, onLongClick = { onMethodEdit(item.method) })
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(c.surface), contentAlignment = Alignment.Center) {
                        Icon(painterResource(item.method.icon.drawableRes()), contentDescription = null, tint = c.text, modifier = Modifier.size(32.dp))
                    }
                    SquareIconButton(R.drawable.ic_more, stringResource(R.string.action_more), { onMethodEdit(item.method) })
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(methodName(item.method), style = TaccaType.cardTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (item.calibrationCount == 0) stringResource(R.string.calibrations_none)
                        else pluralStringResource(R.plurals.calibrations_count, item.calibrationCount, item.calibrationCount),
                        style = TaccaType.bodySmall, color = c.textOnTint,
                    )
                }
            }
        }
        item {
            DashedButton(stringResource(R.string.methods_add), onAdd, Modifier.height(150.dp), radius = 26.dp, vertical = true)
        }
    }

    when (val sheet = state.sheet) {
        MethodSheetState.New -> MethodSheet(
            stringResource(R.string.method_sheet_title_new), method = null, existingNames = state.existingNames,
            onSave = onSave, onDismiss = onDismissSheet, saving = state.saving,
        )
        is MethodSheetState.Edit -> MethodSheet(
            stringResource(R.string.method_sheet_title_edit), method = sheet.method, existingNames = state.existingNames,
            onSave = onSave, onDismiss = onDismissSheet, saving = state.saving, onDelete = { confirmDelete = true },
        )
        null -> Unit
    }
    val editing = state.sheet as? MethodSheetState.Edit
    if (confirmDelete && editing != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title, methodName(editing.method)),
            text = cascadeDeleteText(editing.calibrationCount),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
