package com.saintchigos.studyhub.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the notification inbox arrives without disturbing a student's work.
 *
 * The bell is a new table, so nothing existing should move. Real timetable rows go
 * in at version 7 and are read back afterwards, because a destructive fallback here
 * would wipe a timetable while still producing a working notification_log table.
 */
@RunWith(AndroidJUnit4::class)
class Migration7To8Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudyHubDatabase::class.java,
        emptyList()
    )

    @Test
    fun inboxAppearsWithoutLosingTheTimetable() {
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL(
                "INSERT INTO courses (name, code, credits, colorIndex, syncId, updatedAt, deleted) " +
                    "VALUES ('Calculus', 'MATH101', 3, 0, 'seed-1', 1, 0)"
            )
            val courseId = queryFirstLong("SELECT id FROM courses WHERE code = 'MATH101'")
            execSQL(
                "INSERT INTO class_sessions (courseId, dayOfWeek, startMinute, endMinute, room, syncId, updatedAt, deleted) " +
                    "VALUES ($courseId, 1, 540, 630, 'A12', 'seed-2', 1, 0)"
            )
            execSQL(
                "INSERT INTO daily_alarms (label, minuteOfDay, daysMask, enabled, vibrate, sound) " +
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

        // The existing timetable and alarm are untouched.
        val sessions = runBlocking { dao.observeAllSessions().first() }
        assertEquals(1, sessions.size)
        assertEquals(540, sessions.single().startMinute)
        assertEquals("Wake up Mr Chigos", runBlocking { dao.getAlarm(1L) }?.label)

        // The inbox starts empty, so the bell shows no badge on a fresh upgrade.
        assertEquals(0, runBlocking { dao.observeUnreadCount().first() })
        assertTrue(runBlocking { dao.observeNotifications(200).first() }.isEmpty())

        // Filing a reminder leaves it unread, which is what puts a badge on the bell.
        val id = runBlocking {
            dao.insertNotification(
                NotificationLog(
                    kind = NotificationLog.KIND_CLASS,
                    title = "MATH101 starts in 15 min",
                    body = "Calculus  A12  today at 09:00",
                    createdAt = 1000
                )
            )
        }
        assertEquals(1, runBlocking { dao.observeUnreadCount().first() })
        val entry = runBlocking { dao.observeNotifications(200).first() }.single()
        assertEquals(id, entry.id)
        assertNull("a fresh reminder must start unread", entry.readAt)

        // Opening the inbox clears the badge and leaves the text alone.
        runBlocking { dao.markAllNotificationsRead(2000) }
        assertEquals(0, runBlocking { dao.observeUnreadCount().first() })
        val read = runBlocking { dao.observeNotifications(200).first() }.single()
        assertEquals("MATH101 starts in 15 min", read.title)
        assertEquals(2000L, read.readAt)

        db.close()
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.queryFirstLong(sql: String): Long =
        query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private companion object {
        const val TEST_DB = "migration-7-8-test"
    }
}