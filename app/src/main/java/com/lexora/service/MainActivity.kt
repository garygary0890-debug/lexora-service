package com.lexora.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lexora.service.core.designsystem.LexoraTheme
import com.lexora.service.core.designsystem.PermissionGuard
import com.lexora.service.core.designsystem.SystemStateHost
import com.lexora.service.core.model.*
import com.lexora.service.core.navigation.Routes
import com.lexora.service.core.network.AndroidConnectivityMonitor
import com.lexora.service.core.presentation.LexoraViewModelFactory
import com.lexora.service.di.LexoraServiceContainer
import com.lexora.service.presentation.AppSystemStateController
import com.lexora.service.presentation.WorkspaceViewModel
import com.lexora.service.feature.assets.*
import com.lexora.service.feature.audit.*
import com.lexora.service.feature.clients.*
import com.lexora.service.feature.catalog.*
import com.lexora.service.feature.documents.*
import com.lexora.service.feature.fieldwork.*
import com.lexora.service.feature.home.*
import com.lexora.service.feature.notifications.*
import com.lexora.service.feature.organization.*
import com.lexora.service.feature.planning.*
import com.lexora.service.feature.reports.*
import com.lexora.service.feature.requests.*
import com.lexora.service.feature.search.*
import com.lexora.service.feature.settings.*
import com.lexora.service.feature.tires.*
import com.lexora.service.feature.users.*
import com.lexora.service.feature.vehicles.*
import com.lexora.service.feature.wash.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    companion object { const val EXTRA_GLOBAL_OWNER = "lexora.global_owner" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LexoraServiceApp(intent.getBooleanExtra(EXTRA_GLOBAL_OWNER, false)) }
    }
}

@Composable
private fun LexoraServiceApp(globalOwner: Boolean) {
    LexoraTheme {
        val context = LocalContext.current
        val container = remember(context) { LexoraServiceContainer(context) }
        val workspaceViewModel: WorkspaceViewModel = viewModel(
            factory = LexoraViewModelFactory { WorkspaceViewModel(container.workspace) },
        )
        val workspaceState by workspaceViewModel.state.collectAsStateWithLifecycle()
        val systemState = remember(container.backend) {
            AppSystemStateController(AndroidConnectivityMonitor(context), container.backend.syncQueue)
        }
        DisposableEffect(systemState) {
            val registration = systemState.start()
            onDispose { registration.close() }
        }

        val snapshot = workspaceState.snapshot
        if (snapshot == null) {
            systemState.updateLoading(workspaceState.loading)
            SystemStateHost(systemState.state(hasContent = false)) {}
            return@LexoraTheme
        }
        systemState.updateLoading(false)
        val organization = snapshot.organization
        val user = snapshot.user
        val modules = if (globalOwner) snapshot.modules.map { module ->
            module.copy(
                enabled = true,
                licenseStatus = if (module.id == LexoraModuleId.CORE) ModuleLicenseStatus.NOT_REQUIRED else ModuleLicenseStatus.ACTIVE,
            )
        } else snapshot.modules
        val accessibleModules = if (globalOwner) modules else modules.filter { container.moduleAccessPolicy.isAvailable(it, user) }
        val canManageUsers = globalOwner || container.accessPolicy.can(user, Permission.MANAGE_USERS)
        val canManageOrganization = globalOwner || container.accessPolicy.can(user, Permission.MANAGE_ORGANIZATION)

        LaunchedEffect(organization.id) {
            while (true) {
                runCatching { systemState.refreshSyncIssue(organization.id) }
                delay(5_000)
            }
        }

        val navController = rememberNavController()
        SystemStateHost(
            state = systemState.state(hasContent = true),
            onRetrySync = { ServiceSyncScheduler.enqueue(context, 0L) },
            onRetryTechnicalError = { systemState.clearTechnicalError() },
        ) {
            Scaffold { _ ->
                LexoraNavHost(
                    navController = navController,
                    container = container,
                    workspaceViewModel = workspaceViewModel,
                    snapshot = snapshot,
                    modules = modules,
                    accessibleModules = accessibleModules,
                    globalOwner = globalOwner,
                    canManageUsers = canManageUsers,
                    canManageOrganization = canManageOrganization,
                )
            }
        }
    }
}

