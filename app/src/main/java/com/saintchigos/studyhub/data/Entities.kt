package com.saintchigos.studyhub.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val credits: Int = 3,
    val colorIndex: Int = 0
)

@Entity(
    tableName = "class_sessions",
    foreignKeys = [ForeignKey(
        entity = Course::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("courseId")]
)
data class ClassSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String = ""
)

@Entity(
    tableName = "assignments",
    foreignKeys = [ForeignKey(
        entity = Course::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("courseId"), Index("dueAt")]
)
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val title: String,
    val dueAt: Long,
    val isDone: Boolean = false,
    val priority: Int = 1
)

@Entity(
    tableName = "exams",
    foreignKeys = [ForeignKey(
        entity = Course::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("courseId"), Index("startsAt")]
)
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val title: String,
    val startsAt: Long,
    val durationMinutes: Int = 120,
    val room: String = "",
    val notes: String = ""
)

data class SessionWithCourse(
    val sessionId: Long,
    val courseId: Long,
    val courseName: String,
    val courseCode: String,
    val colorIndex: Int,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String
)

data class AssignmentWithCourse(
    val id: Long,
    val title: String,
    val dueAt: Long,
    val isDone: Boolean,
    val priority: Int,
    val courseName: String,
    val courseCode: String,
    val colorIndex: Int
)

data class ExamWithCourse(
    val id: Long,
    val title: String,
    val startsAt: Long,
    val durationMinutes: Int,
    val room: String,
    val notes: String,
    val courseName: String,
    val courseCode: String,
    val colorIndex: Int
)