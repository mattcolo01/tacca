package it.matteocolombo.tacca.domain

data class Coffee(val id: Long, val name: String, val createdAt: Long)

/** Display name: [customName] if set, otherwise the localized name of [builtIn]. */
data class Method(
    val id: Long,
    val customName: String?,
    val builtIn: BuiltInMethod?,
    val icon: MethodIcon,
    val tint: Tint,
    val sortOrder: Int,
)

data class Grinder(val id: Long, val name: String, val spec: GrinderSpec, val createdAt: Long)

data class Calibration(
    val id: Long,
    val coffee: Coffee,
    val method: Method,
    val grinder: Grinder,
    val setting: GrindSetting,
    val notes: String,
    val updatedAt: Long,
)
