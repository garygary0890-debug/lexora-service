package com.lexora.service.application

import com.lexora.service.core.database.*
import com.lexora.service.core.domain.*
import com.lexora.service.core.model.*
import com.lexora.service.core.data.SyncQueueRepository
import org.json.JSONObject
import java.util.UUID

class ServiceApplicationService(
    private val dao: ServiceDao,
    private val syncQueue: SyncQueueRepository,
) : ServiceOperations {
    override suspend fun ensureDemoClient(organizationId: String) {
        if (dao.clients(organizationId).isNotEmpty() || dao.archivedClients(organizationId).isNotEmpty()) return
        val now = System.currentTimeMillis()
        dao.upsertClient(
            ClientEntity(
                id = "client-demo-1-$organizationId",
                organizationId = organizationId,
                type = ClientType.PERSON.name,
                displayName = "Демонстрационный клиент",
                phone = "+7 900 000-00-00",
                email = null,
                taxId = null,
                kpp = null,
                registrationAddress = null,
                actualAddress = null,
                note = null,
                consentPersonalData = false,
                archived = false,
                syncState = SyncState.PENDING_CREATE.name,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    override suspend fun clients(organizationId: String) = ClientCollection(
        active = dao.clients(organizationId).map(ClientEntity::toModel),
        archived = dao.archivedClients(organizationId).map(ClientEntity::toModel),
    )

    override suspend fun saveClient(organizationId: String, userId: String, draft: SaveClientCommand, existingId: String?) {
        val now = System.currentTimeMillis()
        val id = existingId ?: UUID.randomUUID().toString()
        val existing = existingId?.let { dao.client(it) }
        dao.upsertClient(
            ClientEntity(
                id, organizationId, draft.type.name, draft.displayName,
                draft.phone.ifBlank { null }, draft.email.ifBlank { null }, draft.taxId.ifBlank { null }, draft.kpp.ifBlank { null },
                draft.registrationAddress.ifBlank { null }, draft.actualAddress.ifBlank { null }, draft.note.ifBlank { null },
                draft.consentPersonalData, existing?.archived ?: false,
                if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name,
                existing?.createdAtEpochMs ?: now, now,
            ),
        )
        syncQueue.enqueue(organizationId, "ServiceClient", id, if (existing == null) SyncOperationType.CREATE else SyncOperationType.UPDATE, JSONObject().put("displayName", draft.displayName).put("phone", draft.phone.ifBlank { JSONObject.NULL }).put("email", draft.email.ifBlank { JSONObject.NULL }).toString())
        audit(organizationId, userId, "CLIENT", id, if (existing == null) "CREATE" else "UPDATE", draft.displayName)
    }

    override suspend fun archiveClient(organizationId: String, userId: String, id: String, restore: Boolean) {
        val name = dao.client(id)?.displayName.orEmpty()
        val now = System.currentTimeMillis()
        if (restore) dao.restoreClient(id, SyncState.PENDING_UPDATE.name, now) else dao.archiveClient(id, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "CLIENT", id, if (restore) "RESTORE" else "ARCHIVE", name)
    }

    override suspend fun vehicles(organizationId: String) = VehicleCollection(
        active = dao.vehicles(organizationId).map(VehicleEntity::toModel),
        archived = dao.archivedVehicles(organizationId).map(VehicleEntity::toModel),
    )

    override suspend fun saveVehicle(organizationId: String, userId: String, draft: SaveVehicleCommand, existingId: String?) {
        val now = System.currentTimeMillis()
        val id = existingId ?: UUID.randomUUID().toString()
        val existing = existingId?.let { dao.vehicle(it) }
        dao.upsertVehicle(
            VehicleEntity(
                id, organizationId, draft.clientId, draft.registrationNumber.trim().uppercase(), draft.vin.trim().uppercase().ifBlank { null },
                draft.make.trim().ifBlank { null }, draft.model.trim().ifBlank { null }, draft.year.toIntOrNull(), draft.bodyType.trim().ifBlank { null },
                draft.color.trim().ifBlank { null }, draft.mileageKm.toIntOrNull(), existing?.archived ?: false,
                if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name,
                existing?.createdAtEpochMs ?: now, now,
            ),
        )
        syncQueue.enqueue(organizationId, "ServiceAsset", id, if (existing == null) SyncOperationType.CREATE else SyncOperationType.UPDATE, JSONObject().put("clientId", draft.clientId).put("registrationNumber", draft.registrationNumber.trim().uppercase()).put("make", draft.make.trim()).put("model", draft.model.trim()).toString())
        audit(organizationId, userId, "VEHICLE", id, if (existing == null) "CREATE" else "UPDATE", draft.registrationNumber.trim().uppercase())
    }

    override suspend fun archiveVehicle(organizationId: String, userId: String, id: String, restore: Boolean) {
        val value = dao.vehicle(id)?.registrationNumber.orEmpty()
        val now = System.currentTimeMillis()
        if (restore) dao.restoreVehicle(id, SyncState.PENDING_UPDATE.name, now) else dao.archiveVehicle(id, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "VEHICLE", id, if (restore) "RESTORE" else "ARCHIVE", value)
    }

    override suspend fun assets(organizationId: String) = AssetCollection(
        objects = dao.serviceObjects(organizationId).map(ServiceObjectEntity::toModel),
        archivedObjects = dao.archivedServiceObjects(organizationId).map(ServiceObjectEntity::toModel),
        equipment = dao.equipment(organizationId).map(EquipmentEntity::toModel),
        archivedEquipment = dao.archivedEquipment(organizationId).map(EquipmentEntity::toModel),
    )

    override suspend fun saveServiceObject(organizationId: String, userId: String, draft: SaveServiceObjectCommand, existingId: String?) {
        val now = System.currentTimeMillis(); val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.serviceObject(it) }
        dao.upsertServiceObject(ServiceObjectEntity(id, organizationId, draft.clientId, draft.name.trim(), draft.address.trim().ifBlank { null }, draft.accessMode.trim().ifBlank { null }, draft.responsibleContact.trim().ifBlank { null }, existing?.archived ?: false, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, existing?.createdAtEpochMs ?: now, now))
        audit(organizationId, userId, "SERVICE_OBJECT", id, if (existing == null) "CREATE" else "UPDATE", draft.name.trim())
    }

    override suspend fun archiveServiceObject(organizationId: String, userId: String, id: String, restore: Boolean) {
        val value = dao.serviceObject(id)?.name.orEmpty(); val now = System.currentTimeMillis()
        if (restore) dao.restoreServiceObject(id, SyncState.PENDING_UPDATE.name, now) else dao.archiveServiceObject(id, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "SERVICE_OBJECT", id, if (restore) "RESTORE" else "ARCHIVE", value)
    }

    override suspend fun saveEquipment(organizationId: String, userId: String, draft: SaveEquipmentCommand, existingId: String?) {
        val now = System.currentTimeMillis(); val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.equipmentItem(it) }
        dao.upsertEquipment(EquipmentEntity(id, organizationId, draft.serviceObjectId, draft.type.trim(), draft.make.trim().ifBlank { null }, draft.model.trim().ifBlank { null }, draft.serialNumber.trim().uppercase().ifBlank { null }, draft.inventoryNumber.trim().ifBlank { null }, draft.barcode.trim().ifBlank { null }, draft.commissionedNote.trim().ifBlank { null }, draft.warrantyNote.trim().ifBlank { null }, existing?.archived ?: false, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, existing?.createdAtEpochMs ?: now, now))
        audit(organizationId, userId, "EQUIPMENT", id, if (existing == null) "CREATE" else "UPDATE", draft.serialNumber.ifBlank { draft.type })
    }

    override suspend fun archiveEquipment(organizationId: String, userId: String, id: String, restore: Boolean) {
        val value = dao.equipmentItem(id)?.serialNumber.orEmpty(); val now = System.currentTimeMillis()
        if (restore) dao.restoreEquipment(id, SyncState.PENDING_UPDATE.name, now) else dao.archiveEquipment(id, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "EQUIPMENT", id, if (restore) "RESTORE" else "ARCHIVE", value)
    }

    override suspend fun organization(organizationId: String) = OrganizationCollection(
        branches = dao.branches(organizationId).map(BranchEntity::toModel),
        inactiveBranches = dao.inactiveBranches(organizationId).map(BranchEntity::toModel),
        employees = dao.employees(organizationId).map(EmployeeEntity::toModel),
        inactiveEmployees = dao.inactiveEmployees(organizationId).map(EmployeeEntity::toModel),
    )

    override suspend fun saveBranch(organizationId: String, userId: String, draft: SaveBranchCommand, existingId: String?) {
        val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.branch(it) }; val now = System.currentTimeMillis()
        dao.upsertBranch(BranchEntity(id, organizationId, draft.name, draft.address.ifBlank { null }, draft.phone.ifBlank { null }, draft.email.ifBlank { null }, draft.workSchedule.ifBlank { null }, draft.timeZoneId, existing?.active ?: true, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, now))
        audit(organizationId, userId, "BRANCH", id, if (existing == null) "CREATE" else "UPDATE", draft.name)
    }

    override suspend fun setBranchActive(organizationId: String, userId: String, id: String, active: Boolean) {
        val value = dao.branch(id)?.name.orEmpty(); val now = System.currentTimeMillis()
        if (active) dao.activateBranch(id, SyncState.PENDING_UPDATE.name, now) else dao.deactivateBranch(id, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "BRANCH", id, if (active) "ACTIVATE" else "DEACTIVATE", value)
    }

    override suspend fun saveEmployee(organizationId: String, userId: String, draft: SaveEmployeeCommand, existingId: String?) {
        val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.employee(it) }; val now = System.currentTimeMillis()
        dao.upsertEmployee(EmployeeEntity(id, organizationId, draft.branchId, draft.displayName, draft.position.ifBlank { null }, draft.phone.ifBlank { null }, draft.email.ifBlank { null }, existing?.active ?: true, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, now))
        audit(organizationId, userId, "EMPLOYEE", id, if (existing == null) "CREATE" else "UPDATE", draft.displayName)
    }

    override suspend fun setEmployeeActive(organizationId: String, userId: String, id: String, active: Boolean) {
        val value = dao.employee(id)?.displayName.orEmpty(); val now = System.currentTimeMillis()
        if (active) dao.activateEmployee(id, SyncState.PENDING_UPDATE.name, now) else dao.deactivateEmployee(id, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "EMPLOYEE", id, if (active) "ACTIVATE" else "DEACTIVATE", value)
    }

    override suspend fun requests(organizationId: String): List<ServiceRequest> = dao.serviceRequests(organizationId).map(ServiceRequestEntity::toModel)

    override suspend fun saveRequest(organizationId: String, userId: String, draft: SaveRequestCommand, existingId: String?) {
        val now = System.currentTimeMillis(); val existing = existingId?.let { dao.serviceRequest(it) }; val id = existingId ?: UUID.randomUUID().toString()
        val number = existing?.number ?: run {
            val max = dao.serviceRequests(organizationId).mapNotNull { it.number.removePrefix("REQ-").toIntOrNull() }.maxOrNull() ?: 0
            "REQ-%06d".format(max + 1)
        }
        dao.upsertServiceRequest(ServiceRequestEntity(id, organizationId, number, existing?.clientId, existing?.vehicleId, existing?.serviceObjectId, existing?.equipmentId, existing?.branchId, existing?.assigneeEmployeeId, draft.title, draft.description.ifBlank { null }, existing?.status ?: RequestStatus.NEW.name, draft.priority.name, existing?.plannedAtEpochMs, existing?.dueAtEpochMs, existing?.slaDeadlineEpochMs, existing?.closedAtEpochMs, existing?.archived ?: false, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, existing?.createdAtEpochMs ?: now, now))
        if (existing == null) dao.insertRequestStatusHistory(RequestStatusHistoryEntity(UUID.randomUUID().toString(), id, null, RequestStatus.NEW.name, userId, now, "Создание заявки"))
        audit(organizationId, userId, "SERVICE_REQUEST", id, if (existing == null) "CREATE" else "UPDATE", "$number · ${draft.title}")
    }

    override suspend fun changeRequestStatus(organizationId: String, userId: String, id: String, target: RequestStatus) {
        val current = dao.serviceRequest(id) ?: return; val from = RequestStatus.valueOf(current.status)
        if (!RequestWorkflow.canTransition(from, target) || from == target) return
        val now = System.currentTimeMillis()
        dao.updateRequestStatus(id, target.name, SyncState.PENDING_UPDATE.name, now, if (target == RequestStatus.CLOSED) now else null)
        dao.insertRequestStatusHistory(RequestStatusHistoryEntity(UUID.randomUUID().toString(), id, from.name, target.name, userId, now, null))
        audit(organizationId, userId, "SERVICE_REQUEST", id, "STATUS_CHANGE", "${current.number}: ${from.name} → ${target.name}")
    }

    override suspend fun visits(organizationId: String, selectedVisitId: String?): VisitCollection {
        val visits = dao.serviceVisits(organizationId).map(ServiceVisitEntity::toModel)
        val selected = selectedVisitId?.takeIf { id -> visits.any { it.id == id } } ?: visits.firstOrNull()?.id
        return VisitCollection(visits, selected, selected?.let { checklist(it) }.orEmpty())
    }

    override suspend fun checklist(visitId: String): List<VisitChecklistItem> = dao.visitChecklist(visitId).map(VisitChecklistItemEntity::toModel)

    override suspend fun createVisit(organizationId: String, userId: String, requestId: String, employeeId: String?) {
        val request = dao.serviceRequest(requestId) ?: return; val now = System.currentTimeMillis(); val id = UUID.randomUUID().toString()
        dao.upsertServiceVisit(ServiceVisitEntity(id, organizationId, requestId, request.branchId, employeeId ?: request.assigneeEmployeeId, VisitStatus.PLANNED.name, request.plannedAtEpochMs, request.dueAtEpochMs, null, null, null, null, null, SyncState.PENDING_CREATE.name, now, now))
        audit(organizationId, userId, "SERVICE_VISIT", id, "CREATE", "Выезд по заявке ${request.number}")
    }

    override suspend fun changeVisitStatus(organizationId: String, userId: String, id: String, target: VisitStatus) {
        val current = dao.serviceVisit(id) ?: return; val from = VisitStatus.valueOf(current.status)
        if (from == VisitStatus.COMPLETED || from == VisitStatus.CANCELLED || from == target) return
        val allowed = when (from) {
            VisitStatus.PLANNED -> target == VisitStatus.EN_ROUTE || target == VisitStatus.CANCELLED
            VisitStatus.EN_ROUTE -> target == VisitStatus.ON_SITE || target == VisitStatus.CANCELLED
            VisitStatus.ON_SITE -> target == VisitStatus.COMPLETED || target == VisitStatus.CANCELLED
            VisitStatus.COMPLETED, VisitStatus.CANCELLED -> false
        }
        if (!allowed) return
        val now = System.currentTimeMillis(); val actualStart = if (target == VisitStatus.EN_ROUTE || target == VisitStatus.ON_SITE) now else current.actualStartEpochMs; val actualEnd = if (target == VisitStatus.COMPLETED || target == VisitStatus.CANCELLED) now else null
        dao.updateVisitStatus(id, target.name, actualStart, actualEnd, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "SERVICE_VISIT", id, "STATUS_CHANGE", "${from.name} → ${target.name}")
    }

    override suspend fun addChecklistItem(organizationId: String, userId: String, visitId: String) {
        val now = System.currentTimeMillis(); val nextOrder = dao.visitChecklist(visitId).maxOfOrNull { it.sortOrder }?.plus(1) ?: 0; val id = UUID.randomUUID().toString()
        dao.upsertVisitChecklistItem(VisitChecklistItemEntity(id, visitId, "Новый пункт чек-листа", ChecklistItemState.PENDING.name, null, nextOrder, SyncState.PENDING_CREATE.name, now))
        audit(organizationId, userId, "VISIT_CHECKLIST_ITEM", id, "CREATE", "Добавлен пункт чек-листа")
    }

    override suspend fun toggleChecklistItem(organizationId: String, userId: String, current: VisitChecklistItem) {
        val next = if (current.state == ChecklistItemState.DONE) ChecklistItemState.PENDING else ChecklistItemState.DONE
        dao.updateChecklistItemState(current.id, next.name, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
        audit(organizationId, userId, "VISIT_CHECKLIST_ITEM", current.id, "STATE_CHANGE", "${current.state.name} → ${next.name}")
    }

    override suspend fun finance(organizationId: String) = FinanceCollection(
        documents = dao.serviceDocuments(organizationId).map(ServiceDocumentEntity::toModel),
        payments = dao.payments(organizationId).map(PaymentEntity::toModel),
    )

    override suspend fun createDocument(organizationId: String, userId: String, type: ServiceDocumentType, requestId: String?) {
        val now = System.currentTimeMillis(); val request = requestId?.let { dao.serviceRequest(it) }
        val prefix = when (type) { ServiceDocumentType.WORK_ORDER -> "WO"; ServiceDocumentType.ACT -> "ACT"; ServiceDocumentType.INVOICE -> "INV" }
        val max = dao.serviceDocuments(organizationId).filter { it.number.startsWith("$prefix-") }.mapNotNull { it.number.removePrefix("$prefix-").toIntOrNull() }.maxOrNull() ?: 0
        val number = "$prefix-%06d".format(max + 1); val id = UUID.randomUUID().toString(); val visitId = requestId?.let { dao.visitsForRequest(it).firstOrNull()?.id }
        dao.upsertServiceDocument(ServiceDocumentEntity(id, organizationId, requestId, visitId, request?.clientId, type.name, number, ServiceDocumentStatus.DRAFT.name, null, 0L, "RUB", null, null, false, SyncState.PENDING_CREATE.name, now, now))
        audit(organizationId, userId, "SERVICE_DOCUMENT", id, "CREATE", "$number · ${type.name}")
    }

    override suspend fun changeDocumentStatus(organizationId: String, userId: String, id: String, target: ServiceDocumentStatus) {
        val current = dao.serviceDocument(id) ?: return; val from = ServiceDocumentStatus.valueOf(current.status)
        val allowed = when (from) {
            ServiceDocumentStatus.DRAFT -> target == ServiceDocumentStatus.ISSUED || target == ServiceDocumentStatus.CANCELLED
            ServiceDocumentStatus.ISSUED -> target == ServiceDocumentStatus.SIGNED || target == ServiceDocumentStatus.CANCELLED
            ServiceDocumentStatus.SIGNED, ServiceDocumentStatus.CANCELLED -> false
        }
        if (!allowed) return
        val now = System.currentTimeMillis()
        dao.updateServiceDocumentStatus(id, target.name, if (target == ServiceDocumentStatus.ISSUED) now else current.issuedAtEpochMs, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "SERVICE_DOCUMENT", id, "STATUS_CHANGE", "${current.number}: ${from.name} → ${target.name}")
    }

    override suspend fun createPayment(organizationId: String, userId: String, requestId: String?, currentDocuments: List<ServiceDocument>) {
        val now = System.currentTimeMillis(); val request = requestId?.let { dao.serviceRequest(it) }; val linkedDocument = currentDocuments.firstOrNull { it.requestId == requestId && !it.archived }; val id = UUID.randomUUID().toString()
        dao.upsertPayment(PaymentEntity(id, organizationId, requestId, linkedDocument?.id, request?.clientId, linkedDocument?.totalMinor ?: 0L, linkedDocument?.currency ?: "RUB", PaymentStatus.PLANNED.name, PaymentMethod.BANK_TRANSFER.name, null, null, null, false, SyncState.PENDING_CREATE.name, now, now))
        audit(organizationId, userId, "PAYMENT", id, "CREATE", "Платёж по заявке ${request?.number.orEmpty()}")
    }

    override suspend fun markPaymentPaid(organizationId: String, userId: String, id: String) {
        val current = dao.payment(id) ?: return
        if (PaymentStatus.valueOf(current.status) != PaymentStatus.PLANNED) return
        val now = System.currentTimeMillis()
        dao.updatePaymentStatus(id, PaymentStatus.PAID.name, now, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "PAYMENT", id, "STATUS_CHANGE", "PLANNED → PAID")
    }

    private suspend fun audit(organizationId: String, userId: String, entityType: String, entityId: String?, action: String, summary: String) {
        dao.insertAuditEvent(AuditEventEntity(UUID.randomUUID().toString(), organizationId, userId, entityType, entityId, action, summary, System.currentTimeMillis()))
    }
}

