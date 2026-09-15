package com.lexora.service.core.model

data class ReferenceDirectory(
    val id: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val description: String? = null,
    val system: Boolean = false,
    val active: Boolean = true,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class ReferenceDirectoryItem(
    val id: String,
    val organizationId: String,
    val directoryId: String,
    val code: String,
    val name: String,
    val sortOrder: Int = 0,
    val active: Boolean = true,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)
