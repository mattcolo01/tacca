package it.matteocolombo.tacca.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setAccent(accent: AccentOption)
    suspend fun setThemeMode(mode: ThemeMode)
}

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<AppSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            AppSettings(
                accent = p[ACCENT]?.let { v -> AccentOption.entries.firstOrNull { it.name == v } } ?: AccentOption.TERRACOTTA,
                themeMode = p[THEME_MODE]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.SYSTEM,
            )
        }

    override suspend fun setAccent(accent: AccentOption) {
        store.edit { it[ACCENT] = accent.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    private companion object {
        val ACCENT = stringPreferencesKey("accent")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
