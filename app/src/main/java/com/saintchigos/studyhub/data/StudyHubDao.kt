package com.saintchigos.studyhub.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyHubDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("SELECT * FROM courses ORDER BY code")
    fun observeCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses ORDER BY code")
    suspend fun getCourses(): List<Course>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourse(id: Long): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ClassSession>)

    @Update
    suspend fun updateSession(session: ClassSession)

    @Query("DELETE FROM class_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM class_sessions WHERE courseId = :courseId")
    suspend fun deleteSessionsForCourse(courseId: Long)

    @Query(
        """
        SELECT s.id AS sessionId, s.courseId AS courseId, c.name AS courseName,
               c.code AS courseCode, c.colorIndex AS colorIndex, s.dayOfWeek AS dayOfWeek,
               s.startMinute AS startMinute, s.endMinute AS endMinute, s.room AS room
        FROM class_sessions s
        INNER JOIN courses c ON c.id = s.courseId
        WHERE s.dayOfWeek = :dayOfWeek
        ORDER BY s.startMinute
        """
    )
    fun observeSessionsForDay(dayOfWeek: Int): Flow<List<SessionWithCourse>>

    @Query("SELECT COUNT(*) FROM class_sessions WHERE courseId = :courseId")
    suspend fun countSessionsForCourse(courseId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long

    @Update
    suspend fun updateAssignment(assignment: Assignment)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignment(id: Long)

    @Query(
        """
        SELECT a.id AS id, a.title AS title, a.dueAt AS dueAt, a.isDone AS isDone,
               a.priority AS priority, c.name AS courseName, c.code AS courseCode,
               c.colorIndex AS colorIndex
        FROM assignments a
        INNER JOIN courses c ON c.id = a.courseId
        ORDER BY a.isDone, a.dueAt
        """
    )
    fun observeAssignments(): Flow<List<AssignmentWithCourse>>

    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun getAssignment(id: Long): Assignment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Update
    suspend fun updateExam(exam: Exam)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExam(id: Long)

    @Query(
        """
        SELECT e.id AS id, e.title AS title, e.startsAt AS startsAt,
               e.durationMinutes AS durationMinutes, e.room AS room, e.notes AS notes,
               c.name AS courseName, c.code AS courseCode, c.colorIndex AS colorIndex
        FROM exams e
        INNER JOIN courses c ON c.id = e.courseId
        WHERE e.startsAt >= :now
        ORDER BY e.startsAt
        """
    )
    fun observeUpcomingExams(now: Long): Flow<List<ExamWithCourse>>

    @Query("SELECT COUNT(*) FROM assignments WHERE isDone = 0 AND dueAt < :now")
    fun observeOverdueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM assignments WHERE isDone = 0 AND dueAt >= :now")
    fun observePendingCount(now: Long): Flow<Int>
}