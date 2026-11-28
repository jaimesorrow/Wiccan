package com.jaimesorrow.wiccan.ui.paywall

import com.jaimesorrow.wiccan.domain.model.ContentTier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Promo redemption never unlocks premium locally; Play verification remains the sole gate. */
class PromoRedeemEntitlementTest {
    @Test
    fun `restore reconciliation leaves tier unchanged until Play verifies a purchase`() = runTest {
        val repository = FakeSubscriptionRepository()
        val viewModel = SubscriptionSetupViewModel(repository)
        viewModel.refreshPurchases()
        assertEquals(1, repository.restoreCallCount)
        assertEquals(ContentTier.FREE, repository.observeTier().first())
    }
}
