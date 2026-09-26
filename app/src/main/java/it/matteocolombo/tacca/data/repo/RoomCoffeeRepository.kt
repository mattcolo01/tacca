package it.matteocolombo.tacca.data.repo

import it.matteocolombo.tacca.data.db.CoffeeEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.db.toModel
import it.matteocolombo.tacca.domain.Coffee
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCoffeeRepository(
    private val db: TaccaDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : CoffeeRepository {
    private val dao = db.coffeeDao()

    override fun observeCoffees(): Flow<List<Coffee>> = dao.observeAll().map { list -> list.map { it.toModel() } }
    override suspend fun get(id: Long): Coffee? = dao.get(id)?.toModel()
    override suspend fun add(name: String): Long = dao.insert(CoffeeEntity(name = cleanName(name), createdAt = clock()))
    override suspend fun rename(id: Long, name: String) = dao.rename(id, cleanName(name))
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun calibrationCount(id: Long): Int = db.calibrationDao().countForCoffee(id)
}
