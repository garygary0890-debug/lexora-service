package com.lexora.service.core.domain

import com.lexora.service.core.model.RequestStatus

object RequestWorkflow {
    private val allowedTransitions: Map<RequestStatus, Set<RequestStatus>> = mapOf(
        RequestStatus.NEW to setOf(RequestStatus.QUALIFICATION, RequestStatus.CANCELLED),
        RequestStatus.QUALIFICATION to setOf(RequestStatus.PLANNED, RequestStatus.CANCELLED),
        RequestStatus.PLANNED to setOf(RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED),
        RequestStatus.IN_PROGRESS to setOf(RequestStatus.WAITING, RequestStatus.WORK_COMPLETED, RequestStatus.CANCELLED),
        RequestStatus.WAITING to setOf(RequestStatus.IN_PROGRESS, RequestStatus.WORK_COMPLETED, RequestStatus.CANCELLED),
        RequestStatus.WORK_COMPLETED to setOf(RequestStatus.CONFIRMATION, RequestStatus.IN_PROGRESS),
        RequestStatus.CONFIRMATION to setOf(RequestStatus.CLOSED, RequestStatus.IN_PROGRESS),
        RequestStatus.CLOSED to emptySet(),
        RequestStatus.CANCELLED to emptySet(),
    )

    fun canTransition(from: RequestStatus, to: RequestStatus): Boolean =
        from == to || allowedTransitions[from].orEmpty().contains(to)

    fun nextStatuses(from: RequestStatus): Set<RequestStatus> = allowedTransitions[from].orEmpty()
}
