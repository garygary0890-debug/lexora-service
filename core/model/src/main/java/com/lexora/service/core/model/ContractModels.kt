package com.lexora.service.core.model

enum class ServiceContractStatus { DRAFT, ACTIVE, SUSPENDED, EXPIRED, TERMINATED }

data class ServiceContract(
    val id: String,
    val organizationId: String,
    val counterpartyClientId: String,
    val number: String,
    val signedAtEpochMs: Long? = null,
    val validFromEpochMs: Long,
    val validToEpochMs: Long? = null,
    val status: ServiceContractStatus = ServiceContractStatus.DRAFT,
    val branchIds: Set<String> = emptySet(),
    val vehicleIds: Set<String> = emptySet(),
    val objectIds: Set<String> = emptySet(),
    val priceListId: String? = null,
    val discountBasisPoints: Int? = null,
    val paymentTerms: String? = null,
    val slaCode: String? = null,
    val spendingLimitMinor: Long? = null,
    val documentIds: List<String> = emptyList(),
)

data class ContractControlDate(
    val contractId: String,
    val code: String,
    val dueAtEpochMs: Long,
    val reminderAtEpochMs: Long? = null,
)
