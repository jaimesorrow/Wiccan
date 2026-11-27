package com.jaimesorrow.wiccan.ui.paywall

import android.app.Activity
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionSetupViewModelTest {
    @Test
    fun `resume shows Play's recovery message then reconciles purchases`() = runTest {
        val repository = FakeSubscriptionRepository()
        val viewModel = SubscriptionSetupViewModel(repository)
        val activity = mockk<Activity>()

        viewModel.showTransactionalMessage(activity)
        viewModel.refreshPurchases()

        assertEquals(listOf(activity), repository.transactionalMessageCalls)
        assertEquals(1, repository.restoreCallCount)
        assertFalse(viewModel.reconciling.value)
    }

    @Test
    fun `a failed Play recovery message does not block purchase reconciliation`() = runTest {
        val repository = FakeSubscriptionRepository().apply {
            transactionalMessageResult = AppResult.Failure(AppError.Network)
        }
        val viewModel = SubscriptionSetupViewModel(repository)

        viewModel.showTransactionalMessage(mockk())
        viewModel.refreshPurchases()

        assertEquals(1, repository.transactionalMessageCalls.size)
        assertEquals(1, repository.restoreCallCount)
        assertFalse(viewModel.reconciling.value)
    }

    @Test
    fun `a failed reconciliation still clears the reconciling flag`() = runTest {
        val repository = FakeSubscriptionRepository().apply {
            restoreResult = AppResult.Failure(AppError.Network)
        }
        val viewModel = SubscriptionSetupViewModel(repository)
        assertTrue(viewModel.reconciling.value)

        viewModel.refreshPurchases()

        assertFalse(viewModel.reconciling.value)
    }
}
