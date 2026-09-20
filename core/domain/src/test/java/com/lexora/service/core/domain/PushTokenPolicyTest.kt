package com.lexora.service.core.domain

import com.lexora.service.core.model.PushDeviceToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushTokenPolicyTest {
    private val policy = PushTokenPolicy()

    @Test
    fun `latest active token for user is selected`() {
        val tokens = listOf(
            PushDeviceToken("a", "org", "u", "dev1", "fcm", "old", true, 10),
            PushDeviceToken("b", "org", "u", "dev2", "fcm", "new", true, 20),
            PushDeviceToken("c", "org", "u", "dev3", "fcm", "disabled", false, 30),
        )
        assertEquals("new", policy.select(tokens)?.token)
    }

    @Test
    fun `no active token yields no push target`() {
        assertNull(policy.select(listOf(PushDeviceToken("a", "org", "u", "d", "fcm", "x", false, 1))))
    }
}
