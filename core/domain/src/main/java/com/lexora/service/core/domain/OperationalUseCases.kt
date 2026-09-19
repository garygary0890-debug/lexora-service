package com.lexora.service.core.domain

import com.lexora.service.core.model.*

class LoadOperationalDashboardUseCase(private val operations: ServiceOperations) {
    suspend operator fun invoke(organizationId: String, nowEpochMs: Long, dayStartEpochMs: Long, dayEndEpochMs: Long): OperationalDashboard {
        val requests = operations.requests(organizationId)
        val visits = operations.visits(organizationId, null).visits
        val organization = operations.organization(organizationId)
        val finance = operations.finance(organizationId)
        val clients = operations.clients(organizationId)
        val vehicles = operations.vehicles(organizationId)
        val assets = operations.assets(organizationId)
        val open = requests.filterNot { it.status == RequestStatus.CLOSED || it.status == RequestStatus.CANCELLED }
        val overdue = open.count { request ->
            val deadline = request.slaDeadlineEpochMs ?: request.dueAtEpochMs
            deadline != null && deadline < nowEpochMs
        }
        val today = visits.filter { visit -> visit.plannedStartEpochMs?.let { it in dayStartEpochMs until dayEndEpochMs } == true }
        val unsynced = sequenceOf(
            clients.active + clients.archived,
            vehicles.active + vehicles.archived,
            assets.objects + assets.archivedObjects,
            assets.equipment + assets.archivedEquipment,
            requests,
            visits,
            finance.documents,
            finance.payments,
        ).sumOf { items -> items.count { entity -> syncState(entity) != SyncState.SYNCED } }
        val planning = LoadPlanningUseCase(operations)(organizationId, nowEpochMs, dayEndEpochMs + 7 * DAY_MS)
        return OperationalDashboard(
            openRequests = open.size,
            urgentRequests = open.count { it.priority == RequestPriority.URGENT },
            overdueRequests = overdue,
            todayVisits = today.size,
            activeEmployees = organization.employees.count { it.active },
            unsyncedEntities = unsynced,
            plannedPaymentsMinor = finance.payments.filter { it.status == PaymentStatus.PLANNED }.sumOf { it.amountMinor },
            nextVisits = planning.events.filter { it.startAtEpochMs >= nowEpochMs }.take(5),
        )
    }

    private fun syncState(value: Any): SyncState = when (value) {
        is Client -> value.syncState
        is Vehicle -> value.syncState
        is ServiceObject -> value.syncState
        is Equipment -> value.syncState
        is ServiceRequest -> value.syncState
        is ServiceVisit -> value.syncState
        is ServiceDocument -> value.syncState
        is Payment -> value.syncState
        else -> SyncState.SYNCED
    }

    private companion object { const val DAY_MS = 86_400_000L }
}

class GlobalSearchUseCase(private val operations: ServiceOperations) {
    suspend operator fun invoke(organizationId: String, rawQuery: String, limit: Int = 100): List<GlobalSearchResult> {
        val query = rawQuery.trim().lowercase()
        if (query.length < 2) return emptyList()
        val clients = operations.clients(organizationId).active
        val vehicles = operations.vehicles(organizationId).active
        val assets = operations.assets(organizationId)
        val requests = operations.requests(organizationId)
        val finance = operations.finance(organizationId)
        val organization = operations.organization(organizationId)
        val results = buildList {
            clients.forEach { value -> match(query, value.displayName, value.phone, value.email, value.taxId)?.let { score -> add(GlobalSearchResult(GlobalSearchEntityType.CLIENT, value.id, value.displayName, value.phone ?: value.email, "clients", score)) } }
            vehicles.forEach { value -> match(query, value.registrationNumber, value.vin, value.make, value.model)?.let { score -> add(GlobalSearchResult(GlobalSearchEntityType.VEHICLE, value.id, value.registrationNumber, listOfNotNull(value.make, value.model).joinToString(" ").ifBlank { null }, "vehicles", score)) } }
            assets.objects.forEach { value -> match(query, value.name, value.address, value.responsibleContact)?.let { score -> add(GlobalSearchResult(GlobalSearchEntityType.SERVICE_OBJECT, value.id, value.name, value.address, "assets", score)) } }
            assets.equipment.forEach { value -> match(query, value.type, value.make, value.model, value.serialNumber, value.inventoryNumber, value.barcode)?.let { score -> add(GlobalSearchResult(GlobalSearchEntityType.EQUIPMENT, value.id, listOfNotNull(value.make, value.model).joinToString(" ").ifBlank { value.type }, value.serialNumber ?: value.inventoryNumber, "assets", score)) } }
            requests.forEach { value -> match(query, value.number, value.title, value.description)?.let { score -> add(GlobalSearchResult(GlobalSearchEntityType.REQUEST, value.id, "${value.number} · ${value.title}", value.status.name, "requests", score)) } }
            finance.documents.forEach { value -> match(query, value.number, value.note)?.let { score -> add(GlobalSearchResult(GlobalSearchEntityType.DOCUMENT, value.id, value.number, value.type.name, "documents", score)) } }
            finance.payments.forEach { value -> match(query, value.externalReference, value.note, value.id)?.let { score -> add(GlobalSearchResult(GlobalSearchEntityType.PAYMENT, value.id, "Платёж ${value.amountMinor / 100.0} ${value.currency}", value.status.name, "documents", score)) } }
            organization.employees.forEach { value -> match(query, value.displayName, value.position, value.phone, value.email)?.let { score -> add(GlobalSearchResult(GlobalSearchEntityType.EMPLOYEE, value.id, value.displayName, value.position, "organization", score)) } }
        }
        return results.sortedWith(compareByDescending<GlobalSearchResult> { it.score }.thenBy { it.title }).take(limit)
    }

