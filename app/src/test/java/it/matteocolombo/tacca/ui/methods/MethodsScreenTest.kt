package it.matteocolombo.tacca.ui.methods

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MethodsScreenTest {
    private val res: Resources get() = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    @Test fun newSheetIncludesEveryBuiltInName() {
        val names = sheetExistingNames(MethodsUiState(sheet = MethodSheetState.New), res)
        assertTrue("v60" in names)
        assertTrue("moka" in names)
    }

    @Test fun editingCustomMethodIncludesEveryBuiltInName() {
        val chemex = Method(1, "Chemex", null, MethodIcon.DROP, Tint.SKY, 5)
        val names = sheetExistingNames(MethodsUiState(sheet = MethodSheetState.Edit(chemex, 0)), res)
        assertEquals(BuiltInMethod.entries.size, names.size)
        assertTrue("v60" in names)
    }

    @Test fun editingARenamedBuiltInExcludesItsOwnBuiltInName() {
        // A V60 that was given the custom name "Hario": typing "V60" back into its editor should
        // revert it to the built-in default (see customNameFor), not be flagged as a duplicate.
        val renamedV60 = Method(1, "Hario", BuiltInMethod.V60, MethodIcon.V60, Tint.SKY, 2)
        val names = sheetExistingNames(MethodsUiState(sheet = MethodSheetState.Edit(renamedV60, 0)), res)
        assertFalse("v60" in names)
        // Other built-ins are still guarded against.
        assertTrue("moka" in names)
    }
}
