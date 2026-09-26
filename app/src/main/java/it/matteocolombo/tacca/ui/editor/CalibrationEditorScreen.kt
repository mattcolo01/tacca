package it.matteocolombo.tacca.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.coffees.CoffeeSheet
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.DangerTextButton
import it.matteocolombo.tacca.ui.components.Legend
import it.matteocolombo.tacca.ui.components.PrimaryButton
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.Stepper
import it.matteocolombo.tacca.ui.components.TaccaTextField
import it.matteocolombo.tacca.ui.format.grinderKindText
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.format.settingText
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

data class EditorActions(
    val onClose: () -> Unit,
    val onSelectCoffee: (Long) -> Unit,
    val onSelectMethod: (Long) -> Unit,
    val onSelectGrinder: (Long) -> Unit,
    val onTurnsUp: () -> Unit,
    val onTurnsDown: () -> Unit,
    val onStepsUp: () -> Unit,
    val onStepsDown: () -> Unit,
    val onNotesChange: (String) -> Unit,
    val onShowAllCoffees: () -> Unit,
    val onAddCoffee: (String) -> Unit,
    val onNewGrinder: () -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
) {
    companion object {
        val Noop = EditorActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
fun CalibrationEditorEntry(onClose: () -> Unit, onNewGrinder: () -> Unit, onSaved: (Long, Boolean) -> Unit, onDeleted: () -> Unit) {
    val vm: CalibrationEditorViewModel = viewModel(factory = CalibrationEditorViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) { state.savedId?.let { onSaved(it, state.isEdit) } }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    CalibrationEditorScreen(
        state,
        EditorActions(
            onClose = onClose,
            onSelectCoffee = vm::selectCoffee,
            onSelectMethod = vm::selectMethod,
            onSelectGrinder = vm::selectGrinder,
            onTurnsUp = vm::turnsUp,
            onTurnsDown = vm::turnsDown,
            onStepsUp = vm::stepsUp,
            onStepsDown = vm::stepsDown,
            onNotesChange = vm::onNotesChange,
            onShowAllCoffees = vm::showAllCoffees,
            onAddCoffee = vm::addCoffee,
            onNewGrinder = onNewGrinder,
            onSave = vm::save,
            onDelete = vm::delete,
        ),
    )
}

@Composable
fun CalibrationEditorScreen(state: EditorUiState, actions: EditorActions) {
    val c = TaccaTheme.colors
    var coffeeSheet by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ScreenHeader(
                title = stringResource(if (state.isEdit) R.string.editor_title_edit else R.string.editor_title_new),
                titleStyle = TaccaType.h1Small,
            ) { SquareIconButton(R.drawable.ic_close, stringResource(R.string.action_close), actions.onClose) }

            Section(stringResource(R.string.editor_section_coffee)) {
                if (state.coffees.isEmpty()) Hint(stringResource(R.string.editor_no_coffee))
                state.visibleCoffees.forEach { coffee ->
                    val selected = coffee.id == state.coffeeId
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(c.surface)
                            .border(1.5.dp, if (selected) c.accent else c.outline, RoundedCornerShape(16.dp))
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onSelectCoffee(coffee.id) })
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(18.dp).clip(CircleShape).border(5.dp, if (selected) c.accent else c.tickInactive, CircleShape).background(c.surface))
                        Text(coffee.name, style = TaccaType.label, color = c.text)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.hiddenCoffeeCount > 0) {
                        TextButton(onClick = actions.onShowAllCoffees) {
                            Text(stringResource(R.string.action_show_all, state.coffees.size), style = TaccaType.label, color = c.accentText)
                        }
                    }
                    AddChip(stringResource(R.string.action_new)) { coffeeSheet = true }
                }
            }

            Section(stringResource(R.string.editor_section_method)) {
                state.methods.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { m ->
                            val selected = m.id == state.methodId
                            Column(
                                Modifier.weight(1f).height(78.dp).clip(RoundedCornerShape(18.dp)).background(c.tint(m.tint))
                                    .border(1.5.dp, if (selected) c.text else Color.Transparent, RoundedCornerShape(18.dp))
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onSelectMethod(m.id) })
                                    .padding(horizontal = 2.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                            ) {
                                Icon(painterResource(m.icon.drawableRes()), contentDescription = null, tint = c.text, modifier = Modifier.size(26.dp))
                                Text(methodName(m), style = TaccaType.unit.copy(fontSize = 11.sp, letterSpacing = 0.sp), color = c.text, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Section(stringResource(R.string.editor_section_grinder)) {
                if (state.grinders.isEmpty()) Hint(stringResource(R.string.editor_no_grinder))
                state.grinders.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { g ->
                            val selected = g.id == state.grinderId
                            Column(
                                Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(c.surface)
                                    .border(1.5.dp, if (selected) c.text else c.outline, RoundedCornerShape(16.dp))
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onSelectGrinder(g.id) })
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                            ) {
                                Text(g.name, style = TaccaType.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = c.text)
                                Text(grinderKindText(g.spec), style = TaccaType.bodySmall.copy(fontSize = 12.sp), color = c.textMuted)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                AddChip(stringResource(R.string.action_new), actions.onNewGrinder)
            }

            Section(stringResource(R.string.editor_section_adjustment)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdjustCard(
                        title = stringResource(R.string.editor_turns),
                        hint = stringResource(R.string.editor_optional),
                        highlighted = false,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.turnsEnabled) {
                            Stepper(
                                value = state.setting.turns.toString(),
                                onDecrement = actions.onTurnsDown, onIncrement = actions.onTurnsUp,
                                decrementLabel = stringResource(R.string.editor_fewer_turns),
                                incrementLabel = stringResource(R.string.editor_more_turns),
                                canDecrement = state.setting.turns > 0,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(stringResource(R.string.editor_single_turn_info), style = TaccaType.bodySmall, color = c.textMuted)
                        }
                    }
                    val max = state.maxSteps
                    AdjustCard(
                        title = stringResource(R.string.editor_steps),
                        hint = when {
                            max == null -> stringResource(R.string.editor_steps_unbounded)
                            state.turnsEnabled -> stringResource(R.string.editor_steps_range_after_turns, 0, max)
                            else -> stringResource(R.string.editor_steps_range, 0, max)
                        },
                        highlighted = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        Stepper(
                            value = state.setting.steps.toString(),
                            onDecrement = actions.onStepsDown, onIncrement = actions.onStepsUp,
                            decrementLabel = stringResource(R.string.editor_fewer_steps),
                            incrementLabel = stringResource(R.string.editor_more_steps),
                            canDecrement = state.setting.steps > 0,
                            canIncrement = state.canIncrementSteps,
                            valueColor = c.accentText,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            TaccaTextField(
                value = state.notes,
                onValueChange = actions.onNotesChange,
                label = stringResource(R.string.editor_section_notes),
                placeholder = stringResource(R.string.editor_notes_hint),
                singleLine = false,
                minLines = 2,
                imeAction = androidx.compose.ui.text.input.ImeAction.Default,
            )

            if (state.isEdit) DangerTextButton(stringResource(R.string.editor_delete), { confirmDelete = true })
        }

        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.replaces?.let {
                Text(
                    stringResource(R.string.editor_replaces, settingText(it)),
                    style = TaccaType.bodySmall, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            PrimaryButton(stringResource(R.string.editor_save_summary, settingText(state.setting)), actions.onSave, enabled = state.canSave)
        }
    }

    if (coffeeSheet) {
        CoffeeSheet(
            title = stringResource(R.string.coffee_sheet_title_new),
            initialName = "",
            existingNames = state.coffeeNames,
            onSave = { actions.onAddCoffee(it); coffeeSheet = false },
            onDismiss = { coffeeSheet = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.editor_delete_confirm),
            text = null,
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; actions.onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Legend(title)
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = TaccaType.bodySmall, color = TaccaTheme.colors.textMuted, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun AddChip(label: String, onClick: () -> Unit) {
    val c = TaccaTheme.colors
    Row(
        Modifier.heightIn(min = 48.dp).clip(CircleShape).background(c.surface).border(1.dp, c.outline, CircleShape)
            .selectable(selected = false, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = c.accentText, modifier = Modifier.size(16.dp))
        Text(label, style = TaccaType.label, color = c.text)
    }
}

@Composable
private fun AdjustCard(title: String, hint: String, highlighted: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.heightIn(min = 112.dp).clip(shape).background(c.surface)
            .border(if (highlighted) 1.5.dp else 1.dp, if (highlighted) c.accent else c.outline, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(title, style = TaccaType.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = c.text)
            Text(hint, style = TaccaType.unit.copy(fontSize = 11.sp, letterSpacing = 0.sp), color = c.textMuted)
        }
        content()
    }
}
