package com.lexora.service.core.data

import android.content.Context
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.PriceListEntity
import com.lexora.service.core.database.PriceListItemEntity
import com.lexora.service.core.database.ServiceCatalogItemEntity
import com.lexora.service.core.model.PriceList
import com.lexora.service.core.model.PriceListItem
import com.lexora.service.core.model.ServiceCatalogItem
import com.lexora.service.core.model.SyncState
import java.util.UUID

class CatalogRepository private constructor(private val database: LexoraServiceDatabase) {
    private val dao = database.catalogDao()
    private val serviceDao = database.serviceDao()

    suspend fun services(organizationId: String): List<ServiceCatalogItem> = dao.services(organizationId).map { it.toModel() }
    suspend fun priceLists(organizationId: String): List<PriceList> = dao.priceLists(organizationId).map { it.toModel() }
    suspend fun priceItems(priceListId: String): List<PriceListItem> = dao.priceListItems(priceListId).map { it.toModel() }

    suspend fun addService(organizationId: String, userId: String) {
        val now = System.currentTimeMillis()
        val current = dao.services(organizationId)
        val number = current.size + 1
        val id = UUID.randomUUID().toString()
        val code = "SRV-%04d".format(number)
        dao.upsertService(ServiceCatalogItemEntity(id, organizationId, code, "Новая услуга $number", "Общие услуги", "шт.", 30, true, SyncState.PENDING_CREATE.name, now))
        audit(organizationId, userId, "SERVICE_CATALOG_ITEM", id, "CREATE", "$code · Новая услуга $number", now)
    }

    suspend fun toggleService(item: ServiceCatalogItem, userId: String) {
        val now = System.currentTimeMillis()
        dao.setServiceActive(item.id, !item.active, SyncState.PENDING_UPDATE.name, now)
        audit(item.organizationId, userId, "SERVICE_CATALOG_ITEM", item.id, "ACTIVE_CHANGE", "${item.code}: ${item.active} → ${!item.active}", now)
    }

    suspend fun addPriceList(organizationId: String, userId: String) {
        val now = System.currentTimeMillis()
        val number = dao.priceLists(organizationId).size + 1
        val id = UUID.randomUUID().toString()
        dao.upsertPriceList(PriceListEntity(id, organizationId, "Прайс-лист $number", "RUB", now, null, true, SyncState.PENDING_CREATE.name, now))
        audit(organizationId, userId, "PRICE_LIST", id, "CREATE", "Прайс-лист $number", now)
    }

    suspend fun addPriceItem(priceList: PriceList, service: ServiceCatalogItem, userId: String) {
        val now = System.currentTimeMillis()
        val existing = dao.priceListItems(priceList.id).firstOrNull { it.serviceCatalogItemId == service.id }
        val id = existing?.id ?: UUID.randomUUID().toString()
        val price = existing?.priceMinor ?: 100_000L
        dao.upsertPriceListItem(PriceListItemEntity(id, priceList.id, service.id, price, if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, now))
        audit(priceList.organizationId, userId, "PRICE_LIST_ITEM", id, if (existing == null) "CREATE" else "UPDATE", "${service.code}: $price ${priceList.currency} minor", now)
    }

    private suspend fun audit(organizationId: String, userId: String, entityType: String, entityId: String, action: String, summary: String, now: Long) {
        serviceDao.insertAuditEvent(AuditEventEntity(UUID.randomUUID().toString(), organizationId, userId, entityType, entityId, action, summary, now))
    }

    companion object {
        fun create(context: Context) = CatalogRepository(LexoraServiceDatabase.create(context.applicationContext))
        fun create(database: LexoraServiceDatabase) = CatalogRepository(database)
    }
}

private fun ServiceCatalogItemEntity.toModel() = ServiceCatalogItem(id, organizationId, code, name, category, unit, durationMinutes, active, SyncState.valueOf(syncState))
private fun PriceListEntity.toModel() = PriceList(id, organizationId, name, currency, effectiveFromEpochMs, effectiveToEpochMs, active, SyncState.valueOf(syncState))
private fun PriceListItemEntity.toModel() = PriceListItem(id, priceListId, serviceCatalogItemId, priceMinor, SyncState.valueOf(syncState))
