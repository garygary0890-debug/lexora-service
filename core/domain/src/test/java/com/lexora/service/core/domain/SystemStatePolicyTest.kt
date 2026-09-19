package com.lexora.service.core.domain

import com.lexora.service.core.model.RequiredSystemState
import com.lexora.service.core.model.SyncIssue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemStatePolicyTest {
    private val policy = SystemStatePolicy()

    @Test fun `offline with cached content remains usable`() {
        val state = policy.resolve(loading = false, hasContent = true, online = false)
        assertEquals(RequiredSystemState.OFFLINE, state.primary)
        assertTrue(state.offline)
    }

    @Test fun `sync failure becomes common sync error state`() {
        val state = policy.resolve(false, true, true, syncIssue = SyncIssue(failedCount = 1, lastError = "boom"))
        assertEquals(RequiredSystemState.SYNC_ERROR, state.primary)
    }

    @Test fun `permission denial overrides content`() {
        val state = policy.resolve(false, true, true, permissionGranted = false)
        assertEquals(RequiredSystemState.PERMISSION_DENIED, state.primary)
    }

    @Test fun `fatal error has highest priority`() {
        val state = policy.resolve(false, true, false, permissionGranted = false, technicalReference = "ERR42")
        assertEquals(RequiredSystemState.FATAL_TECHNICAL_ERROR, state.primary)
    }

    @Test fun `offline without local data still shows empty state`() {
        val state = policy.resolve(false, false, false)
        assertEquals(RequiredSystemState.EMPTY, state.primary)
        assertTrue(state.offline)
    }
}