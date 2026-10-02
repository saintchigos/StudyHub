package com.saintchigos.studyhub.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The shared programme catalogue in a plain JSON format.
 *
 * The app ships with `assets/catalogue.json` so it works with no internet at all.
 * Beta testers build programmes inside the app and use **Data and privacy →
 * Export catalogue** to produce a file of the same shape; that file is what the
 * maintainer folds into `catalogue.json` for the next release.
 *
 * Because the file is just text, anybody can also hand-edit it or host it online and
 * point the app at the URL - no paid service and no server required.
 */
object CatalogueJson {

    const val FORMAT_VERSION = 1
    const val ASSET_NAME = "catalogue.json"

    // ---- reading -----------------------------------------------------------

    /** Parses catalogue JSON. Throws [JSONException] if the file is malformed. */
    fun parse(raw: String): List<PlanSeed> {
        val root = JSONObject(raw)
        val programmes = root.optJSONArray("programmes") ?: JSONArray()
        val seeds = mutableListOf<PlanSeed>()
        for (i in 0 until programmes.length()) {
            val programme = programmes.optJSONObject(i) ?: continue
            val name = programme.optString("name").trim()
            if (name.isEmpty()) continue
            val plans = programme.optJSONArray("plans") ?: JSONArray()
            for (j in 0 until plans.length()) {
                val plan = plans.optJSONObject(j) ?: continue
                val courses = plan.optJSONArray("courses") ?: JSONArray()
                val courseSeeds = mutableListOf<PlanCourseSeed>()
                for (k in 0 until courses.length()) {
                    val course = courses.optJSONObject(k) ?: continue
                    val code = course.optString("code").trim()
                    val courseName = course.optString("name").trim()
                    if (courseName.isEmpty()) continue
                    val sessions = course.optJSONArray("sessions") ?: JSONArray()
                    val sessionSeeds = mutableListOf<PlanSessionSeed>()
                    for (s in 0 until sessions.length()) {
                        val session = sessions.optJSONObject(s) ?: continue
                        val day = session.optInt("day", -1)
                        val start = readMinute(session, "start")
                        val end = readMinute(session, "end")
                        if (day !in 1..7 || start == null || end == null || end <= start) continue
                        sessionSeeds.add(
                            PlanSessionSeed(
                                dayOfWeek = day,
                                startMinute = start,
                                endMinute = end,
                                room = session.optString("room").trim()
                            )
                        )
                    }
                    courseSeeds.add(
                        PlanCourseSeed(
                            code = code,
                            name = courseName,
                            credits = course.optInt("credits", 3).coerceIn(1, 12),
                            colorIndex = Math.floorMod(course.optInt("colorIndex", 0), 8),
                            sessions = sessionSeeds
                        )
                    )
                }
                if (courseSeeds.isEmpty()) continue
                seeds.add(
                    PlanSeed(
                        programme = name,
                        year = plan.optInt("year", 1).coerceIn(1, 10),
                        semester = plan.optString("semester", "A").trim().uppercase().ifEmpty { "A" },
                        contributor = plan.optString("contributor").trim().ifEmpty { null },
                        courses = courseSeeds
                    )
                )
            }
        }
        return seeds
    }

    /** Accepts either `"09:00"` (human friendly) or `540` (minutes since midnight). */
    private fun readMinute(obj: JSONObject, key: String): Int? {
        if (!obj.has(key)) return null
        val text = obj.optString(key).trim()
        if (text.isEmpty()) return null
        if (text.contains(':')) {
            val parts = text.split(':')
            val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
            if (hours !in 0..23 || minutes !in 0..59) return null
            return hours * 60 + minutes
        }
        val raw = text.toIntOrNull() ?: return null
        return if (raw in 0..1439) raw else null
    }

    fun formatMinute(minute: Int): String {
        val clamped = minute.coerceIn(0, 1439)
        return "%02d:%02d".format(clamped / 60, clamped % 60)
    }

    // ---- writing -----------------------------------------------------------

    /**
     * Builds a catalogue file from what is currently stored in the database, which is
     * what a beta tester sends back.
     */
    fun build(seeds: List<PlanSeed>, includeContributor: String?): String {
        val grouped = seeds.groupBy { it.programme }
        val programmes = JSONArray()
        grouped.forEach { (programmeName, planSeeds) ->
            val programme = JSONObject()
            programme.put("slug", slugify(programmeName))
            programme.put("name", programmeName)
            val plans = JSONArray()
            planSeeds.forEach { seed ->
                val plan = JSONObject()
                plan.put("year", seed.year)
                plan.put("semester", seed.semester)
                plan.put("contributor", includeContributor ?: seed.contributor ?: "")
                val courses = JSONArray()
                seed.courses.forEach { course ->
                    courses.put(
                        JSONObject()
                            .put("code", course.code)
                            .put("name", course.name)
                            .put("credits", course.credits)
                            .put("colorIndex", course.colorIndex)
                            .put(
                                "sessions",
                                JSONArray().apply {
                                    course.sessions.forEach { session ->
                                        put(
                                            JSONObject()
                                                .put("day", session.dayOfWeek)
                                                .put("start", formatMinute(session.startMinute))
                                                .put("end", formatMinute(session.endMinute))
                                                .put("room", session.room)
                                        )
                                    }
                                }
                            )
                    )
                }
                plan.put("courses", courses)
                plans.put(plan)
            }
            programme.put("plans", plans)
            programmes.put(programme)
        }
        return JSONObject()
            .put("formatVersion", FORMAT_VERSION)
            .put("app", "StudyHub")
            .put("programmes", programmes)
            .toString(2)
    }

    // ---- slugs -------------------------------------------------------------

    fun slugify(value: String): String {
        val slug = value
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return slug.ifEmpty { "programme" }
    }

    fun planSlug(programmeSlug: String, year: Int, semester: String): String =
        "$programmeSlug-y${year}-${semester.lowercase()}"
}