package com.nyxwell.app.ui.subscription

import android.app.Activity
import app.cash.turbine.test
import com.nyxwell.app.domain.model.ContentTier
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeSubscriptionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeSubscriptionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tier reflects the repository's observeTier stream`() = runTest(dispatcher) {
        val viewModel = SubscriptionViewModel(repository)

        viewModel.tier.test {
            assertEquals(ContentTier.FREE, awaitItem())
            repository.emitTier(ContentTier.PREMIUM)
            assertEquals(ContentTier.PREMIUM, awaitItem())
        }
    }

    @Test
    fun `upgrade delegates to the repository's launchPurchaseFlow`() = runTest(dispatcher) {
        val viewModel = SubscriptionViewModel(repository)
        val activity = mockk<Activity>()

        viewModel.upgrade(activity)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(activity), repository.launchCalls)
    }
}
