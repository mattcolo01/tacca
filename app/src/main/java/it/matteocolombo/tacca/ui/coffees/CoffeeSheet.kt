package it.matteocolombo.tacca.ui.coffees

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.ui.components.DangerTextButton
import it.matteocolombo.tacca.ui.components.PrimaryButton
import it.matteocolombo.tacca.ui.components.TaccaBottomSheet
import it.matteocolombo.tacca.ui.components.TaccaTextField

/** Bottom sheet to create or rename a coffee. Also used inline by the calibration editor. */
@Composable
fun CoffeeSheet(
    title: String,
    initialName: String,
    existingNames: Set<String>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    saving: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val normalized = TextMatch.normalize(name)
    val duplicate = normalized.isNotEmpty() && normalized != TextMatch.normalize(initialName) && normalized in existingNames
    TaccaBottomSheet(onDismiss = onDismiss, title = title) {
        TaccaTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.coffee_name_label),
            placeholder = stringResource(R.string.coffee_name_hint),
            fieldModifier = Modifier.focusRequester(focus),
            onImeAction = { if (name.isNotBlank() && !saving) onSave(name) },
            supportingText = if (duplicate) stringResource(R.string.coffee_duplicate_hint) else null,
        )
        PrimaryButton(stringResource(R.string.action_save), { onSave(name) }, enabled = name.isNotBlank() && !saving)
        if (onDelete != null) DangerTextButton(stringResource(R.string.coffee_delete), onDelete)
    }
}
