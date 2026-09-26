package it.matteocolombo.tacca.data.repo

import androidx.room.withTransaction
import it.matteocolombo.tacca.data.db.CalibrationEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.db.toModel
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.GrindSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCalibrationRepository(
    private val db: TaccaDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : CalibrationRepository {
    private val dao = db.calibrationDao()

    override fun observeAll(): Flow<List<Calibration>> = dao.observeRows().map { rows -> rows.map { it.toModel() } }
    override fun observe(id: Long): Flow<Calibration?> = dao.observeRow(id).map { it?.toModel() }

    override suspend fun findExisting(coffeeId: Long, methodId: Long, grinderId: Long): ExistingCalibration? =
        dao.findByCombination(coffeeId, methodId, grinderId)?.let { ExistingCalibration(it.id, GrindSetting(it.turns, it.steps)) }

    override suspend fun save(request: SaveCalibration): Long = db.withTransaction {
        val now = clock()
        val notes = request.notes.trim()
        val existing = dao.findByCombination(request.coffeeId, request.methodId, request.grinderId)
        if (request.editingId == null) {
            if (existing != null) {
                dao.update(existing.copy(turns = request.setting.turns, steps = request.setting.steps, notes = notes, updatedAt = now))
                existing.id
            } else {
                dao.insert(
                    CalibrationEntity(
                        coffeeId = request.coffeeId, methodId = request.methodId, grinderId = request.grinderId,
                        turns = request.setting.turns, steps = request.setting.steps, notes = notes, updatedAt = now,
                    ),
                )
            }
        } else {
            val editing = checkNotNull(dao.get(request.editingId)) { "Calibration ${request.editingId} not found" }
            if (existing != null && existing.id != editing.id) dao.delete(existing.id)
            dao.update(
                editing.copy(
                    coffeeId = request.coffeeId, methodId = request.methodId, grinderId = request.grinderId,
                    turns = request.setting.turns, steps = request.setting.steps, notes = notes, updatedAt = now,
                ),
            )
            editing.id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
