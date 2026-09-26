package it.matteocolombo.tacca.data.backup

import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private val valid = BackupFile(
        format = BackupCodec.FORMAT,
        version = 1,
        exportedAt = "2026-09-26T10:00:00Z",
        coffees = listOf(CoffeeDto(1, "Brasile", 10)),
        methods = listOf(MethodDto(1, null, "moka", MethodIcon.MOKA, Tint.PEACH, 0), MethodDto(2, "Chemex", null, MethodIcon.DROP, Tint.SKY, 1)),
        grinders = listOf(GrinderDto(1, "JX-Pro", true, 40, 10)),
        calibrations = listOf(CalibrationDto(1, 1, 1, 2, 12, "", 20)),
    )

    private fun expectError(expected: BackupError, text: String) {
        try {
            BackupCodec.decode(text)
            fail("expected $expected")
        } catch (e: BackupException) {
            assertEquals(expected, e.error)
        }
    }

    private fun invalid(file: BackupFile) = expectError(BackupError.INVALID_DATA, BackupCodec.encode(file))

    @Test fun roundTrip() = assertEquals(valid, BackupCodec.decode(BackupCodec.encode(valid)))

    @Test fun ignoresUnknownKeys() {
        val text = BackupCodec.encode(valid).replaceFirst("{", "{\"extra\": 1,")
        assertEquals(valid, BackupCodec.decode(text))
    }

    @Test fun notJson() = expectError(BackupError.NOT_A_BACKUP, "hello")
    @Test fun wrongFormat() = expectError(BackupError.NOT_A_BACKUP, """{"format":"other","version":1}""")
    @Test fun jsonArray() = expectError(BackupError.NOT_A_BACKUP, "[1,2]")
    @Test fun newerVersion() = expectError(BackupError.NEWER_VERSION, BackupCodec.encode(valid.copy(version = 2)))
    @Test fun missingFields() = expectError(BackupError.INVALID_DATA, """{"format":"tacca-backup","version":1}""")
    @Test fun unknownEnum() = expectError(BackupError.INVALID_DATA, BackupCodec.encode(valid).replace("\"PEACH\"", "\"NEON\""))
    @Test fun blankCoffeeName() = invalid(valid.copy(coffees = listOf(CoffeeDto(1, " ", 10))))
    @Test fun duplicateIds() = invalid(valid.copy(coffees = listOf(CoffeeDto(1, "a", 1), CoffeeDto(1, "b", 1))))
    @Test fun danglingReference() = invalid(valid.copy(calibrations = listOf(CalibrationDto(9, 1, 1, 0, 0, "", 1))))
    @Test fun negativeValues() = invalid(valid.copy(calibrations = listOf(CalibrationDto(1, 1, 1, -1, 0, "", 1))))
    @Test fun duplicateCombination() = invalid(valid.copy(calibrations = valid.calibrations + valid.calibrations))
    @Test fun badStepsPerTurn() = invalid(valid.copy(grinders = listOf(GrinderDto(1, "g", true, 0, 1))))
    @Test fun customMethodWithoutName() = invalid(valid.copy(methods = listOf(MethodDto(1, null, null, MethodIcon.CUP, Tint.SAND, 0))))
    @Test fun unknownBuiltInKey() = invalid(valid.copy(methods = listOf(MethodDto(1, null, "siphon", MethodIcon.CUP, Tint.SAND, 0))))
}
