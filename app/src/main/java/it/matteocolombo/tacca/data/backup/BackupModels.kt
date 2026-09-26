package it.matteocolombo.tacca.data.backup

import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import kotlinx.serialization.Serializable

@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: String,
    val coffees: List<CoffeeDto>,
    val methods: List<MethodDto>,
    val grinders: List<GrinderDto>,
    val calibrations: List<CalibrationDto>,
)

@Serializable
data class CoffeeDto(val id: Long, val name: String, val createdAt: Long)

@Serializable
data class MethodDto(
    val id: Long,
    val name: String? = null,
    val builtInKey: String? = null,
    val icon: MethodIcon,
    val tint: Tint,
    val sortOrder: Int,
)

@Serializable
data class GrinderDto(val id: Long, val name: String, val multiTurn: Boolean, val stepsPerTurn: Int? = null, val createdAt: Long)

@Serializable
data class CalibrationDto(
    val coffeeId: Long,
    val methodId: Long,
    val grinderId: Long,
    val turns: Int,
    val steps: Int,
    val notes: String = "",
    val updatedAt: Long,
)
