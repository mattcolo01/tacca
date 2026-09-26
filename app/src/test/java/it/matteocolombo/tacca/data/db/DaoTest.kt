package it.matteocolombo.tacca.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: TaccaDatabase

    @Before fun setUp() {
        db = TaccaDatabase.inMemory(ApplicationProvider.getApplicationContext())
    }

    @After fun tearDown() = db.close()

    private suspend fun seedOne(): CalibrationEntity {
        val coffeeId = db.coffeeDao().insert(CoffeeEntity(name = "Brasile", createdAt = 1))
        val grinderId = db.grinderDao().insert(GrinderEntity(name = "JX-Pro", multiTurn = true, stepsPerTurn = 40, createdAt = 1))
        val methodId = db.methodDao().getAll().first().id
        val c = CalibrationEntity(coffeeId = coffeeId, methodId = methodId, grinderId = grinderId, turns = 2, steps = 12, notes = "", updatedAt = 5)
        return c.copy(id = db.calibrationDao().insert(c))
    }

    @Test fun seedsBuiltInMethodsInOrder() = runTest {
        val methods = db.methodDao().getAll()
        assertEquals(BuiltInMethod.entries.map { it.key }, methods.map { it.builtInKey })
        assertEquals(listOf(0, 1, 2, 3, 4), methods.map { it.sortOrder })
        assertEquals(MethodIcon.MOKA, methods[0].icon)
        assertEquals(Tint.PEACH, methods[0].tint)
        assertNull(methods[0].name)
    }

    @Test fun combinationIsUnique() = runTest {
        val c = seedOne()
        try {
            db.calibrationDao().insert(c.copy(id = 0))
            fail("expected unique constraint violation")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test fun deletingCoffeeCascades() = runTest {
        val c = seedOne()
        db.coffeeDao().delete(c.coffeeId)
        assertEquals(0, db.calibrationDao().getAll().size)
    }

    @Test fun deletingGrinderAndMethodCascade() = runTest {
        val c = seedOne()
        db.methodDao().delete(c.methodId)
        assertEquals(0, db.calibrationDao().getAll().size)
        val c2 = seedOne()
        db.grinderDao().delete(c2.grinderId)
        assertEquals(0, db.calibrationDao().getAll().size)
    }

    @Test fun rowsJoinAllFields() = runTest {
        val c = seedOne()
        val row = db.calibrationDao().observeRows().first().single()
        assertEquals("Brasile", row.coffeeName)
        assertEquals("moka", row.methodBuiltInKey)
        assertEquals("JX-Pro", row.grinderName)
        assertEquals(40, row.grinderStepsPerTurn)
        val model = row.toModel()
        assertEquals(GrindSetting(2, 12), model.setting)
        assertEquals(BuiltInMethod.MOKA, model.method.builtIn)
        assertEquals(c.id, db.calibrationDao().observeRow(c.id).first()!!.id)
    }

    @Test fun countsAndLookups() = runTest {
        val c = seedOne()
        assertEquals(1, db.calibrationDao().countForCoffee(c.coffeeId))
        assertEquals(1, db.calibrationDao().countForMethod(c.methodId))
        assertEquals(1, db.calibrationDao().countForGrinder(c.grinderId))
        assertEquals(c.id, db.calibrationDao().findByCombination(c.coffeeId, c.methodId, c.grinderId)!!.id)
        assertEquals(4, db.methodDao().maxSortOrder())
    }
}
