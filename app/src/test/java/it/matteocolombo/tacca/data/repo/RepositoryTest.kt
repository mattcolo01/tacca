package it.matteocolombo.tacca.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private suspend fun ids(): Triple<Long, Long, Long> {
        val coffee = r.coffees.add("Brasile")
        val method = r.builtInMethod(BuiltInMethod.MOKA).id
        val grinder = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        return Triple(coffee, method, grinder)
    }

    @Test fun addTrimsNames() = runTest {
        val id = r.coffees.add("  Etiopia  ")
        assertEquals("Etiopia", r.coffees.get(id)!!.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankNameIsRejected() = runTest {
        r.coffees.add("   ")
    }

    @Test fun saveNewInserts() = runTest {
        val (c, m, g) = ids()
        val id = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 30), " dose 18g "))
        val saved = r.calibrations.observe(id).first()!!
        assertEquals(GrindSetting(1, 30), saved.setting)
        assertEquals("dose 18g", saved.notes)
    }

    @Test fun saveNewOnExistingCombinationReplacesIt() = runTest {
        val (c, m, g) = ids()
        val first = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 30), ""))
        val second = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(2, 5), ""))
        assertEquals(first, second)
        val all = r.calibrations.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(GrindSetting(2, 5), all.single().setting)
    }

    @Test fun editMovingOntoExistingCombinationDeletesTheOther() = runTest {
        val (c, m, g) = ids()
        val v60 = r.builtInMethod(BuiltInMethod.V60).id
        val a = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 1), ""))
        r.calibrations.save(SaveCalibration(null, c, v60, g, GrindSetting(3, 3), ""))
        val moved = r.calibrations.save(SaveCalibration(a, c, v60, g, GrindSetting(2, 2), ""))
        assertEquals(a, moved)
        val all = r.calibrations.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(v60, all.single().method.id)
        assertEquals(GrindSetting(2, 2), all.single().setting)
    }

    @Test fun findExisting() = runTest {
        val (c, m, g) = ids()
        assertNull(r.calibrations.findExisting(c, m, g))
        val id = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(0, 7), ""))
        assertEquals(ExistingCalibration(id, GrindSetting(0, 7)), r.calibrations.findExisting(c, m, g))
    }

    @Test fun countOutOfRange() = runTest {
        val (c, m, g) = ids()
        r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 30), ""))
        assertEquals(0, r.grinders.countOutOfRange(g, GrinderSpec(true, 40)))
        assertEquals(1, r.grinders.countOutOfRange(g, GrinderSpec(true, 20)))
        assertEquals(1, r.grinders.countOutOfRange(g, GrinderSpec(false, 40))) // turns > 0 on single-turn
        assertEquals(0, r.grinders.countOutOfRange(g, GrinderSpec(true, null)))
    }

    @Test fun methodAddAppendsAndUpdateKeepsBuiltIn() = runTest {
        val id = r.methods.add("Chemex", MethodIcon.DROP, Tint.SKY)
        assertEquals(5, r.methods.get(id)!!.sortOrder)
        val moka = r.builtInMethod(BuiltInMethod.MOKA)
        r.methods.update(moka.id, "Bialetti", MethodIcon.MOKA, Tint.SAND)
        val updated = r.methods.get(moka.id)!!
        assertEquals("Bialetti", updated.customName)
        assertEquals(BuiltInMethod.MOKA, updated.builtIn)
        assertEquals(Tint.SAND, updated.tint)
    }

    @Test fun deleteCascadesAndCounts() = runTest {
        val (c, m, g) = ids()
        r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 1), ""))
        assertEquals(1, r.coffees.calibrationCount(c))
        assertEquals(1, r.methods.calibrationCount(m))
        assertEquals(1, r.grinders.calibrationCount(g))
        r.grinders.delete(g)
        assertEquals(0, r.calibrations.observeAll().first().size)
    }

    @Test fun homeFilterStore() {
        val s = HomeFilterStore()
        s.update { it.copy(query = "x") }
        s.showCoffee(3)
        assertEquals(HomeFilter(coffeeId = 3), s.filter.value)
        s.showMethod(2)
        assertEquals(HomeFilter(methodId = 2), s.filter.value)
    }
}
