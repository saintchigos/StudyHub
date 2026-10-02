package com.saintchigos.studyhub.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A degree programme such as "Computers and Statistics". Programmes are the catalogue
 * a student picks from so they never have to type their whole timetable by hand.
 */
@Entity(
    tableName = "programmes",
    indices = [Index(value = ["slug"], unique = true)]
)
data class Programme(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCustom: Boolean = false,
    /**
     * Stable identifier used by the shared catalogue (e.g. "computers-and-statistics").
     * Lets a released catalogue update an existing programme in place instead of
     * creating a duplicate every time the app is updated.
     */
    val slug: String = ""
)

/** One year/semester offering of a programme, e.g. Year 3 Semester A. */
@Entity(
    tableName = "programme_plans",
    foreignKeys = [ForeignKey(
        entity = Programme::class,
        parentColumns = ["id"],
        childColumns = ["programmeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("programmeId"), Index(value = ["slug"], unique = true)]
)
data class ProgrammePlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programmeId: Long,
    val year: Int,
    val semester: String,
    /** Stable identifier for the year/semester offering, e.g. "…-y3-a". */
    val slug: String = "",
    /** Optional credit for the beta tester who typed this timetable in. */
    val contributor: String? = null
)

/** A course belonging to a plan, plus the template used to seed a new student. */
@Entity(
    tableName = "plan_courses",
    foreignKeys = [ForeignKey(
        entity = ProgrammePlan::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("planId")]
)
data class PlanCourse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val code: String,
    val credits: Int = 3,
    val colorIndex: Int = 0
)

/** A template class time, copied into the timetable when a plan is applied. */
@Entity(
    tableName = "plan_sessions",
    foreignKeys = [ForeignKey(
        entity = PlanCourse::class,
        parentColumns = ["id"],
        childColumns = ["planCourseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("planCourseId")]
)
data class PlanSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planCourseId: Long,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String = ""
)

/** Records which plans the student has applied, so they can add or remove later. */
@Entity(tableName = "applied_plans")
data class AppliedPlan(
    @PrimaryKey val planId: Long
)

data class PlanWithProgramme(
    val planId: Long,
    val programmeId: Long,
    val programmeName: String,
    val year: Int,
    val semester: String,
    val isCustom: Boolean,
    val applied: Boolean
)

/** How much a plan would add, shown in the picker. */
data class PlanStats(
    val planId: Long,
    val courses: Int,
    val sessions: Int
)