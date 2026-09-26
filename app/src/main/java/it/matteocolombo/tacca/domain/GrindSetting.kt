package it.matteocolombo.tacca.domain

/** A grinder setting as read on the dial: full turns from zero, then steps. */
data class GrindSetting(val turns: Int, val steps: Int) {
    companion object {
        val ZERO = GrindSetting(0, 0)
    }
}

/**
 * How a grinder dial is organised. For a single-turn grinder [stepsPerTurn] is the
 * total number of steps. `null` means the user does not know it.
 */
data class GrinderSpec(val multiTurn: Boolean, val stepsPerTurn: Int?)
