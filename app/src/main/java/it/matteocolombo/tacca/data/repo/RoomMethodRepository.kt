package it.matteocolombo.tacca.data.repo

import androidx.room.withTransaction
import it.matteocolombo.tacca.data.db.MethodEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.db.toModel
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMethodRepository(private val db: TaccaDatabase) : MethodRepository {
    private val dao = db.methodDao()

    override fun observeMethods(): Flow<List<Method>> = dao.observeAll().map { list -> list.map { it.toModel() } }
    override suspend fun get(id: Long): Method? = dao.get(id)?.toModel()

    override suspend fun add(name: String, icon: MethodIcon, tint: Tint): Long = db.withTransaction {
        dao.insert(MethodEntity(name = cleanName(name), builtInKey = null, icon = icon, tint = tint, sortOrder = dao.maxSortOrder() + 1))
    }

    override suspend fun update(id: Long, customName: String?, icon: MethodIcon, tint: Tint) {
        val current = checkNotNull(dao.get(id)) { "Method $id not found" }
        val name = customName?.let(::cleanName)
        require(name != null || current.builtInKey != null) { "A custom method needs a name" }
        dao.update(current.copy(name = name, icon = icon, tint = tint))
    }

    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun calibrationCount(id: Long): Int = db.calibrationDao().countForMethod(id)
}
