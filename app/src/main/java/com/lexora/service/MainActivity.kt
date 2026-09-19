package com.lexora.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lexora.service.core.data.ModuleLicenseRepository
import com.lexora.service.core.data.OrganizationSessionRepository
import com.lexora.service.core.data.PersistentOrganizationRepository
import com.lexora.service.core.data.PersistentUserRepository
import com.lexora.service.core.data.defaultIntegrationRegistry
import com.lexora.service.application.ServiceApplicationService
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.designsystem.LexoraTheme
import com.lexora.service.core.designsystem.PermissionGuard
import com.lexora.service.core.designsystem.SystemStateHost
import com.lexora.service.core.domain.AccessPolicy
import com.lexora.service.core.domain.ModuleAccessPolicy
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.network.AndroidConnectivityMonitor
import com.lexora.service.presentation.AppSystemStateController
import com.lexora.service.core.model.*
import com.lexora.service.core.navigation.Routes
import com.lexora.service.feature.assets.AssetsScreen
import com.lexora.service.feature.audit.AuditScreen
import com.lexora.service.feature.clients.ClientsScreen
import com.lexora.service.feature.documents.DocumentsScreen
import com.lexora.service.feature.fieldwork.FieldWorkScreen
import com.lexora.service.feature.home.HomeScreen
import com.lexora.service.feature.notifications.NotificationsScreen
import com.lexora.service.feature.organization.OrganizationHubScreen
import com.lexora.service.feature.reports.ReportsScreen
import com.lexora.service.feature.requests.RequestsScreen
import com.lexora.service.feature.settings.SettingsScreen
import com.lexora.service.feature.tires.TiresScreen
import com.lexora.service.feature.users.UsersScreen
import com.lexora.service.feature.vehicles.VehiclesScreen
import com.lexora.service.feature.wash.WashScreen
import kotlinx.coroutines.delay
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
        val backendGraph = remember(database) { LexoraBackendGraph(context, database) }
        val operations: ServiceOperations = remember(dao, backendGraph) { ServiceApplicationService(dao, backendGraph.syncQueue) }
        val organizationRepository = remember(dao) { PersistentOrganizationRepository(dao) }
        val userRepository = remember(database, dao) { PersistentUserRepository(database.userDao(), dao) }
        val accessPolicy = remember { AccessPolicy() }
        val organizationSessionRepository = remember(organizationRepository, userRepository, accessPolicy) {
            OrganizationSessionRepository(organizationRepository, userRepository, accessPolicy)
        }
        val moduleLicenseRepository = remember(dao) { ModuleLicenseRepository(dao) }
        val moduleAccessPolicy = remember { ModuleAccessPolicy() }
        val integrations = remember { defaultIntegrationRegistry() }
        val scope = rememberCoroutineScope()
        val systemState = remember(backendGraph) {
            AppSystemStateController(AndroidConnectivityMonitor(context), backendGraph.syncQueue)
        }
        DisposableEffect(systemState) {
            val registration = systemState.start()
            onDispose { registration.close() }
        }

        var activeOrganization by remember { mutableStateOf<Organization?>(null) }
        var user by remember { mutableStateOf<ServiceUser?>(null) }
        var organizations by remember { mutableStateOf<List<Organization>>(emptyList()) }
        var managedUsers by remember { mutableStateOf<List<ServiceUser>>(emptyList()) }
        var modules by remember { mutableStateOf<List<ModuleDescriptor>>(emptyList()) }
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
        var serviceDocuments by remember { mutableStateOf<List<ServiceDocument>>(emptyList()) }
        var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }

        fun launchSafe(block: suspend () -> Unit) {
            scope.launch {
                try {
                    block()
                } catch (error: Throwable) {
                    systemState.reportTechnicalError(error)
                } finally {
                    activeOrganization?.id?.let { runCatching { systemState.refreshSyncIssue(it) } }
                }
            }
        }

        LaunchedEffect(Unit) {
            try {
                val session = organizationSessionRepository.bootstrap()
                activeOrganization = session.organization
                user = session.user
                organizations = session.organizations
                systemState.refreshSyncIssue(session.organization.id)
            } catch (error: Throwable) {
                systemState.reportTechnicalError(error)
            } finally {
                systemState.updateLoading(false)
            }
        }

        val organization = activeOrganization
        val activeUser = user
        if (organization == null || activeUser == null) {
            SystemStateHost(systemState.state(hasContent = false)) {}
            return@LexoraTheme
        }
        val accessibleModules = modules.filter { moduleAccessPolicy.isAvailable(it, activeUser) }
        val canManageUsers = accessPolicy.can(activeUser, Permission.MANAGE_USERS)
        val canManageOrganization = accessPolicy.can(activeUser, Permission.MANAGE_ORGANIZATION)

        suspend fun reloadUsers() { managedUsers = userRepository.usersForOrganization(organization.id) }
        suspend fun reloadModules() { modules = moduleLicenseRepository.descriptors(organization.id) }
        suspend fun reloadClients() {
            operations.clients(organization.id).also { clients = it.active; archivedClients = it.archived }
        }
        suspend fun reloadVehicles() {
            operations.vehicles(organization.id).also { vehicles = it.active; archivedVehicles = it.archived }
        }
        suspend fun reloadAssets() {
            operations.assets(organization.id).also {
                serviceObjects = it.objects; archivedServiceObjects = it.archivedObjects
                equipment = it.equipment; archivedEquipment = it.archivedEquipment
            }
        }
        suspend fun reloadOrganization() {
            operations.organization(organization.id).also {
                branches = it.branches; inactiveBranches = it.inactiveBranches
                employees = it.employees; inactiveEmployees = it.inactiveEmployees
            }
        }
        suspend fun reloadRequests() { requests = operations.requests(organization.id) }
        suspend fun reloadVisits() {
            operations.visits(organization.id, selectedVisitId).also {
                visits = it.visits; selectedVisitId = it.selectedVisitId; visitChecklist = it.checklist
            }
        }
        suspend fun reloadChecklist(visitId: String) {
            selectedVisitId = visitId
            visitChecklist = operations.checklist(visitId)
        }
        suspend fun reloadDocumentsAndPayments() {
            operations.finance(organization.id).also { serviceDocuments = it.documents; payments = it.payments }
        }

        LaunchedEffect(organization.id, activeUser.id) {
            operations.ensureDemoClient(organization.id)
            reloadUsers(); reloadModules(); reloadClients(); reloadVehicles(); reloadAssets(); reloadOrganization(); reloadRequests(); reloadVisits(); reloadDocumentsAndPayments()
        }

        val navController = rememberNavController()
        LaunchedEffect(organization.id) {
            while (true) {
                runCatching { systemState.refreshSyncIssue(organization.id) }
                delay(5_000)
            }
        }

        SystemStateHost(
            state = systemState.state(hasContent = true),
            onRetrySync = { ServiceSyncScheduler.enqueue(context, 0L) },
            onRetryTechnicalError = {
                systemState.clearTechnicalError()
                launchSafe {
                    reloadUsers(); reloadModules(); reloadClients(); reloadVehicles(); reloadAssets()
                    reloadOrganization(); reloadRequests(); reloadVisits(); reloadDocumentsAndPayments()
                }
            },
        ) {
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
                        onOpenDocuments = { navController.navigate(Routes.Documents) },
                        onOpenReports = { navController.navigate(Routes.Reports) },
                        onOpenCatalog = { navController.navigate(Routes.Catalog) },
                        onOpenNotifications = { navController.navigate(Routes.Notifications) },
                        onOpenAudit = { navController.navigate(Routes.Audit) },
                        onOpenUsers = if (canManageUsers) ({ navController.navigate(Routes.Users) }) else null,
                        onOpenWash = { if (accessibleModules.any { it.id == LexoraModuleId.WASH }) navController.navigate(Routes.Wash) },
                        onOpenTires = { if (accessibleModules.any { it.id == LexoraModuleId.TIRES }) navController.navigate(Routes.Tires) },
                        onOpenSettings = { navController.navigate(Routes.Settings) },
                    )
                }
                composable(Routes.Clients) {
                    ClientsScreen(
                        clients = clients,
                        archivedClients = archivedClients,
                        onSave = { draft, existingId -> launchSafe { operations.saveClient(organization.id, activeUser.id, SaveClientCommand(draft.type, draft.displayName, draft.phone, draft.email, draft.taxId, draft.kpp, draft.registrationAddress, draft.actualAddress, draft.note, draft.consentPersonalData), existingId); reloadClients() } },
                        onArchive = { id -> launchSafe { operations.archiveClient(organization.id, activeUser.id, id, restore = false); reloadClients() } },
                        onRestore = { id -> launchSafe { operations.archiveClient(organization.id, activeUser.id, id, restore = true); reloadClients() } },
                    )
                }
                composable(Routes.Vehicles) {
                    VehiclesScreen(
                        vehicles = vehicles,
                        archivedVehicles = archivedVehicles,
                        clients = clients + archivedClients,
                        onSave = { draft, existingId -> launchSafe { operations.saveVehicle(organization.id, activeUser.id, SaveVehicleCommand(draft.clientId, draft.registrationNumber, draft.vin, draft.make, draft.model, draft.year, draft.bodyType, draft.color, draft.mileageKm), existingId); reloadVehicles() } },
                        onArchive = { id -> launchSafe { operations.archiveVehicle(organization.id, activeUser.id, id, restore = false); reloadVehicles() } },
                        onRestore = { id -> launchSafe { operations.archiveVehicle(organization.id, activeUser.id, id, restore = true); reloadVehicles() } },
                    )
                }
                composable(Routes.Assets) {
                    AssetsScreen(
                        objects = serviceObjects,
                        archivedObjects = archivedServiceObjects,
                        equipment = equipment,
                        archivedEquipment = archivedEquipment,
                        clients = clients + archivedClients,
                        onSaveObject = { draft, existingId -> launchSafe { operations.saveServiceObject(organization.id, activeUser.id, SaveServiceObjectCommand(draft.clientId, draft.name, draft.address, draft.accessMode, draft.responsibleContact), existingId); reloadAssets() } },
                        onArchiveObject = { id -> launchSafe { operations.archiveServiceObject(organization.id, activeUser.id, id, restore = false); reloadAssets() } },
                        onRestoreObject = { id -> launchSafe { operations.archiveServiceObject(organization.id, activeUser.id, id, restore = true); reloadAssets() } },
                        onSaveEquipment = { draft, existingId -> launchSafe { operations.saveEquipment(organization.id, activeUser.id, SaveEquipmentCommand(draft.serviceObjectId, draft.type, draft.make, draft.model, draft.serialNumber, draft.inventoryNumber, draft.barcode, draft.commissionedNote, draft.warrantyNote), existingId); reloadAssets() } },
                        onArchiveEquipment = { id -> launchSafe { operations.archiveEquipment(organization.id, activeUser.id, id, restore = false); reloadAssets() } },
                        onRestoreEquipment = { id -> launchSafe { operations.archiveEquipment(organization.id, activeUser.id, id, restore = true); reloadAssets() } },
                    )
                }
                composable(Routes.Organization) {
                    OrganizationHubScreen(
                        activeOrganization = organization,
                        organizations = organizations,
                        allowedOrganizationIds = activeUser.organizationIds,
                        canManageOrganization = canManageOrganization,
                        onCreateOrganization = { name ->
                            if (canManageOrganization) launchSafe {
                                val session = runCatching { organizationSessionRepository.createAndSwitch(activeUser, name) }.getOrNull() ?: return@launchSafe
                                activeOrganization = session.organization; user = session.user; organizations = session.organizations
                            }
                        },
                        onSwitchOrganization = { organizationId -> launchSafe {
                            val session = runCatching { organizationSessionRepository.switch(activeUser, organizationId) }.getOrNull() ?: return@launchSafe
                            activeOrganization = session.organization; user = session.user; organizations = session.organizations
                        } },
                        branches = branches,
                        inactiveBranches = inactiveBranches,
                        employees = employees,
                        inactiveEmployees = inactiveEmployees,
                        onSaveBranch = { draft, existingId -> launchSafe { operations.saveBranch(organization.id, activeUser.id, SaveBranchCommand(draft.name, draft.address, draft.phone, draft.email, draft.workSchedule, draft.timeZoneId), existingId); reloadOrganization() } },
                        onDeactivateBranch = { id -> launchSafe { operations.setBranchActive(organization.id, activeUser.id, id, active = false); reloadOrganization() } },
                        onActivateBranch = { id -> launchSafe { operations.setBranchActive(organization.id, activeUser.id, id, active = true); reloadOrganization() } },
                        onSaveEmployee = { draft, existingId -> launchSafe { operations.saveEmployee(organization.id, activeUser.id, SaveEmployeeCommand(draft.displayName, draft.position, draft.phone, draft.email, draft.branchId), existingId); reloadOrganization() } },
                        onDeactivateEmployee = { id -> launchSafe { operations.setEmployeeActive(organization.id, activeUser.id, id, active = false); reloadOrganization() } },
                        onActivateEmployee = { id -> launchSafe { operations.setEmployeeActive(organization.id, activeUser.id, id, active = true); reloadOrganization() } },
                    )
                }
                composable(Routes.Requests) {
                    RequestsScreen(
                        requests = requests,
                        onSave = { draft, existingId -> launchSafe { operations.saveRequest(organization.id, activeUser.id, SaveRequestCommand(draft.title, draft.description, draft.priority), existingId); reloadRequests() } },
                        onChangeStatus = { id, target -> launchSafe { operations.changeRequestStatus(organization.id, activeUser.id, id, target); reloadRequests() } },
                    )
                }
                composable(Routes.FieldWork) {
                    FieldWorkScreen(
                        visits = visits,
                        requests = requests,
                        employees = employees,
                        checklist = visitChecklist,
                        onCreateVisit = { requestId, employeeId -> launchSafe { operations.createVisit(organization.id, activeUser.id, requestId, employeeId); reloadVisits() } },
                        onSelectVisit = { id -> launchSafe { reloadChecklist(id) } },
                        onChangeVisitStatus = { id, target -> launchSafe { operations.changeVisitStatus(organization.id, activeUser.id, id, target); reloadVisits() } },
                        onAddChecklistItem = { visitId -> launchSafe { operations.addChecklistItem(organization.id, activeUser.id, visitId); reloadChecklist(visitId) } },
                        onToggleChecklistItem = { itemId -> launchSafe {
                            val current = visitChecklist.firstOrNull { it.id == itemId } ?: return@launchSafe
                            operations.toggleChecklistItem(organization.id, activeUser.id, current); reloadChecklist(current.visitId)
                        } },
                    )
                }
                composable(Routes.Documents) {
                    DocumentsScreen(
                        documents = serviceDocuments,
                        payments = payments,
                        requests = requests,
                        onCreateDocument = { type, requestId -> launchSafe { operations.createDocument(organization.id, activeUser.id, type, requestId); reloadDocumentsAndPayments() } },
                        onChangeDocumentStatus = { id, target -> launchSafe { operations.changeDocumentStatus(organization.id, activeUser.id, id, target); reloadDocumentsAndPayments() } },
                        onCreatePayment = { requestId -> launchSafe { operations.createPayment(organization.id, activeUser.id, requestId, serviceDocuments); reloadDocumentsAndPayments() } },
                        onMarkPaymentPaid = { id -> launchSafe { operations.markPaymentPaid(organization.id, activeUser.id, id); reloadDocumentsAndPayments() } },
                    )
                }
                composable(Routes.Reports) { ReportsScreen(requests = requests, visits = visits, documents = serviceDocuments, payments = payments, integrations = integrations) }
                composable(Routes.Catalog) { AppCatalogRoute(database = database, organization = organization, user = activeUser) }
                composable(Routes.Notifications) { NotificationsScreen(organization = organization) }
                composable(Routes.Audit) { PermissionGuard(accessPolicy.can(activeUser, Permission.VIEW_AUDIT), "Недостаточно прав для просмотра журнала аудита") { AuditScreen(organization = organization) } }
                composable(Routes.Users) {
                    PermissionGuard(canManageUsers, "Недостаточно прав для управления пользователями") {
                    UsersScreen(
                        organization = organization,
                        currentUser = activeUser,
                        users = managedUsers,
                        canManageUsers = canManageUsers,
                        onCreateUser = { displayName, role -> if (canManageUsers) launchSafe { userRepository.createLocalUser(activeUser.id, organization.id, displayName, role); reloadUsers() } },
                        onSetRole = { userId, role, enabled -> if (canManageUsers) launchSafe {
                            userRepository.setRole(activeUser.id, userId, organization.id, role, enabled)
                            reloadUsers()
                            if (userId == activeUser.id) user = userRepository.userInOrganization(activeUser.id, organization.id)
                        } },
                        onSetUserActive = { userId, enabled -> if (canManageUsers) launchSafe {
                            userRepository.setUserActive(activeUser.id, userId, organization.id, enabled)
                            reloadUsers()
                        } },
                    )
                    }
                }
                composable(Routes.Settings) { SettingsScreen(organization = organization, user = activeUser, modules = modules, appVersion = "0.28.0", onModuleEnabledChange = { _, _ -> launchSafe { reloadModules() } }) }
                composable(Routes.Wash) { PermissionGuard(accessPolicy.can(activeUser, Permission.VIEW_WASH) && accessibleModules.any { it.id == LexoraModuleId.WASH }, "Модуль автомойки недоступен по правам или лицензии") { WashScreen(organization = organization) } }
                composable(Routes.Tires) { PermissionGuard(accessPolicy.can(activeUser, Permission.VIEW_TIRES) && accessibleModules.any { it.id == LexoraModuleId.TIRES }, "Модуль шиномонтажа недоступен по правам или лицензии") { TiresScreen(organization = organization) } }
            }
        }
        }
    }
}
