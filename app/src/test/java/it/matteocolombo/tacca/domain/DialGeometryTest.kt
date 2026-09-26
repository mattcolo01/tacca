package it.matteocolombo.tacca.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialGeometryTest {
    private val eps = 0.01f

    @Test fun knownStepsPerTurnMatchesMockup() {
        val m = DialGeometry.build(260f, GrindSetting(2, 12), 40)
        assertEquals(40, m.ticks.size)
        assertEquals(13, m.ticks.count { it.active }) // ticks 0..12 inclusive
        assertEquals(130f, m.ticks[0].outer.x, eps)
        assertEquals(12f, m.ticks[0].outer.y, eps)
        assertTrue(m.ticks[0].major)
        assertEquals(false, m.ticks[1].major)
        val p = m.pointer!!
        assertEquals(242.22f, p.x, eps)
        assertEquals(166.46f, p.y, eps)
        assertEquals(listOf(90f, 81f), m.ringRadii)
        assertEquals(0, m.hiddenTurns)
    }

    @Test fun scalesWithSize() {
        val m = DialGeometry.build(520f, GrindSetting(0, 0), 40)
        assertEquals(236f, m.outerRadius, eps)
        assertEquals(24f, m.ticks[0].outer.y, eps)
    }

    @Test fun unknownStepsPerTurnIsDecorative() {
        val m = DialGeometry.build(260f, GrindSetting(1, 17), null)
        assertEquals(DialGeometry.DECORATIVE_TICKS, m.ticks.size)
        assertEquals(0, m.ticks.count { it.active })
        assertNull(m.pointer)
        assertEquals(1, m.ringRadii.size)
    }

    @Test fun ringsAreCappedAndOverflowCounted() {
        val m = DialGeometry.build(260f, GrindSetting(7, 0), 40)
        assertEquals(DialGeometry.MAX_RINGS, m.ringRadii.size)
        assertEquals(2, m.hiddenTurns)
    }

    @Test fun outOfRangeStepsFillTheDial() {
        val m = DialGeometry.build(260f, GrindSetting(0, 45), 40)
        assertEquals(40, m.ticks.count { it.active })
        assertEquals(130f, m.pointer!!.x, eps)
        assertEquals(12f, m.pointer!!.y, eps)
    }
}
