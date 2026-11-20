package com.jaimesorrow.wiccan.core.time

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Device calendar only identifies the requested day; the server assigns its lesson. */
class LocalCalendarProvider @Inject constructor() {
    val timezone: String get() = ZoneId.systemDefault().id
    val today: String get() = LocalDate.now(ZoneId.systemDefault()).toString()

    /**
     * Emits the current local calendar date immediately, then again at the
     * next local midnight and whenever a time-zone or clock change makes
     * "today" a different date. Collectors that filter cached lessons by
     * [today] would otherwise keep yesterday's row until some other flow emits.
     */
    fun todayFlow(): Flow<String> = flow {
        var last: String? = null
        while (true) {
            val current = today
            if (current != last) {
                last = current
                emit(current)
            }
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            val untilMidnightMs = Duration.between(now, nextMidnight).toMillis()
            // Cap at one minute so a mid-day time-zone change is noticed quickly.
            delay(untilMidnightMs.coerceAtLeast(50L).coerceAtMost(60_000L))
        }
    }
}
