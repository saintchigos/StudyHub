package com.saintchigos.studyhub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Database(
    entities = [Course::class, ClassSession::class, Assignment::class, Exam::class],
    version = 1,
    exportSchema = false
)
abstract class StudyHubDatabase : RoomDatabase() {
    abstract fun dao(): StudyHubDao

    companion object {
        @Volatile
        private var instance: StudyHubDatabase? = null

        fun get(context: Context): StudyHubDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyHubDatabase::class.java,
                    "studyhub.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }

        fun seedIfEmpty(context: Context, scope: CoroutineScope) {
            val db = get(context)
            scope.launch(Dispatchers.IO) {
                val dao = db.dao()
                if (dao.getCourses().isNotEmpty()) return@launch

                val now = LocalDateTime.now()
                val base = now.toLocalDate()

                val maths = dao.insertCourse(
                    Course(name = "Mathematics", code = "MTH201", credits = 4, colorIndex = 0)
                )
                val physics = dao.insertCourse(
                    Course(name = "Physics", code = "PHY201", credits = 4, colorIndex = 1)
                )
                val english = dao.insertCourse(
                    Course(name = "English Literature", code = "ENG201", credits = 3, colorIndex = 2)
                )
                val programming = dao.insertCourse(
                    Course(name = "Computer Programming", code = "CSC201", credits = 4, colorIndex = 3)
                )

                dao.insertSessions(
                    listOf(
                        ClassSession(courseId = programming, dayOfWeek = 1, startMinute = 8 * 60, endMinute = 10 * 60, room = "Lab 3"),
                        ClassSession(courseId = maths, dayOfWeek = 1, startMinute = 14 * 60, endMinute = 16 * 60, room = "B12"),
                        ClassSession(courseId = physics, dayOfWeek = 2, startMinute = 9 * 60, endMinute = 11 * 60, room = "C04"),
                        ClassSession(courseId = english, dayOfWeek = 2, startMinute = 13 * 60, endMinute = 15 * 60, room = "A21"),
                        ClassSession(courseId = maths, dayOfWeek = 3, startMinute = 8 * 60, endMinute = 10 * 60, room = "B12"),
                        ClassSession(courseId = programming, dayOfWeek = 4, startMinute = 11 * 60, endMinute = 13 * 60, room = "Lab 3"),
                        ClassSession(courseId = english, dayOfWeek = 5, startMinute = 10 * 60, endMinute = 12 * 60, room = "A21")
                    )
                )

                val in3days = base.plusDays(3).atTime(23, 59).atZone(java.time.ZoneId.systemDefault())
                val in5days = base.plusDays(5).atTime(23, 59).atZone(java.time.ZoneId.systemDefault())
                val in9days = base.plusDays(9).atTime(23, 59).atZone(java.time.ZoneId.systemDefault())
                val nextDay = base.plusDays(1).atTime(20, 0).atZone(java.time.ZoneId.systemDefault())

                dao.insertAssignment(
                    Assignment(
                        courseId = programming,
                        title = "Lab report: linked lists",
                        dueAt = nextDay.toInstant().toEpochMilli(),
                        priority = 2
                    )
                )
                dao.insertAssignment(
                    Assignment(courseId = maths, title = "Problem set 4", dueAt = in3days.toInstant().toEpochMilli(), priority = 2)
                )
                dao.insertAssignment(
                    Assignment(courseId = english, title = "Read chapter 7 and write a summary", dueAt = in5days.toInstant().toEpochMilli())
                )
                dao.insertAssignment(
                    Assignment(courseId = physics, title = "Momentum worksheet", dueAt = in9days.toInstant().toEpochMilli(), priority = 0)
                )

                val examDay = base.plusDays(12).atTime(9, 0)
                dao.insertExam(
                    Exam(
                        courseId = maths,
                        title = "Midterm Examination",
                        startsAt = examDay.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        durationMinutes = 120,
                        room = "Hall 2",
                        notes = "Covers chapters 1 to 6. Calculator not allowed."
                    )
                )
                dao.insertExam(
                    Exam(
                        courseId = physics,
                        title = "Practical Assessment",
                        startsAt = base.plusDays(19).atTime(14, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        durationMinutes = 60,
                        room = "Physics Lab"
                    )
                )
                dao.insertExam(
                    Exam(
                        courseId = programming,
                        title = "Final Project Demo",
                        startsAt = base.plusDays(26).atTime(10, 30).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        durationMinutes = 45,
                        room = "Lab 3"
                    )
                )
            }
        }
    }
}