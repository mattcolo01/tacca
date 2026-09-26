package it.matteocolombo.tacca.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint

@Entity(tableName = "coffee")
data class CoffeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "method")
data class MethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Null for a built-in method that keeps its localized name. */
    val name: String?,
    val builtInKey: String?,
    val icon: MethodIcon,
    val tint: Tint,
    val sortOrder: Int,
)

@Entity(tableName = "grinder")
data class GrinderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val multiTurn: Boolean,
    val stepsPerTurn: Int?,
    val createdAt: Long,
)

@Entity(
    tableName = "calibration",
    foreignKeys = [
        ForeignKey(entity = CoffeeEntity::class, parentColumns = ["id"], childColumns = ["coffeeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MethodEntity::class, parentColumns = ["id"], childColumns = ["methodId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GrinderEntity::class, parentColumns = ["id"], childColumns = ["grinderId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["coffeeId", "methodId", "grinderId"], unique = true),
        Index(value = ["methodId"]),
        Index(value = ["grinderId"]),
    ],
)
data class CalibrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coffeeId: Long,
    val methodId: Long,
    val grinderId: Long,
    val turns: Int,
    val steps: Int,
    val notes: String = "",
    val updatedAt: Long,
)
