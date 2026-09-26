package it.matteocolombo.tacca.data.db

import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method

fun CoffeeEntity.toModel() = Coffee(id, name, createdAt)

fun MethodEntity.toModel() = Method(id, name, BuiltInMethod.fromKey(builtInKey), icon, tint, sortOrder)

fun GrinderEntity.toModel() = Grinder(id, name, GrinderSpec(multiTurn, stepsPerTurn), createdAt)

fun CalibrationRow.toModel() = Calibration(
    id = id,
    coffee = Coffee(coffeeId, coffeeName, coffeeCreatedAt),
    method = Method(methodId, methodName, BuiltInMethod.fromKey(methodBuiltInKey), methodIcon, methodTint, methodSortOrder),
    grinder = Grinder(grinderId, grinderName, GrinderSpec(grinderMultiTurn, grinderStepsPerTurn), grinderCreatedAt),
    setting = GrindSetting(turns, steps),
    notes = notes,
    updatedAt = updatedAt,
)
