package com.jaimesorrow.wiccan.core.time

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Device calendar only identifies the requested day; the server assigns its lesson. */
class LocalCalendarProvider @Inject constructor() {
    val timezone: String get() = ZoneId.systemDefault().id
    val today: String get() = LocalDate.now(ZoneId.systemDefault()).toString()
}
