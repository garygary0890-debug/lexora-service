package com.lexora.service.core.data

import android.content.Context
import android.net.Uri
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.domain.CatalogOperations
import com.lexora.service.core.domain.CatalogSnapshot
import com.lexora.service.core.domain.CustomerCareOperations
import com.lexora.service.core.domain.CustomerCareSnapshot
import com.lexora.service.core.domain.DocumentFeatureOperations
import com.lexora.service.core.domain.DocumentFeatureSnapshot
import com.lexora.service.core.domain.SettingsOperations
import com.lexora.service.core.domain.SettingsSnapshot
import com.lexora.service.core.model.*

class PersistentSettingsOperations(
    private val moduleRepository: ModuleLicenseRepository,
    private val referenceRepository: ReferenceDataRepository,
) : SettingsOperations {
    override suspend fun snapshot(organizationId: String, selectedDirectoryId: String?): SettingsSnapshot {
        val directories = referenceRepository.directories(organizationId)
        val selected = selectedDirectoryId?.takeIf { id -> directories.any { it.id == id } }
            ?: directories.firstOrNull()?.id
        return SettingsSnapshot(
            modules = moduleRepository.descriptors(organizationId),
            storeItems = moduleRepository.storeCatalog(),
            directories = directories,
            selectedDirectoryId = selected,
            directoryItems = selected?.let { referenceRepository.items(it) }.orEmpty(),
        )
    }
    override suspend fun createDirectory(organizationId: String, userId: String, code: String, name: String): String =
        referenceRepository.createDirectory(organizationId, userId, code, name).id

    override suspend fun createDirectoryItem(organizationId: String, userId: String, directoryId: String, code: String, name: String) {
        referenceRepository.createItem(organizationId, userId, directoryId, code, name)
    }

    override suspend fun setDirectoryActive(organizationId: String, userId: String, id: String, active: Boolean) =
        referenceRepository.setDirectoryActive(organizationId, userId, id, active)

    override suspend fun setDirectoryItemActive(organizationId: String, userId: String, id: String, active: Boolean) =
        referenceRepository.setItemActive(organizationId, userId, id, active)

    override suspend fun setModuleEnabled(organizationId: String, moduleId: LexoraModuleId, enabled: Boolean) =
        moduleRepository.setEnabled(organizationId, moduleId, enabled)

    override suspend fun setModuleLicenseStatus(organizationId: String, moduleId: LexoraModuleId, status: ModuleLicenseStatus) =
        moduleRepository.applyLicenseStatus(organizationId, moduleId, status)
}

class PersistentCustomerCareOperations(
    private val repository: CustomerCareRepository,
) : CustomerCareOperations {
    override suspend fun snapshot(organizationId: String) = CustomerCareSnapshot(
        repository.accounts(organizationId),
        repository.transactions(organizationId),
        repository.qualityRecords(organizationId),
        repository.clientNames(organizationId),
    )
    override suspend fun accrue(organizationId: String, clientId: String, points: Long, comment: String?) =
        repository.accrue(organizationId, clientId, points, comment = comment)

    override suspend fun redeem(organizationId: String, clientId: String, points: Long, comment: String?) =
        repository.redeem(organizationId, clientId, points, comment = comment)

    override suspend fun createQualityCheck(organizationId: String, requestId: String) =
        repository.createQualityCheck(organizationId, requestId)

    override suspend fun completeQualityCheck(record: QualityControlRecord, rating: Int, checklistResult: String, issueDescription: String?) =
        repository.completeQualityCheck(record, rating, checklistResult, issueDescription)

    override suspend fun resolveQualityIssue(record: QualityControlRecord, resolutionNote: String) =
        repository.resolveQualityIssue(record, resolutionNote)
}

