package com.lexora.service.core.model

enum class ServiceQueueStatus { WAITING, CALLED, IN_SERVICE, COMPLETED, CANCELLED }

data class ServiceQueueEntry(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val moduleCode: String,
    val orderId: String,
    val publicNumber: String,
    val position: Int,
    val priority: Int = 0,
    val plannedAtEpochMs: Long? = null,
    val postId: String? = null,
    val status: ServiceQueueStatus = ServiceQueueStatus.WAITING,
    val queuedAtEpochMs: Long,
)

data class PublicQueueItem(
    val publicNumber: String,
    val position: Int,
    val plannedAtEpochMs: Long? = null,
    val postLabel: String? = null,
    val status: ServiceQueueStatus,
)

data class PublicQueueSnapshot(
    val branchId: String,
    val moduleCode: String,
    val generatedAtEpochMs: Long,
    val entries: List<PublicQueueItem>,
)
