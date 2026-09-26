package it.matteocolombo.tacca.testutil

import androidx.test.core.app.ApplicationProvider
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.repo.RoomCalibrationRepository
import it.matteocolombo.tacca.data.repo.RoomCoffeeRepository
import it.matteocolombo.tacca.data.repo.RoomGrinderRepository
import it.matteocolombo.tacca.data.repo.RoomMethodRepository
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Method
import kotlinx.coroutines.flow.first

fun testDatabase(): TaccaDatabase = TaccaDatabase.inMemory(ApplicationProvider.getApplicationContext())

/** Real Room repositories over an in-memory database, with a controllable clock. */
class TestRepos(val db: TaccaDatabase = testDatabase(), var now: Long = 1_000L) {
    private val clock: () -> Long = { now++ }
    val coffees = RoomCoffeeRepository(db, clock)
    val methods = RoomMethodRepository(db)
    val grinders = RoomGrinderRepository(db, clock)
    val calibrations = RoomCalibrationRepository(db, clock)
    val homeFilter = HomeFilterStore()

    suspend fun builtInMethod(m: BuiltInMethod): Method = methods.observeMethods().first().first { it.builtIn == m }
}