    private fun match(query: String, vararg values: String?): Int? {
        val normalized = values.mapNotNull { it?.trim()?.lowercase()?.takeIf(String::isNotBlank) }
        if (normalized.none { query in it }) return null
        return normalized.maxOf { value -> when { value == query -> 100; value.startsWith(query) -> 80; else -> 50 } }
    }
}

class LoadPlanningUseCase(private val operations: ServiceOperations) {
    suspend operator fun invoke(organizationId: String, rangeStartEpochMs: Long, rangeEndEpochMs: Long): PlanningSnapshot {
        require(rangeEndEpochMs > rangeStartEpochMs)
        val visits = operations.visits(organizationId, null).visits
        val requests = operations.requests(organizationId).associateBy { it.id }
        val org = operations.organization(organizationId)
        val employees = org.employees.associateBy { it.id }
        val branches = org.branches.associateBy { it.id }
        val events = visits.mapNotNull { visit ->
            val start = visit.plannedStartEpochMs ?: return@mapNotNull null
            val end = visit.plannedEndEpochMs ?: (start + DEFAULT_VISIT_MS)
            if (end <= rangeStartEpochMs || start >= rangeEndEpochMs) return@mapNotNull null
            val request = requests[visit.requestId]
            val employee = visit.employeeId?.let(employees::get)
            val branch = visit.branchId?.let(branches::get)
            PlanningEvent(
                id = visit.id,
                requestId = visit.requestId,
                requestNumber = request?.number ?: visit.requestId,
                title = request?.title ?: "Сервисный выезд",
                employeeId = employee?.id,
                employeeName = employee?.displayName,
                branchId = branch?.id,
                branchName = branch?.name,
                startAtEpochMs = start,
                endAtEpochMs = end,
                status = visit.status,
                priority = request?.priority ?: RequestPriority.NORMAL,
            )
        }.sortedBy { it.startAtEpochMs }

        val rangeMinutes = ((rangeEndEpochMs - rangeStartEpochMs) / 60_000L).toInt().coerceAtLeast(1)
        val workCapacity = capacityMinutes(rangeMinutes)
        val employeeLoads = org.employees.filter { it.active }.map { employee ->
            resourceLoad(employee.id, PlanningResourceKind.EMPLOYEE, employee.displayName, workCapacity, events.filter { it.employeeId == employee.id })
        }.sortedByDescending { it.utilizationPercent }
        val teamLoads = org.branches.filter { it.active }.map { branch ->
            resourceLoad(branch.id, PlanningResourceKind.TEAM, branch.name, workCapacity * org.employees.count { it.active && it.branchId == branch.id }.coerceAtLeast(1), events.filter { it.branchId == branch.id })
        }.sortedByDescending { it.utilizationPercent }
        return PlanningSnapshot(rangeStartEpochMs, rangeEndEpochMs, events, employeeLoads, teamLoads)
    }

    private fun resourceLoad(id: String, kind: PlanningResourceKind, name: String, capacity: Int, events: List<PlanningEvent>): ResourceLoad {
        val scheduled = events.sumOf { ((it.endAtEpochMs - it.startAtEpochMs).coerceAtLeast(0L) / 60_000L).toInt() }
        val percent = if (capacity <= 0) 0 else ((scheduled * 100.0) / capacity).toInt()
        return ResourceLoad(id, kind, name, scheduled, capacity, percent, events.size, percent > 100)
    }

    private fun capacityMinutes(rangeMinutes: Int): Int {
        val days = (rangeMinutes / (24 * 60.0)).coerceAtLeast(1.0)
        return (days * 8 * 60).toInt()
    }

    private companion object { const val DEFAULT_VISIT_MS = 60 * 60 * 1000L }
}