class PersistentDocumentFeatureOperations(
    context: Context,
    database: LexoraServiceDatabase,
) : DocumentFeatureOperations {
    private val workOrders = WorkOrderRepository.create(context)
    private val contracts = ContractRepository(database.contractDao(), database.serviceDao())
    private val transfer = DocumentTransferRepository(context.applicationContext, database.serviceDao())

    override suspend fun snapshot(organizationId: String, documentId: String?) = DocumentFeatureSnapshot(
        workOrderItems = documentId?.let { workOrders.items(it) }.orEmpty(),
        contracts = contracts.contracts(organizationId),
        archivedContracts = contracts.archivedContracts(organizationId),
    )
    override suspend fun addWorkOrderCatalogItem(organizationId: String, userId: String, documentId: String, additional: Boolean) =
        workOrders.addCatalogItem(organizationId, userId, documentId, additional)

    override suspend fun resolveAdditionalWork(organizationId: String, userId: String, itemId: String, approve: Boolean, comment: String?) =
        workOrders.resolveAdditionalWork(organizationId, userId, itemId, approve, comment)

    override suspend fun createContract(organizationId: String, userId: String, clientId: String, branchId: String?, subject: String, note: String?) {
        contracts.create(organizationId, userId, clientId, branchId, subject, note = note)
    }

    override suspend fun changeContractStatus(organizationId: String, userId: String, contractId: String, status: ContractStatus) =
        contracts.changeStatus(organizationId, userId, contractId, status)

    override suspend fun archiveContract(organizationId: String, userId: String, contractId: String, restore: Boolean) {
        if (restore) contracts.restore(organizationId, userId, contractId) else contracts.archive(organizationId, userId, contractId)
    }

    override suspend fun importAttachment(organizationId: String, userId: String, documentId: String, sourceUri: String) =
        transfer.importAttachment(organizationId, userId, documentId, Uri.parse(sourceUri))

    override suspend fun exportDocument(organizationId: String, userId: String, documentId: String, destinationUri: String) =
        transfer.exportDocument(organizationId, userId, documentId, Uri.parse(destinationUri))
}

class PersistentCatalogOperations(
    private val database: LexoraServiceDatabase,
) : CatalogOperations {
    private val catalog = CatalogRepository.create(database)
    private val constructor = ServiceConstructorRepository(database.serviceConstructorDao(), database.serviceDao())
    override suspend fun snapshot(organizationId: String, userId: String, selectedPriceListId: String?): CatalogSnapshot {
        val services = catalog.services(organizationId)
        services.filter { it.active }.forEach { service ->
            constructor.ensureRecipe(organizationId, userId, service.id, service.name, service.durationMinutes)
        }
        val priceLists = catalog.priceLists(organizationId)
        val selected = selectedPriceListId?.takeIf { id -> priceLists.any { it.id == id } }
            ?: priceLists.firstOrNull()?.id
        return CatalogSnapshot(
            services = services,
            priceLists = priceLists,
            selectedPriceListId = selected,
            priceItems = selected?.let { catalog.priceItems(it) }.orEmpty(),
            recipeSummaries = constructor.recipes(organizationId),
        )
    }

    override suspend fun addService(organizationId: String, userId: String) = catalog.addService(organizationId, userId)
    override suspend fun toggleService(item: ServiceCatalogItem, userId: String) = catalog.toggleService(item, userId)
    override suspend fun addComponent(organizationId: String, userId: String, recipeId: String, type: ServiceComponentType, name: String, quantity: Double, unit: String, unitCostMinor: Long?) {
        constructor.addComponent(organizationId, userId, recipeId, type, name, quantity, unit, unitCostMinor ?: 0L)
    }
    override suspend fun setComponentActive(organizationId: String, userId: String, componentId: String, active: Boolean) =
        constructor.setComponentActive(organizationId, userId, componentId, active)
    override suspend fun addPriceList(organizationId: String, userId: String) = catalog.addPriceList(organizationId, userId)
    override suspend fun addPriceItem(priceList: PriceList, service: ServiceCatalogItem, userId: String) = catalog.addPriceItem(priceList, service, userId)
}
