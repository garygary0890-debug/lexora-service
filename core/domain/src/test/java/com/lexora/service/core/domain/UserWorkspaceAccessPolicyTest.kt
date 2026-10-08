package com.lexora.service.core.domain

import com.lexora.service.core.model.Permission
import com.lexora.service.core.model.UserRole
import com.lexora.service.core.model.UserWorkspaceMembership
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserWorkspaceAccessPolicyTest {
    private val policy = UserWorkspaceAccessPolicy()

    @Test
    fun `workspace owner can read without a membership row`() {
        assertTrue(policy.canRead("owner", "owner", emptyList()))
    }

    @Test
    fun `active team membership grants read access only to its owner workspace`() {
        val memberships = listOf(
            UserWorkspaceMembership("owner", "member", setOf(UserRole.TECHNICIAN), active = true),
        )

        assertTrue(policy.canRead("member", "owner", memberships))
        assertFalse(policy.canRead("member", "another-owner", memberships))
    }

    @Test
    fun `inactive or missing membership denies workspace read`() {
        val inactive = listOf(
            UserWorkspaceMembership("owner", "member", setOf(UserRole.ADMIN), active = false),
        )

        assertFalse(policy.canRead("member", "owner", inactive))
        assertFalse(policy.canRead("stranger", "owner", emptyList()))
    }

    @Test
    fun `team role grants only its mapped permissions`() {
        val memberships = listOf(
            UserWorkspaceMembership("owner", "technician", setOf(UserRole.TECHNICIAN), active = true),
        )

        assertTrue(policy.can("technician", "owner", memberships, Permission.VIEW_WASH))
        assertFalse(policy.can("technician", "owner", memberships, Permission.MANAGE_USERS))
        assertFalse(policy.can("stranger", "owner", memberships, Permission.VIEW_WASH))
    }
}
