package com.wiccan.app.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Unwraps a Compose `LocalContext` (often a ContextWrapper) down to its Activity, if any. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
