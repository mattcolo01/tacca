package it.matteocolombo.tacca.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class DialPoint(val x: Float, val y: Float)

data class DialTick(val inner: DialPoint, val outer: DialPoint, val major: Boolean, val active: Boolean)

data class DialModel(
    val ticks: List<DialTick>,
    /** Null when steps per turn are unknown. */
    val pointer: DialPoint?,
    val ringRadii: List<Float>,
    /** Turns beyond [DialGeometry.MAX_RINGS] that are not drawn as rings. */
    val hiddenTurns: Int,
    val outerRadius: Float,
)

/** Geometry of the mockup dial (designed on a 260 px square), scaled to [size]. */
object DialGeometry {
    const val DECORATIVE_TICKS = 40
    const val MAX_RINGS = 5
    private const val REFERENCE_SIZE = 260f

    fun build(size: Float, setting: GrindSetting, stepsPerTurn: Int?): DialModel {
        val scale = size / REFERENCE_SIZE
        val center = size / 2f
        val known = stepsPerTurn != null
        val count = stepsPerTurn ?: DECORATIVE_TICKS
        val outer = 118f * scale
        val ticks = (0 until count).map { i ->
            val a = angle(i.toFloat() / count)
            val major = i % 5 == 0
            val inner = (if (major) 102f else 108f) * scale
            DialTick(point(center, inner, a), point(center, outer, a), major, known && i <= setting.steps)
        }
        val pointer = if (known) {
            point(center, outer, angle((setting.steps.toFloat() / count).coerceIn(0f, 1f)))
        } else {
            null
        }
        val drawnRings = setting.turns.coerceIn(0, MAX_RINGS)
        val rings = (0 until drawnRings).map { k -> (90f - k * 9f) * scale }
        return DialModel(ticks, pointer, rings, (setting.turns - MAX_RINGS).coerceAtLeast(0), outer)
    }

    /** 0 = twelve o'clock, clockwise. */
    private fun angle(fraction: Float): Float = (fraction * 2.0 * PI - PI / 2).toFloat()

    private fun point(center: Float, radius: Float, angle: Float) =
        DialPoint(center + cos(angle) * radius, center + sin(angle) * radius)
}
