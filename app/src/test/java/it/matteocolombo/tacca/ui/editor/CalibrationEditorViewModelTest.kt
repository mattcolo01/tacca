package it.matteocolombo.tacca.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import it.matteocolombo.tacca.ui.navigation.NEW_GRINDER_RESULT_KEY
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalibrationEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm(args: Map<String, Any?> = emptyMap(), handle: SavedStateHandle = SavedStateHandle(args)) =
        CalibrationEditorViewModel(handle, r.coffees, r.methods, r.grinders, r.calibrations)

    @Test fun presetsAndSingleGrinderArePreselected() = runTest {
        val coffee = r.coffees.add("Brasile")
        r.coffees.add("Etiopia")
        val grinder = r.grinders.add("JX", GrinderSpec(true, 40))
        val v60 = r.builtInMethod(BuiltInMethod.V60).id
        val s = vm(mapOf("coffeeId" to coffee, "methodId" to v60)).uiState.first { it.loaded }
        assertEquals(coffee, s.coffeeId)
        assertEquals(v60, s.methodId)
        assertEquals(grinder, s.grinderId)
        assertTrue(s.canSave)
    }

    @Test fun stepsClampToSingleTurnGrinder() = runTest {
        r.grinders.add("Encore", GrinderSpec(false, 10))
        val vm = vm()
        vm.uiState.first { it.loaded && it.grinderId != null }
        repeat(12) { vm.stepsUp() }
        assertEquals(GrindSetting(0, 10), vm.uiState.value.setting)
        assertFalse(vm.uiState.value.turnsEnabled)
        assertFalse(vm.uiState.value.canIncrementSteps)
    }

    @Test fun selectingAGrinderReclamps() = runTest {
        val big = r.grinders.add("JX", GrinderSpec(true, 40))
        val small = r.grinders.add("Small", GrinderSpec(true, 20))
        val vm = vm()
        vm.uiState.first { it.loaded && it.grinders.size == 2 }
        vm.selectGrinder(big)
        repeat(39) { vm.stepsUp() }
        vm.selectGrinder(small)
        assertEquals(GrindSetting(0, 19), vm.uiState.value.setting)
    }

    @Test fun warnsWhenReplacingAnExistingCombination() = runTest {
        val c = r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        val moka = r.builtInMethod(BuiltInMethod.MOKA).id
        r.calibrations.save(SaveCalibration(null, c, moka, g, GrindSetting(1, 30), ""))
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.selectCoffee(c)
        vm.selectMethod(moka)
        vm.selectGrinder(g)
        assertEquals(GrindSetting(1, 30), vm.uiState.first { it.replaces != null }.replaces)
    }

    @Test fun editLoadsAndSaves() = runTest {
        val c = r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        val moka = r.builtInMethod(BuiltInMethod.MOKA).id
        val id = r.calibrations.save(SaveCalibration(null, c, moka, g, GrindSetting(1, 30), "18g"))
        val vm = vm(mapOf("calibrationId" to id))
        val s = vm.uiState.first { it.loaded }
        assertTrue(s.isEdit)
        assertEquals(GrindSetting(1, 30), s.setting)
        assertEquals("18g", s.notes)
        assertNull(s.replaces)
        vm.stepsUp()
        vm.save()
        assertEquals(id, vm.uiState.first { it.savedId != null }.savedId)
        assertEquals(GrindSetting(1, 31), r.calibrations.observe(id).first()!!.setting)
    }

    @Test fun outOfRangeValuesAreNotClampedOnLoad() = runTest {
        val c = r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        val moka = r.builtInMethod(BuiltInMethod.MOKA).id
        val id = r.calibrations.save(SaveCalibration(null, c, moka, g, GrindSetting(0, 35), ""))
        r.grinders.update(g, "JX", GrinderSpec(true, 20))
        val s = vm(mapOf("calibrationId" to id)).uiState.first { it.loaded }
        assertEquals(GrindSetting(0, 35), s.setting)
    }

    @Test fun doubleSaveCreatesOneCalibration() = runTest {
        val c = r.coffees.add("Brasile")
        r.grinders.add("JX", GrinderSpec(true, 40))
        val vm = vm(mapOf("coffeeId" to c, "methodId" to r.builtInMethod(BuiltInMethod.V60).id))
        vm.uiState.first { it.canSave }
        vm.save()
        vm.save()
        vm.uiState.first { it.savedId != null }
        assertEquals(1, r.calibrations.observeAll().first().size)
    }

    @Test fun newGrinderResultIsSelected() = runTest {
        r.grinders.add("JX", GrinderSpec(true, 40))
        val handle = SavedStateHandle()
        val vm = vm(handle = handle)
        vm.uiState.first { it.loaded }
        val created = r.grinders.add("Comandante", GrinderSpec(true, 30))
        handle[NEW_GRINDER_RESULT_KEY] = created
        assertEquals(created, vm.uiState.first { it.grinderId == created }.grinderId)
    }

    @Test fun recentCoffeesComeFirstAndListIsCollapsed() = runTest {
        val old = r.coffees.add("Old")
        r.coffees.add("B")
        r.coffees.add("C")
        r.coffees.add("D")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        r.calibrations.save(SaveCalibration(null, old, r.builtInMethod(BuiltInMethod.MOKA).id, g, GrindSetting(0, 1), ""))
        val vm = vm()
        val s = vm.uiState.first { it.loaded && it.coffees.size == 4 }
        assertEquals("Old", s.coffees.first().name)
        assertEquals(3, s.visibleCoffees.size)
        assertEquals(1, s.hiddenCoffeeCount)
        vm.showAllCoffees()
        assertEquals(4, vm.uiState.value.visibleCoffees.size)
    }

    @Test fun addCoffeeSelectsIt() = runTest {
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.addCoffee("Kenya")
        val s = vm.uiState.first { it.coffees.any { c -> c.name == "Kenya" } && it.coffeeId != null }
        assertEquals("Kenya", s.coffees.first { it.id == s.coffeeId }.name)
    }
}
