package com.jaimesorrow.wiccan.data.repository

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.billing.PREMIUM_MONTHLY_PRODUCT_ID
import com.jaimesorrow.wiccan.billing.WiccanBillingClient
import com.jaimesorrow.wiccan.billing.obfuscatedAccountId
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.remote.SubscriptionFunctionsDataSource
import com.jaimesorrow.wiccan.data.remote.UserTierDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubscriptionRepositoryImplTest {
    private val billingClient: WiccanBillingClient = mockk()
    private val functions: SubscriptionFunctionsDataSource = mockk()
    private val userTier: UserTierDataSource = mockk()
    private val currentUser: CurrentUserProvider = mockk()
    private val analytics: AnalyticsLogger = mockk(relaxed = true)
    private lateinit var repository: SubscriptionRepositoryImpl

    @Before
    fun setUp() {
        every { billingClient.purchaseUpdates } returns emptyFlow()
        every { currentUser.currentUserId } returns "uid-123"
        repository = SubscriptionRepositoryImpl(billingClient, functions, userTier, currentUser, analytics)
    }

    @Test
    fun `restore reports failure when server verification fails`() = runTest {
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(purchase())
        coEvery { functions.verifyPurchase("token", PREMIUM_MONTHLY_PRODUCT_ID) } throws IOException("offline")

        assertEquals(AppResult.Failure(AppError.Network), repository.restorePurchases())
    }

    @Test
    fun `restore reports success only after verification completes`() = runTest {
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(purchase())
        coEvery { functions.verifyPurchase("token", PREMIUM_MONTHLY_PRODUCT_ID) } returns Unit

        assertTrue(repository.restorePurchases() is AppResult.Success)
        coVerify(exactly = 1) { functions.verifyPurchase("token", PREMIUM_MONTHLY_PRODUCT_ID) }
    }

    @Test
    fun `pending and unrelated products never reach entitlement verification`() = runTest {
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(
            purchase(state = Purchase.PurchaseState.PENDING),
            purchase(product = "unrelated_product"),
        )

        assertTrue(repository.restorePurchases() is AppResult.Success)
        coVerify(exactly = 0) { functions.verifyPurchase(any(), any()) }
    }

    @Test
    fun `restore requires a signed-in Firebase account`() = runTest {
        every { currentUser.currentUserId } returns null

        assertEquals(AppResult.Failure(AppError.Unauthorized), repository.restorePurchases())
        coVerify(exactly = 0) { billingClient.queryOwnedSubscriptions() }
    }

    @Test
    fun `another Firebase account purchase does not block the current account restoration`() = runTest {
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(
            purchase(token = "other-token", accountId = obfuscatedAccountId("other-user")),
            purchase(accountId = obfuscatedAccountId("uid-123")),
        )
        coEvery { functions.verifyPurchase("token", PREMIUM_MONTHLY_PRODUCT_ID) } returns Unit

        assertTrue(repository.restorePurchases() is AppResult.Success)
        coVerify(exactly = 0) { functions.verifyPurchase("other-token", any()) }
        coVerify(exactly = 1) { functions.verifyPurchase("token", PREMIUM_MONTHLY_PRODUCT_ID) }
    }

    @Test
    fun `a rejected legacy token does not prevent verification of later legitimate purchases`() = runTest {
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(
            purchase(token = "legacy-token"),
            purchase(),
        )
        coEvery { functions.verifyPurchase("legacy-token", PREMIUM_MONTHLY_PRODUCT_ID) } throws IOException("verification failed")
        coEvery { functions.verifyPurchase("token", PREMIUM_MONTHLY_PRODUCT_ID) } returns Unit

        assertEquals(AppResult.Failure(AppError.Network), repository.restorePurchases())
        coVerify(exactly = 1) { functions.verifyPurchase("token", PREMIUM_MONTHLY_PRODUCT_ID) }
    }

    @Test(expected = CancellationException::class)
    fun `restore preserves coroutine cancellation`() = runTest {
        coEvery { billingClient.queryOwnedSubscriptions() } throws CancellationException("backgrounded")

        repository.restorePurchases()
    }

    @Test
    fun `purchase launches for the current Firebase account`() = runTest {
        val activity = mockk<Activity>()
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK
        coEvery { billingClient.launchPurchaseFlow(activity, "uid-123", PREMIUM_MONTHLY_PRODUCT_ID) } returns billingResult

        assertTrue(repository.launchPurchaseFlow(activity) is AppResult.Success)
        coVerify(exactly = 1) { billingClient.launchPurchaseFlow(activity, "uid-123", PREMIUM_MONTHLY_PRODUCT_ID) }
    }

    @Test
    fun `purchase cannot launch without an account to bind`() = runTest {
        every { currentUser.currentUserId } returns null

        assertEquals(AppResult.Failure(AppError.Unauthorized), repository.launchPurchaseFlow(mockk()))
        coVerify(exactly = 0) { billingClient.launchPurchaseFlow(any(), any(), any()) }
    }

    private fun purchase(
        state: Int = Purchase.PurchaseState.PURCHASED,
        product: String = PREMIUM_MONTHLY_PRODUCT_ID,
        token: String = "token",
        accountId: String? = null,
    ): Purchase = mockk {
        every { purchaseState } returns state
        every { products } returns listOf(product)
        every { purchaseToken } returns token
        every { isAcknowledged } returns true
        every { accountIdentifiers } returns if (accountId == null) {
            null
        } else {
            mockk {
                every { obfuscatedAccountId } returns accountId
            }
        }
    }
}
