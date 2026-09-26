package it.matteocolombo.tacca.ui.home

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm() = HomeViewModel(r.calibrations, r.methods, r.coffees, r.grinders, r.homeFilter) { m ->
        m.customName ?: when (m.builtIn) {
            BuiltInMethod.FRENCH_PRESS -> "French press"
            else -> m.builtIn?.name.orEmpty()
        }
    }

    private suspend fun seed(): Map<String, Long> {
        val etiopia = r.coffees.add("Etiopia")
        val brasile = r.coffees.add("Brasile")
        val jx = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        val v60 = r.builtInMethod(BuiltInMethod.V60).id
        val french = r.builtInMethod(BuiltInMethod.FRENCH_PRESS).id
        val moka = r.builtInMethod(BuiltInMethod.MOKA).id
        r.calibrations.save(SaveCalibration(null, etiopia, french, jx, GrindSetting(3, 5), ""))
        r.calibrations.save(SaveCalibration(null, etiopia, v60, jx, GrindSetting(2, 12), ""))
        r.calibrations.save(SaveCalibration(null, brasile, moka, jx, GrindSetting(1, 30), ""))
        return mapOf("etiopia" to etiopia, "brasile" to brasile, "v60" to v60, "moka" to moka)
    }

    @Test fun groupsByCoffeeMostRecentFirstAndRowsByMethodOrder() = runTest {
        seed()
        val s = vm().uiState.first { !it.loading && it.groups.size == 2 }
        assertEquals(listOf("Brasile", "Etiopia"), s.groups.map { it.coffee.name })
        assertEquals(listOf(BuiltInMethod.V60, BuiltInMethod.FRENCH_PRESS), s.groups[1].rows.map { it.method.builtIn })
        assertEquals(listOf(BuiltInMethod.MOKA, BuiltInMethod.V60, BuiltInMethod.FRENCH_PRESS), s.chips.map { it.builtIn })
    }

    @Test fun methodFilterAndSearch() = runTest {
        val ids = seed()
        val vm = vm()
        vm.selectMethod(ids.getValue("v60"))
        val filtered = vm.uiState.first { it.selectedMethodId == ids.getValue("v60") && !it.loading }
        assertEquals(1, filtered.groups.sumOf { it.rows.size })
        vm.clearFilters()
        vm.onQueryChange("french")
        val searched = vm.uiState.first { it.query == "french" }
        assertEquals(listOf("Etiopia"), searched.groups.map { it.coffee.name })
        assertTrue(searched.searchOpen)
    }

    @Test fun filterOnDeletedCoffeeIsIgnored() = runTest {
        val ids = seed()
        val vm = vm()
        r.homeFilter.showCoffee(ids.getValue("etiopia"))
        vm.uiState.first { it.coffeeFilter?.id == ids.getValue("etiopia") }
        r.coffees.delete(ids.getValue("etiopia"))
        val s = vm.uiState.first { it.groups.size == 1 && it.coffeeFilter == null }
        assertNull(s.coffeeFilter)
        assertEquals("Brasile", s.groups.single().coffee.name)
    }

    @Test fun needsSetupWithoutGrinders() = runTest {
        r.coffees.add("Etiopia")
        val s = vm().uiState.first { !it.loading }
        assertTrue(s.needsSetup)
        assertEquals(false, s.hasCalibrations)
    }
}
