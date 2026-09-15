package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_contracts",
    indices = [
        Index("organizationId"),
        Index("clientId"),
        Index("branchId"),
        Index("status"),
        Index("endAtEpochMs"),
        Index(value = ["organizationId", "number"], unique = true),
    ],
)
data class ServiceContractEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val clientId: String,
    val branchId: String?,
    val number: String,
    val subject: String,
    val status: String,
    val startAtEpochMs: Long?,
    val endAtEpochMs: Long?,
    val signedAtEpochMs: Long?,
    val externalFileRef: String?,
    val note: String?,
    val archived: Boolean,
    val syncState: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
