package com.saintchigos.studyhub.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves adding the focus timer and alarms does not disturb existing data.
 *
 * A destructive fallback would satisfy "does the new table exist" while quietly
 * wiping a student's timetable, so real rows go in at version 5 and are read back
 * after the migration, with Room validating the new schema against the entities.
 */
@RunWith(AndroidJUnit4::class)
class Migration5To6Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudyHubDatabase::class.java,
        emptyList()
    )

    @Test
    fun focusAndAlarmTablesAppearWithoutLosingExistingWork() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                "INSERT INTO courses (name, code, credits, colorIndex, syncId, updatedAt, deleted) " +
                    "VALUES ('Calculus', 'MATH101', 3, 0, 'seed-1', 1, 0)"
            )
            val courseId = queryFirstLong("SELECT id FROM courses WHERE code = 'MATH101'")
            execSQL(
                "INSERT INTO assignments (courseId, title, dueAt, isDone, priority, syncId, updatedAt, deleted) " +
                    "VALUES ($courseId, 'Problem set 1', 1000, 0, 1, 'seed-2', 1, 0)"
            )
            close()
        }

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            StudyHubDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                StudyHubDatabase.MIGRATION_6_7,
                StudyHubDatabase.MIGRATION_5_6,
                StudyHubDatabase.MIGRATION_4_5
            )
            .allowMainThreadQueries()
            .build()
        val dao = db.dao()

        // Existing student data survived the upgrade.
        assertEquals(1, runBlocking { dao.getCourses().size })
        assertEquals("Calculus", runBlocking { dao.getCourseByCode("MATH101") }?.name)
        assertEquals("seed-1", runBlocking { dao.getCourseByCode("MATH101") }?.syncId)

        // The new tables work, with the defaults the app relies on.
        assertTrue(runBlocking { dao.observeRecentFocusSessions(10).first() }.isEmpty())
        runBlocking {
            dao.insertFocusSession(
                FocusSession(
                    label = "Deep work",
                    courseCode = "MATH101",
                    plannedMinutes = 25,
                    actualMinutes = 25,
                    startedAt = 10,
                    finishedAt = 1510,
                    completed = true
                )
            )
        }
        val stored = runBlocking { dao.observeRecentFocusSessions(10).first() }.single()
        assertEquals("Deep work", stored.label)
        assertEquals(25, stored.actualMinutes)
        assertTrue("syncId default was empty", stored.syncId.isNotBlank())
        assertEquals(0, stored.deleted)

        val alarmId = runBlocking {
            dao.insertAlarm(
                DailyAlarm(label = "Wake up", minuteOfDay = 390, daysMask = DailyAlarm.WEEKDAYS)
            )
        }
        val alarm = runBlocking { dao.getAlarm(alarmId) }
        assertNotNull(alarm)
        assertEquals("Wake up", alarm?.label)
        assertEquals(390, alarm?.minuteOfDay)
        assertEquals(31, alarm?.daysMask)

        db.close()
    }

    private fun SupportSQLiteDatabase.queryFirstLong(sql: String): Long =
        query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private companion object {
        const val TEST_DB = "migration-5-6-test"
    }
}
