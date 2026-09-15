package com.lexora.service.core.model

enum class SyncState { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE, ERROR }
enum class ClientType { PERSON, COMPANY }

data class Client(
    val id: String,
    val organizationId: String,
    val type: ClientType,
    val displayName: String,
    val phone: String? = null,
    val email: String? = null,
    val taxId: String? = null,
    val kpp: String? = null,
    val registrationAddress: String? = null,
    val actualAddress: String? = null,
    val note: String? = null,
    val consentPersonalData: Boolean = false,
    val archived: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class Vehicle(
    val id: String,
    val organizationId: String,
    val clientId: String?,
    val registrationNumber: String,
    val vin: String? = null,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val bodyType: String? = null,
    val color: String? = null,
    val mileageKm: Int? = null,
    val archived: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class Branch(
    val id: String,
    val organizationId: String,
    val name: String,
    val address: String? = null,
    val timeZoneId: String,
    val active: Boolean = true,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class Employee(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val displayName: String,
    val position: String? = null,
    val active: Boolean = true,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)
