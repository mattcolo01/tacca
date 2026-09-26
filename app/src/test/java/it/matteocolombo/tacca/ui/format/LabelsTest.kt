package it.matteocolombo.tacca.ui.format

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LabelsTest {
    private val res: Resources get() = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val moka = Method(1, null, BuiltInMethod.MOKA, MethodIcon.MOKA, Tint.PEACH, 0)

    @Test fun englishSettings() {
        assertEquals("2 turns + 12 steps", settingText(res, GrindSetting(2, 12)))
        assertEquals("1 turn + 1 step", settingText(res, GrindSetting(1, 1)))
        assertEquals("12 steps", settingText(res, GrindSetting(0, 12)))
        assertEquals("0 steps", settingText(res, GrindSetting.ZERO))
    }

    @Test @Config(qualifiers = "it")
    fun italianSettings() {
        assertEquals("2 giri + 12 step", settingText(res, GrindSetting(2, 12)))
        assertEquals("1 giro + 1 step", settingText(res, GrindSetting(1, 1)))
    }

    @Test @Config(qualifiers = "de")
    fun germanSettings() {
        assertEquals("2 Umdrehungen + 12 Klicks", settingText(res, GrindSetting(2, 12)))
    }

    @Test fun methodNames() {
        assertEquals("Moka", methodName(res, moka))
        assertEquals("Bialetti", methodName(res, moka.copy(customName = "Bialetti")))
        assertEquals("French press", methodName(res, moka.copy(builtIn = BuiltInMethod.FRENCH_PRESS)))
    }

    @Test @Config(qualifiers = "fr")
    fun frenchBuiltInName() {
        assertEquals("Cafetière à piston", methodName(res, moka.copy(builtIn = BuiltInMethod.FRENCH_PRESS)))
    }

    @Test fun grinderKinds() {
        assertEquals("Multi-turn · 40/turn", grinderKindText(res, GrinderSpec(true, 40)))
        assertEquals("Single turn · 40 steps", grinderKindText(res, GrinderSpec(false, 40)))
        assertEquals("Multi-turn · steps unknown", grinderKindText(res, GrinderSpec(true, null)))
        assertEquals("Single turn · steps unknown", grinderKindText(res, GrinderSpec(false, null)))
    }
}
