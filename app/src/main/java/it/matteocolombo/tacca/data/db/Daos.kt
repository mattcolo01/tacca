package it.matteocolombo.tacca.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeDao {
    @Query("SELECT * FROM coffee ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<CoffeeEntity>>

    @Query("SELECT * FROM coffee ORDER BY id")
    suspend fun getAll(): List<CoffeeEntity>

    @Query("SELECT * FROM coffee WHERE id = :id")
    suspend fun get(id: Long): CoffeeEntity?

    @Insert suspend fun insert(entity: CoffeeEntity): Long

    @Insert suspend fun insertAll(entities: List<CoffeeEntity>)

    @Query("UPDATE coffee SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM coffee WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM coffee")
    suspend fun deleteAll()
}

@Dao
interface MethodDao {
    @Query("SELECT * FROM method ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<MethodEntity>>

    @Query("SELECT * FROM method ORDER BY sortOrder, id")
    suspend fun getAll(): List<MethodEntity>

    @Query("SELECT * FROM method WHERE id = :id")
    suspend fun get(id: Long): MethodEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM method")
    suspend fun maxSortOrder(): Int

    @Insert suspend fun insert(entity: MethodEntity): Long

    @Insert suspend fun insertAll(entities: List<MethodEntity>)

    @Update suspend fun update(entity: MethodEntity)

    @Query("DELETE FROM method WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM method")
    suspend fun deleteAll()
}

@Dao
interface GrinderDao {
    @Query("SELECT * FROM grinder ORDER BY createdAt, id")
    fun observeAll(): Flow<List<GrinderEntity>>

    @Query("SELECT * FROM grinder ORDER BY id")
    suspend fun getAll(): List<GrinderEntity>

    @Query("SELECT * FROM grinder WHERE id = :id")
    suspend fun get(id: Long): GrinderEntity?

    @Insert suspend fun insert(entity: GrinderEntity): Long

    @Insert suspend fun insertAll(entities: List<GrinderEntity>)

    @Update suspend fun update(entity: GrinderEntity)

    @Query("DELETE FROM grinder WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM grinder")
    suspend fun deleteAll()
}

@Dao
interface CalibrationDao {
    @Query("$CALIBRATION_ROW_SELECT ORDER BY c.updatedAt DESC, c.id DESC")
    fun observeRows(): Flow<List<CalibrationRow>>

    @Query("$CALIBRATION_ROW_SELECT WHERE c.id = :id")
    fun observeRow(id: Long): Flow<CalibrationRow?>

    @Query("SELECT * FROM calibration WHERE coffeeId = :coffeeId AND methodId = :methodId AND grinderId = :grinderId")
    suspend fun findByCombination(coffeeId: Long, methodId: Long, grinderId: Long): CalibrationEntity?

    @Query("SELECT * FROM calibration WHERE id = :id")
    suspend fun get(id: Long): CalibrationEntity?

    @Query("SELECT * FROM calibration ORDER BY id")
    suspend fun getAll(): List<CalibrationEntity>

    @Query("SELECT * FROM calibration WHERE grinderId = :grinderId")
    suspend fun getForGrinder(grinderId: Long): List<CalibrationEntity>

    @Query("SELECT COUNT(*) FROM calibration WHERE coffeeId = :id")
    suspend fun countForCoffee(id: Long): Int

    @Query("SELECT COUNT(*) FROM calibration WHERE methodId = :id")
    suspend fun countForMethod(id: Long): Int

    @Query("SELECT COUNT(*) FROM calibration WHERE grinderId = :id")
    suspend fun countForGrinder(id: Long): Int

    @Insert suspend fun insert(entity: CalibrationEntity): Long

    @Insert suspend fun insertAll(entities: List<CalibrationEntity>)

    @Update suspend fun update(entity: CalibrationEntity)

    @Query("DELETE FROM calibration WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM calibration")
    suspend fun deleteAll()
}
