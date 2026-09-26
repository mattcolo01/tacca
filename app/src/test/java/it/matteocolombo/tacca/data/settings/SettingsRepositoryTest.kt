package it.matteocolombo.tacca.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun defaultsThenPersistsChanges() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "settings.preferences_pb") }
        val repo = DataStoreSettingsRepository(store)
        assertEquals(AppSettings(), repo.settings.first())
        repo.setAccent(AccentOption.PLUM)
        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(AppSettings(AccentOption.PLUM, ThemeMode.DARK), repo.settings.first())
    }
}
