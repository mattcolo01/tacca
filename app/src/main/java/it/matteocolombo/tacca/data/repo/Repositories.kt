package it.matteocolombo.tacca.data.repo

import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.Flow

interface CoffeeRepository {
    fun observeCoffees(): Flow<List<Coffee>>          // ordered by name
    suspend fun get(id: Long): Coffee?
    suspend fun add(name: String): Long               // trims; blank -> IllegalArgumentException
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
    suspend fun calibrationCount(id: Long): Int
}

interface MethodRepository {
    fun observeMethods(): Flow<List<Method>>          // ordered by sortOrder
    suspend fun get(id: Long): Method?
    suspend fun add(name: String, icon: MethodIcon, tint: Tint): Long   // sortOrder = max + 1
    suspend fun update(id: Long, customName: String?, icon: MethodIcon, tint: Tint) // customName null keeps built-in name
    suspend fun delete(id: Long)
    suspend fun calibrationCount(id: Long): Int
}

interface GrinderRepository {
    fun observeGrinders(): Flow<List<Grinder>>        // ordered by createdAt
    suspend fun get(id: Long): Grinder?
    suspend fun add(name: String, spec: GrinderSpec): Long
    suspend fun update(id: Long, name: String, spec: GrinderSpec)
    suspend fun delete(id: Long)
    suspend fun calibrationCount(id: Long): Int
    suspend fun countOutOfRange(id: Long, spec: GrinderSpec): Int
}

data class SaveCalibration(val editingId: Long?, val coffeeId: Long, val methodId: Long, val grinderId: Long, val setting: GrindSetting, val notes: String)
data class ExistingCalibration(val id: Long, val setting: GrindSetting)

interface CalibrationRepository {
    fun observeAll(): Flow<List<Calibration>>         // updatedAt desc
    fun observe(id: Long): Flow<Calibration?>
    suspend fun findExisting(coffeeId: Long, methodId: Long, grinderId: Long): ExistingCalibration?
    suspend fun save(request: SaveCalibration): Long  // insert / replace / move, one transaction; returns saved id
    suspend fun delete(id: Long)
}

/** Trims [name] and rejects blank values. */
internal fun cleanName(name: String): String = name.trim().also { require(it.isNotEmpty()) { "Name must not be blank" } }
