package it.matteocolombo.tacca.ui.icons

import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.MethodIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaccaIconsTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun everyMethodIconHasADistinctDrawable() {
        val ids = MethodIcon.entries.map { it.drawableRes() }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertNotNull(ContextCompat.getDrawable(context, it)) }
    }

    @Test fun uiIconsInflate() {
        listOf(
            R.drawable.ic_brand, R.drawable.ic_tab_calibrations, R.drawable.ic_search, R.drawable.ic_settings,
            R.drawable.ic_bean, R.drawable.ic_grinder, R.drawable.ic_multi_turn, R.drawable.ic_add, R.drawable.ic_minus,
            R.drawable.ic_close, R.drawable.ic_back, R.drawable.ic_chevron_right, R.drawable.ic_edit, R.drawable.ic_more,
            R.drawable.ic_check,
        ).forEach { assertNotNull(ContextCompat.getDrawable(context, it)) }
    }
}
