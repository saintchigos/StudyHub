package com.saintchigos.studyhub.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "courses", indices = [Index(value = ["syncId"], unique = true)])
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val credits: Int = 3,
    val colorIndex: Int = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Int = 0
)

@Entity(
    tableName = "class_sessions",
    foreignKeys = [ForeignKey(
        entity = Course::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("courseId"), Index(value = ["syncId"], unique = true)]
)
data class ClassSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String = "",
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Int = 0
)

@Entity(
    tableName = "assignments",
    foreignKeys = [ForeignKey(
        entity = Course::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("courseId"), Index("dueAt"), Index(value = ["syncId"], unique = true)]
)
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val title: String,
    val dueAt: Long,
    val isDone: Boolean = false,
    val priority: Int = 1,
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Int = 0
)

@Entity(
    tableName = "exams",
    foreignKeys = [ForeignKey(
        entity = Course::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("courseId"), Index("startsAt"), Index(value = ["syncId"], unique = true)]
)
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val title: String,
    val startsAt: Long,
    val durationMinutes: Int = 120,
    val room: String = "",
    val notes: String = "",
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Int = 0
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