package com.jaimesorrow.wiccan.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import javax.inject.Inject

class WidgetUpdater @Inject constructor() {
    suspend fun updateAll(context: Context) {
        WiccanWidget().updateAll(context)
    }
}
