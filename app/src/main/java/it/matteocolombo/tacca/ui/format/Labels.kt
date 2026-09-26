package it.matteocolombo.tacca.ui.format

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.data.backup.BackupError
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.domain.ThemeMode
import it.matteocolombo.tacca.domain.Tint

fun settingText(res: Resources, setting: GrindSetting): String {
    val steps = res.getQuantityString(R.plurals.steps_count, setting.steps, setting.steps)
    if (setting.turns <= 0) return steps
    val turns = res.getQuantityString(R.plurals.turns_count, setting.turns, setting.turns)
    return res.getString(R.string.setting_turns_and_steps, turns, steps)
}

@Composable @ReadOnlyComposable
fun settingText(setting: GrindSetting): String = settingText(LocalResources.current, setting)

@StringRes
fun BuiltInMethod.nameRes(): Int = when (this) {
    BuiltInMethod.MOKA -> R.string.method_moka
    BuiltInMethod.ESPRESSO -> R.string.method_espresso
    BuiltInMethod.V60 -> R.string.method_v60
    BuiltInMethod.FRENCH_PRESS -> R.string.method_french_press
    BuiltInMethod.AEROPRESS -> R.string.method_aeropress
}

fun methodName(res: Resources, method: Method): String =
    method.customName ?: method.builtIn?.let { res.getString(it.nameRes()) }.orEmpty()

@Composable @ReadOnlyComposable
fun methodName(method: Method): String = methodName(LocalResources.current, method)

/** Normalized localized display names of all built-in methods, e.g. "v60" for [BuiltInMethod.V60]. */
fun builtInMethodDisplayNames(res: Resources): Set<String> =
    BuiltInMethod.entries.map { TextMatch.normalize(res.getString(it.nameRes())) }.toSet()

@Composable @ReadOnlyComposable
fun builtInMethodDisplayNames(): Set<String> = builtInMethodDisplayNames(LocalResources.current)

fun grinderKindText(res: Resources, spec: GrinderSpec): String {
    val n = spec.stepsPerTurn
    return when {
        spec.multiTurn && n != null -> res.getString(R.string.grinder_kind_multi, n)
        spec.multiTurn -> res.getString(R.string.grinder_kind_multi_unknown)
        n != null -> res.getString(R.string.grinder_kind_single, res.getQuantityString(R.plurals.steps_count, n, n))
        else -> res.getString(R.string.grinder_kind_single_unknown)
    }
}

@Composable @ReadOnlyComposable
fun grinderKindText(spec: GrinderSpec): String = grinderKindText(LocalResources.current, spec)

@StringRes
fun MethodIcon.labelRes(): Int = when (this) {
    MethodIcon.MOKA -> R.string.method_moka
    MethodIcon.ESPRESSO -> R.string.method_espresso
    MethodIcon.V60 -> R.string.method_v60
    MethodIcon.FRENCH_PRESS -> R.string.method_french_press
    MethodIcon.AEROPRESS -> R.string.method_aeropress
    MethodIcon.DROP -> R.string.icon_drop
    MethodIcon.CUP -> R.string.icon_cup
}

@StringRes
fun Tint.labelRes(): Int = when (this) {
    Tint.PEACH -> R.string.tint_peach
    Tint.SAND -> R.string.tint_sand
    Tint.SKY -> R.string.tint_sky
    Tint.SAGE -> R.string.tint_sage
    Tint.LILAC -> R.string.tint_lilac
}

@StringRes
fun AccentOption.labelRes(): Int = when (this) {
    AccentOption.TERRACOTTA -> R.string.accent_terracotta
    AccentOption.PINE -> R.string.accent_pine
    AccentOption.INDIGO -> R.string.accent_indigo
    AccentOption.PLUM -> R.string.accent_plum
}

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
fun BackupError.messageRes(): Int = when (this) {
    BackupError.NOT_A_BACKUP -> R.string.backup_error_not_a_backup
    BackupError.NEWER_VERSION -> R.string.backup_error_newer
    BackupError.INVALID_DATA -> R.string.backup_error_invalid
    BackupError.IO -> R.string.backup_error_io
}
