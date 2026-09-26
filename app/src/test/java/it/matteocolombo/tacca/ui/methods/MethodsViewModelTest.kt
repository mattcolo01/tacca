package it.matteocolombo.tacca.ui.methods

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.HomeFilter
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
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
class MethodsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm() = MethodsViewModel(r.methods, r.calibrations, r.homeFilter)

    @Test fun listsBuiltInsInOrder() = runTest {
        val s = vm().uiState.first { !it.loading }
        assertEquals(BuiltInMethod.entries.toList(), s.items.map { it.method.builtIn })
    }

    @Test fun addsCustomMethodAtTheEnd() = runTest {
        val vm = vm()
        vm.uiState.first { !it.loading }
        vm.openNew()
        vm.save("Chemex", MethodIcon.DROP, Tint.SKY)
        val s = vm.uiState.first { it.items.size == 6 && it.sheet == null }
        assertEquals("Chemex", s.items.last().method.customName)
    }

    @Test fun editKeepsBuiltInWhenNameIsNull() = runTest {
        val vm = vm()
        val moka = vm.uiState.first { !it.loading }.items.first().method
        vm.openEdit(moka)
        vm.uiState.first { it.sheet is MethodSheetState.Edit }
        vm.save(null, MethodIcon.MOKA, Tint.LILAC)
        val updated = vm.uiState.first { it.items.first().method.tint == Tint.LILAC }.items.first().method
        assertNull(updated.customName)
        assertEquals(BuiltInMethod.MOKA, updated.builtIn)
    }

    @Test fun showOnHome() {
        vm().showOnHome(3)
        assertEquals(HomeFilter(methodId = 3), r.homeFilter.filter.value)
    }

    @Test fun customNameForKeepsBuiltInName() {
        assertNull(customNameFor("Moka ", BuiltInMethod.MOKA, "Moka"))
        assertEquals("Bialetti", customNameFor("Bialetti", BuiltInMethod.MOKA, "Moka"))
        assertEquals("Chemex", customNameFor(" Chemex ", null, null))
    }
}
