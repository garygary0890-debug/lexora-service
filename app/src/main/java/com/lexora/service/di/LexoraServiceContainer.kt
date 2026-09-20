package com.lexora.service.di

import android.content.Context
import com.lexora.service.LexoraBackendGraph
import com.lexora.service.core.data.*
import com.lexora.service.core.database.BusinessOperationsDatabase
import com.lexora.service.core.database.InventoryDatabase
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.NotificationRuntimeDatabase
import com.lexora.service.core.di.DependencyRegistry
import com.lexora.service.core.domain.*
import com.lexora.service.core.model.IntegrationDescriptor

class LexoraServiceContainer(context: Context) {
    private val appContext = context.applicationContext
    private val registry = DependencyRegistry()

    init {
        registry.registerSingleton(LexoraServiceDatabase::class) { LexoraServiceDatabase.create(appContext) }
        registry.registerSingleton(InventoryDatabase::class) { InventoryDatabase.create(appContext) }
        registry.registerSingleton(BusinessOperationsDatabase::class) { BusinessOperationsDatabase.create(appContext) }
        registry.registerSingleton(NotificationRuntimeDatabase::class) { NotificationRuntimeDatabase.create(appContext) }
        registry.registerSingleton(LexoraBackendGraph::class) { LexoraBackendGraph(appContext, database) }
        registry.registerSingleton(ServiceOperations::class) { PersistentServiceOperations(database.serviceDao(), backend.syncQueue) }
        registry.registerSingleton(ReportHubOperations::class) { PersistentReportHubOperations(operations) }
        registry.registerSingleton(VisitExecutionRepository::class) { VisitExecutionRepository(database.serviceDao()) }
        registry.registerSingleton(InventoryRepository::class) { PersistentInventoryRepository(inventoryDatabase.inventoryDao()) }
        registry.registerSingleton(BusinessOperationsRepository::class) {
            val persistent = PersistentBusinessOperationsRepository(businessOperationsDatabase.businessOperationsDao(), inventoryRepository)
            ValidatedBusinessOperationsRepository(persistent, database.serviceDao(), database.contractDao())
        }
        registry.registerSingleton(AccessPolicy::class) { AccessPolicy() }
        registry.registerSingleton(ModuleAccessPolicy::class) { ModuleAccessPolicy(accessPolicy) }
        registry.registerSingleton(PersistentOrganizationRepository::class) { PersistentOrganizationRepository(database.serviceDao()) }
        registry.registerSingleton(PersistentUserRepository::class) { PersistentUserRepository(database.userDao(), database.serviceDao()) }
        registry.registerSingleton(OrganizationSessionRepository::class) { OrganizationSessionRepository(organizationRepository, userRepository, accessPolicy) }
        registry.registerSingleton(ModuleLicenseRepository::class) { ModuleLicenseRepository(database.serviceDao()) }
        registry.registerSingleton(AuditOperations::class) { AuditRepository(database.serviceDao()) }
        registry.registerSingleton(NotificationOperations::class) { NotificationRepository(database.notificationDao(), database.serviceDao(), database.workOrderDao()) }
        registry.registerSingleton(NotificationDeliveryOperations::class) { PersistentNotificationDeliveryRepository(notificationRuntimeDatabase.notificationDeliveryDao()) }
        registry.registerSingleton(WashOperations::class) { WashRepository.create(appContext) }
        registry.registerSingleton(TireOperations::class) { TireRepository.create(appContext) }
        registry.registerSingleton(ServiceHistoryOperations::class) { ServiceHistoryRepository(database.serviceDao()) }
        registry.registerSingleton(SettingsOperations::class) { PersistentSettingsOperations(moduleLicenseRepository, ReferenceDataRepository(database.referenceDataDao(), database.serviceDao())) }
        registry.registerSingleton(CustomerCareOperations::class) { PersistentCustomerCareOperations(CustomerCareRepository(database.customerCareDao(), database.serviceDao())) }
        registry.registerSingleton(DocumentFeatureOperations::class) { PersistentDocumentFeatureOperations(appContext, database) }
        registry.registerSingleton(CatalogOperations::class) { PersistentCatalogOperations(database) }
        registry.registerSingleton(WorkspaceOperations::class) { PersistentWorkspaceOperations(organizationSessionRepository, userRepository, moduleLicenseRepository) }
        registry.registerSingleton(BootstrapWorkspaceUseCase::class) { BootstrapWorkspaceUseCase(workspace) }
        registry.registerSingleton(SwitchWorkspaceUseCase::class) { SwitchWorkspaceUseCase(workspace) }
        registry.registerSingleton(LoadOperationalDashboardUseCase::class) { LoadOperationalDashboardUseCase(operations) }
        registry.registerSingleton(GlobalSearchUseCase::class) { GlobalSearchUseCase(operations) }
        registry.registerSingleton(LoadPlanningUseCase::class) { LoadPlanningUseCase(operations) }
        FieldWorkRuntimeDependencies.install(visitExecutionRepository, inventoryRepository)
        BusinessOperationsRuntimeDependencies.install(businessOperations)
        NotificationDeliveryRuntimeDependencies.install(notificationDeliveryOperations) { delivery ->
            com.lexora.service.notifications.NotificationScheduler.schedule(appContext, delivery)
        }
    }

    val database: LexoraServiceDatabase get() = registry.get()
    val inventoryDatabase: InventoryDatabase get() = registry.get()
    val businessOperationsDatabase: BusinessOperationsDatabase get() = registry.get()
    val notificationRuntimeDatabase: NotificationRuntimeDatabase get() = registry.get()
    val backend: LexoraBackendGraph get() = registry.get()
    val operations: ServiceOperations get() = registry.get()
    val reportHubOperations: ReportHubOperations get() = registry.get()
    val visitExecutionRepository: VisitExecutionRepository get() = registry.get()
    val inventoryRepository: InventoryRepository get() = registry.get()
    val businessOperations: BusinessOperationsRepository get() = registry.get()
    val workspace: WorkspaceOperations get() = registry.get()
    val auditOperations: AuditOperations get() = registry.get()
    val notificationOperations: NotificationOperations get() = registry.get()
    val notificationDeliveryOperations: NotificationDeliveryOperations get() = registry.get()
    val washOperations: WashOperations get() = registry.get()
    val tireOperations: TireOperations get() = registry.get()
    val serviceHistoryOperations: ServiceHistoryOperations get() = registry.get()
    val settingsOperations: SettingsOperations get() = registry.get()
    val customerCareOperations: CustomerCareOperations get() = registry.get()
    val documentFeatureOperations: DocumentFeatureOperations get() = registry.get()
    val catalogOperations: CatalogOperations get() = registry.get()
    val accessPolicy: AccessPolicy get() = registry.get()
    val moduleAccessPolicy: ModuleAccessPolicy get() = registry.get()
    val organizationRepository: PersistentOrganizationRepository get() = registry.get()
    val userRepository: PersistentUserRepository get() = registry.get()
    val organizationSessionRepository: OrganizationSessionRepository get() = registry.get()
    val moduleLicenseRepository: ModuleLicenseRepository get() = registry.get()
    val bootstrapWorkspace: BootstrapWorkspaceUseCase get() = registry.get()
    val switchWorkspace: SwitchWorkspaceUseCase get() = registry.get()
    val loadDashboard: LoadOperationalDashboardUseCase get() = registry.get()
    val globalSearch: GlobalSearchUseCase get() = registry.get()
    val loadPlanning: LoadPlanningUseCase get() = registry.get()
    val integrations: List<IntegrationDescriptor> by lazy { defaultIntegrationRegistry() }
}
