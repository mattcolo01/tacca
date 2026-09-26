package it.matteocolombo.tacca.data.repo

import it.matteocolombo.tacca.data.db.GrinderEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.db.toModel
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.SettingRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGrinderRepository(
    private val db: TaccaDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : GrinderRepository {
    private val dao = db.grinderDao()

    override fun observeGrinders(): Flow<List<Grinder>> = dao.observeAll().map { list -> list.map { it.toModel() } }
    override suspend fun get(id: Long): Grinder? = dao.get(id)?.toModel()

    override suspend fun add(name: String, spec: GrinderSpec): Long {
        require(SettingRules.isValidStepsPerTurn(spec.stepsPerTurn))
        return dao.insert(GrinderEntity(name = cleanName(name), multiTurn = spec.multiTurn, stepsPerTurn = spec.stepsPerTurn, createdAt = clock()))
    }

    override suspend fun update(id: Long, name: String, spec: GrinderSpec) {
        require(SettingRules.isValidStepsPerTurn(spec.stepsPerTurn))
        val current = checkNotNull(dao.get(id)) { "Grinder $id not found" }
        dao.update(current.copy(name = cleanName(name), multiTurn = spec.multiTurn, stepsPerTurn = spec.stepsPerTurn))
    }

    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun calibrationCount(id: Long): Int = db.calibrationDao().countForGrinder(id)

    override suspend fun countOutOfRange(id: Long, spec: GrinderSpec): Int =
        db.calibrationDao().getForGrinder(id).count { !SettingRules.isInRange(GrindSetting(it.turns, it.steps), spec) }
}
