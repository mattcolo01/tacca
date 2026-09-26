package it.matteocolombo.tacca.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    @Test fun sameCoffeeExcludesSelfAndOtherCoffees() = runTest {
        val a = r.coffees.add("Etiopia")
        val b = r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        val v60 = r.builtInMethod(BuiltInMethod.V60).id
        val aero = r.builtInMethod(BuiltInMethod.AEROPRESS).id
        val self = r.calibrations.save(SaveCalibration(null, a, v60, g, GrindSetting(2, 12), ""))
        r.calibrations.save(SaveCalibration(null, a, aero, g, GrindSetting(0, 16), ""))
        r.calibrations.save(SaveCalibration(null, b, v60, g, GrindSetting(1, 1), ""))
        val vm = DetailViewModel(SavedStateHandle(mapOf("calibrationId" to self)), r.calibrations)
        val s = vm.uiState.first { !it.loading && it.sameCoffee.isNotEmpty() }
        assertEquals(self, s.calibration!!.id)
        assertEquals(listOf(BuiltInMethod.AEROPRESS), s.sameCoffee.map { it.method.builtIn })
    }

    @Test fun deletedCalibrationIsNotFound() = runTest {
        val vm = DetailViewModel(SavedStateHandle(mapOf("calibrationId" to 99L)), r.calibrations)
        assertTrue(vm.uiState.first { !it.loading }.notFound)
    }
}
