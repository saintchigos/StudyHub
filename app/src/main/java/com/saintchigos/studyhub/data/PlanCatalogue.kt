package com.saintchigos.studyhub.data

/**
 * Catalogue template shapes used when reading [CatalogueJson] and when writing a
 * catalogue file back out. The actual programme data lives in
 * `app/src/main/assets/catalogue.json` so it can be extended without touching code.
 */
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
    val contributor: String? = null,
    val courses: List<PlanCourseSeed>
)