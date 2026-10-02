package com.saintchigos.studyhub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Course::class,
        ClassSession::class,
        Assignment::class,
        Exam::class,
        Programme::class,
        ProgrammePlan::class,
        PlanCourse::class,
        PlanSession::class,
        AppliedPlan::class
    ],
    version = 2,
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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }

        /**
         * Adds the programme tables. The timetable is the user's real data, so this is
         * a real migration rather than a destructive fallback that would wipe it.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `programmes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `isCustom` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `programme_plans` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`programmeId` INTEGER NOT NULL, " +
                        "`year` INTEGER NOT NULL, `semester` TEXT NOT NULL, " +
                        "FOREIGN KEY(`programmeId`) REFERENCES `programmes`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_programme_plans_programmeId` " +
                        "ON `programme_plans` (`programmeId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `plan_courses` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`planId` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`code` TEXT NOT NULL, `credits` INTEGER NOT NULL, " +
                        "`colorIndex` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`planId`) REFERENCES `programme_plans`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_plan_courses_planId` " +
                        "ON `plan_courses` (`planId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `plan_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`planCourseId` INTEGER NOT NULL, `dayOfWeek` INTEGER NOT NULL, " +
                        "`startMinute` INTEGER NOT NULL, `endMinute` INTEGER NOT NULL, " +
                        "`room` TEXT NOT NULL, " +
                        "FOREIGN KEY(`planCourseId`) REFERENCES `plan_courses`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_plan_sessions_planCourseId` " +
                        "ON `plan_sessions` (`planCourseId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `applied_plans` (" +
                        "`planId` INTEGER PRIMARY KEY NOT NULL)"
                )
            }
        }

        /** Inserts the built-in catalogue once, so the picker is never empty. */
        suspend fun seedCatalogueIfEmpty(dao: StudyHubDao) {
            if (dao.countProgrammes() > 0) return
            for (seed in PlanCatalogue.seeds) {
                    val programmeId = dao.insertProgramme(
                        Programme(name = seed.programme, isCustom = false)
                    )
                    val planId = dao.insertPlan(
                        ProgrammePlan(
                            programmeId = programmeId,
                            year = seed.year,
                            semester = seed.semester
                        )
                    )
                    val courseIds = dao.insertPlanCourses(
                        seed.courses.map {
                            PlanCourse(
                                planId = planId,
                                name = it.name,
                                code = it.code,
                                credits = it.credits,
                                colorIndex = it.colorIndex
                            )
                        }
                    )
                    val sessionRows = mutableListOf<PlanSession>()
                    seed.courses.forEachIndexed { index, course ->
                        val planCourseId = courseIds[index]
                        course.sessions.forEach {
                            sessionRows.add(
                                PlanSession(
                                    planCourseId = planCourseId,
                                    dayOfWeek = it.dayOfWeek,
                                    startMinute = it.startMinute,
                                    endMinute = it.endMinute,
                                    room = it.room
                                )
                            )
                        }
                    }
                    if (sessionRows.isNotEmpty()) dao.insertPlanSessions(sessionRows)
                }
            }

        /**
         * Copies a plan into the student's timetable without destroying anything:
         * courses are matched by code and only missing class times are inserted.
         */
        suspend fun applyPlan(dao: StudyHubDao, planId: Long) {
            val planCourses = dao.getPlanCourses(planId)
            val planSessions = dao.getPlanSessions(planId)
            val codeByPlanCourseId = planCourses.associate { it.id to it.code }
            val templateByCode = planSessions.groupBy { codeByPlanCourseId[it.planCourseId] }

            for (planCourse in planCourses) {
                val existing = dao.getCourseByCode(planCourse.code)
                val courseId = existing?.id ?: dao.insertCourse(
                    Course(
                        name = planCourse.name,
                        code = planCourse.code,
                        credits = planCourse.credits,
                        colorIndex = planCourse.colorIndex
                    )
                )
                val mine = dao.getSessionsForCourse(courseId)
                val toAdd = templateByCode[planCourse.code].orEmpty()
                    .filter { template ->
                        mine.none {
                            it.dayOfWeek == template.dayOfWeek &&
                                it.startMinute == template.startMinute &&
                                it.endMinute == template.endMinute
                        }
                    }
                    .map { template ->
                        ClassSession(
                            courseId = courseId,
                            dayOfWeek = template.dayOfWeek,
                            startMinute = template.startMinute,
                            endMinute = template.endMinute,
                            room = template.room
                        )
                    }
                if (toAdd.isNotEmpty()) dao.insertSessions(toAdd)
            }
            dao.markPlanAppliedRow(AppliedPlan(planId))
        }

        /**
         * Undoes a plan: removes only the class times the plan itself created and
         * deletes a course only once nothing references it any more.
         *
         * [keepCourses] leaves the courses behind, which is what ending a semester
         * needs so old assignments and exams still have a home.
         */
        suspend fun removePlan(dao: StudyHubDao, planId: Long, keepCourses: Boolean = false) {
            val planCourses = dao.getPlanCourses(planId)
            val planSessions = dao.getPlanSessions(planId)
            val codeByPlanCourseId = planCourses.associate { it.id to it.code }
            val templateByCode = planSessions.groupBy { codeByPlanCourseId[it.planCourseId] }

            for (planCourse in planCourses) {
                val course = dao.getCourseByCode(planCourse.code) ?: continue
                val mine = dao.getSessionsForCourse(course.id)
                for (template in templateByCode[planCourse.code].orEmpty()) {
                    val match = mine.firstOrNull {
                        it.dayOfWeek == template.dayOfWeek &&
                            it.startMinute == template.startMinute &&
                            it.endMinute == template.endMinute
                    }
                    if (match != null) dao.deleteSession(match.id)
                }
                val leftSessions = dao.countSessionsForCourse(course.id)
                val leftAssignments = dao.countAssignmentsForCourse(course.id)
                val leftExams = dao.countExamsForCourse(course.id)
                if (!keepCourses && leftSessions == 0 && leftAssignments == 0 && leftExams == 0) {
                    dao.deleteCourse(course)
                }
            }
            dao.unmarkPlanApplied(planId)
        }
    }
}
