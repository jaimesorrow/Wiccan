package com.jaimesorrow.wiccan.data.remote

import app.cash.turbine.test
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import com.jaimesorrow.wiccan.domain.model.VerifiedTier
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
            listener.captured.onEvent(snapshot(Double.POSITIVE_INFINITY, cached = false), null)
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
    fun `mirrored subscription state is read only from a server snapshot`() = runTest {
        val listener = slot<EventListener<DocumentSnapshot>>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val firestore = mockk<FirebaseFirestore>()
        every {
            firestore.collection("users").document("u").addSnapshotListener(MetadataChanges.INCLUDE, capture(listener))
        } returns registration

        UserTierDataSource(firestore).observeSubscriptionState("u").test {
            runCurrent()
            listener.captured.onEvent(stateSnapshot("on_hold", cached = true), null)
            assertEquals(SubscriptionState.UNKNOWN, awaitItem())
            listener.captured.onEvent(stateSnapshot("in_grace_period", cached = false), null)
            assertEquals(SubscriptionState.IN_GRACE_PERIOD, awaitItem())
            listener.captured.onEvent(stateSnapshot("on_hold", cached = false), null)
            assertEquals(SubscriptionState.ON_HOLD, awaitItem())
            listener.captured.onEvent(stateSnapshot("active", cached = false), null)
            assertEquals(SubscriptionState.ACTIVE, awaitItem())
            listener.captured.onEvent(null, mockk())
            assertEquals(SubscriptionState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only a server snapshot confirms free and cache error timeout and expiry stay unverified`() = runTest {
        val listener = slot<EventListener<DocumentSnapshot>>()
        val firestore = mockk<FirebaseFirestore>()
        every {
            firestore.collection("users").document("u").addSnapshotListener(MetadataChanges.INCLUDE, capture(listener))
        } returns mockk(relaxed = true)
        val expiry = System.currentTimeMillis() + 60_000

        UserTierDataSource(firestore).observeVerifiedTier("u").test {
            runCurrent()
            listener.captured.onEvent(snapshot(expiry, cached = true, tier = "free"), null)
            assertEquals("cached free is not confirmation", VerifiedTier.UNVERIFIED, awaitItem())
            listener.captured.onEvent(null, mockk<FirebaseFirestoreException>(relaxed = true))
            assertEquals(VerifiedTier.UNVERIFIED, awaitItem())
            listener.captured.onEvent(snapshot(expiry, cached = false, tier = "free"), null)
            assertEquals(VerifiedTier.FREE, awaitItem())
            listener.captured.onEvent(snapshot(expiry, cached = false), null)
            assertEquals(VerifiedTier.PREMIUM, awaitItem())
            advanceTimeBy(61_000)
            runCurrent()
            assertEquals("local expiry waits for the server", VerifiedTier.UNVERIFIED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mirrored product id is read only from a server snapshot`() = runTest {
        val listener = slot<EventListener<DocumentSnapshot>>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val firestore = mockk<FirebaseFirestore>()
        every {
            firestore.collection("users").document("u").addSnapshotListener(MetadataChanges.INCLUDE, capture(listener))
        } returns registration

        UserTierDataSource(firestore).observeProductId("u").test {
            runCurrent()
            listener.captured.onEvent(productSnapshot("wiccan_premium_monthly", cached = true), null)
            assertEquals(null, awaitItem())
            listener.captured.onEvent(productSnapshot("wiccan_premium_monthly", cached = false), null)
            assertEquals("wiccan_premium_monthly", awaitItem())
            listener.captured.onEvent(productSnapshot("wiccan_premium_yearly", cached = false), null)
            assertEquals("wiccan_premium_yearly", awaitItem())
            listener.captured.onEvent(productSnapshot("  ", cached = false), null)
            assertEquals(null, awaitItem())
            listener.captured.onEvent(null, mockk())
            assertEquals(null, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verification timeout without a server answer is unverified`() = runTest {
        val firestore = mockk<FirebaseFirestore>()
        every {
            firestore.collection("users").document("u").addSnapshotListener(MetadataChanges.INCLUDE, any())
        } returns mockk(relaxed = true)

        UserTierDataSource(firestore).observeVerifiedTier("u").test {
            advanceTimeBy(10_001)
            runCurrent()
            assertEquals(VerifiedTier.UNVERIFIED, awaitItem())
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

    private fun snapshot(expiry: Number, cached: Boolean, tier: String = "premium"): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()
        every { document.metadata } returns mockk { every { isFromCache } returns cached }
        every { document.get("tier") } returns tier
        every { document.get("subscriptionExpiresAtMillis") } returns expiry
        every { document.get("deletionInProgress") } returns false
        return document
    }

    private fun stateSnapshot(state: String?, cached: Boolean): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()
        every { document.metadata } returns mockk { every { isFromCache } returns cached }
        every { document.get("subscriptionState") } returns state
        return document
    }

    private fun productSnapshot(productId: String?, cached: Boolean): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()
        every { document.metadata } returns mockk { every { isFromCache } returns cached }
        every { document.get("productId") } returns productId
        return document
    }
}
