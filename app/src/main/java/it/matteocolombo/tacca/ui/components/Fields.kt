package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

/** Labelled text field in mockup style (label above, 16dp radius, surface fill). */
@Composable
fun TaccaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    supportingText: String? = null,
    fieldModifier: Modifier = Modifier,
) {
    val c = TaccaTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Legend(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier.fillMaxWidth().semantics { contentDescription = label },
            placeholder = { Text(placeholder, style = TaccaType.body.copy(fontSize = 16.sp), color = c.textMuted) },
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(16.dp),
            textStyle = TaccaType.body.copy(fontSize = 16.sp, color = c.text),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            supportingText = supportingText?.let { { Text(it, style = TaccaType.bodySmall, color = c.textMuted) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.outline,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                cursorColor = c.accent,
            ),
        )
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClear: (() -> Unit)? = null,
) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(shape).background(c.surface).border(1.dp, c.outline, shape).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(placeholder, style = TaccaType.body, color = c.textMuted, maxLines = 1)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TaccaType.body.copy(color = c.text),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            )
        }
        if (onClear != null && query.isNotEmpty()) {
            val clearLabel = stringResource(R.string.action_clear_search)
            IconButton(onClick = onClear, modifier = Modifier.size(48.dp).semantics { contentDescription = clearLabel }) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}
