package com.lexora.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lexora.service.core.data.InMemoryModuleRegistry
import com.lexora.service.core.data.InMemoryOrganizationRepository
import com.lexora.service.core.data.InMemoryUserRepository
import com.lexora.service.core.database.*
import com.lexora.service.core.designsystem.LexoraTheme
import com.lexora.service.core.domain.ModuleAccessPolicy
import com.lexora.service.core.domain.RequestWorkflow
import com.lexora.service.core.model.*
import com.lexora.service.core.navigation.Routes
import com.lexora.service.feature.assets.AssetsScreen
import com.lexora.service.feature.clients.ClientsScreen
import com.lexora.service.feature.fieldwork.FieldWorkScreen
import com.lexora.service.feature.home.HomeScreen
import com.lexora.service.feature.organization.OrganizationScreen
import com.lexora.service.feature.requests.RequestsScreen
import com.lexora.service.feature.settings.SettingsScreen
import com.lexora.service.feature.tires.TiresScreen
import com.lexora.service.feature.vehicles.VehiclesScreen
import com.lexora.service.feature.wash.WashScreen
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LexoraServiceApp() }
    }
}

@Composable
private fun LexoraServiceApp() {
    LexoraTheme {
        val context = LocalContext.current
        val database = remember { LexoraServiceDatabase.create(context) }
        val dao = remember(database) { database.serviceDao() }
        val organizationRepository = remember { InMemoryOrganizationRepository() }
        val userRepository = remember { InMemoryUserRepository() }
        val moduleRegistry = remember { InMemoryModuleRegistry() }
        val moduleAccessPolicy = remember { ModuleAccessPolicy() }
        val scope = rememberCoroutineScope()

        var stateVersion by remember { mutableIntStateOf(0) }
        var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
        var archivedClients by remember { mutableStateOf<List<Client>>(emptyList()) }
        var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
        var archivedVehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
        var serviceObjects by remember { mutableStateOf<List<ServiceObject>>(emptyList()) }
        var archivedServiceObjects by remember { mutableStateOf<List<ServiceObject>>(emptyList()) }
        var equipment by remember { mutableStateOf<List<Equipment>>(emptyList()) }
        var archivedEquipment by remember { mutableStateOf<List<Equipment>>(emptyList()) }
        var branches by remember { mutableStateOf<List<Branch>>(emptyList()) }
        var inactiveBranches by remember { mutableStateOf<List<Branch>>(emptyList()) }
        var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
        var inactiveEmployees by remember { mutableStateOf<List<Employee>>(emptyList()) }
        var requests by remember { mutableStateOf<List<ServiceRequest>>(emptyList()) }
        var visits by remember { mutableStateOf<List<ServiceVisit>>(emptyList()) }
        var selectedVisitId by remember { mutableStateOf<String?>(null) }
        var visitChecklist by remember { mutableStateOf<List<VisitChecklistItem>>(emptyList()) }

        val organization = requireNotNull(organizationRepository.activeOrganization())
        val user = userRepository.currentUser()
        val modules = moduleRegistry.modules(organization.id)
        val accessibleModules = modules.filter { moduleAccessPolicy.isAvailable(it, user) }

        suspend fun reloadClients() {
            clients = dao.clients(organization.id).map(ClientEntity::toModel)
            archivedClients = dao.archivedClients(organization.id).map(ClientEntity::toModel)
        }
        suspend fun reloadVehicles() {
            vehicles = dao.vehicles(organization.id).map(VehicleEntity::toModel)
            archivedVehicles = dao.archivedVehicles(organization.id).map(VehicleEntity::toModel)
        }
        suspend fun reloadAssets() {
            serviceObjects = dao.serviceObjects(organization.id).map(ServiceObjectEntity::toModel)
            archivedServiceObjects = dao.archivedServiceObjects(organization.id).map(ServiceObjectEntity::toModel)
            equipment = dao.equipment(organization.id).map(EquipmentEntity::toModel)
            archivedEquipment = dao.archivedEquipment(organization.id).map(EquipmentEntity::toModel)
        }
        suspend fun reloadOrganization() {
            branches = dao.branches(organization.id).map(BranchEntity::toModel)
            inactiveBranches = dao.inactiveBranches(organization.id).map(BranchEntity::toModel)
            employees = dao.employees(organization.id).map(EmployeeEntity::toModel)
            inactiveEmployees = dao.inactiveEmployees(organization.id).map(EmployeeEntity::toModel)
        }
        suspend fun reloadRequests() {
            requests = dao.serviceRequests(organization.id).map(ServiceRequestEntity::toModel)
        }
        suspend fun reloadVisits() {
            visits = dao.serviceVisits(organization.id).map(ServiceVisitEntity::toModel)
            val activeSelection = selectedVisitId?.takeIf { id -> visits.any { it.id == id } }
            selectedVisitId = activeSelection ?: visits.firstOrNull()?.id
            visitChecklist = selectedVisitId?.let { dao.visitChecklist(it).map(VisitChecklistItemEntity::toModel) }.orEmpty()
        }
        suspend fun reloadChecklist(visitId: String) {
            selectedVisitId = visitId
            visitChecklist = dao.visitChecklist(visitId).map(VisitChecklistItemEntity::toModel)
        }
        suspend fun audit(entityType: String, entityId: String?, action: String, summary: String) {
            dao.insertAuditEvent(AuditEventEntity(UUID.randomUUID().toString(), organization.id, user.id, entityType, entityId, action, summary, System.currentTimeMillis()))
        }

        LaunchedEffect(organization.id) {
            val now = System.currentTimeMillis()
            dao.upsertOrganization(OrganizationEntity(organization.id, organization.name, true, now))
            if (dao.clients(organization.id).isEmpty() && dao.archivedClients(organization.id).isEmpty()) {
                dao.upsertClient(ClientEntity("client-demo-1", organization.id, ClientType.PERSON.name, "Демонстрационный клиент", "+7 900 000-00-00", null, null, null, null, null, null, false, false, SyncState.PENDING_CREATE.name, now, now))
            }
            reloadClients(); reloadVehicles(); reloadAssets(); reloadOrganization(); reloadRequests(); reloadVisits()
        }

        val navController = rememberNavController()
        Scaffold { _ ->
            NavHost(navController = navController, startDestination = Routes.Home) {
                composable(Routes.Home) {
                    HomeScreen(
                        organization = organization,
                        modules = accessibleModules,
                        onOpenClients = { navController.navigate(Routes.Clients) },
                        onOpenVehicles = { navController.navigate(Routes.Vehicles) },
                        onOpenAssets = { navController.navigate(Routes.Assets) },
                        onOpenOrganization = { navController.navigate(Routes.Organization) },
                        onOpenRequests = { navController.navigate(Routes.Requests) },
                        onOpenFieldWork = { navController.navigate(Routes.FieldWork) },
                        onOpenWash = { if (accessibleModules.any { it.id == LexoraModuleId.WASH }) navController.navigate(Routes.Wash) },
                        onOpenTires = { if (accessibleModules.any { it.id == LexoraModuleId.TIRES }) navController.navigate(Routes.Tires) },
                        onOpenSettings = { navController.navigate(Routes.Settings) },
                    )
                }
                composable(Routes.Clients) {
                    ClientsScreen(
                        clients = clients,
                        archivedClients = archivedClients,
                        onSave = { draft, existingId -> scope.launch {
                            val now = System.currentTimeMillis(); val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.client(it) }
                            dao.upsertClient(ClientEntity(id, organization.id, draft.type.name, draft.displayName, draft.phone.ifBlank { null }, draft.email.ifBlank { null }, draft.taxId.ifBlank { null }, draft.kpp.ifBlank { null }, draft.registrationAddress.ifBlank { null }, draft.actualAddress.ifBlank { null }, draft.note.ifBlank { null }, draft.consentPersonalData, existing?.archived ?: false, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, existing?.createdAtEpochMs ?: now, now))
                            audit("CLIENT", id, if (existing == null) "CREATE" else "UPDATE", draft.displayName); reloadClients()
                        } },
                        onArchive = { id -> scope.launch { val name = dao.client(id)?.displayName.orEmpty(); dao.archiveClient(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("CLIENT", id, "ARCHIVE", name); reloadClients() } },
                        onRestore = { id -> scope.launch { val name = dao.client(id)?.displayName.orEmpty(); dao.restoreClient(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("CLIENT", id, "RESTORE", name); reloadClients() } },
                    )
                }
                composable(Routes.Vehicles) {
                    VehiclesScreen(
                        vehicles = vehicles,
                        archivedVehicles = archivedVehicles,
                        clients = clients + archivedClients,
                        onSave = { draft, existingId -> scope.launch {
                            val now = System.currentTimeMillis(); val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.vehicle(it) }
                            dao.upsertVehicle(VehicleEntity(id, organization.id, draft.clientId, draft.registrationNumber.trim().uppercase(), draft.vin.trim().uppercase().ifBlank { null }, draft.make.trim().ifBlank { null }, draft.model.trim().ifBlank { null }, draft.year.toIntOrNull(), draft.bodyType.trim().ifBlank { null }, draft.color.trim().ifBlank { null }, draft.mileageKm.toIntOrNull(), existing?.archived ?: false, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, existing?.createdAtEpochMs ?: now, now))
                            audit("VEHICLE", id, if (existing == null) "CREATE" else "UPDATE", draft.registrationNumber.trim().uppercase()); reloadVehicles()
                        } },
                        onArchive = { id -> scope.launch { val value = dao.vehicle(id)?.registrationNumber.orEmpty(); dao.archiveVehicle(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("VEHICLE", id, "ARCHIVE", value); reloadVehicles() } },
                        onRestore = { id -> scope.launch { val value = dao.vehicle(id)?.registrationNumber.orEmpty(); dao.restoreVehicle(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("VEHICLE", id, "RESTORE", value); reloadVehicles() } },
                    )
                }
                composable(Routes.Assets) {
                    AssetsScreen(
                        objects = serviceObjects, archivedObjects = archivedServiceObjects, equipment = equipment, archivedEquipment = archivedEquipment, clients = clients + archivedClients,
                        onSaveObject = { draft, existingId -> scope.launch { val now = System.currentTimeMillis(); val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.serviceObject(it) }; dao.upsertServiceObject(ServiceObjectEntity(id, organization.id, draft.clientId, draft.name.trim(), draft.address.trim().ifBlank { null }, draft.accessMode.trim().ifBlank { null }, draft.responsibleContact.trim().ifBlank { null }, existing?.archived ?: false, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, existing?.createdAtEpochMs ?: now, now)); audit("SERVICE_OBJECT", id, if (existing == null) "CREATE" else "UPDATE", draft.name.trim()); reloadAssets() } },
                        onArchiveObject = { id -> scope.launch { val value = dao.serviceObject(id)?.name.orEmpty(); dao.archiveServiceObject(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("SERVICE_OBJECT", id, "ARCHIVE", value); reloadAssets() } },
                        onRestoreObject = { id -> scope.launch { val value = dao.serviceObject(id)?.name.orEmpty(); dao.restoreServiceObject(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("SERVICE_OBJECT", id, "RESTORE", value); reloadAssets() } },
                        onSaveEquipment = { draft, existingId -> scope.launch { val now = System.currentTimeMillis(); val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.equipmentItem(it) }; dao.upsertEquipment(EquipmentEntity(id, organization.id, draft.serviceObjectId, draft.type.trim(), draft.make.trim().ifBlank { null }, draft.model.trim().ifBlank { null }, draft.serialNumber.trim().uppercase().ifBlank { null }, draft.inventoryNumber.trim().ifBlank { null }, draft.barcode.trim().ifBlank { null }, draft.commissionedNote.trim().ifBlank { null }, draft.warrantyNote.trim().ifBlank { null }, existing?.archived ?: false, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, existing?.createdAtEpochMs ?: now, now)); audit("EQUIPMENT", id, if (existing == null) "CREATE" else "UPDATE", draft.serialNumber.ifBlank { draft.type }); reloadAssets() } },
                        onArchiveEquipment = { id -> scope.launch { val value = dao.equipmentItem(id)?.serialNumber.orEmpty(); dao.archiveEquipment(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("EQUIPMENT", id, "ARCHIVE", value); reloadAssets() } },
                        onRestoreEquipment = { id -> scope.launch { val value = dao.equipmentItem(id)?.serialNumber.orEmpty(); dao.restoreEquipment(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("EQUIPMENT", id, "RESTORE", value); reloadAssets() } },
                    )
                }
                composable(Routes.Organization) {
                    OrganizationScreen(
                        branches = branches, inactiveBranches = inactiveBranches, employees = employees, inactiveEmployees = inactiveEmployees,
                        onSaveBranch = { draft, existingId -> scope.launch { val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.branch(it) }; val now = System.currentTimeMillis(); dao.upsertBranch(BranchEntity(id, organization.id, draft.name, draft.address.ifBlank { null }, draft.phone.ifBlank { null }, draft.email.ifBlank { null }, draft.workSchedule.ifBlank { null }, draft.timeZoneId, existing?.active ?: true, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, now)); audit("BRANCH", id, if (existing == null) "CREATE" else "UPDATE", draft.name); reloadOrganization() } },
                        onDeactivateBranch = { id -> scope.launch { val value = dao.branch(id)?.name.orEmpty(); dao.deactivateBranch(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("BRANCH", id, "DEACTIVATE", value); reloadOrganization() } },
                        onActivateBranch = { id -> scope.launch { val value = dao.branch(id)?.name.orEmpty(); dao.activateBranch(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("BRANCH", id, "ACTIVATE", value); reloadOrganization() } },
                        onSaveEmployee = { draft, existingId -> scope.launch { val id = existingId ?: UUID.randomUUID().toString(); val existing = existingId?.let { dao.employee(it) }; val now = System.currentTimeMillis(); dao.upsertEmployee(EmployeeEntity(id, organization.id, draft.branchId, draft.displayName, draft.position.ifBlank { null }, draft.phone.ifBlank { null }, draft.email.ifBlank { null }, existing?.active ?: true, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, now)); audit("EMPLOYEE", id, if (existing == null) "CREATE" else "UPDATE", draft.displayName); reloadOrganization() } },
                        onDeactivateEmployee = { id -> scope.launch { val value = dao.employee(id)?.displayName.orEmpty(); dao.deactivateEmployee(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("EMPLOYEE", id, "DEACTIVATE", value); reloadOrganization() } },
                        onActivateEmployee = { id -> scope.launch { val value = dao.employee(id)?.displayName.orEmpty(); dao.activateEmployee(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis()); audit("EMPLOYEE", id, "ACTIVATE", value); reloadOrganization() } },
                    )
                }
                composable(Routes.Requests) {
                    RequestsScreen(
                        requests = requests,
                        onSave = { draft, existingId -> scope.launch {
                            val now = System.currentTimeMillis()
                            val existing = existingId?.let { dao.serviceRequest(it) }
                            val id = existingId ?: UUID.randomUUID().toString()
                            val number = existing?.number ?: run {
                                val max = dao.serviceRequests(organization.id).mapNotNull { it.number.removePrefix("REQ-").toIntOrNull() }.maxOrNull() ?: 0
                                "REQ-%06d".format(max + 1)
                            }
                            dao.upsertServiceRequest(ServiceRequestEntity(id, organization.id, number, existing?.clientId, existing?.vehicleId, existing?.serviceObjectId, existing?.equipmentId, existing?.branchId, existing?.assigneeEmployeeId, draft.title, draft.description.ifBlank { null }, existing?.status ?: RequestStatus.NEW.name, draft.priority.name, existing?.plannedAtEpochMs, existing?.dueAtEpochMs, existing?.slaDeadlineEpochMs, existing?.closedAtEpochMs, existing?.archived ?: false, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, existing?.createdAtEpochMs ?: now, now))
                            if (existing == null) dao.insertRequestStatusHistory(RequestStatusHistoryEntity(UUID.randomUUID().toString(), id, null, RequestStatus.NEW.name, user.id, now, "Создание заявки"))
                            audit("SERVICE_REQUEST", id, if (existing == null) "CREATE" else "UPDATE", "$number · ${draft.title}")
                            reloadRequests()
                        } },
                        onChangeStatus = { id, target -> scope.launch {
                            val current = dao.serviceRequest(id) ?: return@launch
                            val from = RequestStatus.valueOf(current.status)
                            if (!RequestWorkflow.canTransition(from, target) || from == target) return@launch
                            val now = System.currentTimeMillis()
                            dao.updateRequestStatus(id, target.name, SyncState.PENDING_UPDATE.name, now, if (target == RequestStatus.CLOSED) now else null)
                            dao.insertRequestStatusHistory(RequestStatusHistoryEntity(UUID.randomUUID().toString(), id, from.name, target.name, user.id, now, null))
                            audit("SERVICE_REQUEST", id, "STATUS_CHANGE", "${current.number}: ${from.name} → ${target.name}")
                            reloadRequests()
                        } },
                    )
                }
                composable(Routes.FieldWork) {
                    FieldWorkScreen(
                        visits = visits,
                        requests = requests,
                        employees = employees,
                        checklist = visitChecklist,
                        onCreateVisit = { requestId, employeeId -> scope.launch {
                            val request = dao.serviceRequest(requestId) ?: return@launch
                            val now = System.currentTimeMillis()
                            val id = UUID.randomUUID().toString()
                            dao.upsertServiceVisit(
                                ServiceVisitEntity(
                                    id = id,
                                    organizationId = organization.id,
                                    requestId = requestId,
                                    branchId = request.branchId,
                                    employeeId = employeeId ?: request.assigneeEmployeeId,
                                    status = VisitStatus.PLANNED.name,
                                    plannedStartEpochMs = request.plannedAtEpochMs,
                                    plannedEndEpochMs = request.dueAtEpochMs,
                                    actualStartEpochMs = null,
                                    actualEndEpochMs = null,
                                    resultNote = null,
                                    customerName = null,
                                    customerSignatureRef = null,
                                    syncState = SyncState.PENDING_CREATE.name,
                                    createdAtEpochMs = now,
                                    updatedAtEpochMs = now,
                                )
                            )
                            audit("SERVICE_VISIT", id, "CREATE", "Выезд по заявке ${request.number}")
                            reloadVisits()
                        } },
                        onSelectVisit = { id -> scope.launch { reloadChecklist(id) } },
                        onChangeVisitStatus = { id, target -> scope.launch {
                            val current = dao.serviceVisit(id) ?: return@launch
                            val from = VisitStatus.valueOf(current.status)
                            if (from == VisitStatus.COMPLETED || from == VisitStatus.CANCELLED || from == target) return@launch
                            val allowed = when (from) {
                                VisitStatus.PLANNED -> target == VisitStatus.EN_ROUTE || target == VisitStatus.CANCELLED
                                VisitStatus.EN_ROUTE -> target == VisitStatus.ON_SITE || target == VisitStatus.CANCELLED
                                VisitStatus.ON_SITE -> target == VisitStatus.COMPLETED || target == VisitStatus.CANCELLED
                                VisitStatus.COMPLETED, VisitStatus.CANCELLED -> false
                            }
                            if (!allowed) return@launch
                            val now = System.currentTimeMillis()
                            val actualStart = if (target == VisitStatus.EN_ROUTE || target == VisitStatus.ON_SITE) now else current.actualStartEpochMs
                            val actualEnd = if (target == VisitStatus.COMPLETED || target == VisitStatus.CANCELLED) now else null
                            dao.updateVisitStatus(id, target.name, actualStart, actualEnd, SyncState.PENDING_UPDATE.name, now)
                            audit("SERVICE_VISIT", id, "STATUS_CHANGE", "${from.name} → ${target.name}")
                            reloadVisits()
                        } },
                        onAddChecklistItem = { visitId -> scope.launch {
                            val now = System.currentTimeMillis()
                            val nextOrder = dao.visitChecklist(visitId).maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
                            val id = UUID.randomUUID().toString()
                            dao.upsertVisitChecklistItem(VisitChecklistItemEntity(id, visitId, "Новый пункт чек-листа", ChecklistItemState.PENDING.name, null, nextOrder, SyncState.PENDING_CREATE.name, now))
                            audit("VISIT_CHECKLIST_ITEM", id, "CREATE", "Добавлен пункт чек-листа")
                            reloadChecklist(visitId)
                        } },
                        onToggleChecklistItem = { itemId -> scope.launch {
                            val current = visitChecklist.firstOrNull { it.id == itemId } ?: return@launch
                            val next = if (current.state == ChecklistItemState.DONE) ChecklistItemState.PENDING else ChecklistItemState.DONE
                            dao.updateChecklistItemState(itemId, next.name, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
                            audit("VISIT_CHECKLIST_ITEM", itemId, "STATE_CHANGE", "${current.state.name} → ${next.name}")
                            reloadChecklist(current.visitId)
                        } },
                    )
                }
                composable(Routes.Settings) { SettingsScreen(organization = organization, user = user, modules = modules, onModuleEnabledChange = { moduleId, enabled -> moduleRegistry.updateEnabled(organization.id, moduleId, enabled); stateVersion++ }) }
                composable(Routes.Wash) { WashScreen() }
                composable(Routes.Tires) { TiresScreen() }
            }
        }
        stateVersion
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
