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
import java.util.UUID

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
        AppliedPlan::class,
        Account::class,
        VerificationCode::class,
        AuthSession::class,
        AccountProgramme::class,
        Connection::class,
        Block::class,
        Report::class,
        Message::class
    ],
    version = 5,
    exportSchema = true
)
abstract class StudyHubDatabase : RoomDatabase() {
    abstract fun dao(): StudyHubDao
    abstract fun communityDao(): CommunityDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
        }

        /**
         * Adds the sync metadata columns to the student's own timetable data.
         *
         * New rows get a fresh `syncId` and `deleted = 0` from the entity
         * defaults, so cloud sync can match rows across devices and treat a deletion
         * as a tombstone instead of losing the row entirely.
         *
         * Existing rows cannot be given a per-row default from `ALTER TABLE`, so they
         * are backfilled here: a shared default would leave every pre-existing row with
         * an empty syncId and collide on the unique index. Timetable, assignment and
         * exam content is never modified, only annotated.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in SYNCED_TABLES) {
                    db.execSQL(
                        "ALTER TABLE `$table` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''"
                    )
                    db.execSQL(
                        "ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0"
                    )
                    db.execSQL(
                        "ALTER TABLE `$table` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0"
                    )
                    backfillSyncFields(db, table)
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_syncId` " +
                            "ON `$table` (`syncId`)"
                    )
                }
            }
        }

        /** Timetable tables that belong to the student and therefore sync. */
        private val SYNCED_TABLES = listOf("courses", "class_sessions", "assignments", "exams")

        /**
         * Gives each existing row its own syncId and a shared upgrade timestamp, so no
         * row is left blank and two rows can never share a sync identity.
         */
        private fun backfillSyncFields(db: SupportSQLiteDatabase, table: String) {
            val upgradedAt = System.currentTimeMillis()
            val ids = mutableListOf<Long>()
            db.query("SELECT `id` FROM `$table`").use { cursor ->
                while (cursor.moveToNext()) ids.add(cursor.getLong(0))
            }
            for (id in ids) {
                db.execSQL(
                    "UPDATE `$table` SET `syncId` = ?, `updatedAt` = ? WHERE `id` = ?",
                    arrayOf<Any>(UUID.randomUUID().toString(), upgradedAt, id)
                )
            }
        }

        /**
         * Adds the account and community tables. Existing timetable, programme and
         * catalogue rows are untouched, so upgrading never loses a student's work.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `accounts` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`username` TEXT NOT NULL, `displayName` TEXT NOT NULL, " +
                        "`passwordHash` TEXT NOT NULL, `verified` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_username` " +
                        "ON `accounts` (`username`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `verification_codes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`username` TEXT NOT NULL, `codeHash` TEXT NOT NULL, " +
                        "`expiresAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, " +
                        "`consumed` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_verification_codes_username` " +
                        "ON `verification_codes` (`username`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `auth_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`accountId` INTEGER NOT NULL, `token` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_auth_sessions_accountId` " +
                        "ON `auth_sessions` (`accountId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `account_programmes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`accountId` INTEGER NOT NULL, `programmeSlug` TEXT NOT NULL, " +
                        "`programmeName` TEXT NOT NULL, `year` INTEGER NOT NULL, " +
                        "`semester` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_account_programmes_accountId` " +
                        "ON `account_programmes` (`accountId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_account_programmes_programmeSlug_year_semester` " +
                        "ON `account_programmes` (`programmeSlug`, `year`, `semester`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `connections` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`requesterId` INTEGER NOT NULL, `addresseeId` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_connections_requesterId` " +
                        "ON `connections` (`requesterId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_connections_addresseeId` " +
                        "ON `connections` (`addresseeId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_connections_requesterId_addresseeId` " +
                        "ON `connections` (`requesterId`, `addresseeId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `blocks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`accountId` INTEGER NOT NULL, `blockedId` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_blocks_accountId` ON `blocks` (`accountId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_blocks_accountId_blockedId` " +
                        "ON `blocks` (`accountId`, `blockedId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reports` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`reporterId` INTEGER NOT NULL, `reportedId` INTEGER NOT NULL, " +
                        "`reason` TEXT NOT NULL, `details` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `handled` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reports_reporterId` ON `reports` (`reporterId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reports_reportedId` ON `reports` (`reportedId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `messages` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`programmeSlug` TEXT NOT NULL, `year` INTEGER NOT NULL, " +
                        "`semester` TEXT NOT NULL, `senderId` INTEGER NOT NULL, " +
                        "`senderName` TEXT NOT NULL, `body` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `synced` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_messages_programmeSlug_year_semester` " +
                        "ON `messages` (`programmeSlug`, `year`, `semester`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_messages_senderId` ON `messages` (`senderId`)"
                )
            }
        }

        /**
         * Gives programmes and plans stable slugs so a released catalogue update edits
         * an existing programme in place instead of duplicating it, and adds the
         * optional beta-tester credit.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `programmes` ADD COLUMN `slug` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `programme_plans` ADD COLUMN `slug` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `programme_plans` ADD COLUMN `contributor` TEXT")

                // Backfill slugs from the existing names so nothing is duplicated on upgrade.
                val programmes = mutableListOf<Pair<Long, String>>()
                db.query("SELECT id, name FROM programmes").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val name = cursor.getString(1) ?: ""
                        val slug = CatalogueJson.slugify(name)
                        db.execSQL("UPDATE programmes SET slug = ? WHERE id = ?", arrayOf<Any>(slug, id))
                        programmes.add(id to slug)
                    }
                }
                db.query("SELECT id, programmeId, year, semester FROM programme_plans").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val programmeId = cursor.getLong(1)
                        val year = cursor.getInt(2)
                        val semester = cursor.getString(3) ?: "A"
                        val base = programmes.firstOrNull { it.first == programmeId }?.second ?: "programme"
                        val slug = CatalogueJson.planSlug(base, year, semester)
                        db.execSQL(
                            "UPDATE programme_plans SET slug = ? WHERE id = ?",
                            arrayOf<Any>(slug, id)
                        )
                    }
                }

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_programmes_slug` ON `programmes` (`slug`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_programme_plans_slug` ON `programme_plans` (`slug`)")
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

        /**
         * Merges catalogue templates into the database, keyed by slug.
         *
         * Runs on every launch and after an import, so a released catalogue update adds
         * new programmes without duplicating existing ones and without ever touching
         * the student's own courses, timetable, assignments or exams.
         */
        suspend fun mergeCatalogue(dao: StudyHubDao, seeds: List<PlanSeed>) {
            for (seed in seeds) {
                val programmeSlug = CatalogueJson.slugify(seed.programme)
                var programme = dao.getProgrammeBySlug(programmeSlug)
                if (programme == null) {
                    val id = dao.insertProgramme(
                        Programme(name = seed.programme, isCustom = false, slug = programmeSlug)
                    )
                    programme = dao.getProgrammeBySlug(programmeSlug)
                        ?: Programme(id = id, name = seed.programme, slug = programmeSlug)
                } else if (programme.name != seed.programme) {
                    dao.renameProgramme(programme.id, seed.programme)
                    programme = programme.copy(name = seed.programme)
                }
                val programmeId = programme.id

                val slug = CatalogueJson.planSlug(programmeSlug, seed.year, seed.semester)
                val existing = dao.getPlanBySlug(slug)
                val planId = if (existing == null) {
                    dao.insertPlan(
                        ProgrammePlan(
                            programmeId = programmeId,
                            year = seed.year,
                            semester = seed.semester,
                            slug = slug,
                            contributor = seed.contributor
                        )
                    )
                } else {
                    if (existing.year != seed.year || existing.semester != seed.semester) {
                        dao.updatePlanDetails(existing.id, seed.year, seed.semester)
                    }
                    if (seed.contributor != null && existing.contributor != seed.contributor) {
                        dao.setPlanContributor(existing.id, seed.contributor)
                    }
                    existing.id
                }

                // Template rows only; replacing them never affects real student data.
                val planCourses = dao.getPlanCourses(planId)
                if (planCourses.isNotEmpty() && sameTemplate(dao, planCourses, seed)) continue

                dao.deletePlanCourses(planId)
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

        /** True when the stored template already matches, so we can skip a rewrite. */
        private suspend fun sameTemplate(dao: StudyHubDao, stored: List<PlanCourse>, seed: PlanSeed): Boolean {
            if (stored.size != seed.courses.size) return false
            return stored.all { row ->
                val match = seed.courses.firstOrNull {
                    it.code.equals(row.code, ignoreCase = true) &&
                        it.name.equals(row.name, ignoreCase = true) &&
                        it.credits == row.credits &&
                        it.colorIndex == row.colorIndex
                } ?: return false
                val sessions = dao.getPlanSessionsForCourse(row.id)
                if (sessions.size != match.sessions.size) return false
                sessions.all { s ->
                    match.sessions.any {
                        it.dayOfWeek == s.dayOfWeek &&
                            it.startMinute == s.startMinute &&
                            it.endMinute == s.endMinute &&
                            it.room.equals(s.room, ignoreCase = true)
                    }
                }
            }
        }

        /** Reads the catalogue that ships inside the APK - always available, offline. */
        fun readBundledCatalogue(context: Context): List<PlanSeed> = try {
            context.assets.open(CatalogueJson.ASSET_NAME).bufferedReader().use { reader ->
                CatalogueJson.parse(reader.readText())
            }
        } catch (error: Exception) {
            emptyList()
        }

        /** Seeds from the bundled file. Safe to call on every launch. */
        suspend fun seedCatalogueIfEmpty(context: Context, dao: StudyHubDao) {
            if (dao.countProgrammes() > 0) return
            mergeCatalogue(dao, readBundledCatalogue(context))
        }

        /** Reads the whole current catalogue back out of the database. */
        suspend fun exportSeeds(dao: StudyHubDao, includeCustom: Boolean = true): List<PlanSeed> {
            val seeds = mutableListOf<PlanSeed>()
            for (programme in dao.getAllProgrammes()) {
                if (programme.isCustom && !includeCustom) continue
                for (plan in dao.getPlansForProgramme(programme.id)) {
                    val courses = dao.getPlanCourses(plan.id)
                    if (courses.isEmpty()) continue
                    seeds.add(
                        PlanSeed(
                            programme = programme.name,
                            year = plan.year,
                            semester = plan.semester,
                            contributor = plan.contributor,
                            courses = courses.map { course ->
                                PlanCourseSeed(
                                    code = course.code,
                                    name = course.name,
                                    credits = course.credits,
                                    colorIndex = course.colorIndex,
                                    sessions = dao.getPlanSessionsForCourse(course.id).map {
                                        PlanSessionSeed(it.dayOfWeek, it.startMinute, it.endMinute, it.room)
                                    }
                                )
                            }
                        )
                    )
                }
            }
            return seeds
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
