package it.matteocolombo.tacca

import android.content.Context
import it.matteocolombo.tacca.data.backup.BackupService
import it.matteocolombo.tacca.data.backup.ContentResolverDocumentStreams
import it.matteocolombo.tacca.data.backup.DocumentStreams
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.CoffeeRepository
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.repo.MethodRepository
import it.matteocolombo.tacca.data.repo.RoomCalibrationRepository
import it.matteocolombo.tacca.data.repo.RoomCoffeeRepository
import it.matteocolombo.tacca.data.repo.RoomGrinderRepository
import it.matteocolombo.tacca.data.repo.RoomMethodRepository
import it.matteocolombo.tacca.data.settings.DataStoreSettingsRepository
import it.matteocolombo.tacca.data.settings.SettingsRepository
import it.matteocolombo.tacca.data.settings.settingsDataStore

/** Manual dependency container, one per process. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: TaccaDatabase by lazy { TaccaDatabase.build(appContext) }
    val coffees: CoffeeRepository by lazy { RoomCoffeeRepository(database) }
    val methods: MethodRepository by lazy { RoomMethodRepository(database) }
    val grinders: GrinderRepository by lazy { RoomGrinderRepository(database) }
    val calibrations: CalibrationRepository by lazy { RoomCalibrationRepository(database) }
    val homeFilter = HomeFilterStore()
    val backup: BackupService by lazy { BackupService(database) }
    val documents: DocumentStreams by lazy { ContentResolverDocumentStreams(appContext.contentResolver) }
    val settings: SettingsRepository by lazy { DataStoreSettingsRepository(appContext.settingsDataStore) }
}
