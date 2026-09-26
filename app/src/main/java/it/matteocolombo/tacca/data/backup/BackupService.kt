package it.matteocolombo.tacca.data.backup

import androidx.room.withTransaction
import it.matteocolombo.tacca.data.db.CalibrationEntity
import it.matteocolombo.tacca.data.db.CoffeeEntity
import it.matteocolombo.tacca.data.db.GrinderEntity
import it.matteocolombo.tacca.data.db.MethodEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

class BackupService(
    private val db: TaccaDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun createBackup(): BackupFile = db.withTransaction {
        BackupFile(
            format = BackupCodec.FORMAT,
            version = BackupCodec.VERSION,
            exportedAt = Instant.ofEpochMilli(clock()).toString(),
            coffees = db.coffeeDao().getAll().map { CoffeeDto(it.id, it.name, it.createdAt) },
            methods = db.methodDao().getAll().map { MethodDto(it.id, it.name, it.builtInKey, it.icon, it.tint, it.sortOrder) },
            grinders = db.grinderDao().getAll().map { GrinderDto(it.id, it.name, it.multiTurn, it.stepsPerTurn, it.createdAt) },
            calibrations = db.calibrationDao().getAll().map {
                CalibrationDto(it.coffeeId, it.methodId, it.grinderId, it.turns, it.steps, it.notes, it.updatedAt)
            },
        )
    }

    /** Replaces all data with [file] in one transaction. [file] must come from [BackupCodec.decode]. */
    suspend fun restore(file: BackupFile) = db.withTransaction {
        db.calibrationDao().deleteAll()
        db.coffeeDao().deleteAll()
        db.methodDao().deleteAll()
        db.grinderDao().deleteAll()
        db.coffeeDao().insertAll(file.coffees.map { CoffeeEntity(it.id, it.name, it.createdAt) })
        db.methodDao().insertAll(file.methods.map { MethodEntity(it.id, it.name, it.builtInKey, it.icon, it.tint, it.sortOrder) })
        db.grinderDao().insertAll(file.grinders.map { GrinderEntity(it.id, it.name, it.multiTurn, it.stepsPerTurn, it.createdAt) })
        db.calibrationDao().insertAll(
            file.calibrations.map {
                CalibrationEntity(coffeeId = it.coffeeId, methodId = it.methodId, grinderId = it.grinderId, turns = it.turns, steps = it.steps, notes = it.notes, updatedAt = it.updatedAt)
            },
        )
    }

    suspend fun export(out: OutputStream) {
        val text = BackupCodec.encode(createBackup())
        withContext(Dispatchers.IO) {
            try {
                out.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            } catch (e: IOException) {
                throw BackupException(BackupError.IO, e)
            }
        }
    }

    suspend fun import(input: InputStream) {
        val text = withContext(Dispatchers.IO) {
            try {
                input.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: IOException) {
                throw BackupException(BackupError.IO, e)
            }
        }
        restore(BackupCodec.decode(text))
    }
}
