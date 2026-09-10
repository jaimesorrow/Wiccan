package com.nyxwell.app.data.remote

import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around the `verifyPurchase` Cloud Function. The actual
 * Google Play verification and `users/{uid}.tier` write happen server
 * side; this call just hands over the purchase token for verification.
 */
class SubscriptionFunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
) {
    suspend fun verifyPurchase(purchaseToken: String, productId: String) {
        functions.getHttpsCallable(FUNCTION_NAME)
            .call(mapOf("purchaseToken" to purchaseToken, "productId" to productId))
            .await()
    }

    private companion object {
        const val FUNCTION_NAME = "verifyPurchase"
    }
}
