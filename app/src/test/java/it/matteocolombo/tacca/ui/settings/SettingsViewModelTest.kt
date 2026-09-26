package it.matteocolombo.tacca.ui.settings

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.backup.BackupError
import it.matteocolombo.tacca.data.backup.BackupService
import it.matteocolombo.tacca.data.backup.DocumentStreams
import it.matteocolombo.tacca.data.repo.HomeFilter
import it.matteocolombo.tacca.data.settings.SettingsRepository
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()
    private val uri = Uri.parse("content://test/backup.json")

    @After fun tearDown() = r.db.close()

    private class FakeSettings : SettingsRepository {
        val state = MutableStateFlow(AppSettings())
        override val settings = state
        override suspend fun setAccent(accent: AccentOption) = state.update { it.copy(accent = accent) }
        override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    }

    private class FakeDocuments(var input: ByteArray? = null) : DocumentStreams {
        val output = ByteArrayOutputStream()
        override fun openInput(uri: Uri): InputStream? = input?.let(::ByteArrayInputStream)
        override fun openOutput(uri: Uri): OutputStream = output
    }

    private val settings = FakeSettings()

    private fun vm(docs: DocumentStreams) = SettingsViewModel(settings, BackupService(r.db), docs, r.homeFilter)

    @Test fun changesAccentAndTheme() = runTest {
        val vm = vm(FakeDocuments())
        vm.setAccent(AccentOption.PINE)
        vm.setThemeMode(ThemeMode.DARK)
        assertEquals(AppSettings(AccentOption.PINE, ThemeMode.DARK), vm.uiState.first { it.settings.themeMode == ThemeMode.DARK }.settings)
    }

    @Test fun exportWritesABackup() = runTest {
        r.coffees.add("Brasile")
        val docs = FakeDocuments()
        val vm = vm(docs)
        vm.export(uri)
        assertEquals(SettingsMessage.Exported, vm.uiState.first { it.message != null }.message)
        assertTrue(docs.output.toString(Charsets.UTF_8).contains("\"tacca-backup\""))
    }

    @Test fun importReplacesDataAndResetsHomeFilter() = runTest {
        r.coffees.add("Brasile")
        val docs = FakeDocuments()
        vm(docs).also { it.export(uri) }.uiState.first { it.message == SettingsMessage.Exported }
        r.coffees.add("Extra")
        r.homeFilter.showCoffee(42)
        docs.input = docs.output.toByteArray()
        val vm = vm(docs)
        vm.import(uri)
        assertEquals(SettingsMessage.Imported, vm.uiState.first { it.message != null }.message)
        assertEquals(listOf("Brasile"), r.coffees.observeCoffees().first().map { it.name })
        assertEquals(HomeFilter(), r.homeFilter.filter.value)
    }

    @Test fun invalidImportReportsError() = runTest {
        val vm = vm(FakeDocuments(input = "not json".toByteArray()))
        vm.import(uri)
        assertEquals(SettingsMessage.Failed(BackupError.NOT_A_BACKUP), vm.uiState.first { it.message != null }.message)
        vm.consumeMessage()
        assertEquals(null, vm.uiState.first { it.message == null }.message)
    }

    @Test fun unreadableDocumentIsAnIoError() = runTest {
        val vm = vm(FakeDocuments(input = null))
        vm.import(uri)
        assertEquals(SettingsMessage.Failed(BackupError.IO), vm.uiState.first { it.message != null }.message)
    }
}
