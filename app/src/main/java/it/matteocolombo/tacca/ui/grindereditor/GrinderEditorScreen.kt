package it.matteocolombo.tacca.ui.grindereditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.DangerTextButton
import it.matteocolombo.tacca.ui.components.PrimaryButton
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.Stepper
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.components.TaccaTextField
import it.matteocolombo.tacca.ui.components.cascadeDeleteText
import it.matteocolombo.tacca.ui.format.settingText
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun GrinderEditorEntry(onClose: () -> Unit, onSaved: (Long) -> Unit, onDeleted: () -> Unit) {
    val vm: GrinderEditorViewModel = viewModel(factory = GrinderEditorViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    GrinderEditorScreen(
        state = state,
        onClose = onClose,
        onNameChange = vm::onNameChange,
        onMultiTurnChange = vm::onMultiTurnChange,
        onStepsDown = vm::decrementSteps,
        onStepsUp = vm::incrementSteps,
        onStepsUnknown = vm::setStepsUnknown,
        onSave = vm::save,
        onDelete = vm::delete,
    )
}

@Composable
fun GrinderEditorScreen(
    state: GrinderEditorUiState,
    onClose: () -> Unit,
    onNameChange: (String) -> Unit,
    onMultiTurnChange: (Boolean) -> Unit,
    onStepsDown: () -> Unit,
    onStepsUp: () -> Unit,
    onStepsUnknown: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = TaccaTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader(
                title = stringResource(if (state.isNew) R.string.grinder_editor_title_new else R.string.grinder_editor_title_edit),
                titleStyle = TaccaType.h1Small,
            ) { SquareIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onClose) }

            TaccaTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.grinder_name_label),
                placeholder = stringResource(R.string.grinder_name_hint),
                supportingText = if (state.isDuplicateName) stringResource(R.string.grinder_duplicate_hint) else null,
            )

            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val multiTitle = stringResource(R.string.grinder_multi_title)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(multiTitle, style = TaccaType.title, color = c.text)
                            Text(stringResource(R.string.grinder_multi_body), style = TaccaType.bodySmall, color = c.textMuted)
                        }
                        Switch(
                            checked = state.multiTurn,
                            onCheckedChange = onMultiTurnChange,
                            modifier = Modifier.semantics { contentDescription = multiTitle },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = c.accent, checkedThumbColor = Color.White,
                                uncheckedTrackColor = c.dashed, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent,
                            ),
                        )
                    }
                    HorizontalDivider(thickness = 1.dp, color = c.outline)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                stringResource(if (state.multiTurn) R.string.grinder_steps_per_turn else R.string.grinder_steps_total),
                                style = TaccaType.title, color = c.text,
                            )
                            Text(stringResource(R.string.grinder_steps_optional), style = TaccaType.bodySmall, color = c.textMuted)
                            if (state.stepsPerTurn != null) {
                                TextButton(onClick = onStepsUnknown, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                    Text(stringResource(R.string.grinder_steps_unknown_action), style = TaccaType.label, color = c.accentText)
                                }
                            }
                        }
                        Stepper(
                            value = state.stepsPerTurn?.toString() ?: stringResource(R.string.grinder_steps_not_set),
                            onDecrement = onStepsDown,
                            onIncrement = onStepsUp,
                            decrementLabel = stringResource(R.string.editor_fewer_steps),
                            incrementLabel = stringResource(R.string.editor_more_steps),
                            canDecrement = state.stepsPerTurn != null,
                            valueStyle = if (state.stepsPerTurn != null) TaccaType.numberM.copy(fontSize = 26.sp) else TaccaType.label,
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
            }

            if (state.outOfRangeCount > 0) {
                Text(
                    pluralStringResource(R.plurals.grinder_out_of_range, state.outOfRangeCount, state.outOfRangeCount),
                    style = TaccaType.label, color = c.danger,
                )
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.highlight).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(c.surface), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_tab_calibrations), contentDescription = null, tint = c.accentText, modifier = Modifier.size(22.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.grinder_preview_label).uppercase(), style = TaccaType.legend, color = c.textOnTint)
                    Text(settingText(state.preview), style = TaccaType.cardTitle, color = c.text)
                }
            }

            if (!state.isNew) DangerTextButton(stringResource(R.string.grinder_delete), { confirmDelete = true })
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 8.dp).navigationBarsPadding().imePadding()) {
            PrimaryButton(stringResource(R.string.action_save), onSave, enabled = state.canSave)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title, state.name),
            text = cascadeDeleteText(state.calibrationCount),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
