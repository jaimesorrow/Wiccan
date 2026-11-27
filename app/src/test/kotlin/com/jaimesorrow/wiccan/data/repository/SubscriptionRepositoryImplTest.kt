package com.jaimesorrow.wiccan.data.repository

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.billing.PREMIUM_MONTHLY_PRODUCT_ID
import com.jaimesorrow.wiccan.billing.PREMIUM_YEARLY_PRODUCT_ID
import com.jaimesorrow.wiccan.billing.WiccanBillingClient
import com.jaimesorrow.wiccan.billing.obfuscatedAccountId
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.remote.SubscriptionFunctionsDataSource
import com.jaimesorrow.wiccan.data.remote.UserTierDataSource
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import com.jaimesorrow.wiccan.domain.model.VerifiedTier
import com.jaimesorrow.wiccan.domain.model.YearlySwitchOffer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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
        every { billingClient.recoveryUpdates } returns emptyFlow()
        every { currentUser.currentUserId } returns "uid-123"
        repository = SubscriptionRepositoryImpl(billingClient, functions, userTier, currentUser, analytics)
    }

    @Test
    fun `only a lapse for the same signed in account logs an event`() = runTest {
        every { userTier.observeTier("uid-123") } returns flowOf(ContentTier.PREMIUM)
        val oldAccountFlow = repository.observeTier()
        oldAccountFlow.toList()

        every { currentUser.currentUserId } returns "other-user"
        every { userTier.observeTier("other-user") } returns flowOf(ContentTier.FREE)
        repository.observeTier().toList()
        oldAccountFlow.toList()
        repository.observeTier().toList()
        verify(exactly = 0) { analytics.logSubscriptionLapsed() }

        every { userTier.observeTier("other-user") } returns flowOf(ContentTier.PREMIUM, ContentTier.FREE, ContentTier.FREE)
        repository.observeTier().toList()
        verify(exactly = 1) { analytics.logSubscriptionLapsed() }
    }

    @Test
    fun `signed-out subscription state is unknown and a signed-in profile is forwarded`() = runTest {
        every { currentUser.currentUserId } returns null
        assertEquals(listOf(SubscriptionState.UNKNOWN), repository.observeSubscriptionState().toList())

        every { currentUser.currentUserId } returns "uid-123"
        every { userTier.observeSubscriptionState("uid-123") } returns flowOf(
            SubscriptionState.IN_GRACE_PERIOD,
            SubscriptionState.ON_HOLD,
        )
        assertEquals(
            listOf(SubscriptionState.IN_GRACE_PERIOD, SubscriptionState.ON_HOLD),
            repository.observeSubscriptionState().toList(),
        )
    }

    @Test
    fun `signed-out product id is unknown and a signed-in profile is forwarded`() = runTest {
        every { currentUser.currentUserId } returns null
        assertEquals(listOf(null), repository.observeProductId().toList())

        every { currentUser.currentUserId } returns "uid-123"
        every { userTier.observeProductId("uid-123") } returns flowOf(
            PREMIUM_MONTHLY_PRODUCT_ID,
            PREMIUM_YEARLY_PRODUCT_ID,
        )
        assertEquals(
            listOf(PREMIUM_MONTHLY_PRODUCT_ID, PREMIUM_YEARLY_PRODUCT_ID),
            repository.observeProductId().toList(),
        )
    }

    @Test
    fun `Play transactional recovery message reports success and Play errors`() = runTest {
        val activity = mockk<Activity>()
        val ok = mockk<BillingResult>()
        every { ok.responseCode } returns BillingClient.BillingResponseCode.OK
        coEvery { billingClient.showTransactionalMessage(activity) } returns ok
        assertEquals(AppResult.Success(Unit), repository.showTransactionalMessage(activity))

        val failed = mockk<BillingResult>()
        every { failed.responseCode } returns BillingClient.BillingResponseCode.ERROR
        every { failed.debugMessage } returns "unavailable"
        coEvery { billingClient.showTransactionalMessage(activity) } returns failed
        assertEquals(
            AppResult.Failure(AppError.PurchaseVerificationFailed("unavailable")),
            repository.showTransactionalMessage(activity),
        )
    }

    @Test(expected = CancellationException::class)
    fun `Play transactional recovery message preserves coroutine cancellation`() = runTest {
        coEvery { billingClient.showTransactionalMessage(any()) } throws CancellationException("backgrounded")
        repository.showTransactionalMessage(mockk())
    }

    @Test
    fun `Play recovery status update reconciles owned subscriptions with the server`() {
        val recovered = purchase()
        every { billingClient.recoveryUpdates } returns flowOf(Unit)
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(recovered)
        coEvery { functions.verifyPurchase("token", PREMIUM_MONTHLY_PRODUCT_ID) } returns Unit

        repository = SubscriptionRepositoryImpl(billingClient, functions, userTier, currentUser, analytics)

        coVerify(timeout = 2_000, exactly = 1) {
            functions.verifyPurchase("token", PREMIUM_MONTHLY_PRODUCT_ID)
        }
    }

    @Test
    fun `overlapping listeners ignore initial free and report a shared lapse once`() = runTest {
        val rootTier = repository.observeTier()
        every { userTier.observeTier("uid-123") } returns flow {
            emit(ContentTier.PREMIUM)
            every { userTier.observeTier("uid-123") } returns flowOf(ContentTier.FREE)
            repository.observeTier().toList()
            verify(exactly = 0) { analytics.logSubscriptionLapsed() }

            every { userTier.observeTier("uid-123") } returns flowOf(ContentTier.PREMIUM, ContentTier.FREE)
            repository.observeTier().toList()
            emit(ContentTier.FREE)
        }

        rootTier.toList()
        verify(exactly = 1) { analytics.logSubscriptionLapsed() }
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
    fun `restore reports a distinct failure when Play cannot be queried`() = runTest {
        coEvery { billingClient.queryOwnedSubscriptions() } throws IllegalStateException("billing unavailable")

        assertEquals(AppResult.Failure(AppError.RestoreFailed), repository.restorePurchases())
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
        val offer = loadOffer()
        coEvery { billingClient.launchPurchaseFlow(activity, "uid-123", PREMIUM_MONTHLY_PRODUCT_ID, offer) } returns billingResult

        assertTrue(repository.launchPurchaseFlow(activity) is AppResult.Success)
        coVerify(exactly = 1) { billingClient.launchPurchaseFlow(activity, "uid-123", PREMIUM_MONTHLY_PRODUCT_ID, offer) }
    }

    @Test
    fun `purchase cannot launch without an account to bind`() = runTest {
        every { currentUser.currentUserId } returns null

        assertEquals(AppResult.Failure(AppError.Unauthorized), repository.launchPurchaseFlow(mockk()))
        coVerify(exactly = 0) { billingClient.launchPurchaseFlow(any(), any(), any(), any()) }
    }

    @Test
    fun `checkout requires an offer loaded for the current account`() = runTest {
        assertTrue(repository.launchPurchaseFlow(mockk()) is AppResult.Failure)
        loadOffer()
        every { currentUser.currentUserId } returns "another-account"
        assertTrue(repository.launchPurchaseFlow(mockk()) is AppResult.Failure)
        coVerify(exactly = 0) { billingClient.launchPurchaseFlow(any(), any(), any(), any()) }
    }

    @Test
    fun `a missing yearly product hides the yearly plan without failing the paywall`() = runTest {
        coEvery { billingClient.queryProductDetails(PREMIUM_YEARLY_PRODUCT_ID) } throws IllegalStateException("not found")
        assertEquals(AppResult.Success(null), repository.getYearlyOffer())
        assertTrue(repository.launchPurchaseFlow(mockk(), SubscriptionPlan.YEARLY) is AppResult.Failure)
        coVerify(exactly = 0) { billingClient.launchPurchaseFlow(any(), any(), any(), any()) }
    }

    @Test
    fun `yearly checkout launches the yearly product with the yearly offer`() = runTest {
        val activity = mockk<Activity>()
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK
        val monthly = loadOffer()
        val yearly = loadYearlyOffer()
        assertEquals(SubscriptionPlan.YEARLY, yearly.plan)
        coEvery { billingClient.launchPurchaseFlow(activity, "uid-123", PREMIUM_YEARLY_PRODUCT_ID, yearly) } returns billingResult

        assertTrue(repository.launchPurchaseFlow(activity, SubscriptionPlan.YEARLY) is AppResult.Success)
        coVerify(exactly = 1) { billingClient.launchPurchaseFlow(activity, "uid-123", PREMIUM_YEARLY_PRODUCT_ID, yearly) }
        coVerify(exactly = 0) { billingClient.launchPurchaseFlow(any(), any(), PREMIUM_MONTHLY_PRODUCT_ID, monthly) }
    }

    @Test
    fun `a yearly purchase is verified with the server as the yearly product`() = runTest {
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(purchase(product = PREMIUM_YEARLY_PRODUCT_ID))
        coEvery { functions.verifyPurchase("token", PREMIUM_YEARLY_PRODUCT_ID) } returns Unit
        assertTrue(repository.restorePurchases() is AppResult.Success)
        coVerify(exactly = 1) { functions.verifyPurchase("token", PREMIUM_YEARLY_PRODUCT_ID) }
    }

    @Test
    fun `switching to yearly launches a replacement with the monthly purchase token`() = runTest {
        val activity = mockk<Activity>()
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK
        val offer = loadYearlySwitchOffer()
        val accountId = obfuscatedAccountId("uid-123")
        coEvery { userTier.currentPurchaseToken("uid-123") } returns null
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(
            purchase(token = "other-monthly", accountId = obfuscatedAccountId("other-user")),
            purchase(token = "monthly-token", accountId = accountId),
        )
        coEvery { billingClient.launchPlanChangeFlow(activity, "uid-123", offer, "monthly-token") } returns billingResult

        assertTrue(repository.launchSwitchToYearly(activity) is AppResult.Success)
        coVerify(exactly = 1) { billingClient.launchPlanChangeFlow(activity, "uid-123", offer, "monthly-token") }
    }

    @Test
    fun `switching to yearly uses the server-verified token when Play omits the account hash`() = runTest {
        val activity = mockk<Activity>()
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK
        val offer = loadYearlySwitchOffer()
        coEvery { userTier.currentPurchaseToken("uid-123") } returns "monthly-token"
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(purchase(token = "monthly-token"))
        coEvery { billingClient.launchPlanChangeFlow(activity, "uid-123", offer, "monthly-token") } returns billingResult

        assertTrue(repository.launchSwitchToYearly(activity) is AppResult.Success)
        coVerify(exactly = 1) { billingClient.launchPlanChangeFlow(activity, "uid-123", offer, "monthly-token") }
    }

    @Test
    fun `switching to yearly fails closed without a loaded offer or a monthly purchase`() = runTest {
        coEvery { userTier.currentPurchaseToken("uid-123") } returns null
        assertTrue(repository.launchSwitchToYearly(mockk()) is AppResult.Failure)
        loadYearlySwitchOffer()
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(purchase(product = PREMIUM_YEARLY_PRODUCT_ID))
        assertTrue(repository.launchSwitchToYearly(mockk()) is AppResult.Failure)
        coEvery { billingClient.queryOwnedSubscriptions() } returns listOf(
            purchase(token = "other-monthly", accountId = obfuscatedAccountId("other-user")),
        )
        val result = repository.launchSwitchToYearly(mockk())
        assertTrue(result is AppResult.Failure)
        assertEquals(
            AppResult.Failure(
                AppError.PurchaseVerificationFailed(
                    "Your monthly subscription could not be found. Open Google Play to manage it, or try again.",
                ),
            ),
            result,
        )
        coVerify(exactly = 0) { billingClient.launchPlanChangeFlow(any(), any(), any(), any()) }
    }

    private suspend fun loadYearlySwitchOffer(): YearlySwitchOffer {
        val intro = mockk<ProductDetails.PricingPhase> {
            every { recurrenceMode } returns ProductDetails.RecurrenceMode.NON_RECURRING
            every { billingPeriod } returns "P1M"
            every { priceAmountMicros } returns 990_000L
            every { priceCurrencyCode } returns "USD"
            every { formattedPrice } returns "$0.99"
            every { billingCycleCount } returns 0
        }
        val renewal = mockk<ProductDetails.PricingPhase> {
            every { recurrenceMode } returns ProductDetails.RecurrenceMode.INFINITE_RECURRING
            every { billingPeriod } returns "P1Y"
            every { priceAmountMicros } returns 39_990_000L
            every { priceCurrencyCode } returns "USD"
            every { formattedPrice } returns "$39.99"
        }
        val phases = mockk<ProductDetails.PricingPhases> { every { pricingPhaseList } returns listOf(intro, renewal) }
        val offer = mockk<ProductDetails.SubscriptionOfferDetails> {
            every { offerId } returns "switch-yearly-1mo-099"
            every { offerToken } returns "switch-token"
            every { offerTags } returns listOf("switch-yearly")
            every { pricingPhases } returns phases
        }
        val product = mockk<ProductDetails> {
            every { productId } returns PREMIUM_YEARLY_PRODUCT_ID
            every { subscriptionOfferDetails } returns listOf(offer)
        }
        coEvery { billingClient.queryProductDetails(PREMIUM_YEARLY_PRODUCT_ID) } returns product
        return (repository.getYearlySwitchOffer() as AppResult.Success).data!!
    }

    private suspend fun loadYearlyOffer(): SubscriptionOffer {
        val phase = mockk<ProductDetails.PricingPhase> {
            every { recurrenceMode } returns ProductDetails.RecurrenceMode.INFINITE_RECURRING
            every { billingPeriod } returns "P1Y"
            every { priceAmountMicros } returns 39_990_000L
            every { priceCurrencyCode } returns "USD"
            every { formattedPrice } returns "$39.99"
        }
        val phases = mockk<ProductDetails.PricingPhases> { every { pricingPhaseList } returns listOf(phase) }
        val offer = mockk<ProductDetails.SubscriptionOfferDetails> {
            every { offerId } returns null
            every { offerToken } returns "yearly-token"
            every { offerTags } returns emptyList()
            every { pricingPhases } returns phases
        }
        val product = mockk<ProductDetails> {
            every { productId } returns PREMIUM_YEARLY_PRODUCT_ID
            every { subscriptionOfferDetails } returns listOf(offer)
        }
        coEvery { billingClient.queryProductDetails(PREMIUM_YEARLY_PRODUCT_ID) } returns product
        return (repository.getYearlyOffer() as AppResult.Success).data!!
    }

    private suspend fun loadOffer(): SubscriptionOffer {
        val phase = mockk<ProductDetails.PricingPhase> {
            every { recurrenceMode } returns ProductDetails.RecurrenceMode.INFINITE_RECURRING
            every { billingPeriod } returns "P1M"
            every { priceAmountMicros } returns 5_990_000L
            every { priceCurrencyCode } returns "USD"
            every { formattedPrice } returns "$5.99"
        }
        val phases = mockk<ProductDetails.PricingPhases> { every { pricingPhaseList } returns listOf(phase) }
        val offer = mockk<ProductDetails.SubscriptionOfferDetails> {
            every { offerId } returns null
            every { offerToken } returns "monthly-token"
            every { offerTags } returns emptyList()
            every { pricingPhases } returns phases
        }
        val product = mockk<ProductDetails> {
            every { productId } returns PREMIUM_MONTHLY_PRODUCT_ID
            every { subscriptionOfferDetails } returns listOf(offer)
        }
        coEvery { billingClient.queryProductDetails(PREMIUM_MONTHLY_PRODUCT_ID) } returns product
        return (repository.getOffer() as AppResult.Success).data
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

    @Test
    fun `verified tier fails closed when signed out or the account changes`() = runTest {
        every { currentUser.currentUserId } returns null
        assertEquals(listOf(VerifiedTier.UNVERIFIED), repository.observeVerifiedTier().toList())

        every { currentUser.currentUserId } returns "uid-123"
        every { userTier.observeVerifiedTier("uid-123") } returns flow {
            emit(VerifiedTier.FREE)
            every { currentUser.currentUserId } returns "other-user"
            emit(VerifiedTier.FREE)
        }
        assertEquals(listOf(VerifiedTier.FREE, VerifiedTier.UNVERIFIED), repository.observeVerifiedTier().toList())
    }
}
