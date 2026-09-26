package it.matteocolombo.tacca.domain

object SettingRules {
    const val MIN_STEPS_PER_TURN = 1
    const val MAX_STEPS_PER_TURN = 500
    const val DEFAULT_STEPS_PER_TURN = 40

    /** Highest allowed steps value, or null when unbounded. */
    fun maxSteps(spec: GrinderSpec): Int? {
        val n = spec.stepsPerTurn ?: return null
        return if (spec.multiTurn) n - 1 else n
    }

    fun clamp(setting: GrindSetting, spec: GrinderSpec): GrindSetting {
        val turns = if (spec.multiTurn) setting.turns.coerceAtLeast(0) else 0
        val steps = setting.steps.coerceIn(0, maxSteps(spec) ?: Int.MAX_VALUE)
        return GrindSetting(turns, steps)
    }

    fun isInRange(setting: GrindSetting, spec: GrinderSpec): Boolean = clamp(setting, spec) == setting

    /** Whether the turns value is shown for this setting on this grinder. */
    fun showsTurns(setting: GrindSetting, spec: GrinderSpec): Boolean = spec.multiTurn || setting.turns > 0

    fun isValidStepsPerTurn(n: Int?): Boolean = n == null || n in MIN_STEPS_PER_TURN..MAX_STEPS_PER_TURN
}
