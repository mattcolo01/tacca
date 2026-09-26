package it.matteocolombo.tacca.ui.grindereditor

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.SettingRules
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrinderEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm(id: Long? = null) = GrinderEditorViewModel(SavedStateHandle(id?.let { mapOf("grinderId" to it) } ?: emptyMap()), r.grinders)

    @Test fun newGrinderDefaults() = runTest {
        val s = vm().uiState.first { it.loaded }
        assertTrue(s.isNew)
        assertTrue(s.multiTurn)
        assertEquals(SettingRules.DEFAULT_STEPS_PER_TURN, s.stepsPerTurn)
        assertEquals(GrindSetting(2, 12), s.preview)
    }

    @Test fun saveNewCreatesGrinder() = runTest {
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.onNameChange("  JX-Pro ")
        vm.onMultiTurnChange(false)
        vm.save()
        val id = vm.uiState.first { it.savedId != null }.savedId!!
        val g = r.grinders.get(id)!!
        assertEquals("JX-Pro", g.name)
        assertEquals(GrinderSpec(false, 40), g.spec)
    }

    @Test fun editLoadsAndWarnsAboutOutOfRange() = runTest {
        val g = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        val c = r.coffees.add("Brasile")
        r.calibrations.save(SaveCalibration(null, c, r.builtInMethod(BuiltInMethod.MOKA).id, g, GrindSetting(1, 30), ""))
        val vm = vm(g)
        val loaded = vm.uiState.first { it.loaded }
        assertEquals("JX-Pro", loaded.name)
        assertEquals(1, loaded.calibrationCount)
        vm.onMultiTurnChange(false)
        assertEquals(1, vm.uiState.first { it.outOfRangeCount == 1 }.outOfRangeCount)
        vm.onMultiTurnChange(true)
        assertEquals(0, vm.uiState.first { it.outOfRangeCount == 0 }.outOfRangeCount)
    }

    @Test fun stepsGoToUnknownBelowOneAndBackToDefault() = runTest {
        val vm = vm()
        vm.uiState.first { it.loaded }
        repeat(40) { vm.decrementSteps() }
        assertNull(vm.uiState.value.stepsPerTurn)
        vm.incrementSteps()
        assertEquals(40, vm.uiState.value.stepsPerTurn)
        vm.setStepsUnknown()
        assertNull(vm.uiState.value.stepsPerTurn)
    }

    @Test fun flagsDuplicateNames() = runTest {
        r.grinders.add("Encore", GrinderSpec(false, 40))
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.onNameChange("encore ")
        assertTrue(vm.uiState.value.isDuplicateName)
    }

    @Test fun deleteRemovesGrinder() = runTest {
        val g = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        val vm = vm(g)
        vm.uiState.first { it.loaded }
        vm.delete()
        vm.uiState.first { it.deleted }
        assertNull(r.grinders.get(g))
    }
}
