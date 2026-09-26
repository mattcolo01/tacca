package it.matteocolombo.tacca.ui.coffees

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.HomeFilter
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoffeesViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm() = CoffeesViewModel(r.coffees, r.calibrations, r.homeFilter)

    @Test fun itemsCarryCountsAndMethods() = runTest {
        val c = r.coffees.add("Etiopia")
        r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        r.calibrations.save(SaveCalibration(null, c, r.builtInMethod(BuiltInMethod.V60).id, g, GrindSetting(2, 12), ""))
        r.calibrations.save(SaveCalibration(null, c, r.builtInMethod(BuiltInMethod.MOKA).id, g, GrindSetting(1, 30), ""))
        val s = vm().uiState.first { it.items.size == 2 && it.items.any { i -> i.calibrationCount == 2 } }
        val etiopia = s.items.first { it.coffee.name == "Etiopia" }
        assertEquals(listOf(BuiltInMethod.MOKA, BuiltInMethod.V60), etiopia.methods.map { it.builtIn })
        assertEquals(2, s.totalCount)
    }

    @Test fun queryIgnoresDiacritics() = runTest {
        r.coffees.add("Colombia Finca El Paraíso")
        r.coffees.add("Brasile")
        val vm = vm()
        vm.onQueryChange("paraiso")
        val s = vm.uiState.first { it.query == "paraiso" && !it.loading }
        assertEquals(listOf("Colombia Finca El Paraíso"), s.items.map { it.coffee.name })
    }

    @Test fun doubleSaveCreatesOneCoffee() = runTest {
        val vm = vm()
        vm.uiState.first { !it.loading }
        vm.openNew()
        vm.save("Kenya")
        vm.save("Kenya")
        vm.uiState.first { it.sheet == null && !it.saving && it.totalCount >= 1 }
        assertEquals(1, r.coffees.observeCoffees().first().size)
    }

    @Test fun editRenamesAndDeleteCascades() = runTest {
        val id = r.coffees.add("Kenia")
        val vm = vm()
        val coffee = vm.uiState.first { it.items.isNotEmpty() }.items.single().coffee
        vm.openEdit(coffee)
        vm.uiState.first { it.sheet is CoffeeSheetState.Edit }
        vm.save("Kenya AA")
        vm.uiState.first { it.sheet == null && it.items.singleOrNull()?.coffee?.name == "Kenya AA" }
        vm.openEdit(r.coffees.get(id)!!)
        vm.uiState.first { it.sheet is CoffeeSheetState.Edit }
        vm.delete()
        vm.uiState.first { it.totalCount == 0 }
        assertNull(r.coffees.get(id))
    }

    @Test fun showOnHomeSetsFilter() {
        vm().showOnHome(7)
        assertEquals(HomeFilter(coffeeId = 7), r.homeFilter.filter.value)
    }
}
