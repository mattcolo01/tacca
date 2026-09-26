package it.matteocolombo.tacca.data.backup

import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.SettingRules
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

enum class BackupError { NOT_A_BACKUP, NEWER_VERSION, INVALID_DATA, IO }

class BackupException(val error: BackupError, cause: Throwable? = null) : Exception(error.name, cause)

object BackupCodec {
    const val FORMAT = "tacca-backup"
    const val VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** Parses and fully validates [text]. Throws [BackupException] on any problem. */
    fun decode(text: String): BackupFile {
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw BackupException(BackupError.NOT_A_BACKUP, e)
        }
        val obj = root as? JsonObject ?: throw BackupException(BackupError.NOT_A_BACKUP)
        if ((obj["format"] as? JsonPrimitive)?.contentOrNull != FORMAT) throw BackupException(BackupError.NOT_A_BACKUP)
        val version = (obj["version"] as? JsonPrimitive)?.intOrNull ?: throw BackupException(BackupError.INVALID_DATA)
        if (version > VERSION) throw BackupException(BackupError.NEWER_VERSION)
        if (version < 1) throw BackupException(BackupError.INVALID_DATA)
        val file = try {
            json.decodeFromJsonElement(BackupFile.serializer(), obj)
        } catch (e: SerializationException) {
            throw BackupException(BackupError.INVALID_DATA, e)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupError.INVALID_DATA, e)
        }
        validate(file)
        return file
    }

    private fun validate(f: BackupFile) {
        fun ensure(ok: Boolean) {
            if (!ok) throw BackupException(BackupError.INVALID_DATA)
        }
        val coffeeIds = f.coffees.map { it.id }.toSet()
        val methodIds = f.methods.map { it.id }.toSet()
        val grinderIds = f.grinders.map { it.id }.toSet()
        ensure(coffeeIds.size == f.coffees.size && methodIds.size == f.methods.size && grinderIds.size == f.grinders.size)
        f.coffees.forEach { ensure(it.name.isNotBlank()) }
        f.methods.forEach {
            ensure(it.name == null || it.name.isNotBlank())
            ensure(it.builtInKey == null || BuiltInMethod.fromKey(it.builtInKey) != null)
            ensure(it.name != null || it.builtInKey != null)
        }
        f.grinders.forEach {
            ensure(it.name.isNotBlank())
            ensure(SettingRules.isValidStepsPerTurn(it.stepsPerTurn))
        }
        f.calibrations.forEach {
            ensure(it.coffeeId in coffeeIds && it.methodId in methodIds && it.grinderId in grinderIds)
            ensure(it.turns >= 0 && it.steps >= 0)
        }
        ensure(f.calibrations.map { Triple(it.coffeeId, it.methodId, it.grinderId) }.toSet().size == f.calibrations.size)
    }
}
