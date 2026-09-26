package it.matteocolombo.tacca.data.db

import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint

/** A calibration joined with its coffee, method and grinder. */
data class CalibrationRow(
    val id: Long,
    val coffeeId: Long,
    val coffeeName: String,
    val coffeeCreatedAt: Long,
    val methodId: Long,
    val methodName: String?,
    val methodBuiltInKey: String?,
    val methodIcon: MethodIcon,
    val methodTint: Tint,
    val methodSortOrder: Int,
    val grinderId: Long,
    val grinderName: String,
    val grinderMultiTurn: Boolean,
    val grinderStepsPerTurn: Int?,
    val grinderCreatedAt: Long,
    val turns: Int,
    val steps: Int,
    val notes: String,
    val updatedAt: Long,
)

internal const val CALIBRATION_ROW_SELECT = """
    SELECT c.id, c.coffeeId, co.name AS coffeeName, co.createdAt AS coffeeCreatedAt,
           c.methodId, m.name AS methodName, m.builtInKey AS methodBuiltInKey, m.icon AS methodIcon,
           m.tint AS methodTint, m.sortOrder AS methodSortOrder,
           c.grinderId, g.name AS grinderName, g.multiTurn AS grinderMultiTurn,
           g.stepsPerTurn AS grinderStepsPerTurn, g.createdAt AS grinderCreatedAt,
           c.turns, c.steps, c.notes, c.updatedAt
    FROM calibration c
    JOIN coffee co ON co.id = c.coffeeId
    JOIN method m ON m.id = c.methodId
    JOIN grinder g ON g.id = c.grinderId
"""
