package com.saintchigos.studyhub.domain

import com.saintchigos.studyhub.data.FocusSession
import com.saintchigos.studyhub.util.TimeUtil
import java.time.LocalDate
import java.time.LocalDateTime

/** Focus minutes for the last seven days, oldest first, ready to draw as bars. */
object WeeklyFocus {

    data class Day(val label: String, val minutes: Int, val isToday: Boolean)

    fun lastSevenDays(sessions: List<FocusSession>, now: LocalDateTime): List<Day> {
        val today: LocalDate = now.toLocalDate()
        val byDate = HashMap<LocalDate, Int>()
        for (s in sessions) {
            if (s.deleted != 0) continue
            val date = TimeUtil.toLocalDateTime(s.startedAt).toLocalDate()
            byDate[date] = (byDate[date] ?: 0) + s.actualMinutes.coerceAtLeast(0)
        }
        return (6 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            Day(
                label = TimeUtil.shortDayLabel(date.dayOfWeek.value),
                minutes = byDate[date] ?: 0,
                isToday = back == 0
            )
        }
    }
}
