package com.jaimesorrow.wiccan.data.remote

import app.cash.turbine.test
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.jaimesorrow.wiccan.domain.model.ContentTier
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserTierDataSourceTest {
    @Test
    fun `disk cache cannot unlock and verified access closes at expiry without another snapshot`() = runTest {
        val listener = slot<EventListener<DocumentSnapshot>>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val firestore = mockk<FirebaseFirestore>()
        every {
            firestore.collection("users").document("u").addSnapshotListener(MetadataChanges.INCLUDE, capture(listener))
        } returns registration
        val expiry = System.currentTimeMillis() + 60_000

        UserTierDataSource(firestore).observeTier("u").test {
            runCurrent()
            listener.captured.onEvent(snapshot(expiry, cached = true), null)
            assertEquals(ContentTier.FREE, awaitItem())
            listener.captured.onEvent(snapshot(expiry, cached = false), null)
            assertEquals(ContentTier.PREMIUM, awaitItem())
            advanceTimeBy(61_000)
            runCurrent()
            assertEquals(ContentTier.FREE, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `missing expired and deleting profiles fail closed`() {
        assertFalse(hasSubscriptionAccess("premium", 0, false, nowMillis = 100))
        assertFalse(hasSubscriptionAccess("premium", 100, false, nowMillis = 100))
        assertFalse(hasSubscriptionAccess("free", 200, false, nowMillis = 100))
        assertFalse(hasSubscriptionAccess("premium", 200, true, nowMillis = 100))
    }

    private fun snapshot(expiry: Long, cached: Boolean): DocumentSnapshot = mockk {
        every { metadata } returns mockk { every { isFromCache } returns cached }
        every { getString("tier") } returns "premium"
        every { get("subscriptionExpiresAtMillis") } returns expiry
        every { getBoolean("deletionInProgress") } returns false
    }
}
