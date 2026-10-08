package com.lexora.service.core.model

/** Grants a user explicit access to another user's workspace. */
data class UserWorkspaceMembership(
    val ownerUserId: String,
    val memberUserId: String,
    val roles: Set<UserRole>,
    val active: Boolean = true,
)
