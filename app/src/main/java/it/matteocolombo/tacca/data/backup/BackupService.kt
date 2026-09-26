package it.matteocolombo.tacca.data.backup

import androidx.room.withTransaction
import it.matteocolombo.tacca.data.db.CalibrationEntity
import it.matteocolombo.tacca.data.db.CoffeeEntity
import it.matteocolombo.tacca.data.db.GrinderEntity
import it.matteocolombo.tacca.data.db.MethodEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

private const val MAX_IMPORT_BYTES = 10L * 1024 * 1024

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
                readBounded(input, MAX_IMPORT_BYTES)
            } catch (e: IOException) {
                throw BackupException(BackupError.IO, e)
            }
        }
        // Parsing/validating a large JSON tree is CPU work: keep it off the caller's dispatcher
        // (typically Dispatchers.Main from a ViewModel) so a big backup does not freeze the UI.
        val file = withContext(Dispatchers.Default) { BackupCodec.decode(text) }
        restore(file)
    }

    /** Reads at most [maxBytes] from [input], closing it. Rejects anything larger as not a backup. */
    private fun readBounded(input: InputStream, maxBytes: Long): String {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        var total = 0L
        input.use { stream ->
            while (true) {
                val n = stream.read(chunk)
                if (n == -1) break
                total += n
                if (total > maxBytes) throw BackupException(BackupError.NOT_A_BACKUP)
                buffer.write(chunk, 0, n)
            }
        }
        return String(buffer.toByteArray(), Charsets.UTF_8)
    }
}
