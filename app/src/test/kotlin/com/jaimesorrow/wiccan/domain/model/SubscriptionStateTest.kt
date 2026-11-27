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
}
