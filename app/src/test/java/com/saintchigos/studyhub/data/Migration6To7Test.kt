package com.saintchigos.studyhub.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves giving each alarm its own ringtone does not disturb existing alarms.
 *
 * A student's saved wake-up alarm is the one thing they will not forgive losing, so
 * real rows go in at version 6 and are read back after the migration. The new column
 * is nullable precisely so those rows need no rewriting: they come back with
 * `soundUri` null, which the alarm treats as "use the phone's own tone".
 */
@RunWith(AndroidJUnit4::class)
class Migration6To7Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudyHubDatabase::class.java,
        emptyList()
    )

    @Test
    fun existingAlarmsSurviveAndGainANullableSoundUri() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                "INSERT INTO courses (name, code, credits, colorIndex, syncId, updatedAt, deleted) " +
                    "VALUES ('Calculus', 'MATH101', 3, 0, 'seed-1', 1, 0)"
            )
            val courseId = queryFirstLong("SELECT id FROM courses WHERE code = 'MATH101'")
            execSQL(
                "INSERT INTO assignments (courseId, title, dueAt, isDone, priority, syncId, updatedAt, deleted) " +
                    "VALUES ($courseId, 'Problem set 1', 1000, 0, 1, 'seed-2', 1, 0)"
            )
            execSQL(
                "INSERT INTO daily_alarms " +
                    "(label, minuteOfDay, daysMask, enabled, vibrate, sound) " +
                    "VALUES ('Wake up Mr Chigos', 390, 31, 1, 1, 1)"
            )
            close()
        }

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            StudyHubDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                                StudyHubDatabase.MIGRATION_7_8,
StudyHubDatabase.MIGRATION_6_7,
                StudyHubDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        val dao = db.dao()

        // The alarm is untouched, and reads back with no custom tone.
        val alarm = runBlocking { dao.getAlarm(1L) }
        assertEquals("Wake up Mr Chigos", alarm?.label)
        assertEquals(390, alarm?.minuteOfDay)
        assertEquals(31, alarm?.daysMask)
        assertEquals(true, alarm?.enabled)
        assertNull("existing alarms should fall back to the system tone", alarm?.soundUri)

        // Unrelated student work is still there.
        assertEquals(1, runBlocking { dao.getCourses().size })
        assertEquals("MATH101", runBlocking { dao.getCourseByCode("MATH101") }?.code)

        // The new column works when the student does pick a tone.
        runBlocking {
            dao.updateAlarm(
                alarm!!.copy(soundUri = "content://media/external/audio/media/42")
            )
        }
        assertEquals(
            "content://media/external/audio/media/42",
            runBlocking { dao.getAlarm(1L) }?.soundUri
        )

        db.close()
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.queryFirstLong(sql: String): Long =
        query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private companion object {
        const val TEST_DB = "migration-6-7-test"
    }
}