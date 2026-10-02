package com.jaimesorrow.wiccan.ui.navigation

import com.jaimesorrow.wiccan.ui.archive.ARCHIVE_DETAIL_CONTENT_ID_ARG

/** Every navigable route in the signed-in app, centralized in one place. */
object WiccanDestinations {
    const val TODAY = "today"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"
    const val ARCHIVE_DETAIL_ROUTE = "archive_detail/{$ARCHIVE_DETAIL_CONTENT_ID_ARG}"

    fun archiveDetail(contentId: String): String = "archive_detail/$contentId"
}

/** Custom-scheme deep links this app resolves, matching the intent-filters declared in the manifest. */
object WiccanDeepLinks {
    const val TODAY_URI = "wiccan://today"
    const val PREMIUM_URI = "wiccan://premium"
}
