package com.saintchigos.studyhub.data

/** Template data for a built-in programme offering. */
data class PlanSessionSeed(
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String
)

data class PlanCourseSeed(
    val code: String,
    val name: String,
    val credits: Int,
    val colorIndex: Int,
    val sessions: List<PlanSessionSeed>
)

data class PlanSeed(
    val programme: String,
    val year: Int,
    val semester: String,
    val courses: List<PlanCourseSeed>
)

/**
 * Catalogue shipped with the app. New students pick a programme, year and semester
 * and their whole timetable is created for them.
 */
object PlanCatalogue {

    val seeds: List<PlanSeed> = listOf(
        PlanSeed(
            programme = "Computers and Statistics",
            year = 3,
            semester = "A",
            courses = listOf(
                PlanCourseSeed(
                    code = "CS3400",
                    name = "DATA STRUCTURES AND ALGORITHM",
                    credits = 4,
                    colorIndex = 3,
                    sessions = listOf(
                        PlanSessionSeed(1, 540, 660, "DTF103"),
                        PlanSessionSeed(2, 730, 910, "DTF104"),
                        PlanSessionSeed(4, 480, 540, "CMP107")
                    )
                ),
                PlanCourseSeed(
                    code = "CS3520",
                    name = "COMPUTER ORGANIZATION AND ARCHITECTURE I",
                    credits = 5,
                    colorIndex = 2,
                    sessions = listOf(
                        PlanSessionSeed(2, 480, 600, "DTF104"),
                        PlanSessionSeed(4, 790, 850, "CMP107"),
                        PlanSessionSeed(5, 670, 790, "ICT Lab")
                    )
                ),
                PlanCourseSeed(
                    code = "CS3541",
                    name = "COMPUTER COMMUNICATIONS AND NETWORKS I",
                    credits = 5,
                    colorIndex = 4,
                    sessions = listOf(
                        PlanSessionSeed(2, 610, 730, "DTF101"),
                        PlanSessionSeed(3, 790, 910, "CMP103"),
                        PlanSessionSeed(4, 670, 790, "CISCO Lab")
                    )
                ),
                PlanCourseSeed(
                    code = "ST3403",
                    name = "Linear Algebra",
                    credits = 4,
                    colorIndex = 0,
                    sessions = listOf(
                        PlanSessionSeed(2, 910, 1030, "DTF107")
                    )
                ),
                PlanCourseSeed(
                    code = "ST3501",
                    name = "Survey Methods and Applications",
                    credits = 5,
                    colorIndex = 1,
                    sessions = listOf(
                        PlanSessionSeed(3, 970, 1030, "ETF3"),
                        PlanSessionSeed(4, 540, 670, "CMP103"),
                        PlanSessionSeed(5, 480, 670, "CMP103")
                    )
                )
            )
        )
    )
}