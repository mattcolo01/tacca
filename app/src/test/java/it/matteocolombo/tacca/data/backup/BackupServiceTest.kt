package it.matteocolombo.tacca.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BackupServiceTest {
    private val source = TestRepos()
    private val target = TestRepos()

    @After fun tearDown() {
        source.db.close()
        target.db.close()
    }

    private suspend fun populate() {
        val c = source.coffees.add("Etiopia — lavato")
        val m = source.builtInMethod(BuiltInMethod.V60).id
        val g = source.grinders.add("JX-Pro", GrinderSpec(true, 40))
        source.methods.add("Chemex", MethodIcon.CUP, Tint.LILAC)
        source.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(2, 12), "18g"))
    }

    @Test fun exportThenImportRestoresIdenticalData() = runTest {
        populate()
        target.coffees.add("to be replaced")
        val out = ByteArrayOutputStream()
        BackupService(source.db).export(out)
        BackupService(target.db).import(ByteArrayInputStream(out.toByteArray()))

        assertEquals(source.db.coffeeDao().getAll(), target.db.coffeeDao().getAll())
        assertEquals(source.db.methodDao().getAll(), target.db.methodDao().getAll())
        assertEquals(source.db.grinderDao().getAll(), target.db.grinderDao().getAll())
        // Calibration ids are not part of the backup format; compare everything else.
        assertEquals(
            source.db.calibrationDao().getAll().map { it.copy(id = 0) },
            target.db.calibrationDao().getAll().map { it.copy(id = 0) },
        )
    }

    @Test fun invalidImportLeavesDataUntouched() = runTest {
        target.coffees.add("keep me")
        try {
            BackupService(target.db).import(ByteArrayInputStream("{\"format\":\"tacca-backup\",\"version\":1}".toByteArray()))
            fail("expected BackupException")
        } catch (e: BackupException) {
            assertEquals(BackupError.INVALID_DATA, e.error)
        }
        assertEquals(listOf("keep me"), target.db.coffeeDao().getAll().map { it.name })
        assertEquals(5, target.db.methodDao().getAll().size)
    }

    @Test fun exportUsesFormatAndVersion() = runTest {
        val file = BackupService(source.db, clock = { 0L }).createBackup()
        assertEquals("tacca-backup", file.format)
        assertEquals(1, file.version)
        assertEquals("1970-01-01T00:00:00Z", file.exportedAt)
        assertEquals(5, file.methods.size)
    }
}
