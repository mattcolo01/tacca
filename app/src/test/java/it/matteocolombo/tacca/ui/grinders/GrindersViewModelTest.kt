package it.matteocolombo.tacca.ui.grinders

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrindersViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    @Test fun listsGrindersWithCountsAndRotatingTints() = runTest {
        val jx = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        r.grinders.add("Encore", GrinderSpec(false, 40))
        val coffee = r.coffees.add("Brasile")
        r.calibrations.save(SaveCalibration(null, coffee, r.builtInMethod(BuiltInMethod.MOKA).id, jx, GrindSetting(1, 2), ""))

        val vm = GrindersViewModel(r.grinders, r.calibrations)
        val state = vm.uiState.first { !it.loading && it.items.size == 2 && it.items[0].calibrationCount == 1 }
        assertEquals(listOf("JX-Pro", "Encore"), state.items.map { it.grinder.name })
        assertEquals(listOf(1, 0), state.items.map { it.calibrationCount })
        assertEquals(listOf(Tint.PEACH, Tint.SAND), state.items.map { it.tint })
    }
}
