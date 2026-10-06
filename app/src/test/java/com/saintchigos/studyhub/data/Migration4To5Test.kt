package com.saintchigos.studyhub.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the sync-metadata migration does not lose or corrupt a student's work.
 *
 * The failure this guards against is silent: if the backfill left `syncId` blank on
 * existing rows, the app would still launch and look correct, then the first sync
 * would collide every row together. So real rows go into a version 4 database, the
 * real migration runs, and Room then validates the result against the entities.
 */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudyHubDatabase::class.java,
        emptyList()
    )

    @Test
    fun syncColumnsAreAddedAndBackfilledWithoutLosingRows() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO courses (name, code, credits, colorIndex) " +
                    "VALUES ('Calculus', 'MATH101', 3, 0)"
            )
            execSQL(
                "INSERT INTO courses (name, code, credits, colorIndex) " +
                    "VALUES ('Physics', 'PHYS102', 3, 1)"
            )
            execSQL(
                "INSERT INTO courses (name, code, credits, colorIndex) " +
                    "VALUES ('Statistics', 'STAT103', 3, 2)"
            )
            val courseId = queryFirstLong("SELECT id FROM courses WHERE code = 'MATH101'")
            execSQL(
                "INSERT INTO class_sessions (courseId, dayOfWeek, startMinute, endMinute, room) " +
                    "VALUES ($courseId, 1, 540, 600, 'LT1')"
            )
            execSQL(
                "INSERT INTO assignments (courseId, title, dueAt, isDone, priority) " +
                    "VALUES ($courseId, 'Problem set 1', 1000, 0, 1)"
            )
            execSQL(
                "INSERT INTO exams (courseId, title, startsAt, durationMinutes, room, notes) " +
                    "VALUES ($courseId, 'Midterm', 2000, 90, 'Hall B', 'cover sheet')"
            )
            close()
        }

        // Opening with Room runs the real migration and validates the resulting
        // schema against the Kotlin entities, so a column added in one place but
        // not the other fails here instead of at a student's first launch.
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            StudyHubDatabase::class.java,
            TEST_DB
        )
            // Both steps are needed: the database is opened at the current version,
            // so Room walks 4 -> 5 -> 6 and would refuse a gap.
            .addMigrations(
                                StudyHubDatabase.MIGRATION_7_8,
StudyHubDatabase.MIGRATION_6_7,
                StudyHubDatabase.MIGRATION_5_6,
                StudyHubDatabase.MIGRATION_4_5
            )
            .allowMainThreadQueries()
            .build()

        runBlocking {
            val courses = db.dao().getCourses()
            val math = db.dao().getCourseByCode("MATH101")

            // Nothing was lost.
            assertEquals(3, courses.size)
            assertNotNull(math)
            assertEquals("Calculus", math?.name)

            // Every row got its own sync identity, and none were left blank. A shared
            // '' default would leave blank ids that collide on the unique index.
            val syncIds = courses.map { it.syncId }
            assertEquals(3, syncIds.size)
            assertTrue("syncId was left blank", syncIds.none { it.isBlank() })
            assertEquals(3, syncIds.toSet().size)
            assertTrue("updatedAt was not stamped", courses.all { it.updatedAt > 0L })
            assertTrue(courses.all { it.deleted == 0 })
        }

        db.close()
    }

    private fun SupportSQLiteDatabase.queryFirstLong(sql: String): Long =
        query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private companion object {
        const val TEST_DB = "migration-4-5-test"
    }
}
