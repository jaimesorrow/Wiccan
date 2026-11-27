package com.jaimesorrow.wiccan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionStateTest {
    @Test
    fun `wire state is strict and missing values fail closed`() {
        assertEquals(SubscriptionState.ON_HOLD, SubscriptionState.fromWire("on_hold"))
        assertEquals(SubscriptionState.IN_GRACE_PERIOD, SubscriptionState.fromWire("in_grace_period"))
        assertEquals(SubscriptionState.UNKNOWN, SubscriptionState.fromWire(null))
        assertEquals(SubscriptionState.UNKNOWN, SubscriptionState.fromWire("ACTIVE"))
    }

    @Test
    fun `every documented Play wire value maps and unknown tokens stay unknown`() {
        val expected = mapOf(
            "active" to SubscriptionState.ACTIVE,
            "in_grace_period" to SubscriptionState.IN_GRACE_PERIOD,
            "on_hold" to SubscriptionState.ON_HOLD,
            "paused" to SubscriptionState.PAUSED,
            "canceled" to SubscriptionState.CANCELED,
            "expired" to SubscriptionState.EXPIRED,
            "pending" to SubscriptionState.PENDING,
        )
        expected.forEach { (wire, state) ->
            assertEquals(wire, state, SubscriptionState.fromWire(wire))
        }
        for (wire in listOf("", " ", "on-hold", "grace", "cancelled", "unknown", "ACTIVE")) {
            assertEquals(wire, SubscriptionState.UNKNOWN, SubscriptionState.fromWire(wire))
        }
    }
}
