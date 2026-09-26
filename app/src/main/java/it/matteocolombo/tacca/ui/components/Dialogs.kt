package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun ConfirmDialog(
    title: String,
    text: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    val c = TaccaTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, style = TaccaType.label, color = if (destructive) c.danger else c.accentText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), style = TaccaType.label, color = c.text) }
        },
        title = { Text(title, style = TaccaType.title.copy(fontSize = 18.sp, lineHeight = 23.sp), color = c.text) },
        text = text?.let { { Text(it, style = TaccaType.body, color = c.textMuted) } },
        containerColor = c.surface,
        shape = RoundedCornerShape(28.dp),
    )
}

@Composable
fun cascadeDeleteText(count: Int): String =
    if (count == 0) stringResource(R.string.confirm_delete_no_cascade)
    else pluralStringResource(R.plurals.confirm_delete_cascade_body, count, count)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaccaBottomSheet(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = TaccaTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background,
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp).imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = TaccaType.h1Small.copy(fontSize = 24.sp), color = c.text)
            content()
        }
    }
}
