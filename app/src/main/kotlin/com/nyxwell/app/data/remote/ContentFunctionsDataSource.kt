package com.nyxwell.app.data.remote

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Thin wrapper around the `getTodayContent` Cloud Function. The daily
 * selection logic (history lookup, published/tier filtering, random pick,
 * pool-exhaustion fallback) lives server-side and is never re-implemented
 * here — this class only invokes it and returns the raw payload.
 */
class ContentFunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
    private val crashlytics: FirebaseCrashlytics,
) {
    suspend fun getTodayContent(): Map<String, Any?> {
        try {
            val result = functions
                .getHttpsCallable(FUNCTION_NAME)
                .call()
                .await()

            @Suppress("UNCHECKED_CAST")
            return result.getData() as? Map<String, Any?>
                ?: error("Unexpected response shape from Cloud Function '$FUNCTION_NAME'")
        } catch (t: Throwable) {
            crashlytics.recordException(t)
            throw t
        }
    }

    private companion object {
        const val FUNCTION_NAME = "getTodayContent"
    }
}
