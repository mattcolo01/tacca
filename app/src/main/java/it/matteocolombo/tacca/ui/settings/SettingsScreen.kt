package it.matteocolombo.tacca.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.ThemeMode
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.FilterPill
import it.matteocolombo.tacca.ui.components.Legend
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.format.labelRes
import it.matteocolombo.tacca.ui.format.messageRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType
import java.time.LocalDate

data class SettingsActions(
    val onBack: () -> Unit,
    val onAccent: (AccentOption) -> Unit,
    val onTheme: (ThemeMode) -> Unit,
    val onLanguage: () -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
) {
    companion object {
        val Noop = SettingsActions({}, {}, {}, {}, {}, {})
    }
}

@Composable
fun SettingsEntry(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val res = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    var languageDialog by rememberSaveable { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri?.toString()
    }
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

    LaunchedEffect(state.message) {
        val text = when (val m = state.message) {
            null -> return@LaunchedEffect
            SettingsMessage.Exported -> res.getString(R.string.export_done)
            SettingsMessage.Imported -> res.getString(R.string.import_done)
            is SettingsMessage.Failed -> res.getString(m.error.messageRes())
        }
        vm.consumeMessage()
        snackbar.showSnackbar(text)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = TaccaTheme.colors.background) { inner ->
        Box(Modifier.padding(inner)) {
            SettingsScreen(
                state = state,
                languageLabel = stringResource(AppLanguage.labelRes(AppLanguage.current())),
                versionName = versionName,
                actions = SettingsActions(
                    onBack = onBack,
                    onAccent = vm::setAccent,
                    onTheme = vm::setThemeMode,
                    onLanguage = {
                        val intent = AppLanguage.systemSettingsIntent(context)
                        if (intent != null) context.startActivity(intent) else languageDialog = true
                    },
                    onExport = { exportLauncher.launch(res.getString(R.string.backup_file_name, LocalDate.now().toString())) },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                ),
            )
        }
    }

    pendingImport?.let { uriString ->
        ConfirmDialog(
            title = stringResource(R.string.import_confirm_title),
            text = stringResource(R.string.import_confirm_body),
            confirmLabel = stringResource(R.string.import_confirm_action),
            onConfirm = { pendingImport = null; vm.import(Uri.parse(uriString)) },
            onDismiss = { pendingImport = null },
        )
    }
    if (languageDialog) {
        LanguageDialog(current = AppLanguage.current(), onPick = { AppLanguage.set(it); languageDialog = false }, onDismiss = { languageDialog = false })
    }
}

@Composable
fun SettingsScreen(state: SettingsUiState, languageLabel: String, versionName: String, actions: SettingsActions) {
    val c = TaccaTheme.colors
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SquareIconButton(R.drawable.ic_back, stringResource(R.string.action_back), actions.onBack)
            ScreenHeader(stringResource(R.string.settings_title), titleStyle = TaccaType.h1Small)
        }

        Section(stringResource(R.string.settings_accent)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentOption.entries.forEach { option ->
                    val selected = option == state.settings.accent
                    val label = stringResource(option.labelRes())
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Color(option.argb))
                            .border(3.dp, if (selected) c.text else Color.Transparent, CircleShape)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onAccent(option) })
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = c.onAccent, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        Section(stringResource(R.string.settings_theme)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterPill(stringResource(mode.labelRes()), mode == state.settings.themeMode, { actions.onTheme(mode) })
                }
            }
        }

        Section(stringResource(R.string.settings_language)) {
            SettingsRow(stringResource(R.string.settings_language), languageLabel, actions.onLanguage)
        }

        Section(stringResource(R.string.settings_data)) {
            SettingsRow(stringResource(R.string.settings_export), stringResource(R.string.settings_export_body), actions.onExport, enabled = !state.busy)
            SettingsRow(stringResource(R.string.settings_import), stringResource(R.string.settings_import_body), actions.onImport, enabled = !state.busy)
        }

        Section(stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.settings_version, versionName), style = TaccaType.body, color = c.textMuted, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Legend(title)
        content()
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true) {
    val c = TaccaTheme.colors
    SurfaceCard(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = TaccaType.title, color = c.text)
                Text(subtitle, style = TaccaType.bodySmall, color = c.textMuted)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LanguageDialog(current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val c = TaccaTheme.colors
    val options = listOf<String?>(null) + AppLanguage.supported
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = c.text) } },
        title = { Text(stringResource(R.string.settings_language), style = TaccaType.title, color = c.text) },
        text = {
            Column {
                options.forEach { tag ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = tag == current, role = Role.RadioButton, onClick = { onPick(tag) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tag == current, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.accent))
                        Text(stringResource(AppLanguage.labelRes(tag)), style = TaccaType.body, color = c.text, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        containerColor = c.surface,
    )
}
