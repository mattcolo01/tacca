package it.matteocolombo.tacca.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.backup.BackupError
import it.matteocolombo.tacca.data.backup.BackupException
import it.matteocolombo.tacca.data.backup.BackupService
import it.matteocolombo.tacca.data.backup.DocumentStreams
import it.matteocolombo.tacca.data.repo.HomeFilter
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.settings.SettingsRepository
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface SettingsMessage {
    data object Exported : SettingsMessage
    data object Imported : SettingsMessage
    data class Failed(val error: BackupError) : SettingsMessage
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val busy: Boolean = false,
    val message: SettingsMessage? = null,
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val backup: BackupService,
    private val documents: DocumentStreams,
    private val homeFilter: HomeFilterStore,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<SettingsMessage?>(null)

    val uiState: StateFlow<SettingsUiState> =
        combine(settings.settings, busy, message) { s, b, m -> SettingsUiState(s, b, m) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setAccent(accent: AccentOption) {
        viewModelScope.launch { settings.setAccent(accent) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun export(uri: Uri) = runBackup(SettingsMessage.Exported) {
        val out = documents.openOutput(uri) ?: throw BackupException(BackupError.IO)
        out.use { backup.export(it) }
    }

    fun import(uri: Uri) = runBackup(SettingsMessage.Imported) {
        val input = documents.openInput(uri) ?: throw BackupException(BackupError.IO)
        input.use { backup.import(it) }
        // Ids may have changed: drop filters that point at old data.
        homeFilter.update { HomeFilter() }
    }

    fun consumeMessage() {
        message.value = null
    }

    private fun runBackup(success: SettingsMessage, block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            message.value = try {
                block()
                success
            } catch (e: BackupException) {
                SettingsMessage.Failed(e.error)
            } catch (e: IOException) {
                SettingsMessage.Failed(BackupError.IO)
            } catch (e: SecurityException) {
                SettingsMessage.Failed(BackupError.IO)
            } finally {
                busy.value = false
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                SettingsViewModel(c.settings, c.backup, c.documents, c.homeFilter)
            }
        }
    }
}