@Composable
private fun LexoraNavHost(
    navController: NavHostController,
    container: LexoraServiceContainer,
    workspaceViewModel: WorkspaceViewModel,
    snapshot: com.lexora.service.core.domain.WorkspaceSnapshot,
    modules: List<ModuleDescriptor>,
    accessibleModules: List<ModuleDescriptor>,
    globalOwner: Boolean,
    canManageUsers: Boolean,
    canManageOrganization: Boolean,
) {
    val organization = snapshot.organization
    val user = snapshot.user
    NavHost(navController = navController, startDestination = Routes.Home) {
        composable(Routes.Home) {
            val vm: HomeViewModel = viewModel(
                key = "home:${organization.id}",
                factory = LexoraViewModelFactory { HomeViewModel(organization.id, container.loadDashboard) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            HomeScreen(
                organization = organization,
                modules = accessibleModules,
                state = state,
                onRefresh = vm::refresh,
                onOpenGlobalSearch = { navController.navigate(Routes.Search) },
                onOpenPlanning = { navController.navigate(Routes.Planning) },
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
                onOpenWash = { navController.navigate(Routes.Wash) },
                onOpenTires = { navController.navigate(Routes.Tires) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable(Routes.Search) {
            val vm: GlobalSearchViewModel = viewModel(
                key = "search:${organization.id}",
                factory = LexoraViewModelFactory { GlobalSearchViewModel(organization.id, container.globalSearch) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            GlobalSearchScreen(
                state = state,
                onQueryChange = vm::setQuery,
                onToggleType = vm::toggleType,
                onSearch = vm::search,
                onOpenResult = { result -> navController.navigate(result.route) },
            )
        }
        composable(Routes.Planning) {
            val vm: PlanningViewModel = viewModel(
                key = "planning:${organization.id}",
                factory = LexoraViewModelFactory { PlanningViewModel(organization.id, container.loadPlanning) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            PlanningScreen(state, vm::setMode, vm::previous, vm::today, vm::next, vm::reload)
        }
        composable(Routes.Clients) {
            val vm: ClientsViewModel = viewModel(
                key = "clients:${organization.id}:${user.id}",
                factory = LexoraViewModelFactory { ClientsViewModel(organization.id, user.id, container.operations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            ClientsScreen(
                clients = state.clients,
                archivedClients = state.archived,
                onSave = vm::save,
                onArchive = { vm.archive(it, false) },
                onRestore = { vm.archive(it, true) },
            )
        }
        composable(Routes.Vehicles) {
            val vm: VehiclesViewModel = viewModel(
                key = "vehicles:${organization.id}:${user.id}",
                factory = LexoraViewModelFactory { VehiclesViewModel(organization.id, user.id, container.operations, container.serviceHistoryOperations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            VehiclesScreen(
                vehicles = state.vehicles,
                archivedVehicles = state.archived,
                clients = state.clients,
                onSave = vm::save,
                onArchive = { vm.archive(it, false) },
                onRestore = { vm.archive(it, true) },
                historyVehicle = state.historyVehicleId?.let { id -> (state.vehicles + state.archived).firstOrNull { it.id == id } },
                historyRecords = state.historyRecords,
                historyLoading = state.historyLoading,
                onOpenHistory = vm::openHistory,
                onCloseHistory = vm::closeHistory,
            )
        }
        composable(Routes.Assets) {
            val vm: AssetsViewModel = viewModel(
                key = "assets:${organization.id}:${user.id}",
                factory = LexoraViewModelFactory { AssetsViewModel(organization.id, user.id, container.operations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            AssetsScreen(
                objects = state.objects,
                archivedObjects = state.archivedObjects,
                equipment = state.equipment,
                archivedEquipment = state.archivedEquipment,
                clients = state.clients,
                onSaveObject = vm::saveObject,
                onArchiveObject = { vm.archiveObject(it, false) },
                onRestoreObject = { vm.archiveObject(it, true) },
                onSaveEquipment = vm::saveEquipment,
                onArchiveEquipment = { vm.archiveEquipment(it, false) },
                onRestoreEquipment = { vm.archiveEquipment(it, true) },
            )
        }
        composable(Routes.Requests) {
            val vm: RequestsViewModel = viewModel(
                key = "requests:${organization.id}:${user.id}",
                factory = LexoraViewModelFactory { RequestsViewModel(organization.id, user.id, container.operations, container.documentFeatureOperations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            RequestsScreen(state, vm::open, vm::close, vm::save, vm::assign, vm::reschedule, vm::changeStatus)
        }
        composable(Routes.Organization) {
            val vm: OrganizationViewModel = viewModel(
                key = "organization:${organization.id}:${user.id}",
                factory = LexoraViewModelFactory { OrganizationViewModel(organization.id, user.id, container.operations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            OrganizationHubScreen(
                activeOrganization = organization,
                organizations = snapshot.organizations,
                allowedOrganizationIds = user.organizationIds,
                canManageOrganization = canManageOrganization,
                onCreateOrganization = workspaceViewModel::createOrganization,
                onSwitchOrganization = workspaceViewModel::switchOrganization,
                branches = state.branches,
                inactiveBranches = state.inactiveBranches,
                employees = state.employees,
                inactiveEmployees = state.inactiveEmployees,
                onSaveBranch = vm::saveBranch,
                onDeactivateBranch = { vm.setBranchActive(it, false) },
                onActivateBranch = { vm.setBranchActive(it, true) },
                onSaveEmployee = vm::saveEmployee,
                onDeactivateEmployee = { vm.setEmployeeActive(it, false) },
                onActivateEmployee = { vm.setEmployeeActive(it, true) },
            )
        }
        composable(Routes.FieldWork) {
            val vm: FieldWorkViewModel = viewModel(
                key = "fieldwork:${organization.id}:${user.id}",
                factory = LexoraViewModelFactory { FieldWorkViewModel(organization.id, user.id, container.operations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            FieldWorkScreen(
                state = state,
                onCreateVisit = vm::createVisit,
                onAssignRequest = vm::assignRequest,
                onSelectVisit = vm::selectVisit,
                onChangeVisitStatus = vm::changeStatus,
                onAddChecklistItem = vm::addChecklistItem,
                onToggleChecklistItem = vm::toggleChecklistItem,
            )
        }
        composable(Routes.Documents) {
            val vm: DocumentsViewModel = viewModel(
                key = "documents:${organization.id}:${user.id}",
                factory = LexoraViewModelFactory { DocumentsViewModel(organization.id, user.id, container.operations, container.documentFeatureOperations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            DocumentsScreen(viewModel = vm)
        }
        composable(Routes.Reports) {
            val vm: ReportsViewModel = viewModel(
                key = "reports:${organization.id}",
                factory = LexoraViewModelFactory { ReportsViewModel(organization.id, container.operations, container.integrations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            val customerVm: CustomerCareViewModel = viewModel(
                key = "customer-care:${organization.id}",
                factory = LexoraViewModelFactory { CustomerCareViewModel(organization.id, container.customerCareOperations) },
            )
            ReportsScreen(state.requests, state.visits, state.documents, state.payments, state.integrations, customerVm)
        }
        composable(Routes.Catalog) {
            val vm: CatalogViewModel = viewModel(
                key = "catalog:${organization.id}:${user.id}",
                factory = LexoraViewModelFactory { CatalogViewModel(organization.id, user.id, container.catalogOperations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            CatalogScreen(state.data, CatalogActions(vm::addService, vm::toggleService, vm::addComponent, vm::setComponentActive, vm::addPriceList, vm::selectPriceList, vm::addPriceItem))
        }
        composable(Routes.Notifications) {
            val vm: NotificationsViewModel = viewModel(
                key = "notifications:${organization.id}",
                factory = LexoraViewModelFactory { NotificationsViewModel(organization.id, container.notificationOperations) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            NotificationsScreen(state, vm::setArchivedMode, vm::markAllRead, vm::markRead, vm::setArchived)
        }
        composable(Routes.Audit) {
            PermissionGuard(globalOwner || container.accessPolicy.can(user, Permission.VIEW_AUDIT), "Недостаточно прав для просмотра журнала аудита") {
                val vm: AuditViewModel = viewModel(
                    key = "audit:${organization.id}",
                    factory = LexoraViewModelFactory { AuditViewModel(organization.id, container.auditOperations) },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                AuditScreen(state, vm::setQuery)
            }
        }
        composable(Routes.Users) {
            PermissionGuard(canManageUsers, "Недостаточно прав для управления пользователями") {
                val vm: UsersViewModel = viewModel(
                    key = "users:${organization.id}:${user.id}",
                    factory = LexoraViewModelFactory { UsersViewModel(organization.id, user.id, container.workspace) },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                UsersScreen(
                    organization = organization, currentUser = user, users = state.users, canManageUsers = canManageUsers,
                    onCreateUser = vm::create, onSetRole = vm::setRole, onSetUserActive = vm::setActive,
                )
            }
        }
        composable(Routes.Settings) {
            val vm: SettingsViewModel = viewModel(
                key = "settings:${organization.id}:${user.id}",
                factory = LexoraViewModelFactory { SettingsViewModel(organization.id, user.id, container.settingsOperations) },
            )
            SettingsScreen(organization = organization, user = user, viewModel = vm, appVersion = "0.28.0", onModuleEnabledChange = { _, _ -> workspaceViewModel.refreshCurrent() })
        }
        composable(Routes.Wash) {
            PermissionGuard(globalOwner || (container.accessPolicy.can(user, Permission.VIEW_WASH) && accessibleModules.any { it.id == LexoraModuleId.WASH }), "Модуль автомойки недоступен по правам или лицензии") {
                val vm: WashViewModel = viewModel(
                    key = "wash:${organization.id}",
                    factory = LexoraViewModelFactory { WashViewModel(organization.id, container.washOperations) },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                WashScreen(state, vm::addPost, vm::togglePost, vm::addQueueItem, vm::advanceQueue, vm::addTechCard, vm::addChemicalUsage)
            }
        }
        composable(Routes.Tires) {
            PermissionGuard(globalOwner || (container.accessPolicy.can(user, Permission.VIEW_TIRES) && accessibleModules.any { it.id == LexoraModuleId.TIRES }), "Модуль шиномонтажа недоступен по правам или лицензии") {
                val vm: TiresViewModel = viewModel(
                    key = "tires:${organization.id}",
                    factory = LexoraViewModelFactory { TiresViewModel(organization.id, container.tireOperations) },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                TiresScreen(state, vm::addQueueItem, vm::advanceQueue, vm::addDiagnostic, vm::addWorkEntry, vm::addStorageItem, vm::issueStorage)
            }
        }
    }
}
