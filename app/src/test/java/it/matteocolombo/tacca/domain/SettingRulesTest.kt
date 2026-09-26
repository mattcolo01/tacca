package it.matteocolombo.tacca.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingRulesTest {
    private val multi40 = GrinderSpec(multiTurn = true, stepsPerTurn = 40)
    private val multiUnknown = GrinderSpec(multiTurn = true, stepsPerTurn = null)
    private val single40 = GrinderSpec(multiTurn = false, stepsPerTurn = 40)
    private val singleUnknown = GrinderSpec(multiTurn = false, stepsPerTurn = null)

    @Test fun maxStepsFollowsTheTable() {
        assertEquals(39, SettingRules.maxSteps(multi40))
        assertNull(SettingRules.maxSteps(multiUnknown))
        assertEquals(40, SettingRules.maxSteps(single40))
        assertNull(SettingRules.maxSteps(singleUnknown))
    }

    @Test fun clampMultiTurnKeepsTurnsAndCapsSteps() {
        assertEquals(GrindSetting(3, 39), SettingRules.clamp(GrindSetting(3, 55), multi40))
        assertEquals(GrindSetting(0, 0), SettingRules.clamp(GrindSetting(-1, -4), multi40))
    }

    @Test fun clampSingleTurnForcesZeroTurns() {
        assertEquals(GrindSetting(0, 40), SettingRules.clamp(GrindSetting(2, 90), single40))
    }

    @Test fun clampUnknownIsUnbounded() {
        assertEquals(GrindSetting(7, 999), SettingRules.clamp(GrindSetting(7, 999), multiUnknown))
        assertEquals(GrindSetting(0, 999), SettingRules.clamp(GrindSetting(7, 999), singleUnknown))
    }

    @Test fun isInRange() {
        assertTrue(SettingRules.isInRange(GrindSetting(2, 12), multi40))
        assertFalse(SettingRules.isInRange(GrindSetting(2, 40), multi40))
        assertFalse(SettingRules.isInRange(GrindSetting(1, 5), single40))
        assertTrue(SettingRules.isInRange(GrindSetting(0, 40), single40))
    }

    @Test fun showsTurns() {
        assertTrue(SettingRules.showsTurns(GrindSetting(0, 3), multi40))
        assertFalse(SettingRules.showsTurns(GrindSetting(0, 3), single40))
        assertTrue(SettingRules.showsTurns(GrindSetting(1, 3), single40))
    }

    @Test fun validStepsPerTurn() {
        assertTrue(SettingRules.isValidStepsPerTurn(null))
        assertTrue(SettingRules.isValidStepsPerTurn(1))
        assertTrue(SettingRules.isValidStepsPerTurn(500))
        assertFalse(SettingRules.isValidStepsPerTurn(0))
        assertFalse(SettingRules.isValidStepsPerTurn(501))
    }
}
