package com.lexora.service

import android.os.Bundle
import androidx.compose.foundation.layout.padding
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
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
        val canViewAudit = globalOwner || container.accessPolicy.can(user, Permission.VIEW_AUDIT)
        val canViewWash = globalOwner || (
            container.accessPolicy.can(user, Permission.VIEW_WASH) &&
                accessibleModules.any { it.id == LexoraModuleId.WASH }
            )
        val canViewTires = globalOwner || (
            container.accessPolicy.can(user, Permission.VIEW_TIRES) &&
                accessibleModules.any { it.id == LexoraModuleId.TIRES }
            )

        val menuDestinations = remember(
            user.id, globalOwner, canManageUsers, canViewAudit, canViewWash, canViewTires,
        ) {
            buildMenuDestinations(
                canManageUsers = canManageUsers,
                canViewAudit = canViewAudit,
                canViewWash = canViewWash,
                canViewTires = canViewTires,
            )
        }
        var selectedBottomRoutes by remember(context, user.id, menuDestinations) {
            mutableStateOf(
                Routes.normalizeBottomDestinations(
                    BottomNavigationPreferences.read(context, user.id),
                    menuDestinations.map { it.route },
                ),
            )
        }
        val selectedBottomDestinations = selectedBottomRoutes.mapNotNull { route ->
            menuDestinations.firstOrNull { it.route == route }
        }

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
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route ?: Routes.Home
            Scaffold(
                topBar = {
                    LexoraTopBar(
                        title = stringResource(titleResourceForRoute(currentRoute)),
                        canNavigateBack = navController.previousBackStackEntry != null,
                        onBack = { navController.navigateUp() },
                        onOpenRoute = { route -> navController.navigate(route) { launchSingleTop = true } },
                        onOpenSearch = if (currentRoute == Routes.Home) {
                            { navController.navigate(Routes.Search) { launchSingleTop = true } }
                        } else null,
                    )
                },
                bottomBar = {
                    if (Routes.usesBottomNavigation(currentRoute, selectedBottomRoutes)) {
                        LexoraBottomBar(
                            currentRoute = currentRoute,
                            selectedDestinations = selectedBottomDestinations,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Routes.Home) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
            ) { innerPadding ->
                LexoraNavHost(
                    navController = navController,
                    container = container,
                    workspaceViewModel = workspaceViewModel,
                    snapshot = snapshot,
                    modules = modules,
                    accessibleModules = accessibleModules,
                    menuDestinations = menuDestinations,
                    selectedBottomRoutes = selectedBottomRoutes,
                    onSelectedBottomRoutesChange = { routes ->
                        val normalized = Routes.normalizeBottomDestinations(
                            routes,
                            menuDestinations.map { it.route },
                        )
                        selectedBottomRoutes = normalized
                        BottomNavigationPreferences.write(context, user.id, normalized)
                    },
                    globalOwner = globalOwner,
                    canManageUsers = canManageUsers,
                    canManageOrganization = canManageOrganization,
                    canViewAudit = canViewAudit,
                    modifier = Modifier.padding(innerPadding),
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
    menuDestinations: List<AppMenuDestination>,
    selectedBottomRoutes: List<String>,
    onSelectedBottomRoutesChange: (List<String>) -> Unit,
    globalOwner: Boolean,
    canManageUsers: Boolean,
    canManageOrganization: Boolean,
    canViewAudit: Boolean,
    modifier: Modifier = Modifier,
) {
    val organization = snapshot.organization
    val user = snapshot.user
    NavHost(navController = navController, startDestination = Routes.Home, modifier = modifier) {
        composable(Routes.Home) {
            val vm: HomeViewModel = viewModel(
                key = "home:${organization.id}",
                factory = LexoraViewModelFactory { HomeViewModel(organization.id, container.loadDashboard) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                onRefresh = vm::refresh,
                onOpenPlanning = { navController.navigate(Routes.Planning) },
            )
        }
        composable(Routes.Menu) {
            LexoraMenuScreen(
                destinations = menuDestinations.filterNot { it.route == Routes.Search },
                selectedRoutes = selectedBottomRoutes,
                onSelectedRoutesChange = onSelectedBottomRoutesChange,
                onOpenProfileSettings = { navController.navigate(Routes.ProfileSettings) },
                onOpenDestination = { route -> navController.navigate(route) { launchSingleTop = true } },
            )
        }
        composable(Routes.ProfileSettings) {
            ProfileSettingsScreen(
                user = user,
                activeOrganization = organization,
                onManageOrganizations = { navController.navigate(Routes.Organization) },
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
            PermissionGuard(canViewAudit, "Недостаточно прав для просмотра журнала аудита") {
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

@StringRes
private fun buildMenuDestinations(
    canManageUsers: Boolean,
    canViewAudit: Boolean,
    canViewWash: Boolean,
    canViewTires: Boolean,
): List<AppMenuDestination> = buildList {
    add(AppMenuDestination(Routes.Planning, R.string.nav_title_planning))
    add(AppMenuDestination(Routes.Requests, R.string.nav_title_requests))
    add(AppMenuDestination(Routes.Clients, R.string.nav_title_clients))
    add(AppMenuDestination(Routes.Vehicles, R.string.nav_title_vehicles))
    add(AppMenuDestination(Routes.Assets, R.string.nav_title_assets))
    add(AppMenuDestination(Routes.FieldWork, R.string.nav_title_field_work))
    add(AppMenuDestination(Routes.Documents, R.string.nav_title_documents))
    add(AppMenuDestination(Routes.Catalog, R.string.nav_title_catalog))
    add(AppMenuDestination(Routes.Reports, R.string.nav_title_reports))
    add(AppMenuDestination(Routes.Notifications, R.string.nav_title_notifications))
    add(AppMenuDestination(Routes.Organization, R.string.nav_title_organization))
    if (canViewAudit) add(AppMenuDestination(Routes.Audit, R.string.nav_title_audit))
    if (canManageUsers) add(AppMenuDestination(Routes.Users, R.string.nav_title_users))
    add(AppMenuDestination(Routes.Settings, R.string.nav_title_settings))
    if (canViewWash) add(AppMenuDestination(Routes.Wash, R.string.nav_title_wash))
    if (canViewTires) add(AppMenuDestination(Routes.Tires, R.string.nav_title_tires))
}

private fun titleResourceForRoute(route: String?): Int = when (route) {
    Routes.Home -> R.string.nav_title_home
    Routes.Menu -> R.string.nav_title_menu
    Routes.ProfileSettings -> R.string.nav_title_profile_settings
    Routes.Search -> R.string.nav_title_search
    Routes.Planning -> R.string.nav_title_planning
    Routes.Clients -> R.string.nav_title_clients
    Routes.Vehicles -> R.string.nav_title_vehicles
    Routes.Assets -> R.string.nav_title_assets
    Routes.Requests -> R.string.nav_title_requests
    Routes.Organization -> R.string.nav_title_organization
    Routes.FieldWork -> R.string.nav_title_field_work
    Routes.Documents -> R.string.nav_title_documents
    Routes.Reports -> R.string.nav_title_reports
    Routes.Catalog -> R.string.nav_title_catalog
    Routes.Notifications -> R.string.nav_title_notifications
    Routes.Audit -> R.string.nav_title_audit
    Routes.Users -> R.string.nav_title_users
    Routes.Settings -> R.string.nav_title_settings
    Routes.Wash -> R.string.nav_title_wash
    Routes.Tires -> R.string.nav_title_tires
    else -> R.string.nav_title_fallback
}
