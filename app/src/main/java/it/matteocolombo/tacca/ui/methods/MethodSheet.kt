package it.matteocolombo.tacca.ui.methods

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.ui.components.DangerTextButton
import it.matteocolombo.tacca.ui.components.Legend
import it.matteocolombo.tacca.ui.components.PrimaryButton
import it.matteocolombo.tacca.ui.components.TaccaBottomSheet
import it.matteocolombo.tacca.ui.components.TaccaTextField
import it.matteocolombo.tacca.ui.format.labelRes
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MethodSheet(
    title: String,
    method: Method?,
    existingNames: Set<String>,
    onSave: (name: String, icon: MethodIcon, tint: Tint) -> Unit,
    onDismiss: () -> Unit,
    saving: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    val c = TaccaTheme.colors
    val initialName = method?.let { methodName(it) }.orEmpty()
    var name by rememberSaveable { mutableStateOf(initialName) }
    var icon by rememberSaveable { mutableStateOf(method?.icon ?: MethodIcon.CUP) }
    var tint by rememberSaveable { mutableStateOf(method?.tint ?: Tint.SAND) }
    val normalized = TextMatch.normalize(name)
    val duplicate = normalized.isNotEmpty() && normalized != TextMatch.normalize(initialName) && normalized in existingNames

    TaccaBottomSheet(onDismiss = onDismiss, title = title) {
        TaccaTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.method_name_label),
            placeholder = stringResource(R.string.method_name_hint),
            supportingText = if (duplicate) stringResource(R.string.method_duplicate_hint) else null,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Legend(stringResource(R.string.method_icon_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MethodIcon.entries.forEach { option ->
                    val selected = option == icon
                    val label = stringResource(option.labelRes())
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c.tint(tint))
                            .border(1.5.dp, if (selected) c.text else Color.Transparent, RoundedCornerShape(14.dp))
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { icon = option })
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) { Icon(painterResource(option.drawableRes()), contentDescription = null, tint = c.text, modifier = Modifier.size(26.dp)) }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Legend(stringResource(R.string.method_tint_label))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tint.entries.forEach { option ->
                    val selected = option == tint
                    val label = stringResource(option.labelRes())
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .border(2.dp, if (selected) c.text else Color.Transparent, CircleShape)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { tint = option })
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size(38.dp).clip(CircleShape).background(c.tint(option))) }
                }
            }
        }
        PrimaryButton(stringResource(R.string.action_save), { onSave(name, icon, tint) }, enabled = name.isNotBlank() && !saving)
        if (onDelete != null) DangerTextButton(stringResource(R.string.method_delete), onDelete)
    }
}
