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

    @Query("SELECT * FROM class_sessions WHERE id = :id")
    suspend fun getSession(id: Long): ClassSession?

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

    @Query(
        """
        SELECT s.id AS sessionId, s.courseId AS courseId, c.name AS courseName,
               c.code AS courseCode, c.colorIndex AS colorIndex, s.dayOfWeek AS dayOfWeek,
               s.startMinute AS startMinute, s.endMinute AS endMinute, s.room AS room
        FROM class_sessions s
        INNER JOIN courses c ON c.id = s.courseId
        ORDER BY s.dayOfWeek, s.startMinute
        """
    )
    fun observeAllSessions(): Flow<List<SessionWithCourse>>

    @Query(
        """
        SELECT s.id AS sessionId, s.courseId AS courseId, c.name AS courseName,
               c.code AS courseCode, c.colorIndex AS colorIndex, s.dayOfWeek AS dayOfWeek,
               s.startMinute AS startMinute, s.endMinute AS endMinute, s.room AS room
        FROM class_sessions s
        INNER JOIN courses c ON c.id = s.courseId
        ORDER BY s.dayOfWeek, s.startMinute
        """
    )
    suspend fun getAllSessions(): List<SessionWithCourse>

    @Query("SELECT COUNT(*) FROM assignments WHERE isDone = 0 AND dueAt < :now")
    fun observeOverdueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM assignments WHERE isDone = 0 AND dueAt >= :now")
    fun observePendingCount(now: Long): Flow<Int>

    // ---- programmes and plans -------------------------------------------------

    @Query("SELECT COUNT(*) FROM programmes")
    suspend fun countProgrammes(): Int

    @Insert
    suspend fun insertProgramme(programme: Programme): Long

    @Insert
    suspend fun insertPlan(plan: ProgrammePlan): Long

    @Insert
    suspend fun insertPlanCourses(courses: List<PlanCourse>): List<Long>

    @Insert
    suspend fun insertPlanSessions(sessions: List<PlanSession>)

    @Query(
        """
        SELECT p.id AS planId, g.id AS programmeId, g.name AS programmeName,
               p.year AS year, p.semester AS semester, g.isCustom AS isCustom,
               CASE WHEN a.planId IS NULL THEN 0 ELSE 1 END AS applied
        FROM programme_plans p
        INNER JOIN programmes g ON g.id = p.programmeId
        LEFT JOIN applied_plans a ON a.planId = p.id
        ORDER BY g.isCustom DESC, g.name, p.year DESC, p.semester
        """
    )
    fun observePlans(): Flow<List<PlanWithProgramme>>

    @Query("SELECT * FROM programme_plans WHERE id = :planId")
    suspend fun getPlan(planId: Long): ProgrammePlan?

    @Query("SELECT * FROM programme_plans")
    suspend fun getAllPlans(): List<ProgrammePlan>

    @Query("SELECT * FROM plan_courses WHERE planId = :planId ORDER BY code")
    suspend fun getPlanCourses(planId: Long): List<PlanCourse>

    @Query("SELECT COUNT(*) FROM plan_courses WHERE planId = :planId")
    suspend fun countPlanCourses(planId: Long): Int

    @Query(
        """
        SELECT ps.* FROM plan_sessions ps
        INNER JOIN plan_courses pc ON pc.id = ps.planCourseId
        WHERE pc.planId = :planId
        ORDER BY ps.dayOfWeek, ps.startMinute
        """
    )
    suspend fun getPlanSessions(planId: Long): List<PlanSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markPlanAppliedRow(applied: AppliedPlan)

    @Query("DELETE FROM applied_plans WHERE planId = :planId")
    suspend fun unmarkPlanApplied(planId: Long)

    @Query("SELECT planId FROM applied_plans")
    suspend fun getAppliedPlanIds(): List<Long>

    @Query(
        """
        SELECT pc.planId AS planId, COUNT(DISTINCT pc.id) AS courses, COUNT(ps.id) AS sessions
        FROM plan_courses pc
        LEFT JOIN plan_sessions ps ON ps.planCourseId = pc.id
        GROUP BY pc.planId
        """
    )
    fun observePlanStats(): Flow<List<PlanStats>>

    // ---- plan application helpers --------------------------------------------

    @Query("SELECT * FROM courses WHERE code = :code LIMIT 1")
    suspend fun getCourseByCode(code: String): Course?

    @Query("SELECT * FROM class_sessions WHERE courseId = :courseId")
    suspend fun getSessionsForCourse(courseId: Long): List<ClassSession>

    @Query("SELECT COUNT(*) FROM assignments WHERE courseId = :courseId")
    suspend fun countAssignmentsForCourse(courseId: Long): Int

    @Query("SELECT COUNT(*) FROM exams WHERE courseId = :courseId")
    suspend fun countExamsForCourse(courseId: Long): Int

    // ---- bulk delete for "delete all my data" -----------------------------

    @Query("DELETE FROM class_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM assignments")
    suspend fun deleteAllAssignments()

    @Query("DELETE FROM exams")
    suspend fun deleteAllExams()

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    @Query("DELETE FROM applied_plans")
    suspend fun clearAppliedPlans()

    // ---- catalogue merging -------------------------------------------------
    // The shared catalogue is keyed by stable slugs so a released update edits an
    // existing programme in place rather than duplicating it.

    @Query("SELECT * FROM programmes WHERE slug = :slug LIMIT 1")
    suspend fun getProgrammeBySlug(slug: String): Programme?

    @Query("SELECT * FROM programme_plans WHERE slug = :slug LIMIT 1")
    suspend fun getPlanBySlug(slug: String): ProgrammePlan?

    @Query("SELECT * FROM programme_plans WHERE programmeId = :programmeId")
    suspend fun getPlansForProgramme(programmeId: Long): List<ProgrammePlan>

    @Query("SELECT * FROM programmes ORDER BY name")
    suspend fun getAllProgrammes(): List<Programme>

    @Query("UPDATE programmes SET name = :name WHERE id = :id")
    suspend fun renameProgramme(id: Long, name: String)

    @Query("UPDATE programme_plans SET year = :year, semester = :semester WHERE id = :id")
    suspend fun updatePlanDetails(id: Long, year: Int, semester: String)

    @Query("UPDATE programme_plans SET contributor = :contributor WHERE id = :id")
    suspend fun setPlanContributor(id: Long, contributor: String?)

    @Query("DELETE FROM plan_courses WHERE planId = :planId")
    suspend fun deletePlanCourses(planId: Long)

    @Query("SELECT * FROM plan_sessions WHERE planCourseId = :planCourseId ORDER BY dayOfWeek, startMinute")
    suspend fun getPlanSessionsForCourse(planCourseId: Long): List<PlanSession>
    @Query("SELECT * FROM programmes WHERE id = :id")
    suspend fun getProgramme(id: Long): Programme?
}