private fun ClientEntity.toModel() = Client(id, organizationId, ClientType.valueOf(type), displayName, phone, email, taxId, kpp, registrationAddress, actualAddress, note, consentPersonalData, archived, SyncState.valueOf(syncState))
private fun VehicleEntity.toModel() = Vehicle(id, organizationId, clientId, registrationNumber, vin, make, model, year, bodyType, color, mileageKm, archived, SyncState.valueOf(syncState))
private fun ServiceObjectEntity.toModel() = ServiceObject(id, organizationId, clientId, name, address, accessMode, responsibleContact, archived, SyncState.valueOf(syncState))
private fun EquipmentEntity.toModel() = Equipment(id, organizationId, serviceObjectId, type, make, model, serialNumber, inventoryNumber, barcode, commissionedNote, warrantyNote, archived, SyncState.valueOf(syncState))
private fun BranchEntity.toModel() = Branch(id, organizationId, name, address, phone, email, workSchedule, timeZoneId, active, SyncState.valueOf(syncState))
private fun EmployeeEntity.toModel() = Employee(id, organizationId, branchId, displayName, position, phone, email, active, SyncState.valueOf(syncState))
private fun ServiceRequestEntity.toModel() = ServiceRequest(id, organizationId, number, clientId, vehicleId, serviceObjectId, equipmentId, branchId, assigneeEmployeeId, title, description, RequestStatus.valueOf(status), RequestPriority.valueOf(priority), plannedAtEpochMs, dueAtEpochMs, slaDeadlineEpochMs, closedAtEpochMs, archived, SyncState.valueOf(syncState))
private fun ServiceVisitEntity.toModel() = ServiceVisit(id, organizationId, requestId, branchId, employeeId, VisitStatus.valueOf(status), plannedStartEpochMs, plannedEndEpochMs, actualStartEpochMs, actualEndEpochMs, resultNote, customerName, customerSignatureRef, SyncState.valueOf(syncState))
private fun VisitChecklistItemEntity.toModel() = VisitChecklistItem(id, visitId, title, ChecklistItemState.valueOf(state), comment, sortOrder, SyncState.valueOf(syncState))
private fun ServiceDocumentEntity.toModel() = ServiceDocument(id, organizationId, requestId, visitId, clientId, ServiceDocumentType.valueOf(type), number, ServiceDocumentStatus.valueOf(status), issuedAtEpochMs, totalMinor, currency, externalFileRef, note, archived, SyncState.valueOf(syncState))
private fun PaymentEntity.toModel() = Payment(id, organizationId, requestId, documentId, clientId, amountMinor, currency, PaymentStatus.valueOf(status), PaymentMethod.valueOf(method), paidAtEpochMs, externalReference, note, archived, SyncState.valueOf(syncState))
