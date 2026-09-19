package com.lexora.service.core.domain

import com.lexora.service.core.model.*

interface AuditOperations {
    suspend fun records(organizationId: String, filter: AuditFilter = AuditFilter(), limit: Int = 250): List<AuditRecord>
}

interface NotificationOperations {
    suspend fun notifications(organizationId: String): List<ServiceNotification>
    suspend fun archivedNotifications(organizationId: String): List<ServiceNotification>
    suspend fun refreshGenerated(organizationId: String, now: Long = System.currentTimeMillis())
    suspend fun markRead(id: String, read: Boolean)
    suspend fun markAllRead(organizationId: String)
    suspend fun setArchived(id: String, archived: Boolean)
}

interface WashOperations {
    suspend fun posts(organizationId: String): List<WashPost>
    suspend fun queue(organizationId: String): List<WashQueueItem>
    suspend fun techCards(organizationId: String): List<WashTechCard>
    suspend fun chemicalUsage(organizationId: String): List<WashChemicalUsage>
    suspend fun addPost(organizationId: String, branchId: String?, name: String)
    suspend fun togglePostStatus(post: WashPost)
    suspend fun addQueueItem(organizationId: String)
    suspend fun advanceQueueItem(organizationId: String, item: WashQueueItem)
    suspend fun addTechCard(organizationId: String)
    suspend fun addChemicalUsage(organizationId: String)
}

interface TireOperations {
    suspend fun queue(organizationId: String): List<TireQueueItem>
    suspend fun diagnostics(organizationId: String): List<TireDiagnostic>
    suspend fun workEntries(organizationId: String): List<TireWorkEntry>
    suspend fun storage(organizationId: String): List<TireStorageItem>
    suspend fun addQueueItem(organizationId: String)
    suspend fun advanceQueueItem(item: TireQueueItem)
    suspend fun addDiagnostic(organizationId: String)
    suspend fun addWorkEntry(organizationId: String)
    suspend fun addStorageItem(organizationId: String)
    suspend fun issueStorageItem(item: TireStorageItem)
}

interface ServiceHistoryOperations {
    suspend fun history(vehicleId: String): List<ServiceHistoryRecord>
}

data class SettingsSnapshot(
    val modules: List<ModuleDescriptor>,
    val storeItems: List<ModuleStoreItem>,
    val directories: List<ReferenceDirectory>,
    val selectedDirectoryId: String?,
    val directoryItems: List<ReferenceDirectoryItem>,
)

interface SettingsOperations {
    suspend fun snapshot(organizationId: String, selectedDirectoryId: String? = null): SettingsSnapshot
    suspend fun createDirectory(organizationId: String, userId: String, code: String, name: String): String
    suspend fun createDirectoryItem(organizationId: String, userId: String, directoryId: String, code: String, name: String)
    suspend fun setDirectoryActive(organizationId: String, userId: String, id: String, active: Boolean)
    suspend fun setDirectoryItemActive(organizationId: String, userId: String, id: String, active: Boolean)
    suspend fun setModuleEnabled(organizationId: String, moduleId: LexoraModuleId, enabled: Boolean)
    suspend fun setModuleLicenseStatus(organizationId: String, moduleId: LexoraModuleId, status: ModuleLicenseStatus)
}

data class CustomerCareSnapshot(
    val accounts: List<LoyaltyAccount>,
    val transactions: List<LoyaltyTransaction>,
    val qualityRecords: List<QualityControlRecord>,
    val clientNames: Map<String, String>,
)

interface CustomerCareOperations {
    suspend fun snapshot(organizationId: String): CustomerCareSnapshot
    suspend fun accrue(organizationId: String, clientId: String, points: Long, comment: String? = null): Boolean
    suspend fun redeem(organizationId: String, clientId: String, points: Long, comment: String? = null): Boolean
    suspend fun createQualityCheck(organizationId: String, requestId: String): QualityControlRecord
    suspend fun completeQualityCheck(record: QualityControlRecord, rating: Int, checklistResult: String, issueDescription: String? = null)
    suspend fun resolveQualityIssue(record: QualityControlRecord, resolutionNote: String)
}

data class DocumentFeatureSnapshot(
    val workOrderItems: List<WorkOrderItem>,
    val contracts: List<ServiceContract>,
    val archivedContracts: List<ServiceContract>,
)

interface DocumentFeatureOperations {
    suspend fun snapshot(organizationId: String, documentId: String?): DocumentFeatureSnapshot
    suspend fun addWorkOrderCatalogItem(organizationId: String, userId: String, documentId: String, additional: Boolean)
    suspend fun resolveAdditionalWork(organizationId: String, userId: String, itemId: String, approve: Boolean, comment: String? = null)
    suspend fun createContract(organizationId: String, userId: String, clientId: String, branchId: String?, subject: String, note: String? = null)
    suspend fun changeContractStatus(organizationId: String, userId: String, contractId: String, status: ContractStatus)
    suspend fun archiveContract(organizationId: String, userId: String, contractId: String, restore: Boolean)
    suspend fun importAttachment(organizationId: String, userId: String, documentId: String, sourceUri: String): Result<String>
    suspend fun exportDocument(organizationId: String, userId: String, documentId: String, destinationUri: String): Result<String>
}

data class CatalogSnapshot(
    val services: List<ServiceCatalogItem>,
    val priceLists: List<PriceList>,
    val selectedPriceListId: String?,
    val priceItems: List<PriceListItem>,
    val recipeSummaries: List<ServiceRecipeSummary>,
)

interface CatalogOperations {
    suspend fun snapshot(organizationId: String, userId: String, selectedPriceListId: String? = null): CatalogSnapshot
    suspend fun addService(organizationId: String, userId: String)
    suspend fun toggleService(item: ServiceCatalogItem, userId: String)
    suspend fun addComponent(organizationId: String, userId: String, recipeId: String, type: ServiceComponentType, name: String, quantity: Double, unit: String, unitCostMinor: Long?)
    suspend fun setComponentActive(organizationId: String, userId: String, componentId: String, active: Boolean)
    suspend fun addPriceList(organizationId: String, userId: String)
    suspend fun addPriceItem(priceList: PriceList, service: ServiceCatalogItem, userId: String)
}
